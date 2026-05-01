(ns app.template.frontend.db.paths
  (:require [app.shared.model-naming :as model-naming]
    [clojure.string :as str]))

;; Navigation & Routing
(defn current-route
  "Returns [:current-route] path vector for the current route in the application state."
  []
  [:current-route])

(defn current-route-name
  "Returns [:current-route :data :name] path vector for the current route name."
  []
  [:current-route :data :name])

(defn admin-route?
  "Returns true when the current route stored in db is an admin route.
  Admin routes have names that start with \"admin\" (e.g. :admin/users)."
  [db]
  (let [route-name (get-in db (current-route-name))
        route-str (cond
                    (keyword? route-name)
                    (if-let [route-ns (namespace route-name)]
                      (str route-ns "/" (name route-name))
                      (name route-name))

                    (string? route-name)
                    route-name

                    :else nil)]
    (boolean (and route-str (str/starts-with? route-str "admin")))))

(defn current-page
  "Returns [:ui :current-page] path vector for the current page in the UI state."
  []
  [:ui :current-page])

(defn entity-prefs-key
  "Returns the route-scoped entity key used for persisted UI preferences.

  Admin routes store browser-local overrides under :admin/<entity> so admin
  list settings do not bleed into the matching user-facing pages."
  [db entity-name]
  (when-let [entity-key (model-naming/ensure-app-keyword entity-name)]
    (if (and (admin-route? db)
          (not= "admin" (namespace entity-key))
          (nil? (namespace entity-key)))
      (keyword "admin" (name entity-key))
      entity-key)))

;; Entity paths
(defn entity-data
  "Returns [:entities entity-type :data] path vector for entity data of a specific entity type."
  [entity-type]
  [:entities entity-type :data])

(defn entity-ids
  "Returns [:entities entity-type :ids] path vector for entity IDs of a specific entity type."
  [entity-type]
  [:entities entity-type :ids])

(defn entity-metadata
  "Returns [:entities entity-type :metadata] path vector for entity metadata of a specific entity type."
  [entity-type]
  [:entities entity-type :metadata])

(defn entity-loading?
  "Returns [:entities entity-type :metadata :loading?] path vector for the loading state of a specific entity type."
  [entity-type]
  [:entities entity-type :metadata :loading?])

(defn entity-error
  "Returns [:entities entity-type :metadata :error] path vector for error state of a specific entity type."
  [entity-type]
  [:entities entity-type :metadata :error])

(defn entity-last-updated
  "Returns [:entities entity-type :metadata :last-updated] path vector for the last-updated marker of a specific entity type."
  [entity-type]
  [:entities entity-type :metadata :last-updated])

(defn entity-success
  "Returns [:entities entity-type :metadata :success] path vector for the success state of a specific entity type."
  [entity-type]
  [:entities entity-type :metadata :success])

;; Form paths
(defn form-values
  "Returns [:forms entity-type :values] path vector for form values of a specific entity type."
  [entity-type]
  [:forms entity-type :values])

(defn form-data
  "Returns the canonical form data path for an entity.

  NOTE: `:values` is the canonical storage key (Fork-compatible). This helper is
  kept as a compatibility alias for older code/tests that used `form-data`."
  [entity-type]
  (form-values entity-type))

(defn form-field
  "Returns [:forms entity-type :values field] path vector for a single form field.

  Compatibility alias for older code that used `:data` under forms."
  [entity-type field]
  (conj (form-data entity-type) field))

(defn form-errors
  "Returns [:forms entity-type :errors] path vector for form validation errors of a specific entity type."
  [entity-type]
  [:forms entity-type :errors])

(defn form-field-error
  "Returns [:forms entity-type :errors field] path vector for validation error of a specific field in an entity type form."
  [entity-type field]
  [:forms entity-type :errors field])

(defn form-submitting?
  "Returns [:forms entity-type :submitting?] path vector for the form submission state of a specific entity type."
  [entity-type]
  [:forms entity-type :submitting?])

(defn form-submitted?
  "Returns [:forms entity-type :submitted?] path vector for tracking if a form has been submitted."
  [entity-type]
  [:forms entity-type :submitted?])

