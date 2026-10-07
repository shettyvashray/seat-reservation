#!/bin/bash

set -e

if [ -z "$1" ]; then
    echo "Usage: ./burst.sh <BASE_URL>"
    exit 2
fi

BASE_URL="$1"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
VENV_DIR="$SCRIPT_DIR/burst/.venv"

if [ ! -d "$VENV_DIR" ]; then
    echo "Creating burst test virtual environment..."
    python3 -m venv "$VENV_DIR"
fi

echo "Installing burst test dependencies..."
"$VENV_DIR/bin/python" -m pip install -q -r "$SCRIPT_DIR/burst/requirements.txt"

echo "Running burst tests against:"
echo "$BASE_URL"
echo

"$VENV_DIR/bin/python" \
    "$SCRIPT_DIR/burst/burst.py" \
    "$BASE_URL"