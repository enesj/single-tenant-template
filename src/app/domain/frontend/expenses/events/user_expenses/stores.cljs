(ns app.domain.frontend.expenses.events.user-expenses.stores
  "User-facing stores + store-aliases list + CRUD (admin/owner only).

  These events call /api/v1/expenses/stores and /api/v1/expenses/store-aliases and
  sync results into the shared template entity store so list-view can render them."
  (:require
    [app.domain.frontend.expenses.admin.adapters.sync :as expenses-sync]
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.xhrio :as x]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.shared.crud.success :as crud-success]
    [re-frame.core :as rf]))

(defn- begin-entity-load
  [db entity-type]
  (-> db
    (assoc-in (paths/entity-loading? entity-type) true)
    (assoc-in (paths/entity-error entity-type) nil)))

(defn- finish-entity-load
  [db entity-type error]
  (-> db
    (assoc-in (paths/entity-loading? entity-type) false)
    (assoc-in (paths/entity-error entity-type) (when error (http/extract-error-message error)))))

(defn- ->id-str
  [id]
  (some-> id str))

(defn- flatten-ui-filters
  "Flatten shared-list UI filter values into flat backend query params.
   Date ranges {:from x :to y} → <field>-from / <field>-to.
   Number ranges {:min x :max y} → <field>-min / <field>-max.
   Select maps {:value v :label _} → scalar v.
   Nil/empty string values are omitted."
  [filters]
  (let [lookup-map-value (fn [m k]
                           (if (contains? m k)
                             (get m k)
                             (get m (name k))))
        normalize-select-item (fn [item]
                                (cond
                                  (map? item) (lookup-map-value item :value)
                                  (keyword? item) (name item)
                                  :else item))]
    (reduce-kv
      (fn [acc k v]
        (cond
          (nil? v) acc
          (and (string? v) (empty? v)) acc

          ;; Date range → <field>-from / <field>-to
          (and (map? v)
            (or (contains? v :from) (contains? v "from")
              (contains? v :to) (contains? v "to")))
          (let [field-name (name k)
                from-val (lookup-map-value v :from)
                to-val (lookup-map-value v :to)
                ->query-value (fn [x]
                                (cond
                                  (instance? js/Date x) (.toISOString x)
                                  (some? x) (str x)
                                  :else nil))]
            (cond-> acc
              (some? from-val) (assoc (keyword (str field-name "-from")) (->query-value from-val))
              (some? to-val) (assoc (keyword (str field-name "-to")) (->query-value to-val))))

          ;; Number range → <field>-min / <field>-max
          (and (map? v)
            (or (contains? v :min) (contains? v "min")
              (contains? v :max) (contains? v "max")))
          (let [field-name (name k)
                min-val (lookup-map-value v :min)
                max-val (lookup-map-value v :max)]
            (cond-> acc
              (some? min-val) (assoc (keyword (str field-name "-min")) min-val)
              (some? max-val) (assoc (keyword (str field-name "-max")) max-val)))

          ;; Multi-select values → scalar for one selection, vector for many
          (vector? v)
          (let [selected-values (->> v
                                  (map normalize-select-item)
                                  (filter some?)
                                  vec)]
            (cond
              (empty? selected-values) acc
              (= 1 (count selected-values)) (assoc acc (keyword (name k)) (first selected-values))
              :else (assoc acc (keyword (name k)) selected-values)))

          ;; Select → extract :value
          (and (map? v) (or (contains? v :value) (contains? v "value")))
          (let [selected-value (lookup-map-value v :value)]
            (if (some? selected-value)
              (assoc acc (keyword (name k)) selected-value)
              acc))

          ;; Scalar (string, number, boolean) → pass through
          :else (assoc acc (keyword (name k)) v)))
      {}
      filters)))

(defn- current-list-page-params
  [db entity-key default-limit]
  (let [per-page (paths/resolved-list-per-page db entity-key default-limit)
        current-page (paths/resolved-list-current-page db entity-key)
        sort-config (or (get-in db [:ui :lists entity-key :sort]) {})
        order-dir (let [direction (:direction sort-config)]
                    (when (contains? #{:asc :desc "asc" "desc"} direction)
                      (name (keyword direction))))
        order-field (when-let [f (:field sort-config)] (name f))
        filters (flatten-ui-filters (or (get-in db [:ui :lists entity-key :filters]) {}))]
    (cond-> (merge {:limit per-page
                    :offset (* (max 0 (dec current-page)) per-page)}
              filters)
      (some? order-dir) (assoc :order-dir order-dir)
      (some? order-field) (assoc :order-by order-field))))

(rf/reg-event-fx
  :user-expenses/refresh-stores-list
  common-interceptors
  (fn [{:keys [db]} [opts]]
    {:dispatch [:user-expenses/fetch-stores (merge (current-list-page-params db :stores 200)
                                              (when (map? opts) opts))]}))

(rf/reg-event-fx
  :user-expenses/refresh-store-aliases-list
  common-interceptors
  (fn [{:keys [db]} [opts]]
    {:dispatch [:user-expenses/fetch-store-aliases (merge (current-list-page-params db :store-aliases 200)
                                                     (when (map? opts) opts))]}))

;; ---------------------------------------------------------------------------
;; Stores
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-stores
  common-interceptors
  (fn [{:keys [db]} [params]]
    (let [request-params (merge {:limit 200 :offset 0} (when (map? params) params))]
      {:db (begin-entity-load db :stores)
       :http-xhrio (x/xhrio db
                     {:method :get
                      :uri endpoints/stores-endpoint
                      :params request-params
                      :on-success [:user-expenses/fetch-stores-success]
                      :on-failure [:user-expenses/fetch-stores-failure]})})))

