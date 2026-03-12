(ns app.template.backend.services.invitation-email
  "Standalone invitation email functions — not part of the EmailService protocol.
   Dispatches to Gmail SMTP (dev) or Gmail API (prod) based on the service record."
  (:require
    [app.template.backend.services.gmail-api :as gmail-api]
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
      (let [result (cond
                     (:smtp-config email-service)
                     (do
                       (log/info "Attempting SMTP send"
                         {:host (get-in email-service [:smtp-config :host])
                          :port (get-in email-service [:smtp-config :port])})
                       (gmail-smtp/send-smtp-email
                         (:smtp-config email-service)
                         (:from-email email-service)
                         to-email subject text html))

                     (:gmail-api-config email-service)
                     (do
                       (log/info "Attempting Gmail API send" {:to-email to-email})
                       (gmail-api/send-gmail-api-email
                         (:gmail-api-config email-service)
                         (:from-email email-service)
                         to-email subject text html))

                     :else
                     (do
                       (log/warn "Cannot send invitation email — no transport on email service"
                         {:service-type (type email-service)
                          :service-keys (keys email-service)})
                       {:success false :error :no-transport}))]
        (if (:success result)
          (log/info "Invitation email sent successfully" {:to-email to-email})
          (when-not (= :no-transport (:error result))
            (log/error "Invitation email NOT delivered" {:to-email to-email :details result})))))
    (catch Exception e
      (log/error e "Failed to send invitation email"
        {:to-email to-email
         :error    (.getMessage e)
         :cause    (some-> (.getCause e) .getMessage)}))))

;; ============================================================================
;; Admin Invitation Email
;; ============================================================================

(defn- build-admin-invitation-email-body
  "Create text + HTML body for a platform admin invitation email."
  [{:keys [inviter-name accept-url role]}]
  {:text (str "Hi,\n\n"
           inviter-name " has invited you to join as a platform " role ".\n\n"
           "Accept the invitation by visiting:\n"
           accept-url "\n\n"
           "This invitation expires in 7 days.\n\n"
           "If you didn't expect this invitation, you can safely ignore this email.\n\n"
           "Best regards,\n"
           "Platform Administration")
   :html (str "<html><body>"
           "<h2>You've been invited!</h2>"
           "<p>" inviter-name " has invited you to join as a platform <strong>" role "</strong>.</p>"
           "<div style='text-align: center; margin: 30px 0;'>"
           "<a href='" accept-url "' style='background-color: #4F46E5; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; display: inline-block;'>Accept Invitation</a>"
           "</div>"
           "<p>Or copy and paste this link into your browser:</p>"
           "<p><a href='" accept-url "'>" accept-url "</a></p>"
           "<p><small>This invitation expires in 7 days.</small></p>"
           "<p>If you didn't expect this invitation, you can safely ignore this email.</p>"
           "<hr style='margin: 30px 0; border: none; border-top: 1px solid #e5e7eb;'>"
           "<p><small>Best regards,<br>Platform Administration</small></p>"
           "</body></html>")})

(defn send-admin-invitation-email!
  "Send an admin invitation email via the underlying SMTP transport.
   `email-service` is a GmailSMTPEmailService or PostmarkEmailService record.
   `params` keys: :to-email, :inviter-name, :accept-url, :role"
  [email-service {:keys [to-email inviter-name accept-url role]}]
  (log/info "Sending admin invitation email"
    {:to-email     to-email
     :accept-url   accept-url
     :service-type (type email-service)})
  (try
    (let [subject (str inviter-name " invited you to join as a platform " role)
          {:keys [text html]} (build-admin-invitation-email-body
                                {:inviter-name inviter-name
                                 :accept-url   accept-url
                                 :role          role})]
      (let [result (cond
                     (:smtp-config email-service)
                     (gmail-smtp/send-smtp-email
                       (:smtp-config email-service)
                       (:from-email email-service)
                       to-email subject text html)

                     (:gmail-api-config email-service)
                     (gmail-api/send-gmail-api-email
                       (:gmail-api-config email-service)
                       (:from-email email-service)
                       to-email subject text html)

                     :else
                     (do
                       (log/warn "Cannot send admin invitation email — no transport on email service")
                       {:success false :error :no-transport}))]
        (if (:success result)
          (log/info "Admin invitation email sent successfully" {:to-email to-email})
          (when-not (= :no-transport (:error result))
            (log/error "Admin invitation email NOT delivered" {:to-email to-email :details result})))))
    (catch Exception e
      (log/error e "Failed to send admin invitation email"
        {:to-email to-email
         :error    (.getMessage e)}))))

(comment
  ;; (require 'app.template.backend.services.invitation-email :reload)
  :rcf)
