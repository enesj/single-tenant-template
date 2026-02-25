(ns app.domain.backend.expenses.integrations.llamaparse
  "LlamaParse OCR integration for receipt parsing."
  (:require
    [cheshire.core :as json]
    [clojure.string :as str]
    [taoensso.timbre :as log]
    [app.domain.backend.expenses.integrations.llamaparse.config :as config]
    [app.domain.backend.expenses.integrations.llamaparse.http :as http]
    [app.domain.backend.expenses.integrations.llamaparse.receipt-extraction :as receipt-extract]
    [app.domain.backend.expenses.integrations.llamaparse.receipt-markdown :as receipt-md])
  (:import
    [java.time Instant]))

;; Re-export config
(def build-config config/build-config)

;; Re-export HTTP utilities (for test stubbing)

(defn- build-upload-configuration
  [{:keys [tier version ocr-languages agentic-custom-prompt]}]
  (cond-> {:tier (or tier "agentic")
           :version (or version "v2")}
    (seq ocr-languages)
    (assoc-in [:input_options :pdf :ocr_parameters] {:languages ocr-languages})

    (seq (some-> agentic-custom-prompt str str/trim not-empty))
    (assoc-in [:agentic_options :custom_prompt] (str/trim (str agentic-custom-prompt)))))

(defn- terminal-status?
  [s]
  (contains? #{"COMPLETED" "FAILED" "CANCELLED" "SUCCESS" "ERROR"} s))

(defn- success-status?
  [s]
  (contains? #{"COMPLETED" "SUCCESS"} s))

(defn- poll-job!
  [cfg job-id]
  (let [started (System/currentTimeMillis)
        poll-interval-ms (long (max 100 (or (:poll-interval-ms cfg) 1500)))
        timeout-ms (long (max poll-interval-ms (or (:poll-timeout-ms cfg) 120000)))
        expand (or (http/normalize-expand (:expand cfg))
                 ["markdown"])]
    (loop []
      (let [resp (http/get-with-retries!
                   cfg
                   job-id
                   {:query-params {:expand (str/join "," expand)}})
            resp-json (or (http/parse-json-body (:body resp))
                        (throw (http/response->ex "LlamaParse returned non-JSON result payload" resp)))
            status (-> (http/response->status resp-json)
                     str/trim
                     str/upper-case)
            elapsed-ms (- (System/currentTimeMillis) started)]
        (cond
          (and (terminal-status? status) (success-status? status))
          resp-json

          (terminal-status? status)
          (throw (ex-info "LlamaParse job finished with non-success status"
                   {:type :llamaparse/job-failed
                    :job-id job-id
                    :status status
                    :response resp-json}))

          (>= elapsed-ms timeout-ms)
          (throw (ex-info "LlamaParse job polling timed out"
                   {:type :llamaparse/job-timeout
                    :job-id job-id
                    :timeout-ms timeout-ms
                    :last-status status}))

          :else
          (do
            (Thread/sleep poll-interval-ms)
            (recur)))))))

(defn ocr-parse!
  "Upload document to LlamaParse and fetch a receipt-like representation.

  LlamaParse provides structured `items` (including table rows) and plain `text`.
  We build a provider-specific extraction + stable markdown representation from
  those fields so the Mistral workflow can remain unchanged.

  Args:
  - cfg: from `build-config`
  - {:keys [bytes filename content-type]}

  Returns:
  {:provider \"llamaparse\"
   :raw <result-json>
   :extraction <map?>
   :parsed-markdown <string>
   :provider-markdown <string>
   :received-at <iso-string>
   :model <string>
   :job-id <string>}"
  [cfg {:keys [bytes filename content-type]}]
  (let [started (System/nanoTime)
        configuration (build-upload-configuration cfg)
        resp (http/upload-with-retries!
               cfg
               {:multipart [{:name "file"
                             :filename (or filename "receipt.bin")
                             :mime-type (or content-type "application/octet-stream")
                             :content bytes}
                            {:name "configuration"
                             :mime-type "application/json"
                             :content (json/generate-string configuration)}]})
        resp-json (or (http/parse-json-body (:body resp))
                    (throw (http/response->ex "LlamaParse upload returned non-JSON response" resp)))
        job-id (or (http/response->job-id resp-json)
                 (throw (ex-info "LlamaParse upload did not return a job id"
                          {:type :llamaparse/missing-job-id
                           :response resp-json})))
        result-json (poll-job! cfg job-id)
        duration-ms (/ (- (System/nanoTime) started) 1000000.0)
        provider-markdown (or (http/response->markdown result-json) "")
        text (http/response->text result-json)
        tables (receipt-md/response->table-markdowns result-json)
        {:keys [extraction parsed-markdown]} (receipt-extract/response->receipt result-json)
        parsed-markdown (or parsed-markdown
                          (receipt-md/normalize-receipt-markdown {:text text
                                                                  :tables tables})
                          provider-markdown
                          "")]
    (log/info "LlamaParse parse complete"
      {:duration-ms duration-ms
       :job-id job-id
       :tier (:tier cfg)
       :version (:version cfg)
       :items-count (count (or (:items extraction) []))
       :has-total? (boolean (get-in extraction [:totals :total]))
       :has-merchant? (boolean (get-in extraction [:merchant :name]))
       :has-items? (boolean (seq tables))
       :has-text? (boolean (seq (some-> text str/trim)))})
    {:provider "llamaparse"
     :raw result-json
     :extraction extraction
     :parsed-markdown parsed-markdown
     :provider-markdown provider-markdown
     :received-at (str (Instant/now))
     :model (str "llamaparse/" (or (:tier cfg) "agentic"))
     :job-id job-id}))

(defn ocr-extract!
  "Call LlamaParse OCR and build structured extraction.

  Args:
  - cfg: from `build-config`
  - {:keys [bytes filename content-type]}

  Returns:
  {:provider \"llamaparse\"
   :raw <result-json>
   :extraction <map?>
   :parsed-markdown <string>
   :received-at <iso-string>
   :model <string>
   :job-id <string>}"
  [cfg {:keys [bytes content-type filename]}]
  (let [parse (ocr-parse! cfg {:bytes bytes
                               :content-type content-type
                               :filename filename})]
    {:provider "llamaparse"
     :raw (:raw parse)
     :extraction (:extraction parse)
     :parsed-markdown (:parsed-markdown parse)
     :received-at (:received-at parse)
     :model (:model parse)
     :job-id (:job-id parse)}))
