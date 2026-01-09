(ns app.shared.frontend-config.schema
  "DB schema index and allowlist handling utilities.

  Loads models.edn and provides normalized schema lookups,
  plus allowlist normalization for field validation."
  (:require
    [clojure.set :as set]
    [app.shared.frontend-config.discovery :as discovery]))

(defn models-index
  "Load resources/db/models.edn and return a normalized schema index.

  Output:
  {:entities #{\"users\" ...}
   :entity->fields
   {\"users\" {:raw [\"id\" \"email\" ...]
             :canonical #{\"id\" \"email\" ...}
             :raw-by-canonical {\"id\" \"id\" ...}}}}"
  ([] (models-index "resources/db/models.edn"))
  ([path]
   (let [data (discovery/read-edn-file path)
         entities (keys data)
         entity->fields
         (reduce
           (fn [acc entity]
             (let [raw-fields (->> (get-in data [entity :fields])
                                (map first)
                                (map name)
                                vec)
                   canonical (set (map discovery/normalize-field-id raw-fields))
                   raw-by-canonical (zipmap (map discovery/normalize-field-id raw-fields) raw-fields)]
               (assoc acc
                 (discovery/normalize-entity-id entity)
                 {:raw raw-fields
                  :canonical canonical
                  :raw-by-canonical raw-by-canonical})))
           {}
           entities)]
     {:entities (set (map discovery/normalize-entity-id entities))
      :entity->fields entity->fields})))

(defn- normalize-allowlist-fields
  [fields]
  (->> fields
    (map discovery/normalize-field-id)
    (remove nil?)
    set))

(defn normalize-allowlist
  "Normalize an EDN allowlist into a map of canonical entity -> set of canonical fields.

  Supported formats:
  - map of entity -> collection of fields
  - set of fields (global allowlist for all entities)

  Use :* or :all as a global allowlist key in the map format."
  [allowlist]
  (cond
    (nil? allowlist) {:* #{}}

    (set? allowlist)
    {:* (normalize-allowlist-fields allowlist)}

    (map? allowlist)
    (reduce-kv
      (fn [acc entity fields]
        (let [entity* (discovery/normalize-entity-id entity)
              key* (if (#{"*" "all"} entity*) :* entity*)
              fields-coll (cond
                            (nil? fields) []
                            (set? fields) fields
                            (sequential? fields) fields
                            :else [fields])
              fields* (normalize-allowlist-fields fields-coll)]
          (update acc key* (fnil set/union #{}) fields*)))
      {:* #{}}
      allowlist)

    :else
    (throw (ex-info "Allowlist must be a map or set" {:type (type allowlist)}))))

(defn allowed-fields
  [{:keys [schema-index entity computed allowlist include-computed?]}]
  (set/union
    (get-in schema-index [:entity->fields entity :canonical] #{})
    (when include-computed? (get computed entity #{}))
    (get allowlist :* #{})
    (get allowlist entity #{})))
