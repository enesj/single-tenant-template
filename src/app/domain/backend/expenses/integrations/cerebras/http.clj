(ns app.domain.backend.expenses.integrations.cerebras.http
  "HTTP utilities and retry logic for Cerebras (OpenAI-compatible)."
  (:require
    [cheshire.core :as json]
    [clj-http.client :as http]
    [clojure.string :as str]
    [taoensso.timbre :as log]
    [app.domain.backend.expenses.integrations.cerebras.config :as config]
    [app.admin.backend.services.admin.audit :as audit]))

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
      (catch Exception _ nil))))

(defn response->ex
  [message {:keys [status body] :as resp}]
  (ex-info message
    {:type :cerebras/http-error
     :status status
     :body-snippet (safe-body-snippet body)
     :response (select-keys resp [:status :headers :reason-phrase])}))

(defn- throwable->log-data [^Throwable t]
  (let [cause (.getCause t)]
    (cond-> {:exception-class (some-> t class .getName)
             :exception-message (.getMessage t)
             :cause-class (some-> cause class .getName)
             :cause-message (some-> cause .getMessage)}
      (instance? clojure.lang.IExceptionInfo t)
      (assoc :ex-data-keys (some-> t ex-data keys sort vec)))))

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

(defn request-with-retries!
  [{:keys [api-key conn-timeout-ms socket-timeout-ms max-retries retry-sleep-ms] :as _cfg}
   url
   request-opts]
  (when-not (seq api-key)
    (throw (ex-info "Missing Cerebras API key (set env var CEREBRAS_API_KEY)"
             {:type :cerebras/missing-api-key})))
  (let [base-opts {:headers {"Authorization" (str "Bearer " api-key)}
                   :conn-timeout conn-timeout-ms
                   :socket-timeout socket-timeout-ms
                   :throw-exceptions false
                   :as :text}
        call! (fn [] (http-post! url (merge base-opts request-opts)))]
    (loop [attempt 0]
      (let [resp (try
                   (call!)
                   (catch Exception e
                     {:exception e}))]
        (cond
          (some? (:exception resp))
          (if (< attempt max-retries)
            (do
              (let [e (:exception resp)]
                (log/warn e
                  "Cerebras request exception; retrying"
                  (merge
                    {:attempt (inc attempt)
                     :max-retries max-retries
                     :url url}
                    (throwable->log-data e))))
              (Thread/sleep (* retry-sleep-ms (inc attempt)))
              (recur (inc attempt)))
            (let [e (:exception resp)]
              (when-let [db (:db _cfg)]
                (audit/log-api-failure! db
                  {:api-name :cerebras :operation "chat-completion"
                   :error-message (or (.getMessage e) "Unknown exception")
                   :error-type (some-> e class .getName)
                   :request-url url :severity :critical
                   :user-id (:user-id _cfg) :user-name (:user-name _cfg)}))
              (throw (ex-info "Cerebras request failed"
                       (merge
                         {:type :cerebras/exception
                          :url url}
                         (throwable->log-data e))
                       e))))

          (and (integer? (:status resp)) (<= 200 (:status resp) 299))
          resp

          (retryable? resp)
          (if (< attempt max-retries)
            (do
              (log/warn "Cerebras non-2xx; retrying" {:status (:status resp) :attempt (inc attempt) :max-retries max-retries})
              (Thread/sleep (* retry-sleep-ms (inc attempt)))
              (recur (inc attempt)))
            (do
              (when-let [db (:db _cfg)]
                (audit/log-api-failure! db
                  {:api-name :cerebras :operation "chat-completion"
                   :http-status (:status resp)
                   :error-message (or (safe-body-snippet (:body resp)) "Non-2xx response")
                   :error-type "http-error" :request-url url :severity :critical
                   :user-id (:user-id _cfg) :user-name (:user-name _cfg)}))
              (throw (response->ex "Cerebras request failed" resp))))

          :else
          (do
            (when-let [db (:db _cfg)]
              (audit/log-api-failure! db
                {:api-name :cerebras :operation "chat-completion"
                 :http-status (:status resp)
                 :error-message (or (safe-body-snippet (:body resp)) "Non-2xx response")
                 :error-type "http-error" :request-url url :severity :error
                 :user-id (:user-id _cfg) :user-name (:user-name _cfg)}))
            (throw (response->ex "Cerebras request failed" resp))))))))

(defn chat-completions-url [{:keys [base-url]}]
  (let [base (or base-url (config/default-base-url-value))
        base (str/replace base #"/+\z" "")]
    (str base "/chat/completions")))
