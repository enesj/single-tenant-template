(ns app.shared.model-naming
  "Utilities for converting database-oriented snake_case identifiers from
   resources/db/models.edn into kebab-case keywords used within the
   application. Provides helpers to translate keywords back to their
   database form when building queries."
  (:require
    [clojure.set :as set]
    [clojure.string :as str]))

(defn- ensure-keyword
  "Best-effort keyword coercion that preserves nil."
  [v]
  (cond
    (keyword? v) v
    (string? v) (keyword v)
    (symbol? v) (keyword (name v))
    (nil? v) nil
    :else (keyword (str v))))

(defn db-keyword->app
  "Convert a keyword or string that uses snake_case into a kebab-case keyword
   or string. Namespaces are also normalised. Non-keyword/string values are
   returned unchanged."
  [v]
  (cond
    (nil? v) nil
    (keyword? v) (let [ns-part (namespace v)
                       name-part (name v)
                       normal-ns (some-> ns-part (str/replace "_" "-"))
                       normal-name (str/replace name-part "_" "-")]
                   (if normal-ns
                     (keyword normal-ns normal-name)
                     (keyword normal-name)))
    (string? v) (str/replace v "_" "-")
    :else v))

(defn app-keyword->db
  "Convert a keyword or string that uses kebab-case into a snake_case keyword
   or string. Namespaces are also normalised. Non-keyword/string values are
   returned unchanged."
  [v]
  (cond
    (nil? v) nil
    (keyword? v) (let [ns-part (namespace v)
                       name-part (name v)
                       normal-ns (some-> ns-part (str/replace "-" "_"))
                       normal-name (str/replace name-part "-" "_")]
                   (if normal-ns
                     (keyword normal-ns normal-name)
                     (keyword normal-name)))
    (string? v) (str/replace v "-" "_")
    :else v))

(defn- derive-field-aliases
  "Create a map of database field keywords to application field keywords for a
   single entity definition."
  [entity-def]
  (->> (:fields entity-def)
    (map (fn [field-def]
           (let [db-field (ensure-keyword (first field-def))]
             (when db-field
               [db-field (db-keyword->app db-field)]))))
    (remove nil?)
    (into {})))

(defn- convert-keyword
  "Convert a keyword using known aliases, falling back to snake->kebab."
  [aliases kw]
  (let [kw* (ensure-keyword kw)]
    (cond
      (nil? kw*) nil
      (contains? aliases kw*) (get aliases kw*)
      :else (db-keyword->app kw*))))

