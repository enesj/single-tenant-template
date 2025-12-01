(ns app.admin.frontend.config.loader
  (:require
    [cljs.reader :as reader]
    [taoensso.timbre :as log]))

(defonce config-cache (atom {}))

(defn- fetch-config-file
  "Fetch and parse an EDN configuration file"
  [file-path]
  (-> (js/fetch file-path)
    (.then (fn [response]
             (if (.-ok response)
               (.text response)
               (js/Promise.reject (js/Error. (str "Failed to load " file-path))))))
    (.then (fn [text]
             (try
               ;; Use cljs.reader to align with preloader behavior and avoid partial parsing issues
               (reader/read-string text)
               (catch js/Error e
                 (log/error "Failed to parse EDN from" file-path e)
                 {}))))
    (.catch (fn [error]
              (log/error "Failed to load config file:" file-path error)
              {}))))

(defn- transform-inverted-config
  "Transform inverted config format to internal format.
   The inverted format specifies what to HIDE/DISABLE rather than what to SHOW/ENABLE."
  [entity-config]
  (let [available (:available-columns entity-config)]
    (if (or (:default-hidden-columns entity-config)
          (:unfilterable-columns entity-config)
          (:unsortable-columns entity-config))
      ;; New inverted format - transform it
      (let [;; Convert hidden to visible
            visible-columns (if-let [hidden (:default-hidden-columns entity-config)]
                              (vec (remove (set hidden) available))
                              (:default-visible-columns entity-config))

            ;; Convert unfilterable to filterable
            filterable-columns (if-let [unfilterable (:unfilterable-columns entity-config)]
                                 (vec (remove (set unfilterable) available))
                                 (:filterable-columns entity-config))

            ;; Convert unsortable to sortable
            sortable-columns (if-let [unsortable (:unsortable-columns entity-config)]
                               (vec (remove (set unsortable) available))
                               (:sortable-columns entity-config))]

        (-> entity-config
            ;; Remove inverted keys
          (dissoc :default-hidden-columns :unfilterable-columns :unsortable-columns)
            ;; Add standard keys
          (assoc :default-visible-columns visible-columns
            :filterable-columns filterable-columns
            :sortable-columns sortable-columns)))
      ;; Fallback - return as is (shouldn't happen after migration)
      entity-config)))

(defn- load-config-file!
  "Load a config file and cache it"
  [config-type file-name]
  (let [file-path (str "/admin/ui-config/" file-name)]
    (-> (fetch-config-file file-path)
      (.then (fn [config]
               ;; Transform table-columns configs from inverted format
               (let [final-config (if (= config-type :table-columns)
                                    (into {} (map (fn [[k v]]
                                                    [k (transform-inverted-config v)])
                                               config))
                                    config)]
                 ;; Merge into existing cache to avoid overwriting preloaded entries
                 (swap! config-cache update config-type (fnil merge {}) final-config)
                 (log/debug "Loaded config:" config-type final-config)
                 final-config))))))

(defn load-all-configs!
  "Load all configuration files"
  []
  (js/Promise.all
    #js [(load-config-file! :table-columns "table-columns.edn")
         (load-config-file! :view-options "view-options.edn")
         (load-config-file! :form-fields "form-fields.edn")]))

(defn load-all-configs
  "Get all configuration data from cache (synchronous version)"
  []
  @config-cache)

(defn get-table-config
  "Get table configuration for an entity"
  [entity-keyword]
  (let [config (get-in @config-cache [:table-columns entity-keyword])]
    config))

(defn get-view-options
  "Get view options for an entity"
  [entity-keyword]
  (let [view-options (get-in @config-cache [:view-options entity-keyword])]
    (when-not view-options
      (log/warn "No view options found for entity:" entity-keyword))
    view-options))

(defn get-form-config
  "Get form configuration for an entity"
  [entity-keyword]
  (let [form-config (get-in @config-cache [:form-fields entity-keyword])]
    (when-not form-config
      (log/warn "No form config found for entity:" entity-keyword))
    form-config))

(defn get-default-visible-columns
  "Get default visible columns for an entity as vector"
  [entity-keyword]
  (get-in @config-cache [:table-columns entity-keyword :default-visible-columns] []))

(defn get-available-columns
  "Get all available columns for an entity"
  [entity-keyword]
  (get-in @config-cache [:table-columns entity-keyword :available-columns] []))

(defn get-computed-fields
  "Get computed field definitions for an entity"
  [entity-keyword]
  (get-in @config-cache [:table-columns entity-keyword :computed-fields] {}))

(defn get-column-config
  "Get column-specific configuration (width, formatter, etc.)"
  [entity-keyword column-key]
  (get-in @config-cache [:table-columns entity-keyword :column-config column-key] {}))

(defn is-always-visible?
  "Check if a column should always be visible"
  [entity-keyword column-key]
  (contains? (set (get-in @config-cache [:table-columns entity-keyword :always-visible] []))
    column-key))

(defn is-computed-field?
  "Check if a column is a computed field"
  [entity-keyword column-key]
  (contains? (get-computed-fields entity-keyword) column-key))

(defn has-vector-config?
  "Check if an entity has vector-based configuration"
  [entity-keyword]
  (boolean (get-in @config-cache [:table-columns entity-keyword])))

(defn register-preloaded-config!
  "Preload configuration data into the in-memory cache before async fetch completes.

   Two arities are supported:
   - `(register-preloaded-config! :table-columns {:users {...}})` merges the provided
     map into the cached config type.
   - `(register-preloaded-config! :table-columns :users {...})` convenience arity for a
     single entity entry.

   Returns the updated cache for the config type."
  ([config-type config-map]
   (swap! config-cache update config-type (fnil merge {}) config-map)
   (get @config-cache config-type))
  ([config-type entity-key config]
   (register-preloaded-config! config-type {entity-key config})))

(defn init-config-loader!
  "Initialize the configuration loader"
  []
  (log/info "Initializing admin UI configuration loader")
  (load-all-configs!))
