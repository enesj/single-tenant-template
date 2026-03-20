(ns app.domain.backend.expenses.integrations.llamaparse.http
  "HTTP utilities and retry logic for LlamaParse."
  (:require
    [cheshire.core :as json]
    [clj-http.client :as http]
    [clojure.string :as str]
    [taoensso.timbre :as log]
    [app.domain.backend.expenses.integrations.llamaparse.config :as config]
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
      (catch Exception _
        nil))))

(defn response->ex
  [message {:keys [status body] :as resp}]
  (ex-info message
    {:type :llamaparse/http-error
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
    (throw (ex-info "Missing LlamaParse API key (set LLAMA_CLOUD_API_KEY; optionally disable with LLAMAPARSE_ENABLED=false)"
             {:type :llamaparse/missing-api-key})))
  (let [base-opts {:headers {"Authorization" (str "Bearer " api-key)}
                   :conn-timeout conn-timeout-ms
                   :socket-timeout socket-timeout-ms
                   :throw-exceptions false
                   :as :text}
        call! (fn []
                (case method
                  :post (http-post! url (merge base-opts request-opts))
                  :get (http-get! url (merge base-opts request-opts))
                  (throw (ex-info "Unsupported method"
                           {:type :llamaparse/unsupported-method
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
              (log/warn "LlamaParse request exception; retrying"
                {:attempt (inc attempt) :max-retries max-retries})
              (Thread/sleep (* retry-sleep-ms (inc attempt)))
              (recur (inc attempt)))
            (do
              (when-let [db (:db _cfg)]
                (audit/log-api-failure! db
                  {:api-name :llamaparse :operation (str "request-" (name method))
                   :error-message (or (.getMessage (:exception resp)) "Unknown exception")
                   :error-type (some-> (:exception resp) class .getName)
                   :request-url url :severity :critical
                   :user-id (:user-id _cfg) :user-name (:user-name _cfg)}))
              (throw (ex-info "LlamaParse request failed"
                       {:type :llamaparse/exception
                        :url url
                        :method method}
                       (:exception resp)))))

          (and (integer? (:status resp)) (<= 200 (:status resp) 299))
          resp

          (retryable? resp)
          (if (< attempt max-retries)
            (do
              (log/warn "LlamaParse non-2xx; retrying"
                {:status (:status resp) :attempt (inc attempt) :max-retries max-retries})
              (Thread/sleep (* retry-sleep-ms (inc attempt)))
              (recur (inc attempt)))
            (do
              (when-let [db (:db _cfg)]
                (audit/log-api-failure! db
                  {:api-name :llamaparse :operation (str "request-" (name method))
                   :http-status (:status resp)
                   :error-message (or (safe-body-snippet (:body resp)) "Non-2xx response")
                   :error-type "http-error" :request-url url :severity :critical
                   :user-id (:user-id _cfg) :user-name (:user-name _cfg)}))
              (throw (response->ex "LlamaParse request failed" resp))))

          :else
          (throw (response->ex "LlamaParse request failed" resp)))))))

(defn parse-upload-url [{:keys [base-url]}]
  (str (or base-url (config/default-base-url-value)) "/api/v2/parse/upload"))

(defn parse-job-url [{:keys [base-url]} job-id]
  (str (or base-url (config/default-base-url-value)) "/api/v2/parse/" job-id))

(defn upload-with-retries!
  [cfg request-opts]
  (request-with-retries! cfg :post (parse-upload-url cfg) request-opts))

(defn get-with-retries!
  [cfg job-id request-opts]
  (request-with-retries! cfg :get (parse-job-url cfg job-id) request-opts))

(defn normalize-expand [expand]
  (->> (if (string? expand)
         (str/split expand #",")
         (or expand []))
    (map str/trim)
    (remove str/blank?)
    vec
    not-empty))

(defn response->job-id
  [resp-json]
  (or (:job_id resp-json)
    (:id resp-json)
    (get-in resp-json [:job :id])))

(defn response->status
  [resp-json]
  (or (some-> resp-json :status str)
    (some-> resp-json :job :status str)
    ""))

(defn response->markdown
  [resp-json]
  (let [page-md (->> (get-in resp-json [:markdown :pages])
                  (keep (fn [page]
                          (or (:markdown page)
                            (:text page)
                            (:content page))))
                  (remove str/blank?)
                  (str/join "\n\n"))]
    (or (not-empty page-md)
      (some-> (get-in resp-json [:markdown :content]) str not-empty)
      (some-> (:markdown resp-json) str not-empty))))

(defn response->text
  "Extract concatenated text from a LlamaParse parse result response.

  This is populated when requesting `expand=text` on the parse job endpoint."
  [resp-json]
  (let [page-text (->> (get-in resp-json [:text :pages])
                    (keep (fn [page]
                            (or (:text page)
                              (:content page)
                              (:value page))))
                    (remove str/blank?)
                    (str/join "\n\n"))]
    (or (not-empty page-text)
      (some-> (get-in resp-json [:text :content]) str not-empty)
      (some-> (:text resp-json) str not-empty))))
