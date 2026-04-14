(ns app.domain.frontend.expenses.events.user-expenses.detail
  "User expense detail events."
  (:require
    [app.domain.frontend.expenses.admin.adapters.sync :as expenses-sync]
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.xhrio :as x]
    [app.shared.adapters.normalization :as normalization]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.db.paths :as paths]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ---------------------------------------------------------------------------
;; Single expense detail
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-expense
  common-interceptors
  (fn [{:keys [db]} [expense-id]]
    {:db (-> db
           (assoc-in [:user-expenses :current-expense :loading?] true)
           (assoc-in [:user-expenses :current-expense :error] nil))
     :http-xhrio (x/xhrio db
                   {:method :get
                    :uri (str endpoints/expense-detail-endpoint "/" expense-id)

                    :on-success [:user-expenses/fetch-expense-success]
                    :on-failure [:user-expenses/fetch-expense-failure]})}))

(defn preserve-existing-item-count
  "Keep the list summary `item_count` when a detail payload omits it.

  The expenses list uses `item_count` to decide whether to render the row
  expand chevron. Fetching detail for view/edit should not strip that summary
  field from the shared template entity store."
  [db expense-raw]
  (let [expense-id (some-> (:id expense-raw) str)
        existing-expense (when expense-id
                           (get-in db (conj (paths/entity-data :expenses) expense-id)))
        existing-item-count (or (:item-count existing-expense)
                              (:item_count existing-expense))]
    (cond-> expense-raw
      (and existing-item-count
        (nil? (:item_count expense-raw))
        (nil? (:item-count expense-raw)))
      (assoc :item_count existing-item-count))))

(rf/reg-event-fx
  :user-expenses/fetch-expense-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [expense-raw (:data response)
          expense-upsert (when expense-raw
                           (preserve-existing-item-count db expense-raw))
          expense (some-> expense-upsert normalization/convert-db-keys->app-keys)
          updated-db (-> db
                       (assoc-in [:user-expenses :current-expense :loading?] false)
                       (assoc-in [:user-expenses :current-expense :error] nil)
                       (assoc-in [:user-expenses :current-expense :data] expense))]
      (cond-> {:db updated-db}
        expense-upsert (assoc :dispatch [::expenses-sync/upsert-expenses [expense-upsert]])))))

(rf/reg-event-db
  :user-expenses/fetch-expense-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch expense detail" {:error error})
    (-> db
      (assoc-in [:user-expenses :current-expense :loading?] false)
      (assoc-in [:user-expenses :current-expense :error] (http/extract-error-message error)))))
