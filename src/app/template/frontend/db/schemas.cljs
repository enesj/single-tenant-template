(ns app.template.frontend.db.schemas
  (:require
    [app.shared.field-specs :refer [excluded-fields]]))

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
                      :string))
                  :string)
          :varchar [:string {:max (first args)}]
          :decimal (if (> (count args) 1)
                     :double
                     :double)
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

(defn make-entity-structure
  [model models-data]
  (into [:map {:closed false}]
    (for [[k field-def] (:fields model)
          :when (not (excluded-fields k))]
      [k {:optional true} (field-type field-def models-data)])))

(defn make-entity-store-schema
  [_model _models-map]
  [:map {:closed false}
   [:data {:optional true} [:map-of entity-id-schema [:map-of keyword-or-string :any]]]
   [:ids {:optional true} [:vector entity-id-schema]]
   [:metadata {:optional true} entity-metadata-schema]])

(defn make-entities-structures
  [md]
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

(def critical-state-schema
  [:map
   [:models-data {:optional true} models-data-schema]
   [:session {:optional true} session-schema]
   [:validation-specs {:optional true} validation-specs-schema]])

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
   [:values {:optional true} [:or [:map-of :keyword :any] [:map-of :int :any]]]
   [:errors {:optional true} [:or :any [:map-of :keyword :any]]]
   [:server-errors {:optional true} [:or :any [:map-of :keyword :any]]]
   [:submitting? {:optional true} :boolean]
   [:submitted? {:optional true} :boolean]
   [:success {:optional true} [:or :boolean [:map-of :keyword :boolean]]]
   [:dirty-fields {:optional true} [:set :keyword]]
   [:waiting {:optional true} [:set :keyword]]])

(defn make-ui-lists-schema
  [md]
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
     [:entity-prefs {:optional true} [:map-of :keyword :any]]
     [:notifications {:optional true} ui-notifications-schema]
     [:sidebar {:optional true} ui-sidebar-schema]
     [:modals {:optional true} [:map-of :keyword :any]]
     [:toasts {:optional true} ui-notifications-schema]]]
     [:forms {:optional true} [:map-of :keyword form-state]]
     [:entity-fetches {:optional true} [:map-of :keyword [:map-of :string :boolean]]]
     [:csrf-token {:optional true} :any]]))

(def app-db-schema
  "Default schema for app-db without models-data"
  (make-app-db-schema nil))
