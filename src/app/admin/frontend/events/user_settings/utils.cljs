(ns app.admin.frontend.events.user-settings.utils
  (:require
    [app.shared.model-naming :as model-naming]))

(def ^:const user-ui-config-uri
  "/admin/api/settings/user-ui-config")

(defn safe-map [x]
  (if (map? x) x {}))

(defn normalize-kw [x]
  (cond
    (nil? x) nil
    (keyword? x) x
    (string? x) (keyword x)
    :else (keyword (str x))))

(defn normalize-entity-kw
  "Normalize an entity identifier to the canonical app keyword.

  This is intentionally entity-key only (top-level). Nested config keys and
  column ids are handled elsewhere to avoid unintended wide-reaching changes." 
  [x]
  (model-naming/ensure-app-keyword x))

(defn normalize-cols [xs]
  (->> (or xs [])
       (keep normalize-kw)
       vec))

(defn normalize-map-keys
  "Keywordize map keys using normalize-kw, skipping keys that normalize to nil.

  This is primarily to handle JSON-decoded responses where entity keys or
  setting keys arrive as strings."
  [m]
  (reduce-kv
    (fn [acc k v]
      (if-let [k' (normalize-kw k)]
        (assoc acc k' v)
        acc))
    {}
    (safe-map m)))

(defn normalize-entity-map
  "Normalize a top-level entity map of {entity-key -> config-map}.

  Entity keys are keywordized. The entity config map's keys are also
  keywordized (values are left as-is)."
  [m]
  (reduce-kv
    (fn [acc entity-k entity-v]
      (if-let [entity-kw (normalize-entity-kw entity-k)]
        (assoc acc entity-kw (if (map? entity-v) (normalize-map-keys entity-v) entity-v))
        acc))
    {}
    (safe-map m)))

(defn normalize-user-view-options
  "Normalize user view-options config.

  - Entity keys are keywordized.
  - Entity config keys are keywordized.
  - Nested maps for defaults/locks (display + columns) have their keys
    keywordized (so :expenses lookups work and bulk controls compute correctly)."
  [view-options]
  (reduce-kv
    (fn [acc entity-k entity-v]
      (if-let [entity-kw (normalize-entity-kw entity-k)]
        (let [cfg (if (map? entity-v) (normalize-map-keys entity-v) {})
              cfg (cond-> cfg
                    (contains? cfg :display-defaults) (update :display-defaults normalize-map-keys)
                    (contains? cfg :display-locks) (update :display-locks normalize-map-keys)
                    (contains? cfg :column-defaults) (update :column-defaults normalize-map-keys)
                    (contains? cfg :column-locks) (update :column-locks normalize-map-keys))]
          (assoc acc entity-kw cfg))
        acc))
    {}
    (safe-map view-options)))

(defn draft-config
  "Return the current draft config structure."
  [db]
  (safe-map (get-in db [:admin :user-settings :draft])))

(defn saved-config
  "Return the last saved config structure."
  [db]
  (safe-map (get-in db [:admin :user-settings :saved])))