(rf/reg-event-fx
  :user-expenses/fetch-stores-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [stores (vec (or (:data response) []))
          total (or (:total response) (count stores))
          date-highlights (:date-highlights response)]
      {:db (cond-> (-> (finish-entity-load db :stores nil)
                     (assoc-in (paths/list-total-items :stores) total))
             (map? date-highlights)
             (assoc-in (conj (paths/list-ui-state :stores) :date-highlights) date-highlights))
       :dispatch [::expenses-sync/sync-stores stores]}))),{}

(rf/reg-event-db
  :user-expenses/fetch-stores-failure
  common-interceptors
  (fn [db [error]]
    (finish-entity-load db :stores error)))

(rf/reg-event-fx
  :user-expenses/create-store-modal
  common-interceptors
  (fn [{:keys [db]} [form-data on-success]]
    {:db (-> db
           (assoc-in [:user-expenses :form :loading?] true)
           (assoc-in [:user-expenses :form :error] nil))
     :http-xhrio (x/xhrio db
                   {:method :post
                    :uri endpoints/stores-endpoint
                    :params (or form-data {})
                    :on-success [:user-expenses/create-store-modal-success on-success]
                    :on-failure [:user-expenses/create-store-modal-failure]})}))

(rf/reg-event-fx
  :user-expenses/create-store-modal-success
  common-interceptors
  (fn [{:keys [db]} [on-success response]]
    (let [store (:data response)
          store-id (:id store)
          highlight-id (some-> store-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] false)
             (assoc-in [:user-expenses :form :error] nil)
             (cond-> highlight-id
               (crud-success/track-recently-created :stores highlight-id)))
       :dispatch-n [[:user-expenses/refresh-stores-list]]
       :fx [(when on-success
              [:dispatch-later {:ms 100
                                :dispatch [:user-expenses/call-modal-callback on-success store]}])]})))

(rf/reg-event-db
  :user-expenses/create-store-modal-failure
  common-interceptors
  (fn [db [error]]
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

(rf/reg-event-fx
  :user-expenses/update-store-modal
  common-interceptors
  (fn [{:keys [db]} [store-id form-data on-success]]
    (let [store-id-str (->id-str store-id)
          {:keys [display_name display-name address]} (or form-data {})
          payload (cond-> {}
                    (some? display-name) (assoc :display-name display-name)
                    (some? display_name) (assoc :display-name display_name)
                    (some? address) (assoc :address address))]

      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] true)
             (assoc-in [:user-expenses :form :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :put
                      :uri (str endpoints/stores-endpoint "/" store-id-str)
                      :params payload
                      :on-success [:user-expenses/update-store-modal-success store-id-str on-success]
                      :on-failure [:user-expenses/update-store-modal-failure]})})))

(rf/reg-event-fx
  :user-expenses/update-store-modal-success
  common-interceptors
  (fn [{:keys [db]} [store-id on-success _response]]
    (let [highlight-id (some-> store-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] false)
             (assoc-in [:user-expenses :form :error] nil)
             (cond-> (seq highlight-id)
               (crud-success/track-recently-updated :stores highlight-id)))
       :dispatch-n [[:user-expenses/refresh-stores-list]]
       :fx [(when on-success
              [:dispatch-later {:ms 100
                                :dispatch [:user-expenses/call-modal-callback on-success]}])]})))

(rf/reg-event-db
  :user-expenses/update-store-modal-failure
  common-interceptors
  (fn [db [error]]
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

(rf/reg-event-fx
  :user-expenses/delete-store
  common-interceptors
  (fn [{:keys [db]} [store-id]]
    (let [store-id-str (->id-str store-id)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] true)
             (assoc-in [:user-expenses :form :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :delete
                      :uri (str endpoints/stores-endpoint "/batch")
                      :params {:ids [store-id-str]}
                      :on-success [:user-expenses/delete-store-success]
                      :on-failure [:user-expenses/delete-store-failure]})})))

(rf/reg-event-fx
  :user-expenses/delete-store-success
  common-interceptors
  (fn [{:keys [db]} [_response]]
    {:db (assoc-in db [:user-expenses :form :loading?] false)
     :dispatch [:user-expenses/refresh-stores-list]}))

