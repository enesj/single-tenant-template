(ns app.domain.frontend.expenses.events.user-expenses.export
  "Export and bulk operations for user expenses."
  (:require
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.xhrio :as x]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ---------------------------------------------------------------------------
;; Export expenses
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/export
  common-interceptors
  (fn [{:keys [db]} [format params]]
    (let [export-format (or format :csv)]
      {:db (-> db
             (assoc-in [:user-expenses :export :loading?] true)
             (assoc-in [:user-expenses :export :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :get
                      :uri (str endpoints/list-endpoint "/export")
                      :admin-uri (str endpoints/admin-expenses-endpoint "/export")
                      :params (merge {:format export-format} params)
                      :on-success [:user-expenses/export-success]
                      :on-failure [:user-expenses/export-failure]})})))

(rf/reg-event-fx
  :user-expenses/export-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [download-url (or (:url response) (get-in response [:data :url]))]
      (cond-> {:db (-> db
                     (assoc-in [:user-expenses :export :loading?] false)
                     (assoc-in [:user-expenses :export :error] nil))}
        download-url
        (assoc :dispatch [:download-file download-url])))))

(rf/reg-event-db
  :user-expenses/export-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to export expenses" {:error error})
    (-> db
      (assoc-in [:user-expenses :export :loading?] false)
      (assoc-in [:user-expenses :export :error] (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Delete all expenses (dangerous operation)
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/delete-all
  common-interceptors
  (fn [{:keys [db]} [confirmation-token]]
    (if (= confirmation-token "DELETE_ALL_EXPENSES")
      {:db (-> db
             (assoc-in [:user-expenses :bulk :loading?] true)
             (assoc-in [:user-expenses :bulk :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :delete
                      :uri (str endpoints/list-endpoint "/all")
                      :admin-uri (str endpoints/admin-expenses-endpoint "/all")
                      :params {:confirmation confirmation-token}
                      :on-success [:user-expenses/delete-all-success]
                      :on-failure [:user-expenses/delete-all-failure]})}
      {:db db
       :dispatch [:toast {:type :error :message "Invalid confirmation token"}]})))

(rf/reg-event-fx
  :user-expenses/delete-all-success
  common-interceptors
  (fn [{:keys [db]} [_response]]
    {:db (-> db
           (assoc-in [:user-expenses :bulk :loading?] false)
           (assoc-in [:user-expenses :bulk :error] nil)
           (assoc-in [:user-expenses :recent :data] [])
           (assoc-in [:user-expenses :summary :data] nil))
     :dispatch-n [[:toast {:type :success :message "All expenses deleted"}]
                  [:user-expenses/fetch-summary]]}))

(rf/reg-event-db
  :user-expenses/delete-all-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to delete all expenses" {:error error})
    (-> db
      (assoc-in [:user-expenses :bulk :loading?] false)
      (assoc-in [:user-expenses :bulk :error] (http/extract-error-message error)))))
