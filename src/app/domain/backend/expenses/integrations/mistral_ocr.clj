(ns app.domain.backend.expenses.integrations.mistral-ocr
  "Mistral OCR integration (POS receipts).

  This namespace is intentionally small and side-effecting:
  - It only knows how to call the provider and normalize responses.
  - It does not persist to the DB (worker/service layer owns persistence).

  Network calls must be stubbed in tests (see `http-post!`).

  Implementation is split into focused submodules:
  - config: Configuration building and JSON schema
  - http: HTTP utilities, retry logic, document helpers
  - batch: Batch API functions"
  (:require
    [cheshire.core :as json]
    [taoensso.timbre :as log]
    [app.domain.backend.expenses.integrations.mistral-ocr.config :as config]
    [app.domain.backend.expenses.integrations.mistral-ocr.http :as http]
    [app.domain.backend.expenses.integrations.mistral-ocr.batch :as batch])
  (:import
    [java.time Instant]))

;; Re-export config
(def receipt-extraction-json-schema config/receipt-extraction-json-schema)
(def build-config config/build-config)

;; Re-export HTTP utilities (for test stubbing)
(def http-post! http/http-post!)
(def http-get! http/http-get!)

;; Re-export batch
(def ocr-extract-batch! batch/ocr-extract-batch!)

(defn ocr-parse!
  "Call Mistral OCR to get markdown per page.

  Args:
  - cfg: from `build-config`
  - {:keys [bytes filename content-type]}

  Returns:
  {:raw <parsed-json> :parsed-markdown <string> :usage-info <map> :model <string>}"
  [cfg {:keys [bytes _filename content-type]}]
  (let [started (System/nanoTime)
        body {:model (or (:model cfg) (config/default-model-name))
              :document (http/build-document-map bytes content-type)}
        resp (http/post-with-retries! cfg {:content-type :json
                                           :body (json/generate-string body)})
        resp-json (or (http/parse-json-body (:body resp))
                    (throw (http/response->ex "Mistral OCR returned non-JSON" resp)))
        duration-ms (/ (- (System/nanoTime) started) 1000000.0)
        markdown (http/pages->markdown resp-json)]
    (log/info "Mistral OCR parse complete" {:duration-ms duration-ms
                                            :pages (count (:pages resp-json))
                                            :model (:model resp-json)})
    {:raw resp-json
     :parsed-markdown markdown
     :usage-info (:usage_info resp-json)
     :model (:model resp-json)}))

(defn ocr-extract!
  "Call Mistral OCR using `response_format` to request structured JSON.

  Args:
  - cfg: from `build-config`
  - {:keys [bytes content-type]}

  Returns:
  {:raw <parsed-json> :extraction <map|nil> :parsed-markdown <string> :received-at <iso-string> :model <string>}"
  [cfg {:keys [bytes content-type]}]
  (let [started (System/nanoTime)
        body {:model (or (:model cfg) (config/default-model-name))
              :document (http/build-document-map bytes content-type)
              :document_annotation_format {:type "json_schema"
                                           :json_schema {:name "receipt_extraction"
                                                         :schema config/receipt-extraction-json-schema}}}
        resp (http/post-with-retries! cfg {:content-type :json
                                           :body (json/generate-string body)})
        resp-json (or (http/parse-json-body (:body resp))
                    (throw (http/response->ex "Mistral OCR returned non-JSON" resp)))
        duration-ms (/ (- (System/nanoTime) started) 1000000.0)
        extraction (http/extract-structured resp-json)
        markdown (http/pages->markdown resp-json)]
    (log/info "Mistral OCR extract complete" {:duration-ms duration-ms
                                              :has-extraction? (boolean extraction)
                                              :model (:model resp-json)})
    {:raw resp-json
     :extraction extraction
     :parsed-markdown markdown
     :received-at (str (Instant/now))
     :model (:model resp-json)}))
