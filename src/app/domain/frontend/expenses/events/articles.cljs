(ns app.domain.frontend.expenses.events.articles
  "Articles domain events - generated using the expenses event factory."
  (:require
    [app.admin.frontend.utils.http :as admin-http]
    [app.domain.frontend.expenses.events.events-factory :as factory]
    [app.domain.frontend.expenses.events.entity-configs :as configs]
    [re-frame.core :as rf]))

(def ^:private base-path [:admin :expenses :articles])
(def ^:private related-records-modal-path (conj base-path :related-records-modal))
(def ^:private default-related-records-limit 200)

(def ^:private initial-related-records-modal-state
  {:open? false
   :article nil
   :step 1
   :selected-type nil
   :records []
   :selected-record nil
   :loading? false
   :error nil})

(def ^:private valid-related-types
  #{"expenses" "receipts" "providers" "stores" "manufacturers" "subcategories"})

(defn- related-type-name
  [related-type]
  (cond
    (keyword? related-type) (name related-type)
    (string? related-type) related-type
    :else nil))

(rf/reg-event-db
  ::open-related-records-modal
  (fn [db [_ article]]
    (assoc-in db related-records-modal-path
              (assoc initial-related-records-modal-state
                :open? true
                :article article
                :step 1))))

(rf/reg-event-db
  ::close-related-records-modal
  (fn [db _]
    (assoc-in db related-records-modal-path initial-related-records-modal-state)))

(rf/reg-event-fx
  ::select-related-type
  (fn [{:keys [db]} [_ related-type]]
    (let [related-type* (related-type-name related-type)]
      {:db (if (contains? valid-related-types related-type*)
             (-> db
               (assoc-in (conj related-records-modal-path :selected-type) related-type*)
               (assoc-in (conj related-records-modal-path :records) [])
               (assoc-in (conj related-records-modal-path :selected-record) nil)
               (assoc-in (conj related-records-modal-path :loading?) false)
               (assoc-in (conj related-records-modal-path :error) nil))
             db)})))

(rf/reg-event-fx
  ::next-related-records-step
  (fn [{:keys [db]} _]
    (case (get-in db (conj related-records-modal-path :step))
      1
      (let [related-type* (get-in db (conj related-records-modal-path :selected-type))
            article-id (get-in db (conj related-records-modal-path :article :id))]
        (if (and article-id (contains? valid-related-types related-type*))
          {:db (-> db
                 (assoc-in (conj related-records-modal-path :step) 2)
                 (assoc-in (conj related-records-modal-path :records) [])
                 (assoc-in (conj related-records-modal-path :selected-record) nil)
                 (assoc-in (conj related-records-modal-path :loading?) true)
                 (assoc-in (conj related-records-modal-path :error) nil))
           :http-xhrio (admin-http/admin-get
                         {:uri (str "/admin/api/expenses/articles/" article-id "/related-records")
                          :params {:type related-type*
                                   :limit default-related-records-limit}
                          :on-success [::related-records-loaded related-type*]
                          :on-failure [::related-records-load-failed related-type*]})}
          {:db db}))

      2
      {:db (if (some? (get-in db (conj related-records-modal-path :selected-record)))
             (-> db
               (assoc-in (conj related-records-modal-path :step) 3)
               (assoc-in (conj related-records-modal-path :error) nil))
             db)}

      {:db db})))

(rf/reg-event-db
  ::related-records-loaded
  (fn [db [_ requested-type response]]
    (let [active-type (get-in db (conj related-records-modal-path :selected-type))
          records (vec (or (:related-records response) []))]
      (if (= requested-type active-type)
        (-> db
          (assoc-in (conj related-records-modal-path :loading?) false)
          (assoc-in (conj related-records-modal-path :error) nil)
          (assoc-in (conj related-records-modal-path :records) records))
        db))))

(rf/reg-event-db
  ::related-records-load-failed
  (fn [db [_ requested-type error]]
    (let [active-type (get-in db (conj related-records-modal-path :selected-type))]
      (if (= requested-type active-type)
        (-> db
          (assoc-in (conj related-records-modal-path :loading?) false)
          (assoc-in (conj related-records-modal-path :records) [])
          (assoc-in (conj related-records-modal-path :error) (admin-http/extract-error-message error)))
        db))))

(rf/reg-event-db
  ::select-related-record
  (fn [db [_ record]]
    (-> db
      (assoc-in (conj related-records-modal-path :selected-record) record)
      (assoc-in (conj related-records-modal-path :error) nil))))

(rf/reg-event-db
  ::back-related-records-step
  (fn [db _]
    (case (get-in db (conj related-records-modal-path :step))
      3 (-> db
          (assoc-in (conj related-records-modal-path :step) 2)
          (assoc-in (conj related-records-modal-path :selected-record) nil))
      2 (-> db
          (assoc-in (conj related-records-modal-path :step) 1)
          (assoc-in (conj related-records-modal-path :selected-type) nil)
          (assoc-in (conj related-records-modal-path :records) [])
          (assoc-in (conj related-records-modal-path :selected-record) nil)
          (assoc-in (conj related-records-modal-path :loading?) false)
          (assoc-in (conj related-records-modal-path :error) nil))
      db)))

;; Register standard CRUD events for articles using the factory
(factory/register-entity-events! configs/articles-config)
