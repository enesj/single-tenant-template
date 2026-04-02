(ns app.domain.frontend.expenses.events.user-expenses.unmapped-aliases
  "User-facing unmapped aliases list events.

  These events call GET /api/v1/expenses/articles/unmapped-aliases and sync the
  results into the shared template entity store so tenant routes can reuse the
  same list-view UI as admin while remaining read-only."
  (:require
    [app.domain.frontend.expenses.admin.adapters.sync :as expenses-sync]
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.xhrio :as x]
    [app.shared.pagination :as pagination]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.db.paths :as paths]
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

(defn- current-unmapped-aliases-page-params
  [db]
  (let [entity-key :unmapped-aliases
        per-page (paths/resolved-list-per-page db entity-key pagination/default-page-size)
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
  :user-expenses/refresh-unmapped-aliases-list
  common-interceptors
  (fn [{:keys [db]} [_ opts]]
    {:dispatch [:user-expenses/fetch-unmapped-aliases
                (merge (current-unmapped-aliases-page-params db)
                  (when (map? opts) opts))]}))

(rf/reg-event-fx
  :user-expenses/fetch-unmapped-aliases
  common-interceptors
  (fn [{:keys [db]} [_ params]]
    (let [request-params (merge {:limit 50 :offset 0} (when (map? params) params))]
      {:db (begin-entity-load db :unmapped-aliases)
       :http-xhrio (x/xhrio db
                     {:method :get
                      :uri endpoints/articles-unmapped-aliases-endpoint
                      :params request-params
                      :on-success [:user-expenses/fetch-unmapped-aliases-success]
                      :on-failure [:user-expenses/fetch-unmapped-aliases-failure]})})))

(rf/reg-event-fx
  :user-expenses/fetch-unmapped-aliases-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [rows (vec (or (:data response) []))
          total (or (:total response) (count rows))]
      {:db (-> (finish-entity-load db :unmapped-aliases nil)
             (assoc-in (paths/list-total-items :unmapped-aliases) total))
       :dispatch [::expenses-sync/sync-unmapped-aliases rows]})))

(rf/reg-event-db
  :user-expenses/fetch-unmapped-aliases-failure
  common-interceptors
  (fn [db [error]]
    (finish-entity-load db :unmapped-aliases error)))
