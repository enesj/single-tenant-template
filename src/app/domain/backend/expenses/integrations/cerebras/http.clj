(ns app.domain.backend.expenses.integrations.cerebras.http
  "HTTP utilities and retry logic for Cerebras (OpenAI-compatible)."
  (:require
    [cheshire.core :as json]
    [clj-http.client :as http]
    [clojure.string :as str]
    [taoensso.timbre :as log]
    [app.domain.backend.expenses.integrations.cerebras.config :as config]))

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
              (log/warn "Cerebras request exception; retrying" {:attempt (inc attempt) :max-retries max-retries})
              (Thread/sleep (* retry-sleep-ms (inc attempt)))
              (recur (inc attempt)))
            (throw (ex-info "Cerebras request failed" {:type :cerebras/exception
                                                       :url url}
                     (:exception resp))))

          (and (integer? (:status resp)) (<= 200 (:status resp) 299))
          resp

          (retryable? resp)
          (if (< attempt max-retries)
            (do
              (log/warn "Cerebras non-2xx; retrying" {:status (:status resp) :attempt (inc attempt) :max-retries max-retries})
              (Thread/sleep (* retry-sleep-ms (inc attempt)))
              (recur (inc attempt)))
            (throw (response->ex "Cerebras request failed" resp)))

          :else
          (throw (response->ex "Cerebras request failed" resp)))))))

(defn chat-completions-url [{:keys [base-url]}]
  (let [base (or base-url (config/default-base-url-value))
        base (str/replace base #"/+\z" "")]
    (str base "/chat/completions")))
