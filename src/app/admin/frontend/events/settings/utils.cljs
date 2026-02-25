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
  "True when the key is one of the list-view display settings.

  In the new schema, these live under :display-defaults / :display-locks.
  (Historically they were top-level keys in view-options.edn.)
  
  Includes :per-page for rows-per-page configuration."
  [k]
  (and (keyword? k)
    (or (re-matches #"show-.*\?" (name k))
      (= k :per-page))))

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

(defn load-failure-effect
  "Build the re-frame effect map for a settings load-failure handler.

  Sets `loading-key` to false and records `error-msg`; adds an
  `:admin/auth-invalid` dispatch when the error is a 401."
  [db loading-key error-msg error]
  (let [db' (-> db
              (assoc-in [:admin :settings loading-key] false)
              (assoc-in [:admin :settings :error] error-msg))]
    (if (unauthorized? error)
      {:db db' :dispatch [:admin/auth-invalid]}
      {:db db'})))

(defn update-failure-effect
  "Build the re-frame effect map for a settings update-failure handler.

  Clears `:saving?`, records `error-msg`, dispatches `reload-event` to
  revert optimistic changes; adds `:admin/auth-invalid` dispatch on 401."
  [db error error-msg reload-event]
  (cond-> {:db (-> db
                 (assoc-in [:admin :settings :saving?] false)
                 (assoc-in [:admin :settings :error] error-msg))
           :fx [[:dispatch reload-event]]}
    (unauthorized? error) (assoc :dispatch [:admin/auth-invalid])))

