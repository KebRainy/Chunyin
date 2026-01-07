from __future__ import annotations

import base64
import os
import re
import unicodedata
import math
from dataclasses import dataclass
from typing import Any, Dict, List, Literal, Optional
from pathlib import Path
from functools import lru_cache

import cv2
import numpy as np
from fastapi import FastAPI, HTTPException
from pydantic import AliasChoices, BaseModel, Field

try:
    from opencc import OpenCC  # type: ignore
except Exception:
    OpenCC = None

try:
    import onnxruntime as ort  # type: ignore
except Exception:
    ort = None

try:
    from tokenizers import Tokenizer  # type: ignore
except Exception:
    Tokenizer = None

Action = Literal["ALLOW", "REVIEW", "BLOCK"]
Category = Literal["AD", "SEXUAL"]


def _env_int(name: str, default: int) -> int:
    try:
        return int(os.getenv(name, str(default)))
    except Exception:
        return default


def _env_float(name: str, default: float) -> float:
    try:
        return float(os.getenv(name, str(default)))
    except Exception:
        return default


MODERATION_MAX_IMAGE_BYTES = _env_int("MODERATION_MAX_IMAGE_BYTES", 6_000_000)
NUDITY_STRICT = _env_float("MODERATION_NUDITY_STRICT", 0.75)
AD_STRICT = _env_float("MODERATION_AD_STRICT", 0.60)
SEXUAL_REVIEW = _env_float("MODERATION_SEXUAL_REVIEW", 0.55)

OPEN_NSFW_DIR = Path(os.getenv("MODERATION_OPEN_NSFW_DIR", str(Path(__file__).resolve().parent / "models" / "open_nsfw")))
OPEN_NSFW_PROTO = OPEN_NSFW_DIR / "deploy.prototxt"
OPEN_NSFW_MODEL = OPEN_NSFW_DIR / "resnet_50_1by2_nsfw.caffemodel"

_OPEN_NSFW_NET = None
if OPEN_NSFW_PROTO.exists() and OPEN_NSFW_MODEL.exists():
    try:
        _OPEN_NSFW_NET = cv2.dnn.readNetFromCaffe(str(OPEN_NSFW_PROTO), str(OPEN_NSFW_MODEL))
    except Exception:
        _OPEN_NSFW_NET = None


class TextRequest(BaseModel):
    text: str = Field(min_length=1, max_length=10_000, validation_alias=AliasChoices("text", "content"))
    scene: Optional[str] = None


class ImageRequest(BaseModel):
    image_base64: str = Field(min_length=1, validation_alias=AliasChoices("image_base64", "imageBase64"))
    mime_type: Optional[str] = Field(default=None, validation_alias=AliasChoices("mime_type", "mimeType"))
    scene: Optional[str] = None


class ModerationResponse(BaseModel):
    action: Action
    categories: List[Category] = Field(default_factory=list)
    scores: Dict[Category, float] = Field(default_factory=dict)
    reasons: List[str] = Field(default_factory=list)


_URL_RE = re.compile(r"(https?://\S+|www\.\S+)", re.IGNORECASE)
_CONTACT_RE = re.compile(
    r"(1[3-9]\d{9})|((微信|vx|v信|V信|wx)[\s:：_-]*[a-zA-Z0-9_-]{3,})|((QQ|扣扣)[\s:：_-]*\d{5,12})",
    re.IGNORECASE,
)
_PORN_TLD_RE = re.compile(r"\b[a-z0-9-]+\.(?:xxx|porn|sex)\b", re.IGNORECASE)
_SEX_EN_RE = re.compile(
    r"\b("
    r"tits?|boobs?|breasts?|nipples?|ass(?:es)?|butt|booty|"
    r"dildo(?:s)?|vibrator(?:s)?|pussy|vagina|penis|cock|dick|"
    r"cum|cumming|cumshot(?:s)?|orgasm|masturbat(?:e|ion)|"
    r"fuck(?:ing)?|f\*+k|slut(?:s)?|whore(?:s)?|"
    r"anal|blowjob|handjob|threesome|milf|bdsm|porn|xxx|nsfw|"
    r"nude|naked|sex(?:y)?|hardcore|uncensored|"
    r"livecam|webcam|onlyfans|"
    r"bbw(?:s)?|ebony|"
    r"hentai|tentacle(?:s)?|schoolgirl(?:s)?|"
    r"rape|raped|raping|rapist"
    r")\b",
    re.IGNORECASE,
)

