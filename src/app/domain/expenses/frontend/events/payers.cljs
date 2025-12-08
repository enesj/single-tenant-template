(ns app.domain.expenses.frontend.events.payers
  (:require
    [ajax.core :as ajax]
    [app.admin.frontend.utils.http :as admin-http]
    [day8.re-frame.http-fx]
    [re-frame.core :as rf]))

(def ^:private base-path [:admin :expenses :payers])

(defn- assoc-path
  [db k v]
  (assoc-in db (conj base-path k) v))

(rf/reg-event-fx
  ::load
  (fn [{:keys [db]} [_ {:keys [type]}]]
    {:db (-> db
           (assoc-path :loading? true)
           (assoc-path :error nil))
     :http-xhrio (admin-http/admin-get
                   {:uri "/admin/api/expenses/payers"
                    :params (cond-> {} type (assoc :type type))
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success [::loaded]
                    :on-failure [::load-failed]})}))

(rf/reg-event-db
  ::loaded
  (fn [db [_ {:keys [payers]}]]
    (-> db
      (assoc-path :loading? false)
      (assoc-path :error nil)
      (assoc-path :items (vec (or payers []))))))

(rf/reg-event-db
  ::load-failed
  (fn [db [_ error]]
    (-> db
      (assoc-path :loading? false)
      (assoc-path :error (admin-http/extract-error-message error)))))
