#!/bin/bash

python3 -m venv .venv
source .venv/bin/activate
pip install --upgrade pip
pip install -r requirements.txt
python download_models.py
export MODERATION_ENABLE_YOLO=true

python server.py
