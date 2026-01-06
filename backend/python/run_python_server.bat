@echo off
setlocal

if not exist ".venv\\Scripts\\python.exe" (
  python -m venv .venv
)

".venv\\Scripts\\pip.exe" install -r requirements.txt
".venv\\Scripts\\python.exe" download_models.py
".venv\\Scripts\\python.exe" server.py
