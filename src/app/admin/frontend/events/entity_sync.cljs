(ns app.admin.frontend.events.entity-sync
  "Bridge events that sync domain-specific admin responses into the shared template entity store.

  This namespace provides a generic `:admin/refresh-entity-list` event that
  dispatches to domain-owned sync events via the domain registry.
  
  Domain adapters register their sync events, and this namespace acts as a dispatcher
  without concrete domain knowledge."
  (:require
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; Dynamic sync event registry - domains register their sync handlers here
(defonce ^:private sync-handlers (atom {}))

(defn register-sync-handler!
  "Register a sync handler for an entity type.
   handler-config is {:sync-event-id keyword :upsert-event-id keyword}
   Domain adapters should call this during initialization."
  [entity-key handler-config]
  (swap! sync-handlers assoc entity-key handler-config)
  (log/debug "Registered sync handler" {:entity entity-key :config handler-config}))

(defn- extract-entities
  "Extract the collection of entities for `entity-key` from a standard admin API response.
  Expected shape: {<entity-key> [...]} plus optional pagination metadata."
  [entity-key response]
  (vec (or (get response (keyword (name entity-key))) [])))

(rf/reg-event-fx
  :admin/refresh-entity-list
  (fn [{:keys [_db]} [_ entity-key response]]
    (let [entities (extract-entities entity-key response)
          count* (count entities)
          handler-config (get @sync-handlers entity-key)]
      (when (pos? count*)
        (log/debug "Syncing admin list into template entity store"
          {:entity entity-key :count count*}))
      (if-let [sync-event-id (:sync-event-id handler-config)]
        {:dispatch [sync-event-id entities]}
        ;; No handler registered - log and no-op
        (do
          (when (pos? count*)
            (log/debug "No sync handler registered for entity" {:entity entity-key}))
          {})))))