(rf/reg-event-db
  :user-expenses/delete-store-failure
  common-interceptors
  (fn [db [error]]
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Store aliases
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-store-aliases
  common-interceptors
  (fn [{:keys [db]} [params]]
    (let [request-params (merge {:limit 200 :offset 0} (when (map? params) params))]
      {:db (begin-entity-load db :store-aliases)
       :http-xhrio (x/xhrio db
                     {:method :get
                      :uri endpoints/store-aliases-endpoint
                      :params request-params
                      :on-success [:user-expenses/fetch-store-aliases-success]
                      :on-failure [:user-expenses/fetch-store-aliases-failure]})})))

(rf/reg-event-fx
  :user-expenses/fetch-store-aliases-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [aliases (vec (or (:data response) []))
          total (or (:total response) (count aliases))
          date-highlights (:date-highlights response)]
      {:db (cond-> (-> (finish-entity-load db :store-aliases nil)
                     (assoc-in (paths/list-total-items :store-aliases) total))
             (map? date-highlights)
             (assoc-in (conj (paths/list-ui-state :store-aliases) :date-highlights) date-highlights))
       :dispatch [::expenses-sync/sync-store-aliases aliases]})))

(rf/reg-event-db
  :user-expenses/fetch-store-aliases-failure
  common-interceptors
  (fn [db [error]]
    (finish-entity-load db :store-aliases error)))

(rf/reg-event-fx
  :user-expenses/update-store-alias-modal
  common-interceptors
  (fn [{:keys [db]} [store-alias-id form-data on-success]]
    (let [store-alias-id-str (->id-str store-alias-id)
          {:keys [raw_label raw-label
                  raw_label_normalized raw-label-normalized
                  store_id store-id
                  confidence]} (or form-data {})
          payload (cond-> {}
                    (some? raw-label) (assoc :raw-label raw-label)
                    (some? raw_label) (assoc :raw-label raw_label)
                    (some? raw-label-normalized) (assoc :raw-label-normalized raw-label-normalized)
                    (some? raw_label_normalized) (assoc :raw-label-normalized raw_label_normalized)
                    (some? store-id) (assoc :store-id store-id)
                    (some? store_id) (assoc :store-id store_id)
                    (some? confidence) (assoc :confidence confidence))]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] true)
             (assoc-in [:user-expenses :form :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :put
                      :uri (str endpoints/store-aliases-endpoint "/" store-alias-id-str)
                      :params payload
                      :on-success [:user-expenses/update-store-alias-modal-success store-alias-id-str on-success]
                      :on-failure [:user-expenses/update-store-alias-modal-failure]})})))

(rf/reg-event-fx
  :user-expenses/update-store-alias-modal-success
  common-interceptors
  (fn [{:keys [db]} [store-alias-id on-success _response]]
    (let [highlight-id (some-> store-alias-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] false)
             (assoc-in [:user-expenses :form :error] nil)
             (cond-> (seq highlight-id)
               (crud-success/track-recently-updated :store-aliases highlight-id)))
       :dispatch-n [[:user-expenses/refresh-store-aliases-list]]
       :fx [(when on-success
              [:dispatch-later {:ms 100
                                :dispatch [:user-expenses/call-modal-callback on-success]}])]})))

(rf/reg-event-db
  :user-expenses/update-store-alias-modal-failure
  common-interceptors
  (fn [db [error]]
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

(rf/reg-event-fx
  :user-expenses/delete-store-alias
  common-interceptors
  (fn [{:keys [db]} [store-alias-id]]
    (let [store-alias-id-str (->id-str store-alias-id)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] true)
             (assoc-in [:user-expenses :form :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :delete
                      :uri (str endpoints/store-aliases-endpoint "/batch")
                      :params {:ids [store-alias-id-str]}
                      :on-success [:user-expenses/delete-store-alias-success]
                      :on-failure [:user-expenses/delete-store-alias-failure]})})))

(rf/reg-event-fx
  :user-expenses/delete-store-alias-success
  common-interceptors
  (fn [{:keys [db]} [_response]]
    {:db (assoc-in db [:user-expenses :form :loading?] false)
     :dispatch [:user-expenses/refresh-store-aliases-list]}))

(rf/reg-event-db
  :user-expenses/delete-store-alias-failure
  common-interceptors
  (fn [db [error]]
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

(rf/reg-event-fx
  :user-expenses/batch-delete-store-aliases
  common-interceptors
  (fn [{:keys [db]} [ids]]
    (let [id-strs (mapv ->id-str ids)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] true)
             (assoc-in [:user-expenses :form :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :delete
                      :uri (str endpoints/store-aliases-endpoint "/batch")
                      :params {:ids id-strs}
                      :on-success [:user-expenses/delete-store-alias-success]
                      :on-failure [:user-expenses/delete-store-alias-failure]})})))