_OPENCC = None
if OpenCC is not None:
    try:
        _OPENCC = OpenCC("t2s")
    except Exception:
        _OPENCC = None

_ZH_NUM = {
    "零": "0",
    "〇": "0",
    "○": "0",
    "一": "1",
    "壹": "1",
    "二": "2",
    "贰": "2",
    "两": "2",
    "三": "3",
    "叁": "3",
    "四": "4",
    "肆": "4",
    "五": "5",
    "伍": "5",
    "六": "6",
    "陆": "6",
    "七": "7",
    "柒": "7",
    "八": "8",
    "捌": "8",
    "九": "9",
    "玖": "9",
}


def _normalize_zh(text: str) -> str:
    text = unicodedata.normalize("NFKC", text)
    text = text.replace("\u200b", "").replace("\ufeff", "")
    if _OPENCC is not None:
        try:
            text = _OPENCC.convert(text)
        except Exception:
            pass
    text = "".join(_ZH_NUM.get(ch, ch) for ch in text)
    return text


def _norm_text(text: str) -> str:
    text = _normalize_zh(text).lower()
    text = re.sub(r"[\s\-\_\.\,\，\。\|\+·•~`!！?？:：;；'\"“”‘’（）()【】\\/\[\]{}<>《》]", "", text)
    return (
        text.replace("0", "o")
        .replace("1", "i")
        .replace("3", "e")
        .replace("4", "a")
        .replace("5", "s")
        .replace("@", "a")
        .replace("$", "s")
    )


_AD_KW = [
    "加微信",
    "加我微信",
    "加vx",
    "加v",
    "加微",
    "加v信",
    "加vx",
    "私聊",
    "代理",
    "招商",
    "加盟",
    "扫码",
    "二维码",
    "vx",
    "微信号",
    "微x",
    "微 信",
    "进群",
    "返利",
    "优惠券",
    "红包",
    "推广",
    "引流",
    "带你赚钱",
    "包赔",
    "包赢",
    "稳赚",
]
_SEX_KW = [
    "裸聊",
    "约炮",
    "色情网",
    "成人视频",
    "色情网",
    "色情网",
    "成人视频",
    "成人",
    "援交",
    "处男",
    "处女",
    "porn",
    "nude",
    "sex",
    "成人视频",
    "看片",
    "强奸",
    "无码视频",
    "福利姬",
    "无码视频",
]


TextLabel = Literal["OK", "AD", "SEXUAL"]


def _env_bool(name: str, default: bool) -> bool:
    val = os.getenv(name)
    if val is None:
        return default
    return val.strip().lower() not in {"0", "false", "off", "no"}


TEXT_MODEL_ENABLED = _env_bool("MODERATION_TEXT_MODEL_ENABLED", True)
TEXT_MODEL_MAX_CHARS = _env_int("MODERATION_TEXT_MODEL_MAX_CHARS", 2000)
TEXT_MODEL_TEMP = _env_float("MODERATION_TEXT_MODEL_TEMP", 1.35)
TEXT_MODEL_ALPHA = _env_float("MODERATION_TEXT_MODEL_ALPHA", 0.8)
TEXT_ONNX_ENABLED = _env_bool("MODERATION_TEXT_ONNX_ENABLED", True)
TEXT_ONNX_MAX_LEN = _env_int("MODERATION_TEXT_ONNX_MAX_LEN", 192)
TEXT_ONNX_MIN_ASCII_RATIO = _env_float("MODERATION_TEXT_ONNX_MIN_ASCII_RATIO", 0.25)
TEXT_ONNX_BASELINE = _env_float("MODERATION_TEXT_ONNX_BASELINE", 0.65)


