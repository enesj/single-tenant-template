(ns app.admin.frontend.events.receipts-detail
  "Admin receipt detail fetch events for the `/admin/receipts` detail modal."
  (:require
    [app.admin.frontend.utils.http :as admin-http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(def ^:private base-path [:admin :receipts :detail])

(rf/reg-event-fx
  :admin/fetch-receipt-detail
  common-interceptors
  (fn [{:keys [db]} [receipt-id]]
    (let [receipt-id* (some-> receipt-id str)]
      (if (seq receipt-id*)
        {:db (-> db
               (assoc-in (conj base-path :loading?) true)
               (assoc-in (conj base-path :error) nil))
         :http-xhrio (admin-http/admin-get
                       {:uri (str "/admin/api/expenses/receipts/" receipt-id*)
                        :on-success [:admin/fetch-receipt-detail-success receipt-id*]
                        :on-failure [:admin/fetch-receipt-detail-failure receipt-id*]})}
        {:db (-> db
               (assoc-in (conj base-path :loading?) false)
               (assoc-in (conj base-path :error) "Receipt ID is required."))}))))

(rf/reg-event-db
  :admin/fetch-receipt-detail-success
  common-interceptors
  (fn [db [receipt-id response]]
    (let [receipt (or (:receipt response)
                    (get-in response [:data :receipt]))]
      (cond-> (-> db
                (assoc-in (conj base-path :loading?) false)
                (assoc-in (conj base-path :error) nil))
        receipt
        (assoc-in (conj base-path :by-id receipt-id) receipt)))))

(rf/reg-event-db
  :admin/fetch-receipt-detail-failure
  common-interceptors
  (fn [db [receipt-id error]]
    (let [message (admin-http/extract-error-message error)]
      (log/warn "Failed to fetch admin receipt detail"
        {:receipt-id receipt-id
         :error error})
      (-> db
        (assoc-in (conj base-path :loading?) false)
        (assoc-in (conj base-path :error) message)))))