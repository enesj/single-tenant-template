(ns app.domain.frontend.expenses.events.related-records-factory
  "Factory for generating related records modal events for any entity.

  Each entity calls `register-related-records-events!` with a config map
  to get the full set of modal events (open, close, select-type, navigate,
  load, select-record)."
  (:require
    [app.admin.frontend.utils.http :as admin-http]
    [re-frame.core :as rf]))

(defn register-related-records-events!
  "Registers related records modal events for an entity.

  Config:
  - :entity-key         - plural keyword, e.g. :suppliers
  - :base-path          - app-db path, e.g. [:admin :expenses :suppliers]
  - :api-endpoint       - base API path, e.g. \"/admin/api/expenses/suppliers\"
  - :valid-related-types - set of strings, e.g. #{\"expenses\" \"receipts\" \"articles\" \"stores\"}
  - :default-limit      - fetch limit (default 200)"
  [{:keys [entity-key base-path api-endpoint valid-related-types default-limit]
    :or {default-limit 200}}]
  (let [modal-path (conj base-path :related-records-modal)
        event-ns (str "app.domain.frontend.expenses.events." (name entity-key))
        initial-state {:open? false
                       :entity nil
                       :step 1
                       :selected-type nil
                       :records []
                       :selected-record nil
                       :loading? false
                       :error nil}]

    (rf/reg-event-db
      (keyword event-ns "open-related-records-modal")
      (fn [db [_ entity]]
        (assoc-in db modal-path
                  (assoc initial-state :open? true :entity entity :step 1))))

    (rf/reg-event-db
      (keyword event-ns "close-related-records-modal")
      (fn [db _]
        (assoc-in db modal-path initial-state)))

    (rf/reg-event-fx
      (keyword event-ns "select-related-type")
      (fn [{:keys [db]} [_ related-type]]
        (let [rt (cond
                   (keyword? related-type) (name related-type)
                   (string? related-type) related-type
                   :else nil)]
          {:db (if (contains? valid-related-types rt)
                 (-> db
                   (assoc-in (conj modal-path :selected-type) rt)
                   (assoc-in (conj modal-path :records) [])
                   (assoc-in (conj modal-path :selected-record) nil)
                   (assoc-in (conj modal-path :loading?) false)
                   (assoc-in (conj modal-path :error) nil))
                 db)})))

    (rf/reg-event-fx
      (keyword event-ns "next-related-records-step")
      (fn [{:keys [db]} _]
        (case (get-in db (conj modal-path :step))
          1
          (let [rt (get-in db (conj modal-path :selected-type))
                entity-id (get-in db (conj modal-path :entity :id))]
            (if (and entity-id (contains? valid-related-types rt))
              {:db (-> db
                     (assoc-in (conj modal-path :step) 2)
                     (assoc-in (conj modal-path :records) [])
                     (assoc-in (conj modal-path :selected-record) nil)
                     (assoc-in (conj modal-path :loading?) true)
                     (assoc-in (conj modal-path :error) nil))
               :http-xhrio (admin-http/admin-get
                             {:uri (str api-endpoint "/" entity-id "/related-records")
                              :params {:type rt :limit default-limit}
                              :on-success [(keyword event-ns "related-records-loaded") rt]
                              :on-failure [(keyword event-ns "related-records-load-failed") rt]})}
              {:db db}))

          2
          {:db (if (some? (get-in db (conj modal-path :selected-record)))
                 (-> db
                   (assoc-in (conj modal-path :step) 3)
                   (assoc-in (conj modal-path :error) nil))
                 db)}

          {:db db})))

    (rf/reg-event-db
      (keyword event-ns "related-records-loaded")
      (fn [db [_ requested-type response]]
        (let [active-type (get-in db (conj modal-path :selected-type))
              records (vec (or (:related-records response) []))]
          (if (= requested-type active-type)
            (-> db
              (assoc-in (conj modal-path :loading?) false)
              (assoc-in (conj modal-path :error) nil)
              (assoc-in (conj modal-path :records) records))
            db))))

    (rf/reg-event-db
      (keyword event-ns "related-records-load-failed")
      (fn [db [_ requested-type error]]
        (let [active-type (get-in db (conj modal-path :selected-type))]
          (if (= requested-type active-type)
            (-> db
              (assoc-in (conj modal-path :loading?) false)
              (assoc-in (conj modal-path :records) [])
              (assoc-in (conj modal-path :error) (admin-http/extract-error-message error)))
            db))))

    (rf/reg-event-db
      (keyword event-ns "select-related-record")
      (fn [db [_ record]]
        (-> db
          (assoc-in (conj modal-path :selected-record) record)
          (assoc-in (conj modal-path :error) nil))))

    (rf/reg-event-db
      (keyword event-ns "back-related-records-step")
      (fn [db _]
        (case (get-in db (conj modal-path :step))
          3 (-> db
              (assoc-in (conj modal-path :step) 2)
              (assoc-in (conj modal-path :selected-record) nil))
          2 (-> db
              (assoc-in (conj modal-path :step) 1)
              (assoc-in (conj modal-path :selected-type) nil)
              (assoc-in (conj modal-path :records) [])
              (assoc-in (conj modal-path :selected-record) nil)
              (assoc-in (conj modal-path :loading?) false)
              (assoc-in (conj modal-path :error) nil))
          db)))))