def _prep_text_for_model(text: str) -> str:
    t = _normalize_zh(text).lower()
    t = _URL_RE.sub(" URL ", t)
    t = _CONTACT_RE.sub(" CONTACT ", t)
    t = re.sub(r"\d", "0", t)
    t = re.sub(r"[^0-9a-z\u4e00-\u9fff]+", " ", t)
    t = re.sub(r"\s+", "", t)
    return t[:TEXT_MODEL_MAX_CHARS]


def _char_ngrams(s: str, n_min: int, n_max: int) -> List[str]:
    if not s:
        return []
    grams: List[str] = []
    max_n = min(n_max, len(s))
    for n in range(n_min, max_n + 1):
        for i in range(0, len(s) - n + 1):
            grams.append(s[i : i + n])
    return grams


@dataclass
class _LocalNgramNB:
    labels: List[TextLabel]
    alpha: float = 0.8
    n_min: int = 2
    n_max: int = 4
    class_doc_counts: Dict[TextLabel, int] = None  # type: ignore[assignment]
    class_token_counts: Dict[TextLabel, Dict[str, int]] = None  # type: ignore[assignment]
    class_total_tokens: Dict[TextLabel, int] = None  # type: ignore[assignment]
    vocab_size: int = 0
    total_docs: int = 0

    def __post_init__(self) -> None:
        self.class_doc_counts = {lbl: 0 for lbl in self.labels}
        self.class_token_counts = {lbl: {} for lbl in self.labels}
        self.class_total_tokens = {lbl: 0 for lbl in self.labels}
        self.vocab_size = 0
        self.total_docs = 0

    def fit(self, samples: List[tuple[TextLabel, str]]) -> "_LocalNgramNB":
        vocab: set[str] = set()
        for label, text in samples:
            t = _prep_text_for_model(text)
            grams = set(_char_ngrams(t, self.n_min, self.n_max))
            self.total_docs += 1
            self.class_doc_counts[label] = self.class_doc_counts.get(label, 0) + 1
            token_counts = self.class_token_counts.setdefault(label, {})
            for g in grams:
                token_counts[g] = token_counts.get(g, 0) + 1
                self.class_total_tokens[label] = self.class_total_tokens.get(label, 0) + 1
                vocab.add(g)
        self.vocab_size = max(1, len(vocab))
        return self

    def predict_proba(self, text: str, *, temperature: float = 1.0) -> Dict[TextLabel, float]:
        t = _prep_text_for_model(text)
        grams = set(_char_ngrams(t, self.n_min, self.n_max))
        if not grams or self.total_docs <= 0:
            return {lbl: (1.0 / len(self.labels)) for lbl in self.labels}

        log_scores: Dict[TextLabel, float] = {}
        denom_prior = self.total_docs + len(self.labels)

        for lbl in self.labels:
            prior = math.log((self.class_doc_counts.get(lbl, 0) + 1) / denom_prior)
            total_tokens = self.class_total_tokens.get(lbl, 0)
            denom = total_tokens + self.alpha * self.vocab_size
            token_counts = self.class_token_counts.get(lbl, {})
            ll = 0.0
            for g in grams:
                ll += math.log((token_counts.get(g, 0) + self.alpha) / denom)
            ll /= max(1, len(grams))
            log_scores[lbl] = prior + ll

        temp = max(1e-6, float(temperature))
        m = max(log_scores.values())
        exp_scores: Dict[TextLabel, float] = {lbl: math.exp((log_scores[lbl] - m) / temp) for lbl in self.labels}
        z = sum(exp_scores.values())
        if z <= 0:
            return {lbl: (1.0 / len(self.labels)) for lbl in self.labels}
        return {lbl: (exp_scores[lbl] / z) for lbl in self.labels}


