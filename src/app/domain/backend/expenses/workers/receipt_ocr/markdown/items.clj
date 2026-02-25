(ns app.domain.backend.expenses.workers.receipt-ocr.markdown.items
  (:require
    [app.domain.backend.expenses.workers.receipt-ocr.markdown.items-pipe :as items-pipe]
    [app.domain.backend.expenses.workers.receipt-ocr.markdown.items-price :as items-price]
    [app.domain.backend.expenses.workers.receipt-ocr.markdown.items-qty :as items-qty]
    [app.domain.backend.expenses.workers.receipt-ocr.markdown.text :as text]))

(defn candidates
  "Parse markdown to best-effort line item candidates.

  Prefers pipe-table rows, then qty lines, then label+price heuristics."
  [markdown]
  (when (string? markdown)
    (let [markdown (if (re-find #"(?is)<td\b" markdown)
                     (text/html-markdown->plain-text markdown)
                     markdown)
          pipe-items (items-pipe/parse markdown)]
      (if (seq pipe-items)
        pipe-items
        (let [qty-items (items-qty/parse markdown)]
          (if (seq qty-items)
            qty-items
            (items-price/parse markdown)))))))
