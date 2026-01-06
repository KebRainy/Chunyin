
python -m venv .venv
.\.venv\Scripts\pip install -r requirements.txt
.\.venv\Scripts\python server.py
.\.venv\Scripts\python download_models.py
set MODERATION_ENABLE_YOLO=true
.\.venv\Scripts\python server.py