def _build_default_text_model() -> _LocalNgramNB:
    samples: List[tuple[TextLabel, str]] = []

    ok = [
        "你好，请问有推荐的饮品吗",
        "这个鸡尾酒怎么做？需要哪些配料",
        "帮我下单两杯拿铁，少糖",
        "今天营业到几点",
        "请给我一份菜单",
        "这杯饮料有点甜，可以换无糖吗",
        "我想了解一下配料表",
        "可以开发票吗",
        "谢谢，麻烦快点出餐",
        "我在店里等取餐",
        "这个酒不错",
    ]

    ad = [
        "加微信了解更多",
        "加vx私聊",
        "扫码进群领取优惠券",
        "代理加盟招商，稳赚不赔",
        "推广引流，进群领红包",
        "微 信 号 abc123",
        "VX:abc123 备注来意",
        "加我v信，带你赚钱",
        "返利优惠券，扫码领取",
        "想赚钱的来私聊",
        "加wx:abc_123",
        "扣扣123456789联系",
        "QQ:1234567 低价批发",
        "click the link for vip access",
        "free today limited offer join now",
        "promo code discount join group",
    ]

    sexual = [
        "裸聊",
        "约炮",
        "成人视频",
        "porn在线观看",
        "nude写真",
        "找福利姬",
        "看片资源分享",
        "强奸",
        "三级片",
        "色图私发",
        "援交",
        "无码资源",
        "成人网站",
        "Giant tits and phat asses shaking live",
        "Watch ebony BBWs oil up and ride huge dildos",
        "VIP access free today bbwlive.xxx",
        "onlyfans nude sexvideo porn xxx",
        "cum screaming huge cock blowjob anal",
        "uncensored hentai monster tentacles raping schoolgirls cumshots",
    ]

    ok_en = [
        "this drink is good",
        "can i have a latte no sugar",
        "what time do you close today",
        "menu please",
        "thank you",
        "the cocktail tastes great",
    ]

    samples.extend([("OK", t) for t in ok])
    samples.extend([("OK", t) for t in ok_en])
    samples.extend([("AD", t) for t in ad])
    samples.extend([("SEXUAL", t) for t in sexual])

    return _LocalNgramNB(labels=["OK", "AD", "SEXUAL"], alpha=TEXT_MODEL_ALPHA, n_min=2, n_max=4).fit(samples)


_TEXT_MODEL: Optional[_LocalNgramNB] = None
if TEXT_MODEL_ENABLED:
    try:
        _TEXT_MODEL = _build_default_text_model()
    except Exception:
        _TEXT_MODEL = None


@dataclass
class _OnnxTextNSFW:
    session: Any
    tokenizer: Any
    nsfw_index: int
    input_ids_name: str
    attention_mask_name: str
    token_type_ids_name: Optional[str] = None


def _ascii_ratio(s: str) -> float:
    if not s:
        return 0.0
    ascii_alnum = 0
    for ch in s:
        o = ord(ch)
        if (48 <= o <= 57) or (65 <= o <= 90) or (97 <= o <= 122):
            ascii_alnum += 1
    return ascii_alnum / max(1, len(s))


