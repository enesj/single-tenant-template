(ns app.shared.frontend.utils.entity
  "Generic entity processing utilities for all domains.

  This namespace provides universal utilities for normalizing entities,
  registering subscriptions, and syncing data to the template system."
  (:require
    [app.template.frontend.db.paths :as paths]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(defn normalize-entity
  "Common helper that coerces entity IDs to strings, synthesizes namespaced keys,
  and allows adapter-specific post-processing.

  Options:
  - `:entity-ns` (required): keyword or string used when namespacing plain keys
  - `:id-keys`: ordered vector of keys checked for a canonical ID (default `[:id]`)
  - `:stringify-keys`: additional keys that should be stringified when present
  - `:alias-keys`: map of source-key -> collection of alias keys to mirror values onto
  - `:fallback-id-fn`: called with the partially-normalized entity when no ID keys
    are present; should return a value convertible to string
  - `:post-transform`: final function applied to the entity before returning"
  [entity {:keys [entity-ns id-keys stringify-keys alias-keys fallback-id-fn post-transform]
           :or {id-keys [:id]
                stringify-keys []
                alias-keys {}
                post-transform identity}}]
  (let [entity (if (map? entity) entity {})
        ns-name (cond
                  (string? entity-ns) entity-ns
                  (keyword? entity-ns) (name ^Keyword entity-ns)
                  :else (throw (ex-info "entity-ns must be string or keyword"
                                 {:provided entity-ns})))
        stringify-set (into [] (concat id-keys stringify-keys))
        entity (reduce (fn [m k]
                         (if-let [v (get m k)]
                           (assoc m k (str v))
                           m))
                 entity
                 stringify-set)
        namespaced (into {}
                     (keep (fn [[k v]]
                             (when (and (keyword? k)
                                     (nil? (namespace ^Keyword k)))
                               [(keyword ns-name (name ^Keyword k)) v])))
                     entity)
        merged (merge namespaced entity)
        merged (reduce (fn [m [source targets]]
                         (if-let [value (get m source)]
                           (reduce (fn [acc target]
                                     (assoc acc target value))
                             m
                             targets)
                           m))
                 merged
                 alias-keys)
        id-value (some (fn [k]
                         (when-let [v (get merged k)]
                           (str v)))
                   id-keys)
        merged (cond-> merged
                 id-value (assoc :id id-value))
        merged (if (and (nil? id-value) fallback-id-fn)
                 (if-let [fallback (fallback-id-fn merged)]
                   (assoc merged :id (str fallback))
                   merged)
                 merged)]
    (post-transform merged)))

(defn register-entity-spec-sub!
  "Register an `:entity-specs/<entity>` subscription that proxies to
  `[:admin/entity-spec <entity>]`. Accepts optional `:spec-keys` collection when
  multiple admin spec keys should be checked (first non-nil wins).

  Options:
  - `:entity-key`: keyword for the entity (required)
  - `:spec-keys`: collection of keys queried from `:admin/entity-spec`
  - `:sub-id`: override the subscription id keyword
  - `:value-fn`: custom handler `(fn [values _])`

  Returns the subscription id keyword for convenience."
  [{:keys [entity-key spec-keys sub-id value-fn]
    :or {spec-keys nil}}]
  (let [spec-keys (seq (or spec-keys [entity-key]))
        sub-id (or sub-id (keyword "entity-specs" (name entity-key)))
        signal-args (mapcat (fn [k] [:<- [:admin/entity-spec k]]) spec-keys)
        multi? (> (count spec-keys) 1)
        handler (or value-fn
                  (if multi?
                    (fn [values _] (some identity values))
                    (fn [value _] value)))
        args (concat [sub-id] signal-args [handler])]
    (apply rf/reg-sub args)
    sub-id))

(defn register-sync-event!
  "Register a standard sync event that normalizes a collection of entities and
  pushes them into the template entity store.

  Options:
  - `:event-id`: re-frame event keyword (required)
  - `:entity-key`: template entity keyword (required)
  - `:normalize-fn`: `(fn [entity] normalized-entity)` (required)
  - `:log-prefix`: optional string prefix for debug logging"
  [{:keys [event-id entity-key normalize-fn log-prefix]}]
  (rf/reg-event-db
    event-id
    (fn [db [_ entities]]
      (let [normalized-pairs (into []
                               (comp
                                 (map normalize-fn)
                                 (keep (fn [entity]
                                         (when-let [id (:id entity)]
                                           [(str id) entity]))))
                               (or entities []))
            ids (mapv first normalized-pairs)
            entities-by-id (into {} normalized-pairs)
            message (or log-prefix
                      (str "Syncing " (name entity-key) " to template system:"))]
        (log/debug message (count normalized-pairs) "entities")
        (-> db
          (assoc-in (paths/entity-data entity-key) entities-by-id)
          (assoc-in (paths/entity-ids entity-key) ids))))))
