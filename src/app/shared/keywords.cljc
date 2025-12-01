(ns app.shared.keywords
  "Cross-platform helpers for keyword/name coercion.

   Use these to safely convert values that may be keywords, symbols, strings,
   numbers or nil into consistent keyword or string forms without throwing.

   Functions are CLJC to support both backend and frontend usage.
  ")

(defn ensure-keyword
  "Best-effort conversion to keyword while preserving nil.

   - keyword -> keyword (unchanged)
   - string  -> (keyword string)
   - symbol  -> (keyword (name symbol))
   - nil     -> nil
   - other   -> (keyword (str v))
  "
  [v]
  (cond
    (keyword? v) v
    (string? v) (keyword v)
    (symbol? v) (keyword (name v))
    (nil? v) nil
    :else (keyword (str v))))

(defn ensure-name
  "Best-effort conversion to a simple (non-namespaced) string name.

   - keyword -> (name kw)
   - symbol  -> (name sym)
   - string  -> string (unchanged)
   - nil     -> nil
   - other   -> (str v)
  "
  [v]
  (cond
    (keyword? v) (name v)
    (symbol? v) (name v)
    (string? v) v
    (nil? v) nil
    :else (str v)))

(defn lower-name
  "Lowercase simple name for a value, or nil when not derivable.
   Uses `ensure-name` first, then lowercases when present."
  [v]
  (when-let [s (ensure-name v)]
    #?(:clj  (.toLowerCase ^String s)
       :cljs (.toLowerCase s))))