def _load_text_onnx() -> Optional[_OnnxTextNSFW]:
    if not TEXT_ONNX_ENABLED or ort is None or Tokenizer is None:
        return None

    model_dir = Path(
        os.getenv("MODERATION_TEXT_ONNX_DIR", str(Path(__file__).resolve().parent / "models" / "text_nsfw_onnx"))
    )
    onnx_path = model_dir / "onnx" / "decoder_model_merged_quantized.onnx"
    tokenizer_path = model_dir / "tokenizer.json"
    config_path = model_dir / "config.json"
    if not (onnx_path.exists() and tokenizer_path.exists() and config_path.exists()):
        return None

    try:
        import json

        cfg = json.loads(config_path.read_text(encoding="utf-8"))
        id2label = cfg.get("id2label") or {}
        nsfw_ids = []
        for k, v in id2label.items():
            try:
                idx = int(k)
            except Exception:
                continue
            label = str(v).lower()
            if "nsfw" in label or "porn" in label or "sexual" in label:
                nsfw_ids.append(idx)
        nsfw_index = max(nsfw_ids) if nsfw_ids else 1

        tokenizer = Tokenizer.from_file(str(tokenizer_path))
        tokenizer.enable_truncation(max_length=TEXT_ONNX_MAX_LEN)
        tokenizer.enable_padding(length=TEXT_ONNX_MAX_LEN)

        sess_opts = ort.SessionOptions()
        sess_opts.intra_op_num_threads = int(os.getenv("MODERATION_TEXT_ONNX_THREADS", "1"))
        providers = ["CPUExecutionProvider"]
        session = ort.InferenceSession(str(onnx_path), sess_options=sess_opts, providers=providers)

        input_list = session.get_inputs()
        input_names = [i.name for i in input_list]
        input_set = set(input_names)
        input_ids_name = next((n for n in input_names if "id" in n.lower()), input_names[0])
        attention_mask_name = next((n for n in input_names if "mask" in n.lower()), "")
        token_type_ids_name = next((n for n in input_names if "type" in n.lower()), None)
        if not attention_mask_name:
            attention_mask_name = "attention_mask" if "attention_mask" in input_set else ""
        return _OnnxTextNSFW(
            session=session,
            tokenizer=tokenizer,
            nsfw_index=nsfw_index,
            input_ids_name=input_ids_name,
            attention_mask_name=attention_mask_name,
            token_type_ids_name=token_type_ids_name,
        )
    except Exception:
        return None


_TEXT_ONNX: Optional[_OnnxTextNSFW] = _load_text_onnx()


def _softmax(xs: np.ndarray) -> np.ndarray:
    x = xs.astype(np.float64)
    x = x - np.max(x, axis=-1, keepdims=True)
    e = np.exp(x)
    return e / np.sum(e, axis=-1, keepdims=True)


@lru_cache(maxsize=2048)
def _onnx_nsfw_score_cached(text: str) -> float:
    if _TEXT_ONNX is None:
        return 0.0
    if _ascii_ratio(text) < TEXT_ONNX_MIN_ASCII_RATIO:
        return 0.0

    enc = _TEXT_ONNX.tokenizer.encode(text)
    input_ids = np.asarray([enc.ids], dtype=np.int64)
    attention_mask = np.asarray([enc.attention_mask], dtype=np.int64)
    feed: Dict[str, Any] = {
        _TEXT_ONNX.input_ids_name: input_ids,
    }
    if _TEXT_ONNX.attention_mask_name:
        feed[_TEXT_ONNX.attention_mask_name] = attention_mask
    if _TEXT_ONNX.token_type_ids_name is not None:
        feed[_TEXT_ONNX.token_type_ids_name] = np.asarray([getattr(enc, "type_ids", [0] * len(enc.ids))], dtype=np.int64)

    outs = _TEXT_ONNX.session.run(None, feed)
    logits = np.asarray(outs[0])
    probs = _softmax(logits)[0]
    idx = int(_TEXT_ONNX.nsfw_index)
    if idx < 0 or idx >= probs.shape[-1]:
        idx = int(np.argmax(probs))
    return float(max(0.0, min(1.0, probs[idx])))


def _calibrate_onnx_nsfw(raw: float) -> float:
    baseline = max(0.0, min(0.99, float(TEXT_ONNX_BASELINE)))
    if raw <= baseline:
        return 0.0
    return max(0.0, min(1.0, (raw - baseline) / (1.0 - baseline)))


