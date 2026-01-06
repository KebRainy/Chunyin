from __future__ import annotations

import hashlib
import os
from pathlib import Path
from urllib.request import urlopen


MODELS_DIR = Path(__file__).resolve().parent / "models"
MODELS_DIR.mkdir(parents=True, exist_ok=True)

# Ultralytics 官方发布的 YOLOv5n ONNX（COCO 80 类），体积小、推理快
YOLOV5N_URL = "https://github.com/ultralytics/yolov5/releases/download/v7.0/yolov5n.onnx"
YOLOV5N_PATH = MODELS_DIR / "yolov5n.onnx"


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def download(url: str, path: Path) -> None:
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


def main() -> None:
    download(YOLOV5N_URL, YOLOV5N_PATH)
    print(f"SHA256: {sha256(YOLOV5N_PATH)}")
    print("\nNext:")
    print("  set MODERATION_ENABLE_YOLO=true")
    print("  python server.py")


if __name__ == "__main__":
    os.environ.setdefault("PYTHONUTF8", "1")
    main()

