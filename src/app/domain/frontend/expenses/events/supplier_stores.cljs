(ns app.domain.frontend.expenses.events.supplier-stores
  "Per-supplier store fetch — lazy expand feature on the Suppliers list page."
  (:require
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; =============================================================================
;; Per-supplier store fetch (list-page expand feature)
;; =============================================================================

(rf/reg-event-fx
  ::fetch-stores-for-supplier
  common-interceptors
  (fn [{:keys [db]} [supplier-id]]
    ;; Skip fetch if stores are already cached for this supplier.
    (if (get-in db [:supplier-stores :by-supplier supplier-id :stores])
      {}
      {:db (assoc-in db [:supplier-stores :by-supplier supplier-id :loading?] true)
       :http-xhrio (http/api-request
                     {:method :get
                      :uri endpoints/stores-endpoint
                      :params {:supplier_id (str supplier-id) :limit 500}
                      :on-success [::fetch-stores-for-supplier-success supplier-id]
                      :on-failure [::fetch-stores-for-supplier-failure supplier-id]})})))

(rf/reg-event-db
  ::fetch-stores-for-supplier-success
  common-interceptors
  (fn [db [supplier-id response]]
    (assoc-in db [:supplier-stores :by-supplier supplier-id]
              {:stores (vec (or (:data response) []))
               :loading? false})))

(rf/reg-event-db
  ::fetch-stores-for-supplier-failure
  common-interceptors
  (fn [db [supplier-id error]]
    (log/warn "Failed to fetch stores for supplier" {:supplier-id supplier-id :error error})
    (assoc-in db [:supplier-stores :by-supplier supplier-id :loading?] false)))
