(ns app.template.frontend.db.db
  (:require
    [app.shared.field-specs :refer [excluded-fields]]
    [malli.core :as m]
    [malli.error :as me]
    [re-frame.core :as re-frame]))

(goog-define ^boolean ENABLE_APP_DB_SPEC true)
(goog-define ^boolean STRICT_APP_DB_SPEC false)

(def ^:private validation-log-limit 5)
(def ^:private validation-log-window-ms 5000)
(defonce ^:private validation-log-state (volatile! {:window-start 0 :count 0}))

(defn- validation-enabled? []
  ENABLE_APP_DB_SPEC)

(defn- strict-validation-enabled? []
  (and (validation-enabled?) STRICT_APP_DB_SPEC))

(defn- now-ms []
  (.now js/Date))

(defn- allow-validation-log? []
  (let [timestamp (now-ms)
        {:keys [window-start count]} @validation-log-state
        window-elapsed? (> (- timestamp window-start) validation-log-window-ms)]
    (if window-elapsed?
      (do (vreset! validation-log-state {:window-start timestamp :count 1})
        true)
      (if (< count validation-log-limit)
        (do (vswap! validation-log-state update :count inc)
          true)
        false))))

(defn- log-validation-error! [strict? event exception]
  (when (allow-validation-log?)
    (let [event-id (when (vector? event) (first event))
          data (some-> (ex-data exception) (dissoc :db))
          schema-path (get-in data [:explanation :schema-path])
          payload (cond-> {:event event
                           :event-id event-id
                           :strict-mode? strict?
                           :message (ex-message exception)}
                    (:error data) (assoc :humanized (:error data))
                    schema-path (assoc :schema-path schema-path))
          log-fn (if strict? js/console.error js/console.warn)]
      (log-fn "app-db spec validation failed" (:event-id payload)))))

(def ^:private initialization-events
  #{:app.template.frontend.events.bootstrap/initialize-db
    :app.template.frontend.events.config/fetch-config-success
    :page/init-entities
    :page/init-login
    :page/init-logout
    :page/init-onboarding})

(defn- should-validate-event? [models-data event-id]
  (or models-data
    (nil? event-id)
    (not (contains? initialization-events event-id))))

(defn models-data->map
  "Normalizes models-data into a keyword-indexed map regardless of JSON/EDN shape."
  [md]
  (cond
    (map? md) md
    (vector? md) (into {} md)
    (seq? md) (into {} md)
    :else {}))

(defn normalize-entity-key
  "Ensures entity keys are keywords for consistent storage and spec generation."
  [k]
  (cond
    (keyword? k) k
    (string? k) (keyword k)
    :else k))

(defn compute-types-map
  "Build a lookup of `type-key → props` for every enum type declared in the
  models metadata. Works both for EDN-loaded maps (keyword keys) and the
  JSON-encoded `[k v]` vector form where keys and type names are strings."
  [md]
  (let [md-map (models-data->map md)]
    (->> md-map
      vals
      (mapcat (fn [table-def]
                ;; Each :types entry is [type-name base-type props]
                ;; where `type-name` may be string when coming from JSON.
                (map (fn [[type-name _ props]]
                       [(keyword type-name) props])
                  (:types table-def))))
      (into {}))))

