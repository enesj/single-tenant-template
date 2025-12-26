#!/bin/bash
# Wrapper script for receipt OCR worker
# Auto-sources API credentials before invoking the Clojure script
#
# Usage: ./receipt_ocr_worker.sh [dev|test] [options]

# Auto-source API credentials if available
if [ -f ".api_credentials.sh" ]; then
  source .api_credentials.sh
fi

# Invoke the Clojure script with all passed arguments
exec clj -M scripts/bb/expenses/receipt_ocr_worker.clj "$@"
