(ns app.domain.backend.expenses.integrations.ocr-provider
  "Provider selection for receipt OCR workflows."
  (:require
    [app.domain.backend.expenses.integrations.llamaparse :as llamaparse]
    [app.domain.backend.expenses.integrations.mistral-ocr :as mistral-ocr]
    [clojure.string :as str]
    [taoensso.timbre :as log]))

(def ^:private default-workflow :mistral)

(defn- unwrap-quotes
  [s]
  (let [s (some-> s str str/trim)
        quoted? (fn [q]
                  (and s
                    (>= (count s) 2)
                    (str/starts-with? s q)
                    (str/ends-with? s q)))]
    (cond
      (quoted? "\"") (subs s 1 (dec (count s)))
      (quoted? "'") (subs s 1 (dec (count s)))
      :else s)))

(defn- parse-workflow
  [raw]
  (let [wf (some-> raw unwrap-quotes str/trim str/lower-case)]
    (case wf
      (nil "") default-workflow
      "mistral" :mistral
      "llamaparse" :llamaparse
      "llama-parse" :llamaparse
      (do
        (log/warn "Invalid RECEIPT_OCR_WORKFLOW; defaulting to mistral"
          {:value raw
           :valid-values ["mistral" "llamaparse"]})
        default-workflow))))

(defn selected-workflow
  "Resolve OCR workflow from `RECEIPT_OCR_WORKFLOW` env var.

  Supported values:
  - mistral (default)
  - llamaparse"
  ([]
   (selected-workflow nil))
  ([{:keys [getenv]
     :or {getenv (fn [k] (System/getenv k))}}]
   (parse-workflow (getenv "RECEIPT_OCR_WORKFLOW"))))

(defn- to-runtime-config
  [workflow provider-name cfg parse-fn extract-fn]
  (merge cfg
    {:provider workflow
     :provider-name provider-name
     :parse! (fn [req] (parse-fn cfg req))
     :extract! (fn [req] (extract-fn cfg req))}))

(defn build-provider
  "Build the selected OCR provider runtime config.

  Returns a map with normalized keys:
  - :provider keyword
  - :provider-name string
  - :enabled? boolean
  - :api-key string?
  - :auto-post-after-upload? boolean
  - :parse! fn
  - :extract! fn"
  [app-config]
  (case (selected-workflow)
    :llamaparse
    (to-runtime-config :llamaparse
      "LlamaParse"
      (llamaparse/build-config app-config)
      llamaparse/ocr-parse!
      llamaparse/ocr-extract!)

    :mistral
    (to-runtime-config :mistral
      "Mistral OCR"
      (mistral-ocr/build-config app-config)
      mistral-ocr/ocr-parse!
      mistral-ocr/ocr-extract!)

    (to-runtime-config :mistral
      "Mistral OCR"
      (mistral-ocr/build-config app-config)
      mistral-ocr/ocr-parse!
      mistral-ocr/ocr-extract!)))

(defn disabled-message
  [{:keys [provider]}]
  (case provider
    :llamaparse "Receipt OCR is disabled (set LLAMAPARSE_ENABLED=true to enable)"
    "Receipt OCR is disabled (set MISTRAL_OCR_ENABLED=true to enable)"))

(defn missing-api-key-message
  [{:keys [provider]}]
  (case provider
    :llamaparse "Receipt OCR is not configured (missing LLAMA_CLOUD_API_KEY)"
    "Receipt OCR is not configured (missing MISTRAL_API_KEY)"))
