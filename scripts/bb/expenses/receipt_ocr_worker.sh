#!/bin/bash
# Wrapper script for receipt OCR worker
# Auto-sources API credentials before invoking the Clojure script
#
# Usage: ./receipt_ocr_worker.sh [dev|test] [options]

# Auto-source local env vars if available (optional; never committed).
# `.env` supports KEY=VALUE lines (exported via `set -a`).
if [ -f ".env" ]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

# Invoke the Clojure script with all passed arguments
exec clj -M scripts/bb/expenses/receipt_ocr_worker.clj "$@"
