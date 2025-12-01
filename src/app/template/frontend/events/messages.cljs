(ns app.template.frontend.events.messages
  "Simple message/notification events used by template frontend.

   These handlers exist primarily to avoid missing-handler warnings and to
   provide a single place to record the last user-facing message in app-db.

   UI components can subscribe to [:ui :last-message] if they want to render
   toasts or banners."
  (:require
    [re-frame.core :as rf]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [taoensso.timbre :as log]))

(rf/reg-event-db
  :app.template.frontend.events.messages/show-success
  common-interceptors
  (fn [db [_ title message]]
    (log/info "UI success message:" {:title title :message message})
    (assoc-in db [:ui :last-message] {:type :success
                                      :title title
                                      :message message})))

(rf/reg-event-db
  :app.template.frontend.events.messages/show-error
  common-interceptors
  (fn [db [_ title message]]
    (log/warn "UI error message:" {:title title :message message})
    (assoc-in db [:ui :last-message] {:type :error
                                      :title title
                                      :message message})))

