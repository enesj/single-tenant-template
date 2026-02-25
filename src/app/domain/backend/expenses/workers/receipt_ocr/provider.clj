(ns app.domain.backend.expenses.workers.receipt-ocr.provider
  "Provider selection and parse/extract wrapper functions."
  (:require
    [app.domain.backend.expenses.integrations.mistral-ocr :as mistral-ocr]))

(defn provider-key
  [ocr-cfg]
  (or (some-> (:provider ocr-cfg) name)
    "mistral"))

(defn parse-with-provider!
  [ocr-cfg req]
  (let [parse-fn (or (:parse! ocr-cfg)
                   (fn [req] (mistral-ocr/ocr-parse! ocr-cfg req)))
        result (parse-fn req)]
    (if (contains? result :provider)
      result
      (assoc result :provider (provider-key ocr-cfg)))))

(defn extract-with-provider!
  [ocr-cfg req]
  (let [extract-fn (or (:extract! ocr-cfg)
                     (fn [req] (mistral-ocr/ocr-extract! ocr-cfg req)))
        result (extract-fn req)]
    (if (contains? result :provider)
      result
      (assoc result :provider (provider-key ocr-cfg)))))
