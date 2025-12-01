(ns app.backend.services.gmail-smtp
  "Gmail SMTP email service implementation for development/testing"
  (:require
    [app.template.backend.auth.email-verification :as email-verification]
    [postal.core :as postal]
    [taoensso.timbre :as log]))

(defn create-verification-email-body
  "Create email body for verification email"
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
  "Create email body for verification success email"
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

;; ============================================================================
;; Password Reset Email Templates
;; ============================================================================

(defn create-password-reset-email-body
  "Create email body for password reset email"
  [full-name reset-url]
  (let [user-name (or full-name "there")
        tenant-name "Your Organization"
        expiry-hours 1]
    {:text (str "Hi " user-name ",\n\n"
             "You requested to reset your password for " tenant-name ".\n\n"
             "Click this link to reset your password:\n"
             reset-url "\n\n"
             "This link will expire in " expiry-hours " hour.\n\n"
             "If you didn't request this, you can safely ignore this email. "
             "Your password will remain unchanged.\n\n"
             "Best regards,\n"
             "The " tenant-name " Team")
     :html (str "<html><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>"
             "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>"
             "<h2 style='color: #4F46E5;'>Password Reset Request</h2>"
             "<p>Hi " user-name ",</p>"
             "<p>You requested to reset your password for " tenant-name ".</p>"
             "<div style='text-align: center; margin: 30px 0;'>"
             "<a href='" reset-url "' style='background-color: #4F46E5; color: white; "
             "padding: 14px 28px; text-decoration: none; border-radius: 6px; "
             "display: inline-block; font-weight: bold;'>Reset Password</a>"
             "</div>"
             "<p>Or copy and paste this link into your browser:</p>"
             "<p style='word-break: break-all; color: #4F46E5;'>"
             "<a href='" reset-url "'>" reset-url "</a></p>"
             "<p style='color: #666; font-size: 14px;'>"
             "This link will expire in " expiry-hours " hour.</p>"
             "<p style='color: #666; font-size: 14px;'>"
             "If you didn't request this, you can safely ignore this email. "
             "Your password will remain unchanged.</p>"
             "<hr style='margin: 30px 0; border: none; border-top: 1px solid #e5e7eb;'>"
             "<p style='font-size: 12px; color: #999;'>Best regards,<br>The " tenant-name " Team</p>"
             "</div></body></html>")}))

(defn create-password-changed-email-body
  "Create email body for password changed confirmation email"
  [full-name base-url]
  (let [user-name (or full-name "there")
        tenant-name "Your Organization"
        login-url (str base-url "/login")]
    {:text (str "Hi " user-name ",\n\n"
             "Your password for " tenant-name " has been successfully changed.\n\n"
             "If you made this change, no further action is required.\n\n"
             "If you did NOT make this change, please contact our support team immediately "
             "and reset your password.\n\n"
             "Best regards,\n"
             "The " tenant-name " Team")
     :html (str "<html><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>"
             "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>"
             "<h2 style='color: #10B981;'>Password Changed Successfully</h2>"
             "<p>Hi " user-name ",</p>"
             "<p>Your password for " tenant-name " has been successfully changed.</p>"
             "<p>If you made this change, no further action is required.</p>"
             "<div style='background-color: #FEF3C7; border-left: 4px solid #F59E0B; "
             "padding: 12px 16px; margin: 20px 0;'>"
             "<p style='margin: 0; color: #92400E;'><strong>Important:</strong> "
             "If you did NOT make this change, please contact our support team immediately "
             "and reset your password.</p>"
             "</div>"
             "<div style='text-align: center; margin: 30px 0;'>"
             "<a href='" login-url "' style='background-color: #4F46E5; color: white; "
             "padding: 12px 24px; text-decoration: none; border-radius: 6px; "
             "display: inline-block;'>Login to Your Account</a>"
             "</div>"
             "<hr style='margin: 30px 0; border: none; border-top: 1px solid #e5e7eb;'>"
             "<p style='font-size: 12px; color: #999;'>Best regards,<br>The " tenant-name " Team</p>"
             "</div></body></html>")}))

(defn send-smtp-email
  "Send email via SMTP using postal library"
  [smtp-config from-email to-email subject text-body html-body]
  (try
    (let [message {:from from-email
                   :to [to-email]
                   :subject subject
                   :body [{:type "text/plain; charset=utf-8"
                           :content text-body}
                          {:type "text/html; charset=utf-8"
                           :content html-body}]}
          result (postal/send-message smtp-config message)]

      (log/info "SMTP send result:" result)

      (if (= :SUCCESS (:error result))
        (do
          (log/info "Email sent successfully via SMTP to:" to-email)
          {:success true :message-id (:id result)})
        (do
          (log/error "Failed to send email via SMTP. Error:" (:message result))
          {:success false :error :smtp-error :details result})))

    (catch Exception e
      (log/error e "Exception occurred while sending email via SMTP")
      {:success false :error :smtp-exception :message (.getMessage e)})))

;; ============================================================================
;; Password Reset Email Functions (standalone, not protocol-based)
;; ============================================================================

(defn send-password-reset-email
  "Send password reset email via SMTP"
  [smtp-config from-email to-email _token reset-url full-name]
  (let [{:keys [text html]} (create-password-reset-email-body full-name reset-url)
        tenant-name "Your Organization"
        subject (str "Reset your password for " tenant-name)]
    (send-smtp-email smtp-config from-email to-email subject text html)))

(defn send-password-changed-email
  "Send password changed confirmation email via SMTP"
  [smtp-config from-email to-email full-name base-url]
  (let [{:keys [text html]} (create-password-changed-email-body full-name base-url)
        tenant-name "Your Organization"
        subject (str "Your password has been changed - " tenant-name)]
    (send-smtp-email smtp-config from-email to-email subject text html)))

(defrecord GmailSMTPEmailService [smtp-config from-email base-url]
  email-verification/EmailService

  (send-verification-email [_service user token]
    (log/info "Sending verification email to" (:email user) "via Gmail SMTP")
    (let [{:keys [text html]} (create-verification-email-body user token base-url)
          tenant-name "Your Organization"
          subject (str "Verify your email address for " tenant-name)]

      (send-smtp-email smtp-config from-email (:email user) subject text html)))

  (send-verification-success-email [_service user]
    (log/info "Sending verification success email to" (:email user) "via Gmail SMTP")
    (let [{:keys [text html]} (create-success-email-body user base-url)
          tenant-name "Your Organization"
          subject (str "Email verified successfully - " tenant-name)]

      (send-smtp-email smtp-config from-email (:email user) subject text html))))

(defn create-gmail-smtp-email-service
  "Create a Gmail SMTP email service with SMTP configuration"
  [smtp-config from-email base-url]
  (when (and smtp-config from-email)
    (log/info "Creating Gmail SMTP email service with from email:" from-email "and base-url:" base-url)
    (->GmailSMTPEmailService smtp-config from-email base-url)))