(defn form-dirty-fields
  "Returns [:forms entity-type :dirty-fields] path vector for tracking modified fields in a form of a specific entity type."
  [entity-type]
  [:forms entity-type :dirty-fields])

(defn form-server-errors
  "Returns [:forms entity-type :server-errors field] path vector for server-side errors of a specific field in an entity type form."
  [entity-type field]
  [:forms entity-type :server-errors field])

(defn form-server-errors-all
  "Returns [:forms entity-type :server-errors] path vector for all server-side errors in an entity type form."
  [entity-type]
  [:forms entity-type :server-errors])

(defn form-success-all
  "Returns [:forms entity-type :success] path vector for all success states in a specific entity type form."
  [entity-type]
  [:forms entity-type :success])

(defn form-success
  "Returns [:forms entity-type :success field] path vector for success state of a specific field in an entity type form."
  [entity-type field]
  [:forms entity-type :success field])

(defn form-waiting
  "Returns [:forms entity-type :waiting] path vector for the waiting state of a specific entity type form."
  [entity-type]
  [:forms entity-type :waiting])

;; List UI paths
(defn list-ui-state
  "Returns [:ui :lists entity-type] path vector for UI state of a list for a specific entity type."
  [entity-type]
  [:ui :lists entity-type])

(defn list-sorts
  "Returns `[:ui :lists entity-type :sorts]` for the ordered sort entries of a list."
  [entity-type]
  [:ui :lists entity-type :sorts])

(defn list-current-page
  "Returns [:ui :lists entity-type :current-page] path vector for current page number of a list for a specific entity type."
  [entity-type]
  [:ui :lists entity-type :current-page])

(declare list-per-page entity-prefs-display)

(defn parse-positive-int
  "Parse positive numeric values from numbers or strings, returning nil otherwise."
  [value]
  (cond
    (number? value) (when (pos? value) (long value))
    (string? value) (let [n (js/parseInt value 10)]
                      (when (and (number? n) (not (js/isNaN n)) (pos? n))
                        (long n)))
    :else nil))

(defn- configured-view-options
  [db entity-type]
  (let [entity-key (if (keyword? entity-type) entity-type (keyword entity-type))]
    (if (admin-route? db)
      (merge
        (get-in db [:admin :config :view-options entity-key])
        (get-in db [:admin :settings :view-options entity-key]))
      (get-in db [:domain :config :view-options entity-key]))))

(defn- configured-view-options-per-page
  [db entity-type]
  (let [view-options (configured-view-options db entity-type)]
    (or (parse-positive-int (get-in view-options [:display-defaults :per-page]))
      (parse-positive-int (get-in view-options [:display-settings :per-page]))
      (parse-positive-int (get view-options :per-page)))))

(defn resolved-list-per-page
  "Resolve an entity list's per-page value from list UI state, browser prefs, or config.

  Order of precedence:
  1. Canonical list state
  2. Legacy list state mirrors
  3. Persisted browser display prefs
  4. Configured view-options per-page
  5. Provided fallback"
  [db entity-type fallback]
  (let [prefs-key (entity-prefs-key db entity-type)]
    (or (parse-positive-int (get-in db (list-per-page entity-type)))
      (parse-positive-int (get-in db (conj (list-ui-state entity-type) :per-page)))
      (parse-positive-int (get-in db (conj (list-ui-state entity-type) :pagination :per-page)))
      (parse-positive-int (get-in db (conj (entity-prefs-display prefs-key) :per-page)))
      (configured-view-options-per-page db entity-type)
      fallback)))

(defn resolved-list-current-page
  "Resolve an entity list's current page from canonical or legacy list UI state."
  [db entity-type]
  (or (parse-positive-int (get-in db (list-current-page entity-type)))
    (parse-positive-int (get-in db (conj (list-ui-state entity-type) :current-page)))
    (parse-positive-int (get-in db (conj (list-ui-state entity-type) :pagination :current-page)))
    1))

(defn- normalize-sort-direction
  [direction]
  (cond
    (or (= direction :asc)
      (= direction "asc"))
    :asc

    (or (= direction :desc)
      (= direction "desc"))
    :desc

    :else nil))