def moderate_text_fast(text: str) -> ModerationResponse:
    normalized = _norm_text(text)
    raw_lower = _normalize_zh(text).lower()
    scores: Dict[Category, float] = {"AD": 0.0, "SEXUAL": 0.0}
    reasons: List[str] = []
    categories: List[Category] = []

    if _URL_RE.search(text) or _CONTACT_RE.search(text):
        scores["AD"] = max(scores["AD"], 0.95)
        reasons.append("疑似广告/引流：包含链接或联系方式")
    for kw in _AD_KW:
        if kw.lower().replace(" ", "") in normalized:
            scores["AD"] = max(scores["AD"], 0.70)
            reasons.append(f"疑似广告/引流：命中关键词 {kw}")
            break

    for kw in _SEX_KW:
        if kw.lower().replace(" ", "") in normalized:
            scores["SEXUAL"] = max(scores["SEXUAL"], 0.80)
            reasons.append(f"疑似色情：命中关键词 {kw}")
            break

    sex_en_hits = _SEX_EN_RE.findall(raw_lower)
    porn_tld = _PORN_TLD_RE.search(raw_lower) is not None
    if sex_en_hits or porn_tld:
        hit_count = len(sex_en_hits)
        unique_count = len(set(sex_en_hits))
        strength = 0.55 * hit_count + 0.35 * unique_count + (1.2 if porn_tld else 0.0)
        sex_rule_score = 1.0 - math.exp(-strength)
        scores["SEXUAL"] = max(scores["SEXUAL"], min(0.99, sex_rule_score))
        if sex_rule_score >= 0.35:
            reasons.append(f"疑似色情（英文特征）：hits={hit_count} unique={unique_count} porn_tld={porn_tld}")

    onnx_raw = _onnx_nsfw_score_cached(text)
    onnx_nsfw = _calibrate_onnx_nsfw(onnx_raw)
    if onnx_nsfw > 0:
        scores["SEXUAL"] = max(scores["SEXUAL"], onnx_nsfw)
        if onnx_nsfw >= 0.35:
            reasons.append(f"ONNX NSFW 文本模型: raw={onnx_raw:.2f} cal={onnx_nsfw:.2f}")

    if _TEXT_MODEL is not None:
        prep = _prep_text_for_model(text)
        probs = _TEXT_MODEL.predict_proba(text, temperature=TEXT_MODEL_TEMP)
        ok_p = float(probs.get("OK", 0.0))
        ad_p = float(probs.get("AD", 0.0))
        sex_p = float(probs.get("SEXUAL", 0.0))
        ad_score = max(0.0, ad_p - ok_p)
        sex_score = max(0.0, sex_p - ok_p)
        length_factor = max(0.10, min(1.0, len(prep) / 80.0))
        scores["AD"] = max(scores["AD"], ad_score * length_factor)
        scores["SEXUAL"] = max(scores["SEXUAL"], sex_score * length_factor)
        if max(ad_score, sex_score) * length_factor >= 0.35:
            reasons.append(f"本地文本模型：OK={ok_p:.2f} AD={ad_p:.2f} SEXUAL={sex_p:.2f}")

    sexual_review = min(NUDITY_STRICT, max(0.0, SEXUAL_REVIEW))
    if scores["SEXUAL"] >= sexual_review:
        categories.append("SEXUAL")
    if scores["AD"] >= AD_STRICT:
        categories.append("AD")

    if scores["SEXUAL"] >= NUDITY_STRICT:
        action: Action = "BLOCK"
    elif "SEXUAL" in categories or "AD" in categories:
        action = "REVIEW"
    else:
        action = "ALLOW"

    return ModerationResponse(action=action, categories=categories, scores=scores, reasons=reasons)


@dataclass
class ImageSignals:
    qr_found: bool = False
    qr_text: str = ""
    nsfw_score: float = 0.0


def _decode_image(b64: str) -> np.ndarray:
    try:
        raw = base64.b64decode(b64, validate=True)
    except Exception:
        raise HTTPException(status_code=400, detail="invalid_base64")
    if len(raw) > MODERATION_MAX_IMAGE_BYTES:
        raise HTTPException(status_code=413, detail="image_too_large")
    arr = np.frombuffer(raw, dtype=np.uint8)
    img = cv2.imdecode(arr, cv2.IMREAD_COLOR)
    if img is None:
        raise HTTPException(status_code=400, detail="invalid_image")
    return img