(defn field-type
  "Field type function that takes models-data as parameter instead of using shared state"
  [field-def models-data]
  (let [types-map (compute-types-map models-data)]
    (if (vector? field-def)
      (let [[base-type & args] field-def
            ;; Handle both keyword and string base types (from JSON serialization)
            base-type-kw (if (keyword? base-type) base-type (keyword base-type))]
        (case base-type-kw
          :enum (if-let [enum-ref (first args)]
                  (let [enum-ref-kw (if (keyword? enum-ref) enum-ref (keyword enum-ref))]
                    (if-let [choices (get-in types-map [enum-ref-kw :choices])]
                      (into [:enum] choices)
                      :string))                             ; fallback if no choices found
                  :string)                                  ; fallback if no enum ref
          :varchar [:string {:max (first args)}]
          :decimal (if (> (count args) 1)
                     :double                                ; decimal with precision/scale
                     :double)                               ; simple decimal
          :array (if-let [element-type (first args)]
                   (let [element-type-kw (if (keyword? element-type) element-type (keyword element-type))]
                     [:vector (case element-type-kw
                                :uuid :string
                                :any)])
                   [:vector :any])
          base-type-kw))
      (let [field-def-kw (if (keyword? field-def) field-def (keyword field-def))]
        (case field-def-kw
          :text [:string {:min 0}]
          :integer :int
          :decimal :double
          :timestamp inst?
          :timestamptz inst?
          :jsonb [:map]
          :serial :int
          :uuid :string
          :inet :string
          :boolean :boolean
          :date inst?
          :any)))))

(def entity-id-schema
  [:or :int :string :keyword])

(def boolean-or-nil
  [:maybe :boolean])

(def keyword-or-string
  [:or :keyword :string])

(def entity-metadata-schema
  [:map {:closed false}
   [:loading? {:optional true} :boolean]
   [:error {:optional true} :any]
   [:last-updated {:optional true} :any]
   [:success {:optional true} :boolean]
   [:pending? {:optional true} :boolean]
   [:stats {:optional true} :any]
   [:context {:optional true} :any]])

(defn make-entity-structure [model models-data]
  (into [:map {:closed false}]
    (for [[k field-def] (:fields model)
          :when (not (excluded-fields k))]
      [k {:optional true} (field-type field-def models-data)])))

(defn make-entity-store-schema [_model _models-map]
  [:map {:closed false}
   [:data {:optional true} [:map-of entity-id-schema [:map-of keyword-or-string :any]]]
   [:ids {:optional true} [:vector entity-id-schema]]
   [:metadata {:optional true} entity-metadata-schema]])

(defn make-entities-structures [md]
  (let [models-map (models-data->map md)]
    (into [:map {:closed false}
           [:ui {:optional true} [:map-of :keyword :any]]
           [:specs {:optional true} [:map-of :keyword :any]]]
      (for [[k v] models-map
            :let [ek (normalize-entity-key k)]]
        [ek {:optional true} (make-entity-store-schema v models-map)]))))

(def models-data-schema
  [:maybe
   [:or
    [:map-of :any :any]
    [:vector [:tuple :any :any]]]])

(def validation-specs-schema
  [:maybe
   [:map-of [:or :keyword :string] :any]])

(def session-schema
  [:maybe
   [:map {:closed false}
    [:loading? {:optional true} :boolean]
    [:authenticated? {:optional true} :boolean]
    [:session-valid? {:optional true} :boolean]
    [:session-valid {:optional true} :boolean]
    [:legacy-session? {:optional true} :boolean]
    [:oauth2/access-tokens {:optional true} :any]
    [:ring.middleware.oauth2/access-tokens {:optional true} :any]
    [:provider {:optional true} [:maybe :string]]
    [:user {:optional true} [:maybe [:map-of :any :any]]]
    [:tenant {:optional true} [:maybe [:map-of :any :any]]]
    [:permissions {:optional true} [:maybe [:set :any]]]
    [:error {:optional true} [:maybe :string]]
    [:auth-session {:optional true} [:maybe [:map-of :any :any]]]]])

(def ui-controls-schema
  [:map {:closed false}
   [:show-timestamps-control? {:optional true} :boolean]
   [:show-edit-control? {:optional true} :boolean]
   [:show-delete-control? {:optional true} :boolean]
   [:show-highlights-control? {:optional true} :boolean]
   [:show-select-control? {:optional true} :boolean]
   [:show-filtering-control? {:optional true} :boolean]
   [:show-invert-selection? {:optional true} :boolean]
   [:show-delete-selected? {:optional true} :boolean]])

