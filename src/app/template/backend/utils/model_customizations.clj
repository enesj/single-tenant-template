(ns app.template.backend.utils.model-customizations
  "Schema-stripping helpers (`strip-*`) used by migration/alignment tooling
   to remove UI config from schema sources.")

;; ---------------------------------------------------------------------------
;; Clean separation: Strip admin configuration from models
;; ---------------------------------------------------------------------------

(defn strip-field-admin-config
  "Remove :admin configuration from a field definition.
   This supports the clean separation of UI config from database schema.

   Input:  [:email [:varchar 255] {:admin {:display-order 2 :width '200px'}}]
   Output: [:email [:varchar 255]]

   Input:  [:email [:varchar 255] {:admin {:display-order 2} :null false}]
   Output: [:email [:varchar 255] {:null false}]"
  [field-def]
  (if (and (vector? field-def) (>= (count field-def) 3))
    (let [[field-name field-type constraints] field-def
          cleaned-constraints (dissoc constraints :admin)]
      (if (empty? cleaned-constraints)
        [field-name field-type]
        [field-name field-type cleaned-constraints]))
    field-def))

(defn strip-entity-admin-config
  "Remove all :admin configuration from an entity definition.
   Also removes UI-specific keys like :computed-fields that belong in UI config.

   Input:  {:fields [...] :computed-fields {...} :indexes [...]}
   Output: {:fields [...] :indexes [...]}"
  [entity-def]
  (-> entity-def
      ;; Remove UI-specific top-level keys
    (dissoc :computed-fields :ui-config :admin-config)
      ;; Clean the fields
    (update :fields (fn [fields]
                      (mapv strip-field-admin-config fields)))))

(defn strip-all-admin-config
  "Remove all :admin configuration from complete models data.
   This ensures clean separation between database schema and UI configuration.

   Accepts either a map (EDN) or a vector of pairs (JSON-like)."
  [models-data]
  (let [md-map (cond
                 (map? models-data) models-data
                 (vector? models-data) (into {} models-data)
                 :else {})]
    (->> md-map
      (map (fn [[entity-name entity-def]]
             [entity-name (strip-entity-admin-config entity-def)]))
      (into {}))))
