(ns app.template.frontend.events.auth.ids)

(def fetch-auth-status :app.template.frontend.events.auth/fetch-auth-status)
(def fetch-auth-status-success :app.template.frontend.events.auth/fetch-auth-status-success)
(def fetch-auth-status-failure :app.template.frontend.events.auth/fetch-auth-status-failure)

(def clear-auth-state :app.template.frontend.events.auth/clear-auth-state)

(def logout :app.template.frontend.events.auth/logout)
(def logout-success :app.template.frontend.events.auth/logout-success)
(def logout-failure :app.template.frontend.events.auth/logout-failure)

(def register-user :app.template.frontend.events.auth/register-user)
(def register-user-success :app.template.frontend.events.auth/register-user-success)
(def register-user-failure :app.template.frontend.events.auth/register-user-failure)

(def login-with-password :app.template.frontend.events.auth/login-with-password)
(def login-with-password-success :app.template.frontend.events.auth/login-with-password-success)
(def login-with-password-failure :app.template.frontend.events.auth/login-with-password-failure)

(def verify-email :app.template.frontend.events.auth/verify-email)
(def verify-email-success :app.template.frontend.events.auth/verify-email-success)
(def verify-email-failure :app.template.frontend.events.auth/verify-email-failure)

(def request-password-reset :app.template.frontend.events.auth/request-password-reset)
(def request-password-reset-success :app.template.frontend.events.auth/request-password-reset-success)
(def request-password-reset-failure :app.template.frontend.events.auth/request-password-reset-failure)

(def verify-reset-token :app.template.frontend.events.auth/verify-reset-token)
(def verify-reset-token-success :app.template.frontend.events.auth/verify-reset-token-success)
(def verify-reset-token-failure :app.template.frontend.events.auth/verify-reset-token-failure)

(def reset-password-with-token :app.template.frontend.events.auth/reset-password-with-token)
(def reset-password-with-token-success :app.template.frontend.events.auth/reset-password-with-token-success)
(def reset-password-with-token-failure :app.template.frontend.events.auth/reset-password-with-token-failure)

(def change-password :app.template.frontend.events.auth/change-password)
(def change-password-success :app.template.frontend.events.auth/change-password-success)
(def change-password-failure :app.template.frontend.events.auth/change-password-failure)

