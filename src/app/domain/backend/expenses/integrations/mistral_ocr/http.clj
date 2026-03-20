(ns app.domain.backend.expenses.integrations.mistral-ocr.http
  "HTTP utilities and retry logic for Mistral OCR."
  (:require
    [cheshire.core :as json]
    [clj-http.client :as http]
    [clojure.string :as str]
    [taoensso.timbre :as log]
    [app.domain.backend.expenses.integrations.mistral-ocr.config :as config]
    [app.admin.backend.services.admin.audit :as audit])
  (:import
    [java.util Base64]))

(defn safe-body-snippet [s]
  (when (string? s)
    (let [s (str/replace s #"\s+" " ")]
      (if (> (count s) 1000)
        (str (subs s 0 1000) "…")
        s))))

(defn parse-json-body [body]
  (when (and (string? body) (not (str/blank? body)))
    (try
      (json/parse-string body true)
      (catch Exception _
        nil))))

(defn response->ex
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
  "Low-level HTTP GET. Separated for stubbing in tests."
  [url opts]
  (http/get url opts))

(defn request-with-retries!
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
            (do
              (when-let [db (:db _cfg)]
                (audit/log-api-failure! db
                  {:api-name :mistral-ocr :operation (str "request-" (name method))
                   :error-message (or (.getMessage (:exception resp)) "Unknown exception")
                   :error-type (some-> (:exception resp) class .getName)
                   :request-url url :severity :critical
                   :user-id (:user-id _cfg) :user-name (:user-name _cfg)}))
              (throw (ex-info "Mistral request failed" {:type :mistral/exception
                                                        :url url
                                                        :method method} (:exception resp)))))

          (and (integer? (:status resp)) (<= 200 (:status resp) 299))
          resp

          (retryable? resp)
          (if (< attempt max-retries)
            (do
              (log/warn "Mistral non-2xx; retrying" {:status (:status resp) :attempt (inc attempt) :max-retries max-retries})
              (Thread/sleep (* retry-sleep-ms (inc attempt)))
              (recur (inc attempt)))
            (do
              (when-let [db (:db _cfg)]
                (audit/log-api-failure! db
                  {:api-name :mistral-ocr :operation (str "request-" (name method))
                   :http-status (:status resp)
                   :error-message (or (safe-body-snippet (:body resp)) "Non-2xx response")
                   :error-type "http-error" :request-url url :severity :critical
                   :user-id (:user-id _cfg) :user-name (:user-name _cfg)}))
              (throw (response->ex "Mistral request failed" resp))))

          :else
          (throw (response->ex "Mistral request failed" resp)))))))

(defn ocr-url [{:keys [base-url]}]
  (str (or base-url (config/default-base-url-value)) "/v1/ocr"))

(defn post-with-retries!
  [cfg request-opts]
  (request-with-retries! cfg :post (ocr-url cfg) request-opts))

(defn pages->markdown [resp-json]
  (->> (:pages resp-json)
    (keep :markdown)
    (remove str/blank?)
    (str/join "\n\n")))

(defn bytes->base64 [^bytes b]
  (.encodeToString (Base64/getEncoder) b))

(defn build-document-map [bytes content-type]
  (let [b64 (bytes->base64 bytes)
        mime (or content-type "image/jpeg")]
    (if (str/includes? mime "pdf")
      {:type "document_url"
       :document_url (str "data:" mime ";base64," b64)}
      {:type "image_url"
       :image_url (str "data:" mime ";base64," b64)})))


