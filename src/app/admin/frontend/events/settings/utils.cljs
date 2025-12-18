(ns app.admin.frontend.events.settings.utils)

(defn safe-map
  "Normalize nil to empty map for comparisons/merges."
  [x]
  (if (map? x) x {}))

(defn unauthorized?
  "Return true when an XHR error represents a 401/unauthorized response."
  [error]
  (= 401 (or (:status error) (get-in error [:response :status]))))

(defn display-setting-key?
  "True when the key is one of the list-view display toggles.

  In the new schema, these live under :display-defaults / :display-locks.
  (Historically they were top-level keys in view-options.edn.)"
  [k]
  (and (keyword? k)
    (re-matches #"show-.*\?" (name k))))

(defn normalize-kw
  [x]
  (cond
    (nil? x) nil
    (keyword? x) x
    (string? x) (keyword x)
    :else (keyword (str x))))

(defn normalize-kws
  [xs]
  (->> (or xs [])
    (keep normalize-kw)
    vec))

