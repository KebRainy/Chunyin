from __future__ import annotations

import base64
import os
import re
from dataclasses import dataclass
from typing import Any, Dict, List, Literal, Optional
from pathlib import Path

import cv2
import numpy as np
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field


Action = Literal["ALLOW", "REVIEW", "BLOCK"]
Category = Literal["AD", "SEXUAL", "VIOLENCE"]


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
VIOLENCE_STRICT = _env_float("MODERATION_VIOLENCE_STRICT", 0.65)
AD_STRICT = _env_float("MODERATION_AD_STRICT", 0.60)

YOLO_ENABLE = os.getenv("MODERATION_ENABLE_YOLO", "false").strip().lower() in {"1", "true", "yes", "y"}
YOLO_MODEL_PATH = Path(os.getenv("MODERATION_YOLO_MODEL", str(Path(__file__).resolve().parent / "models" / "yolov5n.onnx")))
_YOLO_NET = None
if YOLO_ENABLE and YOLO_MODEL_PATH.exists():
    try:
        _YOLO_NET = cv2.dnn.readNetFromONNX(str(YOLO_MODEL_PATH))
    except Exception:
        _YOLO_NET = None


class TextRequest(BaseModel):
    text: str = Field(min_length=1, max_length=10_000)
    scene: Optional[str] = None


class ImageRequest(BaseModel):
    image_base64: str = Field(min_length=1)
    mime_type: Optional[str] = None
    scene: Optional[str] = None


class ModerationResponse(BaseModel):
    action: Action
    categories: List[Category] = Field(default_factory=list)
    scores: Dict[Category, float] = Field(default_factory=dict)
    reasons: List[str] = Field(default_factory=list)


_URL_RE = re.compile(r"(https?://\S+|www\.\S+)", re.IGNORECASE)
_CONTACT_RE = re.compile(
    r"(1[3-9]\d{9})|(微信|vx|v信|V信|wx)[\s:：_-]*[a-zA-Z0-9_-]{3,}|(QQ)[\s:：_-]*\d{5,12}",
    re.IGNORECASE,
)


def _norm_text(text: str) -> str:
    return (
        text.lower()
        .replace(" ", "")
        .replace("\n", "")
        .replace("\t", "")
        .replace("0", "o")
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
    "私聊",
    "代理",
    "招商",
    "加盟",
    "扫码",
    "二维码",
    "进群",
    "返利",
    "优惠券",
    "红包",
    "推广",
]
_SEX_KW = [
    "裸聊",
    "约炮",
    "色情网",
    "成人视频",
    "色情网",
    "色情网",
    "porn",
    "nude",
    "sex",
    "成人视频",
    "看片",
    "强奸",
]
_VIOL_KW = [
    "恐怖袭击",
    "炸弹",
    "爆炸",
    "屠杀",
    "杀人",
    "砍人",
    "枪支",
    "枪",
    "极端组织",
]


def moderate_text_fast(text: str) -> ModerationResponse:
    normalized = _norm_text(text)
    scores: Dict[Category, float] = {"AD": 0.0, "SEXUAL": 0.0, "VIOLENCE": 0.0}
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

    for kw in _VIOL_KW:
        if kw.lower().replace(" ", "") in normalized:
            scores["VIOLENCE"] = max(scores["VIOLENCE"], 0.80)
            reasons.append(f"疑似暴恐/暴力：命中关键词 {kw}")
            break

    if scores["SEXUAL"] >= NUDITY_STRICT:
        categories.append("SEXUAL")
    if scores["VIOLENCE"] >= VIOLENCE_STRICT:
        categories.append("VIOLENCE")
    if scores["AD"] >= AD_STRICT:
        categories.append("AD")

    if "SEXUAL" in categories or "VIOLENCE" in categories:
        action: Action = "BLOCK"
    elif "AD" in categories:
        action = "REVIEW"
    else:
        action = "ALLOW"

    return ModerationResponse(action=action, categories=categories, scores=scores, reasons=reasons)


@dataclass
class ImageSignals:
    qr_found: bool = False
    skin_ratio: float = 0.0
    red_ratio: float = 0.0


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


def _skin_ratio_bgr(img: np.ndarray) -> float:
    ycrcb = cv2.cvtColor(img, cv2.COLOR_BGR2YCrCb)
    cr = ycrcb[:, :, 1]
    cb = ycrcb[:, :, 2]
    skin = cv2.inRange(ycrcb, (0, 133, 77), (255, 173, 127))
    # 额外约束：弱化极端光照
    skin = cv2.medianBlur(skin, 5)
    return float(np.mean(skin > 0))


