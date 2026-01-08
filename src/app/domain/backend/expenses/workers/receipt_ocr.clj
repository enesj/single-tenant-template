(ns app.domain.backend.expenses.workers.receipt-ocr
  "Receipt OCR worker.

  This namespace is the stable public entry-point used by routes, handlers, and
  one-shot runners.

  Implementation lives in smaller namespaces under `receipt_ocr/` to keep the
  orchestration, parsing, and persistence logic easier to navigate."
  (:require
    [app.domain.backend.expenses.workers.receipt-ocr.core :as core]))

(def process-receipt! core/process-receipt!)
(def process-pending! core/process-pending!)
(def process-receipts-by-ids! core/process-receipts-by-ids!)
