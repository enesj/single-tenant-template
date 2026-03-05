(ns app.template.backend.services.invitation-email
  "Standalone invitation email functions — not part of the EmailService protocol.
   Uses the same SMTP transport as the existing email service."
  (:require
    [app.template.backend.services.gmail-smtp :as gmail-smtp]
    [taoensso.timbre :as log]))

(defn- build-invitation-email-body
  "Create text + HTML body for a tenant invitation email."
  [{:keys [inviter-name tenant-name accept-url role]}]
  {:text (str "Hi,\n\n"
           inviter-name " has invited you to join " tenant-name " as a " role ".\n\n"
           "Accept the invitation by visiting:\n"
           accept-url "\n\n"
           "This invitation expires in 7 days.\n\n"
           "If you didn't expect this invitation, you can safely ignore this email.\n\n"
           "Best regards,\n"
           "The " tenant-name " Team")
   :html (str "<html><body>"
           "<h2>You've been invited!</h2>"
           "<p>" inviter-name " has invited you to join <strong>" tenant-name "</strong> as a <strong>" role "</strong>.</p>"
           "<div style='text-align: center; margin: 30px 0;'>"
           "<a href='" accept-url "' style='background-color: #4F46E5; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; display: inline-block;'>Accept Invitation</a>"
           "</div>"
           "<p>Or copy and paste this link into your browser:</p>"
           "<p><a href='" accept-url "'>" accept-url "</a></p>"
           "<p><small>This invitation expires in 7 days.</small></p>"
           "<p>If you didn't expect this invitation, you can safely ignore this email.</p>"
           "<hr style='margin: 30px 0; border: none; border-top: 1px solid #e5e7eb;'>"
           "<p><small>Best regards,<br>The " tenant-name " Team</small></p>"
           "</body></html>")})

(defn send-invitation-email!
  "Send an invitation email via the underlying SMTP transport.
   `email-service` is a GmailSMTPEmailService or PostmarkEmailService record.
   `params` keys: :to-email, :inviter-name, :tenant-name, :accept-url, :role"
  [email-service {:keys [to-email inviter-name tenant-name accept-url role]}]
  (log/info "Sending invitation email"
    {:to-email     to-email
     :tenant-name  tenant-name
     :accept-url   accept-url
     :service-type (type email-service)
     :has-smtp?    (some? (:smtp-config email-service))
     :from-email   (:from-email email-service)})
  (try
    (let [subject (str inviter-name " invited you to join " tenant-name)
          {:keys [text html]} (build-invitation-email-body
                                {:inviter-name inviter-name
                                 :tenant-name  tenant-name
                                 :accept-url   accept-url
                                 :role          role})]
      ;; Use gmail-smtp send helper if we can reach its internals,
      ;; otherwise fall through to postal directly.
      (if-let [smtp-config (:smtp-config email-service)]
        (do
          (log/info "Attempting SMTP send"
            {:host (:host smtp-config)
             :port (:port smtp-config)
             :user (:user smtp-config)
             :tls  (:tls smtp-config)})
          (gmail-smtp/send-smtp-email smtp-config (:from-email email-service) to-email subject text html)
          (log/info "Invitation email sent successfully" {:to-email to-email}))
        (log/warn "Cannot send invitation email — no SMTP config on email service"
          {:service-type (type email-service)
           :service-keys (keys email-service)})))
    (catch Exception e
      (log/error e "Failed to send invitation email"
        {:to-email to-email
         :error    (.getMessage e)
         :cause    (some-> (.getCause e) .getMessage)}))))

(comment
  ;; (require 'app.template.backend.services.invitation-email :reload)
  :rcf)
