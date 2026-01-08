(ns app.domain.backend.expenses.integrations.mistral-ocr
  "Mistral OCR integration (POS receipts).

  This namespace is intentionally small and side-effecting:
  - It only knows how to call the provider and normalize responses.
  - It does not persist to the DB (worker/service layer owns persistence).

  Network calls must be stubbed in tests (see `http-post!`)."
  (:require
    [cheshire.core :as json]
    [clj-http.client :as http]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [taoensso.timbre :as log])
  (:import
    [java.io File]
    [java.time Instant]
    [java.util Base64]))

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

  Batch API (Mistral Batch jobs) support:
  - `MISTRAL_OCR_BATCH_ENABLED` (true/false, default true)
  - `MISTRAL_OCR_BATCH_POLL_MS` (default 2000)
  - `MISTRAL_OCR_BATCH_TIMEOUT_MS` (default 600000)
  - `MISTRAL_OCR_BATCH_MAX_REQUESTS` (default 50)

  App config keys (optional):
  {:mistral {:api-key <token> :base-url <url> :ocr-model <model>
             :ocr-enabled? true
             :ocr-batch-enabled? true
             :ocr-batch-poll-ms 2000
             :ocr-batch-timeout-ms 600000
             :ocr-batch-max-requests 50
             :conn-timeout-ms 5000 :socket-timeout-ms 30000
             :max-retries 2 :retry-sleep-ms 500}}

  The returned map is suitable for `ocr-parse!` / `ocr-extract!` and the Batch API helpers.

  NOTE: Batch support is currently used only in the receipt worker's batch-by-ids flow."
  [app-config]
  (let [cfg (or (:mistral app-config) {})
        getenv (fn [k] (some-> (System/getenv k) str/trim (not-empty)))
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
        env-enabled (some-> (getenv "MISTRAL_OCR_ENABLED") parse-bool)
        enabled? (if (some? env-enabled)
                   env-enabled
                   (if (contains? cfg :ocr-enabled?)
                     (:ocr-enabled? cfg)
                     true))
        env-batch-enabled (some-> (getenv "MISTRAL_OCR_BATCH_ENABLED") parse-bool)
        batch-enabled? (if (some? env-batch-enabled)
                         env-batch-enabled
                         (if (contains? cfg :ocr-batch-enabled?)
                           (:ocr-batch-enabled? cfg)
                           true))
        batch-poll-ms (or (some-> (getenv "MISTRAL_OCR_BATCH_POLL_MS") parse-int)
                        (:ocr-batch-poll-ms cfg)
                        2000)
        batch-timeout-ms (or (some-> (getenv "MISTRAL_OCR_BATCH_TIMEOUT_MS") parse-int)
                           (:ocr-batch-timeout-ms cfg)
                           600000)
        batch-max-requests (or (some-> (getenv "MISTRAL_OCR_BATCH_MAX_REQUESTS") parse-int)
                             (:ocr-batch-max-requests cfg)
                             50)]
    {:enabled? enabled?
     :batch-enabled? batch-enabled?
     :batch-poll-ms batch-poll-ms
     :batch-timeout-ms batch-timeout-ms
     :batch-max-requests batch-max-requests
     :api-key (or (getenv "MISTRAL_API_KEY") (:api-key cfg))
     :base-url (or (getenv "MISTRAL_OCR_BASE_URL") (:base-url cfg) default-base-url)
     :model (or (getenv "MISTRAL_OCR_MODEL") (:ocr-model cfg) default-model)
     :conn-timeout-ms (or (some-> (getenv "MISTRAL_OCR_CONN_TIMEOUT_MS") parse-int)
                        (:conn-timeout-ms cfg)
                        default-conn-timeout-ms)
     :socket-timeout-ms (or (some-> (getenv "MISTRAL_OCR_SOCKET_TIMEOUT_MS") parse-int)
                          (:socket-timeout-ms cfg)
                          default-socket-timeout-ms)
     :max-retries (or (some-> (getenv "MISTRAL_OCR_MAX_RETRIES") parse-int)
                    (:max-retries cfg)
                    default-max-retries)
     :retry-sleep-ms (or (some-> (getenv "MISTRAL_OCR_RETRY_SLEEP_MS") parse-int)
                       (:retry-sleep-ms cfg)
                       default-retry-sleep-ms)}))