def _red_ratio_bgr(img: np.ndarray) -> float:
    b, g, r = cv2.split(img)
    red_mask = (r > 150) & (r > g + 40) & (r > b + 40)
    return float(np.mean(red_mask))


def _qr_found(img: np.ndarray) -> bool:
    detector = cv2.QRCodeDetector()
    data, points, _ = detector.detectAndDecode(img)
    if data and data.strip():
        return True
    return points is not None and len(points) > 0


def _yolo_knife_score(img: np.ndarray) -> float:
    if _YOLO_NET is None:
        return 0.0

    h0, w0 = img.shape[:2]
    size = 640
    scale = min(size / w0, size / h0)
    new_w, new_h = int(round(w0 * scale)), int(round(h0 * scale))
    resized = cv2.resize(img, (new_w, new_h), interpolation=cv2.INTER_LINEAR)
    canvas = np.full((size, size, 3), 114, dtype=np.uint8)
    pad_w, pad_h = (size - new_w) // 2, (size - new_h) // 2
    canvas[pad_h:pad_h + new_h, pad_w:pad_w + new_w] = resized

    blob = cv2.dnn.blobFromImage(canvas, 1 / 255.0, (size, size), swapRB=True, crop=False)
    _YOLO_NET.setInput(blob)
    out = _YOLO_NET.forward()
    out = np.squeeze(out, axis=0)  # (25200, 85)
    if out.ndim != 2 or out.shape[1] < 6:
        return 0.0

    obj = out[:, 4]
    # COCO classes index: 43=knife, 76=scissors
    knife_prob = out[:, 5 + 43] if out.shape[1] > 5 + 43 else np.zeros_like(obj)
    scissors_prob = out[:, 5 + 76] if out.shape[1] > 5 + 76 else np.zeros_like(obj)
    conf = obj * np.maximum(knife_prob, scissors_prob)
    max_conf = float(np.max(conf))
    return max_conf


def moderate_image_fast(img: np.ndarray) -> ModerationResponse:
    signals = ImageSignals(
        qr_found=_qr_found(img),
        skin_ratio=_skin_ratio_bgr(img),
        red_ratio=_red_ratio_bgr(img),
    )

    scores: Dict[Category, float] = {"AD": 0.0, "SEXUAL": 0.0, "VIOLENCE": 0.0}
    reasons: List[str] = []
    categories: List[Category] = []

    if signals.qr_found:
        scores["AD"] = max(scores["AD"], 0.9)
        reasons.append("疑似广告/引流：检测到二维码")

    # 低准确率但极快：肤色比例作为“疑似色情”信号
    if signals.skin_ratio > 0.35:
        scores["SEXUAL"] = max(scores["SEXUAL"], min(0.95, 0.5 + signals.skin_ratio))
        reasons.append(f"疑似色情：肤色区域占比偏高({signals.skin_ratio:.2f})")

    # 极简暴恐信号：红色占比（血/暴力场景）+ 高对比边缘密度（这里用红色占比近似）
    if signals.red_ratio > 0.06:
        scores["VIOLENCE"] = max(scores["VIOLENCE"], min(0.90, 0.4 + signals.red_ratio * 5))
        reasons.append(f"疑似暴恐/暴力：红色占比偏高({signals.red_ratio:.2f})")

    # 可选：YOLOv5n（knife/scissors）作为暴恐/暴力的额外信号（速度优先）
    yolo_score = _yolo_knife_score(img)
    if yolo_score > 0.35:
        scores["VIOLENCE"] = max(scores["VIOLENCE"], min(0.98, 0.5 + yolo_score))
        reasons.append(f"疑似暴恐/暴力：检测到刀具/剪刀(置信度 {yolo_score:.2f})")

    if scores["SEXUAL"] >= NUDITY_STRICT:
        categories.append("SEXUAL")
    if scores["VIOLENCE"] >= VIOLENCE_STRICT:
        categories.append("VIOLENCE")
    if scores["AD"] >= AD_STRICT:
        categories.append("AD")

    if "SEXUAL" in categories or "VIOLENCE" in categories:
        action: Action = "BLOCK"
    elif "AD" in categories:
        action = "REVIEW"
    else:
        action = "ALLOW"

    return ModerationResponse(action=action, categories=categories, scores=scores, reasons=reasons)


app = FastAPI(title="Local Moderation", version="0.1.0")


@app.get("/health")
def health() -> Dict[str, Any]:
    return {"ok": True}


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
