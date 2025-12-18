(ns app.template.frontend.events.auth.utils
  (:require
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.events.auth.ids :as ids]
    [re-frame.core :as rf]))

;; ========================================================================
;; Utility Events
;; ========================================================================

;; Event to clear authentication state (for form resets)
(rf/reg-event-db
  ids/clear-auth-state
  common-interceptors
  (fn [db _]
    (-> db
      (assoc-in [:session :loading?] false)
      (update :session dissoc
        :error
        :registration-message
        :verification-message
        :login-message
        :registered?
        :verification-required?))))

;; Event to set authentication error (for form validation)
#_(rf/reg-event-db
    :app.template.frontend.events.auth/set-auth-error
    common-interceptors
    (fn [db [_ error-message]]
      (assoc-in db [:session :error] error-message)))

