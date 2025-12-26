(ns app.domain.backend.expenses.integrations.mistral-ocr
  "Mistral OCR integration (POS receipts).

  This namespace is intentionally small and side-effecting:
  - It only knows how to call the provider and normalize responses.
  - It does not persist to the DB (worker/service layer owns persistence).

  Network calls must be stubbed in tests (see `http-post!`)."
  (:require
    [cheshire.core :as json]
    [clj-http.client :as http]
    [clojure.string :as str]
    [taoensso.timbre :as log])
  (:import
    [java.time Instant]
    [java.util Base64]))

(def ^:private default-base-url "https://api.mistral.ai")
(def ^:private default-model "mistral-ocr-2512")
(def ^:private default-document-type "receipt")

(def ^:private default-conn-timeout-ms 5000)
(def ^:private default-socket-timeout-ms 30000)
(def ^:private default-max-retries 2)
(def ^:private default-retry-sleep-ms 500)

(def receipt-extraction-json-schema
  "JSON Schema (draft-07) used for structured extraction.

  Stored provider responses are persisted in `receipts.raw_extract_json`.
  This schema is also mirrored by the Malli schema in the worker."
  {"$schema" "http://json-schema.org/draft-07/schema#"
   "title" "ReceiptExtractionV1"
   "type" "object"
   "properties"
   {"merchant" {"type" "object"
                "properties" {"name" {"type" "string"}
                              "address" {"type" ["string" "null"]}
                              "tax_id" {"type" ["string" "null"]}}
                "required" ["name"]}

    "purchased_at" {"type" ["string" "null"]
                    "description" "ISO-8601 timestamp if available"}

    "currency" {"type" ["string" "null"]
                "description" "ISO 4217, e.g. USD/EUR/BAM"}

    "totals" {"type" "object"
              "properties" {"subtotal" {"type" ["number" "null"]}
                            "tax" {"type" ["number" "null"]}
                            "total" {"type" "number"}}
              "required" ["total"]}

    "payment_hints" {"type" ["object" "null"]
                     "properties" {"method" {"type" ["string" "null"]
                                             "description" "cash|card|account|person|unknown"}
                                   "card_last4" {"type" ["string" "null"]}}}

    "items" {"type" "array"
             "items" {"type" "object"
                      "properties" {"raw_label" {"type" "string"}
                                    "qty" {"type" ["number" "null"]}
                                    "unit_price" {"type" ["number" "null"]}
                                    "line_total" {"type" "number"}}
                      "required" ["raw_label" "line_total"]}}}
   "required" ["merchant" "totals" "items"]})

(defn build-config
  "Build a provider config from an app config map (Aero) and environment.

  Environment variables override app config when present:
  - `MISTRAL_API_KEY`
  - `MISTRAL_OCR_BASE_URL`
  - `MISTRAL_OCR_MODEL`
  - `MISTRAL_OCR_ENABLED` (true/false, default true)

  App config keys (optional):
  {:mistral {:api-key <token> :base-url <url> :ocr-model <model> :ocr-enabled? true
             :conn-timeout-ms 5000 :socket-timeout-ms 30000
             :max-retries 2 :retry-sleep-ms 500}}

  The returned map is suitable for `ocr-parse!` / `ocr-extract!`."
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
                    (when (and s (re-matches #"\d+" s))
                      (Long/parseLong s)))
        env-enabled (some-> (getenv "MISTRAL_OCR_ENABLED") parse-bool)
        enabled? (if (some? env-enabled)
                   env-enabled
                   (if (contains? cfg :ocr-enabled?)
                     (:ocr-enabled? cfg)
                     true))]
    {:enabled? enabled?
     :api-key (or (getenv "MISTRAL_API_KEY") (:api-key cfg))
     :base-url (or (getenv "MISTRAL_OCR_BASE_URL") (:base-url cfg) default-base-url)
     :model (or (getenv "MISTRAL_OCR_MODEL") (:ocr-model cfg) default-model)
     :document-type (or (:document-type cfg) default-document-type)
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

(defn- post-with-retries!
  [{:keys [api-key conn-timeout-ms socket-timeout-ms max-retries retry-sleep-ms] :as cfg} request-opts]
  (when-not (seq api-key)
    (throw (ex-info "Missing Mistral API key (set env var MISTRAL_API_KEY; optionally disable with MISTRAL_OCR_ENABLED=false)" {:type :mistral/missing-api-key})))
  (let [url (ocr-url cfg)
        base-opts {:headers {"Authorization" (str "Bearer " api-key)}
                   :conn-timeout conn-timeout-ms
                   :socket-timeout socket-timeout-ms
                   :throw-exceptions false
                   :as :text}]
    (loop [attempt 0]
      (let [resp (try
                   (http-post! url (merge base-opts request-opts))
                   (catch Exception e
                     {:exception e}))]
        (cond
          (some? (:exception resp))
          (if (< attempt max-retries)
            (do
              (log/warn "Mistral OCR request exception; retrying" {:attempt (inc attempt) :max-retries max-retries})
              (Thread/sleep (* retry-sleep-ms (inc attempt)))
              (recur (inc attempt)))
            (throw (ex-info "Mistral OCR request failed" {:type :mistral/exception} (:exception resp))))

          (and (integer? (:status resp)) (<= 200 (:status resp) 299))
          resp

          (retryable? resp)
          (if (< attempt max-retries)
            (do
              (log/warn "Mistral OCR non-2xx; retrying" {:status (:status resp) :attempt (inc attempt) :max-retries max-retries})
              (Thread/sleep (* retry-sleep-ms (inc attempt)))
              (recur (inc attempt)))
            (throw (response->ex "Mistral OCR request failed" resp)))

          :else
          (throw (response->ex "Mistral OCR request failed" resp)))))))

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
      (contains? resp-json :merchant)
      (contains? resp-json :totals)
      (contains? resp-json :items))
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
