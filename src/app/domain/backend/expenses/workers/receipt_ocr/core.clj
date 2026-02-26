(ns app.domain.backend.expenses.workers.receipt-ocr.core
  "Compatibility facade — delegates to focused sub-modules.

  New code should prefer:
    runner/run-pending!
    runner/run-by-ids!
    runner/run-receipt!
    ui-queue/enqueue!"
  (:require
    [app.domain.backend.expenses.workers.receipt-ocr.runner :as runner]
    [app.domain.backend.expenses.workers.receipt-ocr.ui-queue :as ui-queue]))

(def process-receipts-by-ids! runner/run-by-ids!)
(def queue-ui-ocr! ui-queue/enqueue!)
