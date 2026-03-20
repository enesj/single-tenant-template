(ns app.template.backend.services.gmail-api
  "Send email via the Gmail REST API (HTTPS port 443).
   Works on Railway where outbound SMTP (465/587) is blocked.
   Requires env vars: GOOGLE_OAUTH_CLIENT_ID, GOOGLE_OAUTH_CLIENT_SECRET, GMAIL_REFRESH_TOKEN."
  (:require
    [app.template.backend.auth.email-verification :as email-verification]
    [app.template.backend.services.gmail-smtp :as gmail-smtp]
    [cheshire.core :as json]
    [clj-http.client :as http]
    [taoensso.timbre :as log]
    [app.admin.backend.services.admin.audit :as audit]))

(defn- get-access-token
  "Exchange a refresh token for a short-lived OAuth2 access token."
  [{:keys [client-id client-secret refresh-token]}]
  (let [resp (http/post "https://oauth2.googleapis.com/token"
               {:form-params        {:grant_type    "refresh_token"
                                     :refresh_token refresh-token
                                     :client_id     client-id
                                     :client_secret client-secret}
                :as                 :json
                :socket-timeout     10000
                :connection-timeout 10000
                :throw-exceptions   false})]
    (if (= 200 (:status resp))
      (get-in resp [:body :access_token])
      (throw (ex-info "Failed to obtain Gmail OAuth2 access token"
               {:status (:status resp)
                :body   (:body resp)})))))

(defn- build-mime-message
  "Build an RFC 2822 multipart/alternative email message string."
  [from to subject text html]
  (let [boundary (str "=_mimepart_" (java.util.UUID/randomUUID))]
    (str "From: " from "\r\n"
      "To: " to "\r\n"
      "Subject: " subject "\r\n"
      "MIME-Version: 1.0\r\n"
      "Content-Type: multipart/alternative; boundary=\"" boundary "\"\r\n"
      "\r\n"
      "--" boundary "\r\n"
      "Content-Type: text/plain; charset=utf-8\r\n"
      "\r\n"
      text "\r\n"
      "--" boundary "\r\n"
      "Content-Type: text/html; charset=utf-8\r\n"
      "\r\n"
      html "\r\n"
      "--" boundary "--")))

(defn- base64url-encode
  "URL-safe base64 encode without padding, as required by the Gmail API."
  [^String s]
  (.encodeToString
    (.withoutPadding (java.util.Base64/getUrlEncoder))
    (.getBytes s "UTF-8")))

(defn send-gmail-api-email
  "Send an email via the Gmail REST API.
   `api-config` must contain :client-id, :client-secret, :refresh-token."
  [api-config from-email to-email subject text html]
  (try
    (let [access-token (get-access-token api-config)
          raw-msg      (build-mime-message from-email to-email subject text html)
          encoded      (base64url-encode raw-msg)
          resp         (http/post "https://gmail.googleapis.com/gmail/v1/users/me/messages/send"
                         {:headers            {"Authorization" (str "Bearer " access-token)}
                          :content-type       :json
                          :body               (json/generate-string {:raw encoded})
                          :as                 :json
                          :socket-timeout     15000
                          :connection-timeout 10000
                          :throw-exceptions   false})]
      (if (= 200 (:status resp))
        (do
          (log/info "Email sent via Gmail API" {:to to-email :id (get-in resp [:body :id])})
          {:success true :message-id (get-in resp [:body :id])})
        (do
          (log/error "Gmail API send failed"
            {:status (:status resp) :body (:body resp) :to to-email})
          (when-let [db (:db api-config)]
            (audit/log-api-failure! db
              {:api-name :gmail :operation "send-email"
               :http-status (:status resp)
               :error-message (str "Gmail API non-200: " (:status resp))
               :error-type "http-error" :request-url "https://gmail.googleapis.com" :severity :error}))
          {:success false :error :gmail-api-error :details (:body resp)})))
    (catch Exception e
      (log/error e "Exception while sending email via Gmail API" {:to to-email})
      (when-let [db (:db api-config)]
        (audit/log-api-failure! db
          {:api-name :gmail :operation "send-email"
           :error-message (.getMessage e)
           :error-type (some-> e class .getName)
           :request-url "https://gmail.googleapis.com" :severity :error}))
      {:success false :error :exception :message (.getMessage e)})))

;; ============================================================================
;; Password Reset Email Functions (mirrors gmail-smtp equivalents)
;; ============================================================================

(defn send-password-reset-email
  "Send a password reset email via the Gmail API."
  [api-config from-email to-email _token reset-url full-name]
  (let [{:keys [text html]} (gmail-smtp/create-password-reset-email-body full-name reset-url)
        subject             "Reset your password for Your Organization"]
    (send-gmail-api-email api-config from-email to-email subject text html)))

(defn send-password-changed-email
  "Send a password changed confirmation email via the Gmail API."
  [api-config from-email to-email full-name base-url]
  (let [{:keys [text html]} (gmail-smtp/create-password-changed-email-body full-name base-url)
        subject             "Your password has been changed - Your Organization"]
    (send-gmail-api-email api-config from-email to-email subject text html)))

;; ============================================================================
;; EmailService protocol implementation
;; ============================================================================

(defrecord GmailAPIEmailService [gmail-api-config from-email base-url db]
  email-verification/EmailService

  (send-verification-email [_service user token]
    (log/info "Sending verification email via Gmail API" {:to (:email user)})
    (let [{:keys [text html]} (gmail-smtp/create-verification-email-body user token base-url)
          subject             "Verify your email address for Your Organization"
          cfg                 (assoc gmail-api-config :db db)]
      (send-gmail-api-email cfg from-email (:email user) subject text html)))

  (send-verification-success-email [_service user]
    (log/info "Sending verification success email via Gmail API" {:to (:email user)})
    (let [{:keys [text html]} (gmail-smtp/create-success-email-body user base-url)
          subject             "Email verified successfully - Your Organization"
          cfg                 (assoc gmail-api-config :db db)]
      (send-gmail-api-email cfg from-email (:email user) subject text html))))

(defn create-gmail-api-email-service
  "Create a Gmail API email service.
   Returns nil (logging the missing keys) when :client-id, :client-secret,
   or :refresh-token are absent from `api-config`.
   Optional `db` datasource enables audit logging of send failures."
  ([api-config from-email base-url]
   (create-gmail-api-email-service api-config from-email base-url nil))
  ([api-config from-email base-url db]
   (if (and api-config from-email
         (:client-id api-config)
         (:client-secret api-config)
         (:refresh-token api-config))
     (do
       (log/info "Creating Gmail API email service" {:from-email from-email :base-url base-url})
       (->GmailAPIEmailService api-config from-email base-url db))
     (do
       (log/error "Gmail API config incomplete — email sending will not work"
         {:has-client-id     (boolean (:client-id api-config))
          :has-client-secret (boolean (:client-secret api-config))
          :has-refresh-token (boolean (:refresh-token api-config))
          :has-from-email    (boolean from-email)})
       nil))))

(comment
  ;; Test token exchange (requires env vars set):
  ;; (get-access-token {:client-id     (System/getenv "GOOGLE_OAUTH_CLIENT_ID")
  ;;                    :client-secret (System/getenv "GOOGLE_OAUTH_CLIENT_SECRET")
  ;;                    :refresh-token (System/getenv "GMAIL_REFRESH_TOKEN")})
  :rcf)