(def ui-search-schema
  [:map {:closed false}
   [:term {:optional true} [:maybe :string]]
   [:columns {:optional true} [:vector keyword-or-string]]
   [:pending? {:optional true} :boolean]
   [:mode {:optional true} [:maybe :keyword]]
   [:last-executed {:optional true} :any]])

(def ui-column-visibility-schema
  [:map-of keyword-or-string :boolean])

(def ui-defaults-schema
  [:map {:closed false}
   [:show-timestamps? {:optional true} boolean-or-nil]
   [:show-edit? {:optional true} boolean-or-nil]
   [:show-delete? {:optional true} boolean-or-nil]
   [:show-highlights? {:optional true} boolean-or-nil]
   [:show-select? {:optional true} boolean-or-nil]
   [:show-filtering? {:optional true} boolean-or-nil]
   [:show-pagination? {:optional true} boolean-or-nil]
   [:controls {:optional true} ui-controls-schema]])

(def ui-entity-config-schema
  [:map {:closed false}
   [:show-timestamps? {:optional true} boolean-or-nil]
   [:show-edit? {:optional true} boolean-or-nil]
   [:show-delete? {:optional true} boolean-or-nil]
   [:show-highlights? {:optional true} boolean-or-nil]
   [:show-select? {:optional true} boolean-or-nil]
   [:show-filtering? {:optional true} boolean-or-nil]
   [:show-pagination? {:optional true} boolean-or-nil]
   [:controls {:optional true} ui-controls-schema]
   [:filterable-fields {:optional true} [:map-of :keyword :any]]
   [:visible-columns {:optional true} ui-column-visibility-schema]
   [:column-order {:optional true} [:vector keyword-or-string]]
   [:fields {:optional true} [:vector :any]]
   [:effective-spec {:optional true} [:maybe [:map-of :keyword :any]]]
   [:search {:optional true} ui-search-schema]
   [:overrides {:optional true} [:map-of :keyword :any]]
   [:defaults {:optional true} [:map-of :keyword :any]]])

(def ui-pagination-schema
  [:map {:closed false}
   [:current-page {:optional true} :int]
   [:per-page {:optional true} :int]
   [:total-items {:optional true} :int]])

(def ui-filter-modal-schema
  [:map {:closed false}
   [:open? {:optional true} :boolean]
   [:entity {:optional true} [:maybe :keyword]]
   [:field {:optional true} [:maybe :keyword]]
   [:context {:optional true} :any]])

(def ui-batch-edit-inline-schema
  [:map-of :keyword
   [:map {:closed false}
    [:entity-type {:optional true} [:maybe :keyword]]
    [:selected-ids {:optional true} [:set :any]]
    [:open? {:optional true} :boolean]
    [:form {:optional true} [:map-of :keyword :any]]]])

(def ui-notifications-schema
  [:vector
   [:map {:closed false}
    [:id {:optional true} [:or :keyword :string]]
    [:type {:optional true} [:maybe :keyword]]
    [:level {:optional true} [:maybe :keyword]]
    [:message {:optional true} :any]
    [:timestamp {:optional true} :any]
    [:context {:optional true} :any]]])

(def ui-sidebar-schema
  [:map {:closed false}
   [:collapsed? {:optional true} :boolean]
   [:section {:optional true} [:maybe :keyword]]
   [:open-panels {:optional true} [:set :keyword]]])

