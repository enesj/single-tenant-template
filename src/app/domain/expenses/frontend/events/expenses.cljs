(ns app.domain.expenses.frontend.events.expenses
  (:require
    [ajax.core :as ajax]
    [app.admin.frontend.utils.http :as admin-http]
    [day8.re-frame.http-fx]
    [re-frame.core :as rf]))

(def ^:private base-path [:admin :expenses :entries])

(defn- assoc-path
  [db k v]
  (assoc-in db (conj base-path k) v))

(rf/reg-event-fx
  ::load-list
  (fn [{:keys [db]} [_ {:keys [limit offset] :or {limit 50 offset 0}}]]
    {:db (-> db
           (assoc-path :loading? true)
           (assoc-path :error nil))
     :http-xhrio (admin-http/admin-get
                   {:uri "/admin/api/expenses/entries"
                    :params {:limit limit :offset offset}
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success [::list-loaded]
                    :on-failure [::load-failed]})}))

(rf/reg-event-db
  ::list-loaded
  (fn [db [_ {:keys [expenses]}]]
    (-> db
      (assoc-path :loading? false)
      (assoc-path :error nil)
      (assoc-path :items (vec (or expenses []))))))

(rf/reg-event-db
  ::load-failed
  (fn [db [_ error]]
    (-> db
      (assoc-path :loading? false)
      (assoc-path :error (admin-http/extract-error-message error)))))

(rf/reg-event-fx
  ::load-detail
  (fn [{:keys [db]} [_ entry-id]]
    {:db (-> db
           (assoc-path :detail-loading? true)
           (assoc-path :error nil))
     :http-xhrio (admin-http/admin-get
                   {:uri (str "/admin/api/expenses/entries/" entry-id)
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success [::detail-loaded entry-id]
                    :on-failure [::load-failed]})}))

(rf/reg-event-db
  ::detail-loaded
  (fn [db [_ entry-id {:keys [expense]}]]
    (-> db
      (assoc-path :detail-loading? false)
      (assoc-path :error nil)
      (assoc-in (conj base-path :by-id entry-id) expense))))
