(ns app.template.frontend.events.auth.logout
  (:require
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.events.auth.ids :as ids]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ========================================================================
;; Logout Events
;; ========================================================================

;; Handle logout action
(rf/reg-event-fx
  ids/logout
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (-> db
           ;; Clear local session state immediately
           (assoc-in [:session :authenticated?] false)
           (assoc-in [:session :user] nil)
           (assoc-in [:session :tenant] nil)
           (assoc-in [:session :permissions] nil)
           (update :session dissoc :oauth2/access-tokens :provider))
     ;; Call backend logout to clear server session
     :http-xhrio (http/api-request
                   {:method :post
                    :uri "/auth/logout"
                    :on-success [ids/logout-success]
                    :on-failure [ids/logout-failure]})}))

;; Handle successful logout
(rf/reg-event-fx
  ids/logout-success
  common-interceptors
  (fn [{:keys [db]} _]
    (log/info "Logout successful, redirecting to home")
    {:db db
     :redirect "/"}))

;; Handle logout failure
(rf/reg-event-fx
  ids/logout-failure
  common-interceptors
  (fn [{:keys [db]} _]
    (log/error "Logout failed, but clearing local session anyway")
    {:db db
     :redirect "/"}))

