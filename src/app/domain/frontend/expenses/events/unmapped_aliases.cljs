(ns app.domain.frontend.expenses.events.unmapped-aliases
  "Admin-only helper events for the articles/unmapped-aliases endpoint.

  This endpoint returns a list shaped as:
  {:unmapped-aliases [...]}"
  (:require
    [app.admin.frontend.adapters.core :as adapters.core]
    [app.admin.frontend.utils.http :as admin-http]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(rf/reg-event-fx
  ::load-list
  (fn [{:keys [db]} [_ params]]
    (if (adapters.core/admin-token db)
      {:http-xhrio (admin-http/admin-request {:method :get
                                              :uri "/admin/api/expenses/articles/unmapped-aliases"
                                              :params params
                                              :on-success [::load-list-success]
                                              :on-failure [::load-list-failure]})}
      {:dispatch [:admin/redirect-to-login]})))

(rf/reg-event-fx
  ::load-list-success
  (fn [_cofx [_ response]]
    {:dispatch [:admin/refresh-entity-list :unmapped-aliases response]}))

(rf/reg-event-fx
  ::load-list-failure
  (fn [_cofx [_ error]]
    (log/error "Failed to load unmapped aliases" {:error error})
    {}))
