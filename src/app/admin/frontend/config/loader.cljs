(ns app.admin.frontend.config.loader
  (:require
    [app.admin.frontend.auth.persistence :as auth-persist]
    [taoensso.timbre :as log]))

(defonce config-cache (atom {}))

(def ^:private config-endpoints
  {:table-columns {:url "/admin/api/settings/table-columns"
                   :response-key :table-columns}
   :view-options {:url "/admin/api/settings"
                  :response-key :view-options}
   :form-fields {:url "/admin/api/settings/form-fields"
                 :response-key :form-fields}})

(defn- fetch-config
  "Fetch config data from the admin API and return a Clojure map"
  [config-type]
  (if-let [{:keys [url response-key]} (get config-endpoints config-type)]
    (let [token (auth-persist/get-persisted-token)
          opts (if token
                 #js {:credentials "include"
                      :headers #js {"x-admin-token" token}}
                 #js {:credentials "include"})]
      (-> (js/fetch url opts)
        (.then (fn [response]
                 (if (.-ok response)
                   (.json response)
                   (js/Promise.reject (js/Error. (str "Failed to load " url))))))
        (.then (fn [data]
                 (let [parsed (js->clj data :keywordize-keys true)]
                   (get parsed response-key {}))))
        (.catch (fn [error]
                  (log/error "Failed to load config" {:config-type config-type
                                                      :url url
                                                      :error error})
                  {}))))
    (do
      (log/error "No endpoint configured for config type" config-type)
      (js/Promise.resolve {}))))

(defn- load-config!
  "Load a config type from the admin API and cache it"
  [config-type]
  (-> (fetch-config config-type)
    (.then (fn [config]
             ;; Merge into existing cache to avoid overwriting preloaded entries
             (swap! config-cache update config-type (fnil merge {}) config)
             (log/debug "Loaded config:" config-type config)
             config))))

(defn load-all-configs!
  "Load all configuration files from the admin API"
  []
  (js/Promise.all
    #js [(load-config! :table-columns)
         (load-config! :view-options)
         (load-config! :form-fields)]))

(defn load-all-configs
  "Get all configuration data from cache (synchronous version)"
  []
  @config-cache)

(defn get-table-config
  "Get table configuration for an entity"
  [entity-keyword]
  (let [config (get-in @config-cache [:table-columns entity-keyword])]
    (when-not config
      (log/warn "No table-columns config found for entity:"
        entity-keyword
        "available entities:" (keys (get @config-cache :table-columns))))
    config))

(defn get-view-options
  "Get view options for an entity"
  [entity-keyword]
  (let [view-options (get-in @config-cache [:view-options entity-keyword])]
    (when-not view-options
      (log/warn "No view options found for entity:"
        entity-keyword
        "available entities:" (keys (get @config-cache :view-options))))
    view-options))

(defn get-form-config
  "Get form configuration for an entity"
  [entity-keyword]
  (let [form-config (get-in @config-cache [:form-fields entity-keyword])]
    (when-not form-config
      (log/warn "No form config found for entity:"
        entity-keyword
        "available entities:" (keys (get @config-cache :form-fields))))
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

(defn get-all-view-options
  "Get all view options from config cache.
   Returns a map of entity-keyword -> view-options."
  []
  (get @config-cache :view-options {}))

(defn get-all-form-fields
  "Get all form fields from config cache.
   Returns a map of entity-keyword -> form-fields config."
  []
  (get @config-cache :form-fields {}))

(defn get-all-table-columns
  "Get all table columns from config cache.
   Returns a map of entity-keyword -> table-columns config."
  []
  (get @config-cache :table-columns {}))

(defn init-config-loader!
  "Initialize the configuration loader"
  []
  (log/info "Initializing admin UI configuration loader")
  (load-all-configs!))
