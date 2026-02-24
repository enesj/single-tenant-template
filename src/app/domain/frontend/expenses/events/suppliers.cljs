(ns app.domain.frontend.expenses.events.suppliers
  "Suppliers domain events - generated using the expenses event factory.

   Also includes inline-create events used by the admin expense form."
  (:require
    [ajax.core :as ajax]
    [app.admin.frontend.utils.http :as admin-http]
    [app.domain.frontend.expenses.events.entity-configs :as configs]
    [app.domain.frontend.expenses.events.events-factory :as factory]
    [app.domain.frontend.expenses.events.related-records-factory :as rr-factory]
    [clojure.string :as str]
    [re-frame.core :as rf]))

;; Register standard CRUD events for suppliers using the factory
(factory/register-entity-events! configs/suppliers-config)

;; Register related records modal events
(rr-factory/register-related-records-events!
  {:entity-key :suppliers
   :base-path [:admin :expenses :suppliers]
   :api-endpoint "/admin/api/expenses/suppliers"
   :valid-related-types #{"expenses" "receipts" "articles" "stores"}})

(def ^:private base-path [:admin :expenses :suppliers])
(def ^:private inline-create-path (conj base-path :inline-create))
(def ^:private delete-path (conj base-path :delete))

(defn- supplier-sort-key
  [supplier]
  (-> (or (:display-name supplier) "")
    str
    str/trim
    str/lower-case))

(defn- upsert-supplier-into-list
  [db supplier]
  (let [items (vec (or (get-in db (conj base-path :items)) []))
        supplier-id (:id supplier)
        items* (->> (conj (vec (remove #(= (:id %) supplier-id) items)) supplier)
                 (sort-by supplier-sort-key)
                 vec)]
    (assoc-in db (conj base-path :items) items*)))

(rf/reg-event-db
  ::clear-inline-create
  (fn [db _]
    (-> db
      (assoc-in (conj inline-create-path :loading?) false)
      (assoc-in (conj inline-create-path :error) nil)
      (assoc-in (conj inline-create-path :last-created) nil))))

(rf/reg-event-fx
  ::create-inline
  (fn [{:keys [db]} [_ {:keys [display-name] :as form-data} on-success]]
    (let [name* (some-> display-name str str/trim)]
      (if (str/blank? name*)
        {:db (-> db
               (assoc-in (conj inline-create-path :loading?) false)
               (assoc-in (conj inline-create-path :error) "Display name is required."))}
        {:db (-> db
               (assoc-in (conj inline-create-path :loading?) true)
               (assoc-in (conj inline-create-path :error) nil))
         :http-xhrio (admin-http/admin-post
                       {:uri "/admin/api/expenses/suppliers"
                        :params (assoc form-data :display-name name*)
                        :response-format (ajax/json-response-format {:keywords? true})
                        :on-success [::create-inline-success on-success]
                        :on-failure [::create-inline-failed]})}))))

(rf/reg-event-fx
  ::create-inline-success
  (fn [{:keys [db]} [_ on-success response]]
    (let [supplier (:supplier response)
          supplier-id (:id supplier)]
      {:db (cond-> (-> db
                     (assoc-in (conj inline-create-path :loading?) false)
                     (assoc-in (conj inline-create-path :error) nil)
                     (assoc-in (conj inline-create-path :last-created) supplier-id))
             (map? supplier) (upsert-supplier-into-list supplier))
       :dispatch [::load-list {:limit 200 :offset 0}]
       :fx [(when (fn? on-success)
              [:dispatch-later {:ms 50
                                :dispatch [::call-inline-callback on-success supplier]}])]})))

;; ---------------------------------------------------------------------------
;; Supplier lifecycle operations
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  ::delete-supplier
  (fn [{:keys [db]} [_ supplier-id]]
    {:db (-> db
           (assoc-in (conj delete-path :loading?) true)
           (assoc-in (conj delete-path :error) nil))
     :http-xhrio (admin-http/admin-delete
                   {:uri (str "/admin/api/expenses/suppliers/" supplier-id)
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success [::delete-supplier-success supplier-id]
                    :on-failure [::delete-supplier-failed supplier-id]})}))

(rf/reg-event-fx
  ::delete-supplier-success
  (fn [{:keys [db]} [_ _supplier-id _response]]
    {:db (-> db
           (assoc-in (conj delete-path :loading?) false)
           (assoc-in (conj delete-path :error) nil)
           (assoc-in (conj base-path :error) nil))
     :dispatch-n [[:admin/show-success-message "Supplier deleted."]
                  [::close-detail-modal]
                  [:admin/navigate-client "/admin/suppliers"]
                  [::load-list]]}))

(rf/reg-event-fx
  ::delete-supplier-failed
  (fn [{:keys [db]} [_ _supplier-id error]]
    (let [msg (admin-http/extract-error-message error)]
      {:db (-> db
             (assoc-in (conj delete-path :loading?) false)
             (assoc-in (conj delete-path :error) msg)
             (assoc-in (conj base-path :error) msg))
       :dispatch [:admin/show-error-message msg]})))

(rf/reg-event-fx
  ::create-inline-failed
  (fn [{:keys [db]} [_ error]]
    {:db (-> db
           (assoc-in (conj inline-create-path :loading?) false)
           (assoc-in (conj inline-create-path :error) (admin-http/extract-error-message error)))}))

(rf/reg-event-fx
  ::call-inline-callback
  (fn [_ [_ callback supplier]]
    (when callback
      (callback supplier))
    {}))
