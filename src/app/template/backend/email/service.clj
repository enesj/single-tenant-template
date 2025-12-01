(ns app.template.backend.email.service
  "Email service for sending verification and notification emails"
  (:require
    [app.backend.services.gmail-smtp :as gmail-smtp]
    [app.backend.services.postmark-email :as postmark-email]
    [taoensso.timbre :as log]))

(defn create-base-url
  "Create base URL from webserver configuration"
  [config]
  (let [host (get-in config [:webserver :host] "localhost")
        port (get-in config [:webserver :port] 8085) ; Updated default to match dev config
        protocol (if (or (= port 443) (contains? config :https)) "https" "http")]
    (str protocol "://" host ":" port)))

;; Email service supports both Postmark API and Gmail SMTP - configurable via :type

(defn create-email-service
  "Create email service based on configuration - supports Postmark API and Gmail SMTP"
  [config]
  (let [email-type (get config :type :postmark)
        base-url (create-base-url config)]
    (case email-type
      :postmark
      (let [postmark-api-key (-> config :postmark :api-key)
            from-email (-> config :postmark :from-email)]
        (if (and postmark-api-key from-email)
          (do
            (log/info "Creating Postmark email service with from-email:" from-email)
            (postmark-email/create-postmark-email-service postmark-api-key from-email base-url))
          (do
            (log/error "Postmark API key or from-email not configured. Email sending will fail.")
            (throw (ex-info "Postmark email service configuration missing"
                     {:missing-config {:postmark-api-key (nil? postmark-api-key)
                                       :from-email (nil? from-email)}})))))

      :gmail-smtp
      (let [smtp-config (get config :smtp)
            from-email (get config :from-email)]
        (if (and smtp-config from-email)
          (do
            (log/info "Creating Gmail SMTP email service with from-email:" from-email)
            (gmail-smtp/create-gmail-smtp-email-service smtp-config from-email base-url))
          (do
            (log/error "Gmail SMTP configuration missing. Email sending will fail.")
            (throw (ex-info "Gmail SMTP email service configuration missing"
                     {:missing-config {:smtp-config (nil? smtp-config)
                                       :from-email (nil? from-email)}})))))

      ;; Default case
      (do
        (log/error "Unknown email service type:" email-type "Supported types: :postmark, :gmail-smtp")
        (throw (ex-info "Unknown email service type"
                 {:supported-types [:postmark :gmail-smtp]
                  :provided-type email-type}))))))

;; Email service component for dependency injection
(defrecord EmailServiceComponent [config]
  ;; Implement lifecycle protocol if needed
  )
(defn create-email-service-component
  "Create email service component for dependency injection"
  [config]
  (->EmailServiceComponent config))
