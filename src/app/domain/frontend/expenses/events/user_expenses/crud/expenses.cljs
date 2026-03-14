(ns app.domain.frontend.expenses.events.user-expenses.crud.expenses
  "User expense CRUD events (create, update, delete, post)."
  (:require
    [app.domain.frontend.expenses.admin.adapters.sync :as expenses-sync]
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.xhrio :as x]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.shared.crud.success :as crud-success]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ---------------------------------------------------------------------------
;; Create expense (modal)
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/create-expense-modal
  common-interceptors
  (fn [{:keys [db]} [expense-data on-success]]
    {:db (-> db
           (assoc-in [:user-expenses :form :loading?] true)
           (assoc-in [:user-expenses :form :error] nil))
     :http-xhrio (x/xhrio db
                   {:method :post
                    :uri endpoints/list-endpoint
                    :params expense-data
                    :on-success [:user-expenses/create-expense-modal-success on-success]
                    :on-failure [:user-expenses/create-expense-modal-failure]})}))

(rf/reg-event-fx
  :user-expenses/create-expense-modal-success
  common-interceptors
  (fn [{:keys [db]} [on-success response]]
    (let [expense-id (or (get-in response [:data :id])
                       (get-in response [:expense :id]))
          highlight-id (some-> expense-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] false)
             (assoc-in [:user-expenses :form :error] nil)
             (cond-> highlight-id
               (crud-success/track-recently-created :expenses highlight-id)))
       :dispatch-n [[:user-expenses/fetch-recent {:limit 25 :offset 0}]]
       :fx [(when on-success
              [:dispatch-later {:ms 100
                                :dispatch [:user-expenses/call-modal-callback on-success]}])]})))

(rf/reg-event-db
  :user-expenses/create-expense-modal-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to create expense (modal)" {:error error})
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Update expense (modal)
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/update-expense-modal
  common-interceptors
  (fn [{:keys [db]} [expense-id expense-data on-success]]
    {:db (-> db
           (assoc-in [:user-expenses :form :loading?] true)
           (assoc-in [:user-expenses :form :error] nil))
     :http-xhrio (x/xhrio db
                   {:method :put
                    :uri (str endpoints/list-endpoint "/" expense-id)
                    :params expense-data
                    :on-success [:user-expenses/update-expense-modal-success expense-id on-success]
                    :on-failure [:user-expenses/update-expense-modal-failure]})}))

(rf/reg-event-fx
  :user-expenses/update-expense-modal-success
  common-interceptors
  (fn [{:keys [db]} [expense-id on-success response]]
    (let [expense (:data response)
          highlight-id (some-> expense-id str)]
      (cond-> {:db (-> db
                     (assoc-in [:user-expenses :form :loading?] false)
                     (assoc-in [:user-expenses :form :error] nil)
                     (cond-> highlight-id
                       (crud-success/track-recently-updated :expenses highlight-id)))
               :dispatch-n [[:user-expenses/fetch-recent {:limit 25 :offset 0}]
                            [:user-expenses/fetch-expense expense-id]]
               :fx [(when on-success
                      [:dispatch-later {:ms 100
                                        :dispatch [:user-expenses/call-modal-callback on-success]}])]}
        expense
        (assoc :dispatch [::expenses-sync/upsert-expenses [expense]])))))

(rf/reg-event-db
  :user-expenses/update-expense-modal-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to update expense (modal)" {:error error})
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

;; Helper event to call modal callback
(rf/reg-event-fx
  :user-expenses/call-modal-callback
  common-interceptors
  (fn [_cofx [callback & args]]
    (when (fn? callback)
      (if (seq args)
        (apply callback args)
        (callback)))
    {}))

;; ---------------------------------------------------------------------------
;; Delete expense
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/delete-expense
  common-interceptors
  (fn [{:keys [db]} [expense-id]]
    {:db (assoc-in db [:user-expenses :form :loading?] true)
     :http-xhrio (x/xhrio db
                   {:method :delete
                    :uri (str endpoints/list-endpoint "/batch")
                    :params {:ids [(str expense-id)]}
                    :on-success [:user-expenses/delete-expense-success]
                    :on-failure [:user-expenses/delete-expense-failure]})}))

(rf/reg-event-fx
  :user-expenses/delete-expense-success
  common-interceptors
  (fn [{:keys [db]} [_response]]
    {:db (assoc-in db [:user-expenses :form :loading?] false)
     :dispatch-n [[:user-expenses/refresh-expenses-list]
                  [:navigate-to "/expenses/list"]]}))

(rf/reg-event-db
  :user-expenses/delete-expense-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to delete expense" {:error error})
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Post expense (mark as posted)
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/post-expense
  common-interceptors
  (fn [{:keys [db]} [expense-id]]
    {:db (assoc-in db [:user-expenses :form :loading?] true)
     :http-xhrio (x/xhrio db
                   {:method :put
                    :uri (str endpoints/list-endpoint "/" expense-id)
                    :params {:is_posted true}
                    :on-success [:user-expenses/post-expense-success expense-id]
                    :on-failure [:user-expenses/post-expense-failure]})}))

(rf/reg-event-fx
  :user-expenses/post-expense-success
  common-interceptors
  (fn [{:keys [db]} [expense-id _response]]
    {:db (assoc-in db [:user-expenses :form :loading?] false)
     :dispatch [:user-expenses/fetch-expense expense-id]}))

(rf/reg-event-db
  :user-expenses/post-expense-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to post expense" {:error error})
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))