def _qr_found(img: np.ndarray) -> bool:
    detector = cv2.QRCodeDetector()
    data, points, _ = detector.detectAndDecode(img)
    if data and data.strip():
        return True
    return points is not None and len(points) > 0


def _qr_decode(img: np.ndarray) -> str:
    detector = cv2.QRCodeDetector()
    data, _, _ = detector.detectAndDecode(img)
    return data.strip() if data else ""


def _open_nsfw_score(img: np.ndarray) -> float:
    if _OPEN_NSFW_NET is None:
        return 0.0
    blob = cv2.dnn.blobFromImage(
        img,
        scalefactor=1.0,
        size=(224, 224),
        mean=(104.0, 117.0, 123.0),
        swapRB=False,
        crop=False,
    )
    _OPEN_NSFW_NET.setInput(blob)
    out = _OPEN_NSFW_NET.forward()
    out = np.asarray(out).reshape(-1)
    if out.size >= 2:
        nsfw = float(out[1])
        return max(0.0, min(1.0, nsfw))
    return 0.0


def moderate_image_fast(img: np.ndarray) -> ModerationResponse:
    signals = ImageSignals(
        qr_found=_qr_found(img),
        qr_text=_qr_decode(img),
        nsfw_score=_open_nsfw_score(img),
    )

    scores: Dict[Category, float] = {"AD": 0.0, "SEXUAL": 0.0}
    reasons: List[str] = []
    categories: List[Category] = []

    if signals.qr_found:
        scores["AD"] = max(scores["AD"], 0.9)
        reasons.append("疑似广告/引流：检测到二维码")
        if signals.qr_text:
            reasons.append(f"二维码内容: {signals.qr_text[:200]}")

    if _OPEN_NSFW_NET is None:
        reasons.append("提示：未安装 OpenNSFW 模型（运行 backend/python/download_models.py 下载）")
    else:
        scores["SEXUAL"] = max(scores["SEXUAL"], signals.nsfw_score)
        reasons.append(f"OpenNSFW 分数: {signals.nsfw_score:.3f}")

    if scores["SEXUAL"] >= NUDITY_STRICT:
        categories.append("SEXUAL")
    if scores["AD"] >= AD_STRICT:
        categories.append("AD")

    if "SEXUAL" in categories:
        action: Action = "BLOCK"
    elif "AD" in categories:
        action = "REVIEW"
    else:
        action = "ALLOW"

    return ModerationResponse(action=action, categories=categories, scores=scores, reasons=reasons)


app = FastAPI(title="Local Moderation", version="0.1.0")


@app.get("/health")
def health() -> Dict[str, Any]:
    return {
        "ok": True,
        "open_nsfw_loaded": _OPEN_NSFW_NET is not None,
        "open_nsfw_dir": str(OPEN_NSFW_DIR),
        "text_model_enabled": TEXT_MODEL_ENABLED,
        "text_model_loaded": _TEXT_MODEL is not None,
        "text_onnx_enabled": TEXT_ONNX_ENABLED,
        "text_onnx_loaded": _TEXT_ONNX is not None,
        "text_onnx_baseline": TEXT_ONNX_BASELINE,
    }


@app.post("/v1/moderate/text", response_model=ModerationResponse)
def moderate_text(req: TextRequest) -> ModerationResponse:
    return moderate_text_fast(req.text)


@app.post("/v1/moderate/image", response_model=ModerationResponse)
def moderate_image(req: ImageRequest) -> ModerationResponse:
    img = _decode_image(req.image_base64)
    return moderate_image_fast(img)


if __name__ == "__main__":
    import uvicorn

    port = _env_int("MODERATION_PORT", 8099)
    uvicorn.run(app, host="127.0.0.1", port=port, log_level="info")