(def default-session-state
  {:loading? false
   :authenticated? false
   :session-valid? true
   :legacy-session? false
   :permissions #{}})

(def critical-state-schema
  [:map
   [:models-data {:optional true} models-data-schema]
   [:session {:optional true} session-schema]
   [:validation-specs {:optional true} validation-specs-schema]])

(defn- debug-validate-critical-state [db]
  (when ^boolean goog.DEBUG
    (when-let [_error (m/explain critical-state-schema db)]
      nil))
  db)

(def list-ui-state
  [:map {:closed false}
   [:current-page {:optional true} :int]
   [:per-page {:optional true} :int]
   [:total-items {:optional true} :int]
   [:pagination {:optional true} ui-pagination-schema]
   [:sort {:optional true}
    [:map {:closed false}
     [:field {:optional true} :keyword]
     [:direction {:optional true} [:enum :asc :desc]]]]
   [:selected-ids {:optional true} [:set :any]]
   [:filters {:optional true} [:map-of :keyword :any]]
   [:active-filters {:optional true} [:map-of :keyword :any]]
   [:search {:optional true} ui-search-schema]
   [:column-visibility {:optional true} ui-column-visibility-schema]
   [:filter-modal {:optional true} ui-filter-modal-schema]
   [:batch-edit {:optional true}
    [:map {:closed false}
     [:popup {:optional true} [:map {:closed false} [:open? {:optional true} :boolean]]]
     [:inline {:optional true} [:map {:closed false} [:open? {:optional true} :boolean]]]]]
   [:loading? {:optional true} :boolean]
   [:error {:optional true} :any]
   [:last-refreshed-at {:optional true} :any]])

(def form-state
  [:map
   [:data {:optional true} [:or [:map-of :keyword :any] [:map-of :int :any]]]
   [:errors {:optional true} [:or :any [:map-of :keyword :any]]]
   [:server-errors {:optional true} [:or :any [:map-of :keyword :any]]]
   [:submitting? {:optional true} :boolean]
   [:submitted? {:optional true} :boolean]
   [:success {:optional true} [:or :boolean [:map-of :keyword :boolean]]]
   [:dirty-fields {:optional true} [:set :keyword]]
   [:waiting {:optional true} [:set :keyword]]])

(defn make-default-list-state []
  {:current-page 1
   :per-page 10
   :total-items 0
   :pagination {:current-page 1
                :per-page 10
                :total-items 0}
   :sort {:field :id
          :direction :asc}
   :selected-ids #{}
   :filters {}
   :active-filters {}
   :search {:term nil
            :columns []
            :pending? false}
   :column-visibility {}
   :filter-modal {:open? false}
   :batch-edit {:popup {:open? false}
                :inline {:open? false}}
   :loading? false
   :error nil
   :last-refreshed-at nil})

(defn make-default-entity-configs
  "Construct per-entity UI configuration placeholders keyed by entity keyword.
  Keeps toggles nil so defaults fall back to global values while ensuring maps exist."
  [md]
  (let [md-map (models-data->map md)]
    (into {}
      (for [[entity-key _] md-map
            :let [ek (normalize-entity-key entity-key)]]
        [ek {:show-timestamps? nil
             :show-edit? nil
             :show-delete? nil
             :show-highlights? nil
             :show-select? nil
             :show-filtering? nil
             :show-pagination? nil
             :controls {}
             :filterable-fields {}
             :visible-columns {}
             :column-order []
             :fields []
             :effective-spec nil
             :search {:term nil :columns []}
             :overrides {}
             :defaults {}}]))))

(defn make-default-entities [md]
  (let [md-map (models-data->map md)
        entities (into {}
                   (for [[k _] md-map
                         :let [ek (normalize-entity-key k)]]
                     [ek {:data {}
                          :ids []
                          :metadata {:loading? false
                                     :error nil
                                     :last-updated nil}}]))
        first-entity (some-> md-map keys first normalize-entity-key)]
    (assoc entities
      :ui {:entity-name first-entity
           :current-page nil
           :theme "light"}
      :specs {})))

(def default-db
  {:current-route nil
   :controllers []
   :entities {}                                             ; Will be populated when models-data is available
   :models-data nil
   :specs {}                                                ; Initialize specs map
   :validation-specs {}                                     ; Initialize validation specs map
   :session default-session-state                           ; Seed session envelope with safe defaults
   :forms {}
   :ui {:current-page nil
        :entity-name nil
        :current-entity-type nil
        :theme "light"
        :show-add-form false
        :editing nil
        :editing-id nil
        :filter-modal {:open? false}
        :batch-edit {:active? false}
        :batch-edit-popup {:open? false}
        :batch-edit-inline {}
        :recently-updated {}                                ; Initialize the recently-updated map
        :recently-created {}                                ; Initialize the recently-created map
        :show-timestamps? false                             ;; Default to hiding timestamp columns
        :show-edit? true                                    ;; Default to showing edit action
        :show-delete? true                                  ;; Default to showing delete action
        :show-highlights? true                              ;; Default to showing highlights
        :show-select? false                                 ;; Default to hiding select checkboxes
        :show-filtering? true                               ;; Default to enabling filtering controls
        :show-pagination? true                              ;; Default to enabling pagination controls
        :defaults {}                                        ;; Initialize defaults map
        :controls {}                                        ;; Initialize controls map
        :entity-configs {}                                  ;; Initialize entity-configs map
        :lists {}                                           ;; Will be populated when models-data is available
        :notifications []
        :sidebar {:collapsed? false}
        :modals {}
        :toasts []}
   :csrf-token nil})

(defn make-db-with-models-data
  "Creates a complete database with models-data populated"
  [base-db models-data]
  (let [models-map (models-data->map models-data)
        entity-keys (->> models-map keys (map normalize-entity-key))
        default-configs (make-default-entity-configs models-map)
        existing-configs (get-in base-db [:ui :entity-configs] {})
        merged-configs (merge-with
                         (fn [defaults overrides]
                           (if (and (map? defaults) (map? overrides))
                             (merge defaults overrides)
                             (or overrides defaults)))
                         default-configs
                         existing-configs)
        lists (into {}
                (for [entity-key entity-keys]
                  [entity-key (make-default-list-state)]))
        first-entity (first entity-keys)]
    (-> base-db
      (assoc :models-data models-data)
      (assoc :entities (make-default-entities models-map))
      (assoc-in [:ui :lists] lists)
      (assoc-in [:ui :entity-configs] merged-configs)
      (update-in [:ui :entity-name] #(or % first-entity))
      (update-in [:ui :current-entity-type] #(or % first-entity))
      (debug-validate-critical-state))))

(defn make-ui-lists-schema [md]
  (let [md-map (models-data->map md)]
    (if (seq md-map)
      (into [:map {:closed false}]
        (for [[k _] md-map
              :let [ek (normalize-entity-key k)]]
          [ek {:optional true} list-ui-state]))
      [:map-of :keyword :any])))

(defn make-app-db-schema
  "Creates app-db schema with optional models-data"
  [models-data]
  (let [entities-schema (if models-data
                          (make-entities-structures models-data)
                          [:map-of :keyword :any])
        lists-schema (if models-data
                       (make-ui-lists-schema models-data)
                       [:map-of :keyword :any])]
    [:map
     [:current-route {:optional true} [:maybe [:map
                                               [:template {:optional true} :string]
                                               [:data {:optional true} :map]
                                               [:result {:optional true} :any
                                                [:path-params {:optional true} [:map-of :keyword :any]]]
                                               [:query-params {:optional true} [:map-of :keyword :any]]
                                               [:fragment {:optional true} [:maybe :string]]
                                               [:parameters {:optional true} :map]
                                               [:path {:optional true} :string]]]]
     [:controllers {:optional true} [:vector [:map
                                              [:start {:optional true} fn?]
                                              [:stop {:optional true} fn?]
                                              [:reitit.frontend.controllers/identity {:optional true} [:maybe :any]]]]]
     [:entities {:optional true} entities-schema]
     [:models-data {:optional true} models-data-schema]
     [:validation-specs {:optional true} validation-specs-schema]
     [:session {:optional true} session-schema]
     [:specs {:optional true} [:map-of :keyword :any]]
     [:ui {:optional true}
      [:map {:closed false}
       [:current-page {:optional true} [:maybe :keyword]]
       [:entity-name {:optional true} [:maybe :keyword]]
       [:current-entity-type {:optional true} [:maybe :keyword]]
       [:theme {:optional true} [:maybe :string]]
       [:show-add-form {:optional true} :boolean]
       [:editing {:optional true} :any]
       [:editing-id {:optional true} :any]
       [:filter-modal {:optional true} ui-filter-modal-schema]
       [:batch-edit {:optional true}
        [:map {:closed false}
         [:active? {:optional true} :boolean]
         [:context {:optional true} :any]]]
       [:batch-edit-popup {:optional true}
        [:map {:closed false}
         [:open? {:optional true} :boolean]
         [:entity-type {:optional true} [:maybe :keyword]]
         [:context {:optional true} :any]]]
       [:batch-edit-inline {:optional true} ui-batch-edit-inline-schema]
       [:recently-updated {:optional true} [:map-of :keyword [:set :any]]]
       [:recently-created {:optional true} [:map-of :keyword [:set :any]]]
       [:show-timestamps? {:optional true} :boolean]
       [:show-edit? {:optional true} :boolean]
       [:show-delete? {:optional true} :boolean]
       [:show-highlights? {:optional true} :boolean]
       [:show-select? {:optional true} :boolean]
       [:show-filtering? {:optional true} :boolean]
       [:show-pagination? {:optional true} :boolean]
       [:lists {:optional true} lists-schema]
       [:defaults {:optional true} ui-defaults-schema]
       [:controls {:optional true} ui-controls-schema]
       [:entity-configs {:optional true} [:map-of :keyword ui-entity-config-schema]]
       [:notifications {:optional true} ui-notifications-schema]
       [:sidebar {:optional true} ui-sidebar-schema]
       [:modals {:optional true} [:map-of :keyword :any]]
       [:toasts {:optional true} ui-notifications-schema]]]
     [:forms {:optional true} [:map-of :keyword form-state]]
     [:csrf-token {:optional true} :any]]))

(def app-db-schema
  "Default schema for app-db without models-data"
  (make-app-db-schema nil))

(defn validate-db
  "Validates the db against the schema. Returns the db if valid, throws an error if not."
  ([db]
   (validate-db db nil))
  ([db models-data]
   (let [schema (if models-data
                  (make-app-db-schema models-data)
                  app-db-schema)]
     (if-let [error (m/explain schema db)]
       (let [humanized (me/humanize error)
             error-details (-> error
                             (dissoc :value)
                             (assoc :schema-path (->> error :errors (mapv :path))))]
         (when ^boolean goog.DEBUG
           nil)
         (throw (ex-info "app-db validation failed"
                  {:error humanized
                   :explanation error-details
                   :db db})))
       db))))

;; Interceptors
(def check-spec-interceptor
  (re-frame/->interceptor
    :id :check-spec
    :after (fn [context]
             (let [db (get-in context [:effects :db])]
               (when (and (validation-enabled?) db)
                 (let [event (get-in context [:coeffects :event])
                       event-id (when (vector? event) (first event))
                       models-data (:models-data db)]
                   (when (should-validate-event? models-data event-id)
                     (try
                       (validate-db db models-data)
                       (catch :default exception
                         (let [strict? (strict-validation-enabled?)
                               initialization-event? (contains? initialization-events event-id)]
                           (log-validation-error! strict? event exception)
                           (when (and strict? (not initialization-event?))
                             (throw exception)))))))))
             context)))

(def common-interceptors
  (cond-> [re-frame/trim-v]
    ^boolean goog.DEBUG (conj re-frame/debug)
    (validation-enabled?) (conj check-spec-interceptor)))
