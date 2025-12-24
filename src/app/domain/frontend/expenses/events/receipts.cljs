(ns app.domain.frontend.expenses.events.receipts
  "Receipts domain events - generated using the expenses event factory."
  (:require
    [app.admin.frontend.utils.http :as admin-http]
    [app.domain.frontend.expenses.events.events-factory :as factory]
    [app.domain.frontend.expenses.events.entity-configs :as configs]
    [re-frame.core :as rf]))

(def ^:private base-path [:admin :expenses :receipts])

;; Register standard CRUD events for receipts using the factory
(factory/register-entity-events! configs/receipts-config)

(rf/reg-event-fx
  ::retry-extraction
  (fn [{:keys [db]} [_ receipt-id]]
    {:db (-> db
           (assoc-in (conj base-path :action-loading?) true)
           (assoc-in (conj base-path :error) nil))
     :http-xhrio (admin-http/admin-post
                   {:uri (str "/admin/api/expenses/receipts/" receipt-id "/retry")
                    :on-success [::retry-extraction-success receipt-id]
                    :on-failure [::retry-extraction-failure receipt-id]})}))

(rf/reg-event-db
  ::retry-extraction-success
  (fn [db [_ receipt-id response]]
    (-> db
      (assoc-in (conj base-path :action-loading?) false)
      (assoc-in (conj base-path :error) nil)
      (assoc-in (conj base-path :by-id receipt-id) (:receipt response)))))

(rf/reg-event-db
  ::retry-extraction-failure
  (fn [db [_ _ error]]
    (-> db
      (assoc-in (conj base-path :action-loading?) false)
      (assoc-in (conj base-path :error) (admin-http/extract-error-message error)))))

(rf/reg-event-fx
  ::update-status
  (fn [{:keys [db]} [_ receipt-id new-status]]
    {:db (-> db
           (assoc-in (conj base-path :action-loading?) true)
           (assoc-in (conj base-path :error) nil))
     :http-xhrio (admin-http/admin-post
                   {:uri (str "/admin/api/expenses/receipts/" receipt-id "/status")
                    :params {:status new-status}
                    :on-success [::update-status-success receipt-id]
                    :on-failure [::update-status-failure receipt-id]})}))

(rf/reg-event-db
  ::update-status-success
  (fn [db [_ receipt-id response]]
    (-> db
      (assoc-in (conj base-path :action-loading?) false)
      (assoc-in (conj base-path :error) nil)
      (assoc-in (conj base-path :by-id receipt-id) (:receipt response)))))

(rf/reg-event-db
  ::update-status-failure
  (fn [db [_ _ error]]
    (-> db
      (assoc-in (conj base-path :action-loading?) false)
      (assoc-in (conj base-path :error) (admin-http/extract-error-message error)))))