(defn- normalize-sort-entry
  [entry]
  (let [field (some-> (:field entry) model-naming/ensure-app-keyword)
        direction (normalize-sort-direction (:direction entry))]
    (when (and field direction)
      {:field field
       :direction direction})))

(defn resolved-list-sorts
  "Resolve an entity list's current ordered sort entries from list UI state.

  Prefer canonical `:sorts`, while still reading a single `:sort` map because
  a few list initializers and persisted UI states still seed that shape."
  [db entity-type]
  (->> (or (get-in db (list-sorts entity-type))
         (when-let [sort (get-in db (conj (list-ui-state entity-type) :sort))]
           [sort])
         [])
    (keep normalize-sort-entry)
    vec))

(defn resolved-list-sort-query-params
  "Resolve current list sort state to API query params."
  [db entity-type]
  (let [encoded-sort (some->> (resolved-list-sorts db entity-type)
                       (map (fn [{:keys [field direction]}]
                              (when (and field direction)
                                (str (name field) ":" (name direction)))))
                       (remove nil?)
                       seq
                       (str/join ","))]
    (cond-> {}
      (seq encoded-sort) (assoc :sort encoded-sort))))

(defn list-total-items
  "Returns [:ui :lists entity-type :total-items] path vector for total items count in a list for a specific entity type."
  [entity-type]
  [:ui :lists entity-type :total-items])

(defn list-per-page
  "Returns [:ui :lists entity-type :per-page] path vector for items per page setting of a list for a specific entity type."
  [entity-type]
  [:ui :lists entity-type :per-page])

(defn list-pagination-mode
  "Returns [:ui :lists entity-type :pagination-mode] path vector for pagination mode of a list.

  Supported modes:
  - :client (default)
  - :server (opt-in)"
  [entity-type]
  [:ui :lists entity-type :pagination-mode])

(defn list-refresh-event
  "Returns [:ui :lists entity-type :refresh-event] path vector for optional per-entity
  refresh dispatch configuration used by server-backed list pagination flows."
  [entity-type]
  [:ui :lists entity-type :refresh-event])

(defn entity-selected-ids
  "Returns [:ui :lists entity-type :selected-ids] path vector for the set of selected IDs in a list for a specific entity type."
  [entity-type]
  [:ui :lists entity-type :selected-ids])

(defn list-filters
  "Returns [:ui :lists entity-type :filters] path vector for filter state of a list."
  [entity-type]
  [:ui :lists entity-type :filters])

(defn entity-display-settings
  "Returns [:ui :entity-configs entity-name] path vector for the display settings of a specific entity."
  [entity-name]
  [:ui :entity-configs entity-name])

(defn entity-prefs-display
  "Returns [:ui :entity-prefs entity-name :display] path vector for display preferences."
  [entity-name]
  [:ui :entity-prefs entity-name :display])

(defn entity-prefs-columns-visible
  "Returns [:ui :entity-prefs entity-name :columns :visible] path vector for visible columns."
  [entity-name]
  [:ui :entity-prefs entity-name :columns :visible])

(defn entity-prefs-columns-visible-order
  "Returns [:ui :entity-prefs entity-name :columns :visible-order] path vector for column order."
  [entity-name]
  [:ui :entity-prefs entity-name :columns :visible-order])

(defn entity-prefs-columns-order
  "Returns [:ui :entity-prefs entity-name :columns :order] path vector for full column order."
  [entity-name]
  [:ui :entity-prefs entity-name :columns :order])

(defn entity-prefs-columns-width
  "Returns [:ui :entity-prefs entity-name :columns :width] path vector for column width."
  [entity-name]
  [:ui :entity-prefs entity-name :columns :width])

(defn entity-prefs-table-height
  "Returns [:ui :entity-prefs entity-name :table :height] path vector for table height."
  [entity-name]
  [:ui :entity-prefs entity-name :table :height])

(defn entity-prefs-filters-fields
  "Returns [:ui :entity-prefs entity-name :filters :fields] path vector for filterable fields."
  [entity-name]
  [:ui :entity-prefs entity-name :filters :fields])

;; Entity fetch paths
(defn entity-fetches
  "Returns [:entity-fetches entity-type] path vector for in-flight entity fetch tracking."
  [entity-type]
  [:entity-fetches entity-type])