(defn- ocr-url [{:keys [base-url]}]
  (str (or base-url default-base-url) "/v1/ocr"))

;; Forward declarations: batch helpers are defined above the single-request helpers.
(declare safe-body-snippet parse-json-body response->ex request-with-retries!
  pages->markdown build-document-map extract-structured)

(defn- files-url [{:keys [base-url]}]
  (str (or base-url default-base-url) "/v1/files"))

(defn- file-content-url [{:keys [base-url]} file-id]
  (str (or base-url default-base-url) "/v1/files/" file-id "/content"))

(defn- batch-jobs-url [{:keys [base-url]}]
  (str (or base-url default-base-url) "/v1/batch/jobs"))

(defn- batch-job-url [{:keys [base-url]} job-id]
  (str (or base-url default-base-url) "/v1/batch/jobs/" job-id))

(defn- terminal-batch-status? [status]
  (contains? #{"SUCCESS" "FAILED" "CANCELLED" "CANCELED"}
    (some-> status str/trim str/upper-case)))

(defn- parse-jsonl-lines
  "Parse a JSONL string into a seq of Clojure maps.

  Ignores blank lines."
  [s]
  (->> (str/split-lines (or s ""))
    (map str/trim)
    (remove str/blank?)
    (map #(json/parse-string % true))))

(defn- write-jsonl-temp-file!
  "Write JSONL lines to a temp file and return the File.

  Caller is responsible for deleting it."
  [lines]
  (let [^File f (File/createTempFile "mistral-ocr-batch-" ".jsonl")]
    (with-open [w (io/writer f)]
      (doseq [line lines]
        (.write w (json/generate-string line))
        (.write w "\n")))
    f))

(defn- upload-batch-input-file!
  "Upload an input JSONL to Mistral Files API (purpose=batch) and return file id."
  [cfg ^File f]
  (let [resp (request-with-retries!
               cfg
               :post
               (files-url cfg)
               {:multipart [{:name "purpose" :content "batch"}
                            {:name "file" :content f :filename (.getName f)}]})
        resp-json (or (parse-json-body (:body resp))
                    (throw (response->ex "Mistral file upload returned non-JSON" resp)))
        file-id (or (:id resp-json) (:file_id resp-json))]
    (when-not (seq file-id)
      (throw (ex-info "Mistral file upload missing id" {:type :mistral/file-upload-missing-id
                                                        :response resp-json})))
    file-id))

(defn- create-batch-job!
  "Create a batch job for the given endpoint and input file."
  [cfg endpoint input-file-id]
  (let [body {:input_files [input-file-id]
              :model (or (:model cfg) default-model)
              :endpoint endpoint}
        resp (request-with-retries!
               cfg
               :post
               (batch-jobs-url cfg)
               {:content-type :json
                :body (json/generate-string body)})
        resp-json (or (parse-json-body (:body resp))
                    (throw (response->ex "Mistral batch job create returned non-JSON" resp)))]
    (when-not (seq (:id resp-json))
      (throw (ex-info "Mistral batch job create missing id" {:type :mistral/batch-create-missing-id
                                                             :response resp-json})))
    resp-json))

(defn- get-batch-job!
  [cfg job-id]
  (let [resp (request-with-retries! cfg :get (batch-job-url cfg job-id) {})]
    (or (parse-json-body (:body resp))
      (throw (response->ex "Mistral batch job get returned non-JSON" resp)))))

(defn- await-batch-job!
  "Poll a batch job until it reaches a terminal state or timeout.

  Returns the final job map."
  [cfg job-id]
  (let [poll-ms (long (max 250 (or (:batch-poll-ms cfg) 2000)))
        timeout-ms (long (max 1000 (or (:batch-timeout-ms cfg) 600000)))
        deadline (+ (System/currentTimeMillis) timeout-ms)]
    (loop []
      (let [job (get-batch-job! cfg job-id)
            status (:status job)]
        (cond
          (terminal-batch-status? status) job

          (> (System/currentTimeMillis) deadline)
          (throw (ex-info "Mistral batch job timed out"
                   {:type :mistral/batch-timeout
                    :job-id job-id
                    :last-status status
                    :timeout-ms timeout-ms}))

          :else
          (do
            (Thread/sleep poll-ms)
            (recur)))))))

(defn- download-file-content!
  "Download a file from Mistral Files API and return it as text."
  [cfg file-id]
  (let [resp (request-with-retries! cfg :get (file-content-url cfg file-id) {:as :text})]
    (:body resp)))

(defn- batch-line->ocr-response
  "Normalize a single JSONL output line from Mistral Batch.

  Returns:
  - {:custom-id <string> :resp-json <map>} on success
  - {:custom-id <string> :error <map>} on per-request error or non-2xx
  - {:error <map>} if the line can't be attributed to a request"
  [line]
  (let [custom-id (some-> (or (:custom_id line) (:custom-id line) (:id line)) str)
        err (:error line)
        response (:response line)]
    (cond
      (not (seq custom-id))
      {:error {:type :mistral/batch-missing-custom-id
               :line line}}

      err
      {:custom-id custom-id
       :error {:type :mistral/batch-request-error
               :error err
               :line (dissoc line :response)}}

      :else
      (let [status-code (or (:status_code response) (:status-code response))
            body (:body response)
            body-json (cond
                        (map? body) body
                        (string? body) (or (parse-json-body body) {:raw body})
                        :else {:raw body})]
        (if (and (integer? status-code) (<= 200 status-code 299))
          {:custom-id custom-id
           :resp-json body-json}
          {:custom-id custom-id
           :error {:type :mistral/batch-non-2xx
                   :status status-code
                   :body-snippet (safe-body-snippet (when (string? body) body))
                   :line (dissoc line :response)}})))))

(defn- resp-json->extract-result
  [cfg resp-json]
  (let [extraction (extract-structured resp-json)
        markdown (pages->markdown resp-json)]
    {:raw resp-json
     :extraction extraction
     :parsed-markdown markdown
     :received-at (str (Instant/now))
     :model (or (:model resp-json) (:model cfg) default-model)}))

(defn ocr-extract-batch!
  "Run Mistral OCR extraction via Batch API.

  requests: seq of {:custom-id <string> :bytes <byte-array> :content-type <string|nil>}

  Returns:
  {:results {<custom-id> <extract-result>}
   :errors {<custom-id> <error-map>}
   :jobs [<job-map> ...]}"
  [cfg requests]
  (when-not (:batch-enabled? cfg)
    (throw (ex-info "Batch OCR is disabled" {:type :mistral/batch-disabled})))
  (let [max-n (long (max 1 (or (:batch-max-requests cfg) 50)))
        requests (vec requests)
        groups (partition-all max-n requests)]
    (reduce
      (fn [acc group]
        (let [lines (mapv
                      (fn [{:keys [custom-id bytes content-type]}]
                        {:custom_id (str custom-id)
                         :body {:model (or (:model cfg) default-model)
                                :document (build-document-map bytes content-type)
                                :document_annotation_format {:type "json_schema"
                                                             :json_schema {:name "receipt_extraction"
                                                                           :schema receipt-extraction-json-schema}}}})
                      group)
              ^File input-file (write-jsonl-temp-file! lines)
              job (try
                    (let [file-id (upload-batch-input-file! cfg input-file)
                          job (create-batch-job! cfg "/v1/ocr" file-id)
                          job-id (:id job)
                          final (await-batch-job! cfg job-id)
                          out-id (:output_file final)
                          err-id (:error_file final)]
                      (when (or (= "FAILED" (some-> (:status final) str/upper-case))
                              (and (not (seq out-id)) (seq err-id)))
                        (throw (ex-info "Mistral batch job failed" {:type :mistral/batch-job-failed
                                                                    :job final
                                                                    :error-file err-id})))
                      (when-not (seq out-id)
                        (throw (ex-info "Mistral batch job missing output_file" {:type :mistral/batch-missing-output-file
                                                                                 :job final})))
                      (let [out-text (download-file-content! cfg out-id)
                            out-lines (parse-jsonl-lines out-text)
                            parsed (mapv batch-line->ocr-response out-lines)
                            ok-lines (filter :resp-json parsed)
                            err-lines (filter :error parsed)
                            results (into {}
                                      (map (fn [{:keys [custom-id resp-json]}]
                                             [custom-id (resp-json->extract-result cfg resp-json)]))
                                      ok-lines)
                            errors (into {}
                                     (keep (fn [{:keys [custom-id error]}]
                                             (when (seq custom-id)
                                               [custom-id error])))
                                     err-lines)]
                        (doseq [unkeyed (keep :error (remove :custom-id err-lines))]
                          (log/warn "Unattributed batch output line" unkeyed))
                        {:job final
                         :results results
                         :errors errors}))
                    (finally
                      (try (.delete input-file) (catch Exception _ nil))))
              group-ids (set (map (comp str :custom-id) group))
              results (:results job)
              missing (seq (remove #(or (contains? results %) (contains? (:errors job) %)) group-ids))
              errors (merge
                       (:errors job)
                       (into {}
                         (map (fn [cid]
                                [cid {:type :mistral/batch-missing-result
                                      :message "Missing result in batch output"}]))
                         (or missing [])))]
          (-> acc
            (update :jobs conj (:job job))
            (update :results merge results)
            (update :errors merge errors))))
      {:jobs [] :results {} :errors {}}
      groups)))

(defn- safe-body-snippet [s]
  (when (string? s)
    (let [s (str/replace s #"\s+" " ")]
      (if (> (count s) 1000)
        (str (subs s 0 1000) "…")
        s))))

(defn- parse-json-body [body]
  (when (and (string? body) (not (str/blank? body)))
    (try
      (json/parse-string body true)
      (catch Exception _
        nil))))

(defn- response->ex
  [message {:keys [status body] :as resp}]
  (ex-info message
    {:type :mistral/http-error
     :status status
     :body-snippet (safe-body-snippet body)
     :response (select-keys resp [:status :headers :reason-phrase])}))

(defn- retryable?
  [{:keys [status exception]}]
  (or (some? exception)
    (= status 429)
    (= status 408)
    (= status 409)
    (and (integer? status) (<= 500 status 599))))

(defn http-post!
  "Low-level HTTP POST. Separated for stubbing in tests."
  [url opts]
  (http/post url opts))

(defn http-get!
  "Wrapper around clj-http `get`.

  Kept as a var so tests can stub network calls."
  [url opts]
  (http/get url opts))

(defn- request-with-retries!
  [{:keys [api-key conn-timeout-ms socket-timeout-ms max-retries retry-sleep-ms] :as _cfg}
   method
   url
   request-opts]
  (when-not (seq api-key)
    (throw (ex-info "Missing Mistral API key (set env var MISTRAL_API_KEY; optionally disable with MISTRAL_OCR_ENABLED=false)" {:type :mistral/missing-api-key})))
  (let [base-opts {:headers {"Authorization" (str "Bearer " api-key)}
                   :conn-timeout conn-timeout-ms
                   :socket-timeout socket-timeout-ms
                   :throw-exceptions false
                   :as :text}
        call! (fn []
                (case method
                  :post (http-post! url (merge base-opts request-opts))
                  :get (http-get! url (merge base-opts request-opts))
                  (throw (ex-info "Unsupported method" {:type :mistral/unsupported-method
                                                        :method method
                                                        :url url}))))]
    (loop [attempt 0]
      (let [resp (try
                   (call!)
                   (catch Exception e
                     {:exception e}))]
        (cond
          (some? (:exception resp))
          (if (< attempt max-retries)
            (do
              (log/warn "Mistral request exception; retrying" {:attempt (inc attempt) :max-retries max-retries})
              (Thread/sleep (* retry-sleep-ms (inc attempt)))
              (recur (inc attempt)))
            (throw (ex-info "Mistral request failed" {:type :mistral/exception
                                                      :url url
                                                      :method method} (:exception resp))))

          (and (integer? (:status resp)) (<= 200 (:status resp) 299))
          resp

          (retryable? resp)
          (if (< attempt max-retries)
            (do
              (log/warn "Mistral non-2xx; retrying" {:status (:status resp) :attempt (inc attempt) :max-retries max-retries})
              (Thread/sleep (* retry-sleep-ms (inc attempt)))
              (recur (inc attempt)))
            (throw (response->ex "Mistral request failed" resp)))

          :else
          (throw (response->ex "Mistral request failed" resp)))))))

(defn- post-with-retries!
  [cfg request-opts]
  (request-with-retries! cfg :post (ocr-url cfg) request-opts))

(defn- pages->markdown [resp-json]
  (->> (:pages resp-json)
    (keep :markdown)
    (remove str/blank?)
    (str/join "\n\n")))

(defn- bytes->base64 [^bytes b]
  (.encodeToString (Base64/getEncoder) b))

(defn- build-document-map [bytes content-type]
  (let [b64 (bytes->base64 bytes)
        mime (or content-type "image/jpeg")]
    (if (str/includes? mime "pdf")
      {:type "document_url"
       :document_url (str "data:" mime ";base64," b64)}
      {:type "image_url"
       :image_url (str "data:" mime ";base64," b64)})))

(defn- extract-structured
  "Best-effort extraction of the structured object from a provider response.

  Because provider contracts can evolve, this tries a few common shapes.
  The worker persists the full raw response regardless."
  [resp-json]
  (cond
    (and (map? resp-json)
      (contains? resp-json :totals))
    resp-json

    (map? (:extraction resp-json))
    (:extraction resp-json)

    (string? (:document_annotation resp-json))
    (parse-json-body (:document_annotation resp-json))

    (map? (:data resp-json))
    (:data resp-json)

    (map? (:result resp-json))
    (:result resp-json)

    :else
    nil))

(defn ocr-parse!
  "Call Mistral OCR to get markdown per page.

  Args:
  - cfg: from `build-config`
  - {:keys [bytes filename content-type]}

  Returns:
  {:raw <parsed-json> :parsed-markdown <string> :usage-info <map> :model <string>}"
  [cfg {:keys [bytes _filename content-type]}]
  (let [started (System/nanoTime)
        ;; Use JSON + base64 for consistency with ocr-extract!
        ;; This avoids multipart parsing issues on the provider side.
        body {:model (or (:model cfg) default-model)
              :document (build-document-map bytes content-type)}
        resp (post-with-retries! cfg {:content-type :json
                                      :body (json/generate-string body)})
        resp-json (or (parse-json-body (:body resp))
                    (throw (response->ex "Mistral OCR returned non-JSON" resp)))
        duration-ms (/ (- (System/nanoTime) started) 1000000.0)
        markdown (pages->markdown resp-json)]
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
        body {:model (or (:model cfg) default-model)
              :document (build-document-map bytes content-type)
              :document_annotation_format {:type "json_schema"
                                           :json_schema {:name "receipt_extraction"
                                                         :schema receipt-extraction-json-schema}}}
        resp (post-with-retries! cfg {:content-type :json
                                      :body (json/generate-string body)})
        resp-json (or (parse-json-body (:body resp))
                    (throw (response->ex "Mistral OCR returned non-JSON" resp)))
        duration-ms (/ (- (System/nanoTime) started) 1000000.0)
        extraction (extract-structured resp-json)
        markdown (pages->markdown resp-json)]
    (log/info "Mistral OCR extract complete" {:duration-ms duration-ms
                                              :has-extraction? (boolean extraction)
                                              :model (:model resp-json)})
    {:raw resp-json
     :extraction extraction
     :parsed-markdown markdown
     :received-at (str (Instant/now))
     :model (:model resp-json)}))
