(ns app.domain.expenses.frontend.events.suppliers
  (:require
    [ajax.core :as ajax]
    [app.admin.frontend.utils.http :as admin-http]
    [day8.re-frame.http-fx]
    [re-frame.core :as rf]))

(def ^:private base-path [:admin :expenses :suppliers])

(defn- assoc-path
  [db k v]
  (assoc-in db (conj base-path k) v))

(rf/reg-event-fx
  ::load
  (fn [{:keys [db]} [_ {:keys [search limit offset] :or {limit 100 offset 0}}]]
    {:db (-> db
           (assoc-path :loading? true)
           (assoc-path :error nil))
     :http-xhrio (admin-http/admin-get
                   {:uri "/admin/api/expenses/suppliers"
                    :params (cond-> {:limit limit :offset offset}
                              search (assoc :search search))
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success [::loaded]
                    :on-failure [::load-failed]})}))

(rf/reg-event-db
  ::loaded
  (fn [db [_ {:keys [suppliers]}]]
    (-> db
      (assoc-path :loading? false)
      (assoc-path :error nil)
      (assoc-path :items (vec (or suppliers []))))))

(rf/reg-event-db
  ::load-failed
  (fn [db [_ error]]
    (-> db
      (assoc-path :loading? false)
      (assoc-path :error (admin-http/extract-error-message error)))))
