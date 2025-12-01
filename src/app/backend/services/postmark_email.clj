(ns app.backend.services.postmark-email
  "Postmark email service implementation for real email sending"
  (:require
    [app.template.backend.auth.email-verification :as email-verification]
    [cheshire.core :as json]
    [clj-http.client :as http]
    [taoensso.timbre :as log]))

(def postmark-api-url "https://api.postmarkapp.com/email")

(defn create-verification-email-body
  "Create HTML email body for verification email"
  [user token base-url]
  (let [verify-url (str base-url "/verify-email?token=" token)
        user-name (or (:full_name user) (:email user))
        tenant-name "Your Organization"]
    {:text (str "Hi " user-name ",\n\n"
             "Welcome to " tenant-name "! Please verify your email address by clicking the link below:\n\n"
             verify-url "\n\n"
             "This link will expire in 24 hours.\n\n"
             "If you didn't request this verification, please ignore this email.\n\n"
             "Best regards,\n"
             "The " tenant-name " Team")
     :html (str "<html><body>"
             "<h2>Welcome to " tenant-name "!</h2>"
             "<p>Hi " user-name ",</p>"
             "<p>Please verify your email address by clicking the button below:</p>"
             "<div style='text-align: center; margin: 30px 0;'>"
             "<a href='" verify-url "' style='background-color: #4F46E5; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; display: inline-block;'>Verify Email Address</a>"
             "</div>"
             "<p>Or copy and paste this link into your browser:</p>"
             "<p><a href='" verify-url "'>" verify-url "</a></p>"
             "<p><small>This link will expire in 24 hours.</small></p>"
             "<p>If you didn't request this verification, please ignore this email.</p>"
             "<hr style='margin: 30px 0; border: none; border-top: 1px solid #e5e7eb;'>"
             "<p><small>Best regards,<br>The " tenant-name " Team</small></p>"
             "</body></html>")}))

(defn create-success-email-body
  "Create HTML email body for verification success email"
  [user base-url]
  (let [user-name (or (:full_name user) (:email user))
        tenant-name "Your Organization"
        login-url (str base-url "/login")]
    {:text (str "Hi " user-name ",\n\n"
             "Great news! Your email address has been successfully verified.\n\n"
             "You can now access all features of " tenant-name ".\n\n"
             "Login here: " login-url "\n\n"
             "Best regards,\n"
             "The " tenant-name " Team")
     :html (str "<html><body>"
             "<h2>Email Verified Successfully!</h2>"
             "<p>Hi " user-name ",</p>"
             "<p>Great news! Your email address has been successfully verified.</p>"
             "<p>You can now access all features of " tenant-name ".</p>"
             "<div style='text-align: center; margin: 30px 0;'>"
             "<a href='" login-url "' style='background-color: #10B981; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; display: inline-block;'>Login Now</a>"
             "</div>"
             "<hr style='margin: 30px 0; border: none; border-top: 1px solid #e5e7eb;'>"
             "<p><small>Best regards,<br>The " tenant-name " Team</small></p>"
             "</body></html>")}))

(defn send-postmark-email
  "Send email via Postmark API"
  [api-key from-email to-email subject text-body html-body]
  (try
    (let [request-body {:From from-email
                        :To to-email
                        :Subject subject
                        :TextBody text-body
                        :HtmlBody html-body
                        :MessageStream "outbound"}
          response (http/post postmark-api-url
                     {:headers {"X-Postmark-Server-Token" api-key
                                "Content-Type" "application/json"
                                "Accept" "application/json"}
                      :body (json/generate-string request-body)
                      :throw-exceptions false})]

      (log/info "Postmark API response status:" (:status response))

      (if (= 200 (:status response))
        (let [response-data (json/parse-string (:body response) true)]
          (log/info "Email sent successfully via Postmark. MessageID:" (:MessageID response-data))
          {:success true :message-id (:MessageID response-data)})
        (let [error-data (json/parse-string (:body response) true)]
          (log/error "Failed to send email via Postmark. Status:" (:status response) "Error:" error-data)
          {:success false :error :postmark-api-error :details error-data})))

    (catch Exception e
      (log/error e "Exception occurred while sending email via Postmark")
      {:success false :error :postmark-exception :message (.getMessage e)})))

(defrecord PostmarkEmailService [api-key from-email base-url]
  email-verification/EmailService

  (send-verification-email [_service user token]
    (log/info "Sending verification email to" (:email user) "via Postmark")
    (let [{:keys [text html]} (create-verification-email-body user token base-url)
          tenant-name "Your Organization"
          subject (str "Verify your email address for " tenant-name)]

      (send-postmark-email api-key from-email (:email user) subject text html)))

  (send-verification-success-email [_service user]
    (log/info "Sending verification success email to" (:email user) "via Postmark")
    (let [{:keys [text html]} (create-success-email-body user base-url)
          tenant-name "Your Organization"
          subject (str "Email verified successfully - " tenant-name)]

      (send-postmark-email api-key from-email (:email user) subject text html))))

(defn create-postmark-email-service
  "Create a Postmark email service with API key and from email"
  [api-key from-email base-url]
  (when (and api-key from-email)
    (log/info "Creating Postmark email service with from email:" from-email "and base-url:" base-url)
    (->PostmarkEmailService api-key from-email base-url)))