(defn- convert-node
  "Recursively convert keywords inside a value using the provided alias map."
  [aliases value]
  (cond
    (map? value)
    (into (empty value)
      (map (fn [[k v]]
             [(convert-keyword aliases k)
              (convert-node aliases v)]))
      value)

    (vector? value)
    (mapv #(convert-node aliases %) value)

    (set? value)
    (into (empty value) (map #(convert-node aliases %) value))

    (list? value)
    (apply list (map #(convert-node aliases %) value))

    (sequential? value)
    (map #(convert-node aliases %) value)

    (keyword? value)
    (convert-keyword aliases value)

    :else value))

(defn- convert-entity-definition
  "Convert a single entity definition to use kebab-case field identifiers
   while recording alias lookups for bidirectional conversion."
  [entity-def]
  (let [aliases (derive-field-aliases entity-def)
        converted (convert-node aliases entity-def)
        app->db (set/map-invert aliases)]
    (assoc converted
      :db/field-aliases aliases
      :app/field-aliases app->db)))

(defn convert-models
  "Convert the full models.edn map to use kebab-case entity and field names.

   Returns a map keyed by kebab-case entity keywords. Metadata on the
   returned map stores entity alias information so code can translate between
   runtime (kebab) and database (snake) identifiers. Each entity value also
   carries :db/entity, :db/field-aliases and :app/field-aliases entries."
  [models]
  (let [db->app-entities (->> models
                           (map (fn [[entity-key _]]
                                  (let [db-kw (ensure-keyword entity-key)
                                        app-kw (db-keyword->app db-kw)]
                                    [db-kw app-kw])))
                           (into {}))
        converted (reduce-kv
                    (fn [acc db-entity entity-def]
                      (let [db-entity-kw (ensure-keyword db-entity)
                            app-entity (get db->app-entities db-entity-kw)
                            converted-entity (-> (convert-entity-definition entity-def)
                                               (assoc :db/entity db-entity-kw
                                                 :app/entity app-entity))]
                        (assoc acc app-entity converted-entity)))
                    {}
                    models)
        app->db-entities (set/map-invert db->app-entities)]
    (with-meta converted
      {:db/entity-aliases db->app-entities
       :app/entity-aliases app->db-entities
       :model-naming/converted true})))

(defn db-entity->app
  "Translate a database entity keyword to its kebab-case application keyword.
   Falls back to snake->kebab conversion when metadata is not available."
  [models db-entity]
  (let [aliases (:db/entity-aliases (meta models))
        db-kw (ensure-keyword db-entity)]
    (or (some-> aliases (get db-kw))
      (db-keyword->app db-kw))))

(defn app-entity->db
  "Translate a kebab-case application entity keyword back to its database
   snake_case form. Falls back to kebab->snake conversion when metadata is
   not present."
  [models app-entity]
  (let [aliases (:app/entity-aliases (meta models))
        app-kw (ensure-keyword app-entity)]
    (or (some-> aliases (get app-kw))
      (app-keyword->db app-kw))))

(defn db-field->app
  "Translate a database field keyword to its kebab-case application keyword
   using the alias map stored on an entity definition."
  [entity field]
  (let [db-kw (ensure-keyword field)
        aliases (:db/field-aliases entity)]
    (or (some-> aliases (get db-kw))
      (db-keyword->app db-kw))))

(defn app-field->db
  "Translate an application field keyword back to the snake_case database
   keyword using an entity definition's alias map."
  [entity field]
  (let [app-kw (ensure-keyword field)
        aliases (:app/field-aliases entity)]
    (or (some-> aliases (get app-kw))
      (app-keyword->db app-kw))))

(defn attach-app-models
  "Attach converted kebab-case models as metadata onto the original models map.
   Returns the original map with updated metadata so existing snake_case
   consumers continue working."
  [raw-models]
  (let [converted (convert-models raw-models)]
    (vary-meta raw-models assoc
      :model-naming/app-models converted)))

(defn app-models
  "Return the kebab-case models representation associated with the raw models
   map. If metadata isn't present, the conversion is performed eagerly.
   The original map is left untouched so callers can decide whether to cache
   the converted variant using `attach-app-models`."
  [raw-models]
  (or (:model-naming/app-models (meta raw-models))
    (convert-models raw-models)))

(defn entity-definition
  "Locate the converted entity definition within models, accepting either app
   or database identifiers. Returns nil when the entity is unknown."
  [models entity]
  (when models
    (let [app-entity (db-entity->app models entity)]
      (get models app-entity))))

(defn app-map->db
  "Convert a map keyed by application (kebab-case) field keywords into their
   database (snake_case) equivalents using the entity definition aliases.
   Non-keyword keys are preserved."
  [models entity data]
  (when data
    (let [entity-def (entity-definition models entity)]
      (into (empty data)
        (map (fn [[k v]]
               [(if (keyword? k)
                  (app-field->db entity-def k)
                  k)
                v]))
        data))))

(defn db-map->app
  "Convert a database-oriented map (snake_case keys) into application
   kebab-case keys using the entity definition aliases. Non-keyword keys are
   preserved."
  [models entity data]
  (when data
    (let [entity-def (entity-definition models entity)]
      (into (empty data)
        (map (fn [[k v]]
               [(if (keyword? k)
                  (db-field->app entity-def k)
                  k)
                v]))
        data))))

(defn app-filters->db
  "Convert a filters map expressed with application field keywords into the
   database representation suitable for query execution."
  [models entity filters]
  (app-map->db models entity filters))

(defn db-rows->app
  "Convert a collection of database result rows into application field names."
  [models entity rows]
  (when rows
    (cond
      (vector? rows) (mapv #(db-map->app models entity %) rows)
      (seq? rows) (map #(db-map->app models entity %) rows)
      :else rows)))
