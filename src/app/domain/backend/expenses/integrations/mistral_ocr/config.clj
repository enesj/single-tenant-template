(ns app.domain.backend.expenses.integrations.mistral-ocr.config
  "Mistral OCR configuration and schema definitions."
  (:require
    [clojure.string :as str]))

(def ^:private default-base-url "https://api.mistral.ai")
(def ^:private default-model "mistral-ocr-2512")
(def ^:private default-conn-timeout-ms 5000)
(def ^:private default-socket-timeout-ms 30000)
(def ^:private default-max-retries 2)
(def ^:private default-retry-sleep-ms 500)

(def receipt-extraction-json-schema
  "JSON Schema (draft-07) used for structured extraction of receipt metadata.

  Stored provider responses are persisted in `receipts.raw_extract_json`.
  The worker augments this metadata with line items parsed from OCR markdown."
  {"$schema" "http://json-schema.org/draft-07/schema#"
   "title" "ReceiptMetaExtractionV1"
   "type" "object"
   "properties"
   {"merchant" {"type" "object"
                "description" "Seller/merchant printed on the receipt."
                "properties" {"name" {"type" "string"
                                      "description" "Merchant/store name (as printed)."}
                              "address" {"type" ["string" "null"]
                                         "description" "Address if present."}
                              "tax_id" {"type" ["string" "null"]
                                        "description" "Merchant tax/VAT id if present."}}
                "required" ["name"]}

    "purchased_at" {"type" ["string" "null"]
                    "description" "ISO-8601 timestamp (local time) if available, e.g. 2023-12-23T19:23:00"}

    "currency" {"type" ["string" "null"]
                "description" "ISO 4217 currency code (e.g. BAM/EUR/USD). Use null if unknown."}

    "totals" {"type" "object"
              "description" "Totals printed on the receipt."
              "properties" {"subtotal" {"type" ["number" "null"]
                                        "description" "Subtotal before tax/fees if present."}
                            "tax" {"type" ["number" "null"]
                                   "description" "Total tax amount if present."}
                            "total" {"type" "number"
                                     "description" "Grand total paid; prefer the final total."}}
              "required" ["total"]}}
   "required" ["totals"]})

(defn build-config
  "Build a provider config from an app config map (Aero) and environment.

   Environment variables override app config when present:
   - `MISTRAL_API_KEY`
   - `MISTRAL_OCR_BASE_URL`
   - `MISTRAL_OCR_MODEL`
   - `MISTRAL_OCR_ENABLED` (true/false, default true)

  App config keys (optional):
  {:mistral {:api-key <token> :base-url <url> :ocr-model <model>
             :ocr-enabled? true
             :ocr-auto-post-after-upload? true
             :conn-timeout-ms 5000 :socket-timeout-ms 30000
             :max-retries 2 :retry-sleep-ms 500}}

  The returned map is suitable for `ocr-parse!` / `ocr-extract!`."
  ([app-config]
   (build-config app-config nil))
  ([app-config {:keys [getenv]
                :or {getenv (fn [k] (System/getenv k))}}]
   (let [cfg (or (:mistral app-config) {})
         ;; Allow `getenv` injection for tests; normalize blanks to nil.
         getenv* (fn [k] (some-> (getenv k) str/trim (not-empty)))
         parse-bool (fn [s]
                      (when s
                        (let [s (str/lower-case (str/trim s))]
                          (cond
                            (contains? #{"1" "true" "yes" "y" "on"} s) true
                            (contains? #{"0" "false" "no" "n" "off"} s) false
                            :else nil))))
         parse-int (fn [s]
                     (when (and s (re-matches #"\\d+" s))
                       (Long/parseLong s)))
         env-enabled (some-> (getenv* "MISTRAL_OCR_ENABLED") parse-bool)
         enabled? (if (some? env-enabled)
                    env-enabled
                    (if (contains? cfg :ocr-enabled?)
                      (:ocr-enabled? cfg)
                      true))]
     {:enabled? enabled?
       :auto-post-after-upload? (if (contains? cfg :ocr-auto-post-after-upload?)
                                  (:ocr-auto-post-after-upload? cfg)
                                  true)
      :api-key (or (getenv* "MISTRAL_API_KEY") (:api-key cfg))
      :base-url (or (getenv* "MISTRAL_OCR_BASE_URL") (:base-url cfg) default-base-url)
      :model (or (getenv* "MISTRAL_OCR_MODEL") (:ocr-model cfg) default-model)
      :conn-timeout-ms (or (some-> (getenv* "MISTRAL_OCR_CONN_TIMEOUT_MS") parse-int)
                         (:conn-timeout-ms cfg)
                         default-conn-timeout-ms)
      :socket-timeout-ms (or (some-> (getenv* "MISTRAL_OCR_SOCKET_TIMEOUT_MS") parse-int)
                           (:socket-timeout-ms cfg)
                           default-socket-timeout-ms)
      :max-retries (or (some-> (getenv* "MISTRAL_OCR_MAX_RETRIES") parse-int)
                     (:max-retries cfg)
                     default-max-retries)
      :retry-sleep-ms (or (some-> (getenv* "MISTRAL_OCR_RETRY_SLEEP_MS") parse-int)
                        (:retry-sleep-ms cfg)
                        default-retry-sleep-ms)})))

(defn default-model-name []
  default-model)

(defn default-base-url-value []
  default-base-url)
