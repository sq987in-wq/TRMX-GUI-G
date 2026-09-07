#!/data/data/com.termux/files/usr/bin/bash
# ==============================================================================
# Termux CommandDeck - Localhost Backend Startup Script
# Run this inside Termux on your Android device:
#   pkg update && pkg install -y python ffmpeg
#   pip install -r requirements.txt
#   bash start_server.sh
# ==============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo " Termux CommandDeck - Backend Service"
echo " Host: 127.0.0.1 (Localhost Only)"
echo " Port: 8080"
echo "=========================================="

# Check if Python is installed
if ! command -v python3 &> /dev/null; then
    echo "[!] python3 could not be found. Run: pkg install python"
    exit 1
fi

# Run FastAPI backend strictly bound to 127.0.0.1
exec python3 -m uvicorn main:app --host 127.0.0.1 --port 8080 --workers 1
