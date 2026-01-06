from __future__ import annotations

import argparse
import os
from pathlib import Path
from urllib.request import urlopen

OPEN_NSFW_DIR = Path(__file__).resolve().parent / "models" / "open_nsfw"
OPEN_NSFW_DIR.mkdir(parents=True, exist_ok=True)

OPEN_NSFW_PROTO_URL = "https://raw.githubusercontent.com/yahoo/open_nsfw/master/nsfw_model/deploy.prototxt"
OPEN_NSFW_MODEL_URL = "https://raw.githubusercontent.com/yahoo/open_nsfw/master/nsfw_model/resnet_50_1by2_nsfw.caffemodel"

OPEN_NSFW_PROTO_PATH = OPEN_NSFW_DIR / "deploy.prototxt"
OPEN_NSFW_MODEL_PATH = OPEN_NSFW_DIR / "resnet_50_1by2_nsfw.caffemodel"

TEXT_NSFW_DIR = Path(__file__).resolve().parent / "models" / "text_nsfw_onnx"
TEXT_NSFW_ONNX_DIR = TEXT_NSFW_DIR / "onnx"
TEXT_NSFW_DIR.mkdir(parents=True, exist_ok=True)
TEXT_NSFW_ONNX_DIR.mkdir(parents=True, exist_ok=True)

HF_BASE = "https://huggingface.co/TrumpMcDonaldz/michellejieli-NSFW_text_classifier-ONNX/resolve/main"
TEXT_FILES = [
    ("config.json", TEXT_NSFW_DIR / "config.json"),
    ("special_tokens_map.json", TEXT_NSFW_DIR / "special_tokens_map.json"),
    ("tokenizer.json", TEXT_NSFW_DIR / "tokenizer.json"),
    ("tokenizer_config.json", TEXT_NSFW_DIR / "tokenizer_config.json"),
    ("vocab.txt", TEXT_NSFW_DIR / "vocab.txt"),
    ("onnx/decoder_model_merged_quantized.onnx", TEXT_NSFW_ONNX_DIR / "decoder_model_merged_quantized.onnx"),
]


def _download(url: str, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_suffix(path.suffix + ".tmp")
    if tmp.exists():
        tmp.unlink()
    print(f"Downloading: {url}")
    with urlopen(url) as resp, tmp.open("wb") as f:
        while True:
            chunk = resp.read(1024 * 256)
            if not chunk:
                break
            f.write(chunk)
    tmp.replace(path)
    print(f"Saved: {path} ({path.stat().st_size} bytes)")


def _maybe_download_open_nsfw() -> None:
    if OPEN_NSFW_PROTO_PATH.exists() and OPEN_NSFW_MODEL_PATH.exists():
        return
    _download(OPEN_NSFW_PROTO_URL, OPEN_NSFW_PROTO_PATH)
    _download(OPEN_NSFW_MODEL_URL, OPEN_NSFW_MODEL_PATH)


def _maybe_download_text_nsfw_onnx() -> None:
    missing = [dst for _, dst in TEXT_FILES if not dst.exists()]
    if not missing:
        return
    for rel, dst in TEXT_FILES:
        _download(f"{HF_BASE}/{rel}", dst)


def main() -> None:
    parser = argparse.ArgumentParser(description="Download local moderation models")
    parser.add_argument("--open-nsfw", action="store_true", help="Download OpenNSFW image model")
    parser.add_argument("--text-nsfw", action="store_true", help="Download NSFW text ONNX model")
    args = parser.parse_args()

    if not args.open_nsfw and not args.text_nsfw:
        do_open = True
        do_text = True
    else:
        do_open = args.open_nsfw
        do_text = args.text_nsfw

    if do_open:
        _maybe_download_open_nsfw()
    if do_text:
        _maybe_download_text_nsfw_onnx()

    print("\nDone. Next:")
    print("  python server.py")


if __name__ == "__main__":
    os.environ.setdefault("PYTHONUTF8", "1")
    main()
