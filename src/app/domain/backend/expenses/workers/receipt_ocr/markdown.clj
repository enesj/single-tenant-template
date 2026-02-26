(ns app.domain.backend.expenses.workers.receipt-ocr.markdown
  "Compatibility facade for OCR markdown parsing.

  New code should use focused namespaces under
  `app.domain.backend.expenses.workers.receipt-ocr.markdown.*`."
  (:require
    [app.domain.backend.expenses.workers.receipt-ocr.markdown.header :as header]
    [app.domain.backend.expenses.workers.receipt-ocr.markdown.items :as items]
    [app.domain.backend.expenses.workers.receipt-ocr.markdown.text :as text]))

(defn label-present-in-markdown?
  [markdown raw-label]
  (text/label-present? markdown raw-label))

(defn strip-legal-suffix
  [name]
  (header/strip-legal-suffix name))

(defn split-store-name-and-address
  [s]
  (header/split-store-name-and-address s))

(defn markdown->line-item-candidates
  [markdown]
  (items/candidates markdown))
