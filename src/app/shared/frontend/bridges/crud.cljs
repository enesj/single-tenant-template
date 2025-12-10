(ns app.shared.frontend.bridges.crud
  "Generic CRUD bridge system for context-specific event customization.

  This system allows multiple domains (admin, financial, hosting, etc.) to
  register custom CRUD operation handlers that selectively override template
  system behavior based on context predicates."
  (:require
    [app.shared.frontend.crud.success :as crud-success]
    [app.shared.http :as http]
    [app.template.frontend.api.http :as template-http]
    [app.template.frontend.db.paths :as paths]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ============================================================================
;; Bridge Registry and Configuration
;; ============================================================================

(defonce ^:private bridge-registry (atom {}))

(defn- merge-operation-configs
  "Merge existing and new operation configuration maps without losing nested keys."
  [existing new]
  (reduce (fn [acc [op cfg]]
            (assoc acc op (merge (get acc op {}) cfg)))
    (or existing {})
    new))

(defn- entity-name
  "Convert entity type to string name for API calls."
  [entity-type]
  (cond
    (keyword? entity-type) (name ^Keyword entity-type)
    (string? entity-type) entity-type
    (map? entity-type) (or (:value entity-type) (str entity-type))
    :else (str entity-type)))

(defn- clear-loading
  "Clear loading state for entity type in database."
  [db entity-type]
  (if (keyword? entity-type)
    (assoc-in db (paths/entity-loading? entity-type) false)
    db))

;; ============================================================================
;; Default CRUD Handlers
;; ============================================================================

(defn default-crud-success [{:keys [db]} entity-type response]
  ; Default success handler for delete/create CRUD operations.
  (let [entity-id (crud-success/extract-entity-id response)
        db* (-> db
              (clear-loading entity-type)
              (crud-success/track-recently-created entity-type entity-id)
              (update-in [:forms entity-type] merge (crud-success/clear-form-success-state)))]
    (if (keyword? entity-type)
      {:db db*
       :dispatch [:app.template.frontend.events.list.crud/fetch-entities entity-type]}
      {:db db*})))

(defn default-update-success [{:keys [db]} entity-type id _response]
  ; Default success handler for update operations - tracks recently-updated for highlights.
  (let [db* (-> db
              (clear-loading entity-type)
              (crud-success/track-recently-updated entity-type id)
              (update-in [:forms entity-type] merge (crud-success/clear-form-success-state)))]
    (log/debug "Update success for" entity-type "id:" id)
    (if (keyword? entity-type)
      {:db db*
       :dispatch [:app.template.frontend.events.list.crud/fetch-entities entity-type]}
      {:db db*})))

(defn- failure-message
  "Extract error message (and optional suggestion) from response or use default."
  [default-msg error]
  (let [response (:response error)
        message (or (some-> error http/extract-error-message)
                  (:message response)
                  (:status-text error))
        suggestion (or (get-in response [:body :details :suggestion])
                     (get-in response [:body :details :error-details :suggestion])
                     (get-in response [:details :suggestion])
                     (get-in response [:details :error-details :suggestion])
                     (get-in response [:error-details :suggestion])
                     (:suggestion response)
                     (get-in response [:body :suggestion])
                     (get-in response [:body :error-details :suggestion])
                     (:suggestion error))
        sanitized-msg (let [msg (when (and (string? message)
                                        (not (str/blank? message)))
                                  message)]
                        (when (and msg (not= "An error occurred" msg))
                          msg))
        sanitized-suggestion (when (and (string? suggestion)
                                     (not (str/blank? suggestion)))
                               suggestion)]
    (cond
      (and sanitized-msg sanitized-suggestion)
      (str sanitized-msg ". " sanitized-suggestion)

      sanitized-msg sanitized-msg

      :else default-msg)))

(defn default-crud-failure [{:keys [db]} entity-type operation error]
  ; Default failure handler with operation-specific error messages.
  (let [base (case operation
               :delete "Failed to delete item"
               :create (str "Failed to create " (entity-name entity-type))
               :update (str "Failed to update " (entity-name entity-type))
               (str "Failed to complete operation on " (entity-name entity-type)))
        message (failure-message base error)
        db* (if (keyword? entity-type)
              (-> db
                (assoc-in (paths/entity-loading? entity-type) false)
                (assoc-in (paths/entity-error entity-type) message))
              db)]
    {:db db*}))

;; Convenience wrappers so the default failure handler receives the
;; correct operation keyword. run-bridge-operation does not pass the
;; operation into default-effect-fn, so the event registrations below
;; use these wrappers.
(defn default-delete-failure [cofx entity-type error]
  (default-crud-failure cofx entity-type :delete error))

(defn default-create-failure [cofx entity-type error]
  (default-crud-failure cofx entity-type :create error))

(defn default-update-failure [cofx entity-type error]
  (default-crud-failure cofx entity-type :update error))

;; ============================================================================
;; Default Request Handlers
;; ============================================================================

(defn default-delete-request [{:keys [db]} entity-type id]
  ; Default delete request handler using template HTTP.
  {:db (assoc-in db (paths/entity-loading? entity-type) true)
   :http-xhrio (template-http/delete-entity
                 {:entity-name (entity-name entity-type)
                  :id id
                  :on-success [:app.template.frontend.events.list.crud/delete-success entity-type id]
                  :on-failure [:app.template.frontend.events.list.crud/delete-failure entity-type]})})

(defn default-create-request [{:keys [db]} entity-type form-data]
  ; Default create request handler using template HTTP.
  {:db (assoc-in db (paths/entity-loading? entity-type) true)
   :http-xhrio (template-http/create-entity
                 {:entity-name (entity-name entity-type)
                  :data form-data
                  :on-success [:app.template.frontend.events.list.crud/create-success entity-type]
                  :on-failure [:app.template.frontend.events.list.crud/create-failure entity-type]})})

(defn default-update-request [{:keys [db]} entity-type id form-data]
  ; Default update request handler using template HTTP.
  {:db (assoc-in db (paths/entity-loading? entity-type) true)
   :http-xhrio (template-http/update-entity
                 {:entity-name (entity-name entity-type)
                  :id id
                  :data form-data
                  :on-success [:app.template.frontend.events.list.crud/update-success entity-type id]
                  :on-failure [:app.template.frontend.events.list.crud/update-failure entity-type]})})

;; ============================================================================
;; Bridge Registration and Management
;; ============================================================================

(defn register-crud-bridge!
  "Register overrides for template CRUD events.

  Expected options:
  - `:entity-key` (keyword, required): Entity type this bridge handles
  - `:bridge-id` (keyword, required): Unique identifier for this bridge (e.g., :admin, :financial)
  - `:operations` map keyed by `:delete`, `:create`, and/or `:update`. Each entry may
    provide `:request`, `:on-success`, and `:on-failure` functions that receive
    `(cofx entity-type & args default-effect)` and should return an effects map. When a
    handler returns nil the default template behavior is used.
  - `:context-pred` optional predicate `(fn [db])` controlling when overrides apply.
    Defaults to a function that always returns true.
  - `:priority` optional number for bridge ordering (higher = applied first, default 100)

  Returns the bridge configuration for verification."
  [{:keys [entity-key bridge-id operations context-pred priority] :as opts}]
  (when-not (keyword? entity-key)
    (throw (ex-info "entity-key must be a keyword" {:provided entity-key :opts opts})))
  (when-not (keyword? bridge-id)
    (throw (ex-info "bridge-id must be a keyword" {:provided bridge-id :opts opts})))
  (when (empty? operations)
    (throw (ex-info "operations map is required" {:entity-key entity-key :bridge-id bridge-id :opts opts})))

  (swap! bridge-registry
    (fn [registry]
      (let [existing-bridges (get registry entity-key [])
            existing-bridge (some (fn [bridge] (= bridge-id (:bridge-id bridge))) existing-bridges)
            merged-ops (if existing-bridge
                         (merge-operation-configs (:operations existing-bridge) operations)
                         operations)
            effective-context (or context-pred (:context-pred existing-bridge) (constantly true))
            effective-priority (or priority (:priority existing-bridge) 100)
            new-bridge {:entity-key entity-key
                        :bridge-id bridge-id
                        :context-pred effective-context
                        :priority effective-priority
                        :operations merged-ops}
            updated-bridges (if existing-bridge
                              (map (fn [bridge]
                                     (if (= bridge-id (:bridge-id bridge))
                                       new-bridge
                                       bridge))
                                existing-bridges)
                              (conj existing-bridges new-bridge))]
        (assoc registry entity-key updated-bridges))))
  {:entity-key entity-key :bridge-id bridge-id})

(defn unregister-crud-bridge!
  "Remove a specific bridge registration."
  [entity-key bridge-id]
  (swap! bridge-registry
    (fn [registry]
      (let [existing-bridges (get registry entity-key [])
            filtered-bridges (remove (fn [bridge] (= bridge-id (:bridge-id bridge))) existing-bridges)]
        (if (seq filtered-bridges)
          (assoc registry entity-key filtered-bridges)
          (dissoc registry entity-key)))))
  nil)

(defn get-bridges-for-entity
  "Get all registered bridges for an entity type, sorted by priority (highest first)."
  [entity-key]
  (some->> (get @bridge-registry entity-key)
    (sort-by :priority >)
    (vec)))

;; ============================================================================
;; Bridge Execution Engine
;; ============================================================================

(defn- should-bridge?
  "Check if a bridge should be applied in the current context."
  [bridge db]
  (try
    (boolean ((:context-pred bridge) db))
    (catch :default e
      (log/error e "CRUD bridge context predicate failed"
        {:entity (:entity-key bridge)
         :bridge (:bridge-id bridge)})
      false)))

(defn- apply-bridge-handler
  "Apply a single bridge handler and return the modified effect or nil for fallback."
  [bridge operation handler-type cofx entity-type args default-effect]
  (when-let [handler (get-in bridge [:operations operation handler-type])]
    (try
      (let [result (apply handler cofx entity-type (conj args default-effect))]
        (when (some? result)
          result))
      (catch :default e
        (log/error e "CRUD bridge handler failed"
          {:entity (:entity-key bridge)
           :bridge (:bridge-id bridge)
           :operation operation
           :handler-type handler-type})
        nil))))

(defn run-bridge-operation
  "Execute a CRUD operation through the bridge system.

  Args:
  - `operation`: :delete, :create, or :update
  - `handler-type`: :request, :on-success, or :on-failure
  - `default-effect-fn`: Function to calculate default template behavior
  - `cofx`: Re-frame cofx map
  - `entity-type`: Entity type being operated on
  - `args`: Additional arguments for the operation

  Returns effects map, potentially modified by applicable bridges."
  [operation handler-type default-effect-fn cofx entity-type args]
  (log/info "🔍 run-bridge-operation:" {:operation operation
                                        :handler-type handler-type
                                        :entity-type entity-type
                                        :entity-type-type (type entity-type)
                                        :args-count (count args)
                                        :registry-keys (keys @bridge-registry)})
  (let [default-effect (apply default-effect-fn cofx entity-type args)
        bridges (get-bridges-for-entity entity-type)
        _ (log/info "🌉 bridges found:" {:bridges-count (count bridges)
                                         :bridge-ids (mapv :bridge-id bridges)})
        applicable-bridges (filter (fn [bridge] (should-bridge? bridge (:db cofx))) bridges)]
    (log/info "✅ applicable bridges:" {:count (count applicable-bridges)})
    (loop [bridges applicable-bridges
           current-effect default-effect]
      (if-let [bridge (first bridges)]
        (let [modified-effect (apply-bridge-handler bridge operation handler-type cofx entity-type args current-effect)]
          (recur (rest bridges) (or modified-effect current-effect)))
        current-effect))))

;; ============================================================================
;; Template Event Registration
;; ============================================================================

(defonce ^:private handlers-registered?
  (atom false))

(defn register-template-crud-events!
  "Register the main CRUD event handlers that use the bridge system.

  This should be called once during application initialization to set up
  the bridge-based event handling for template CRUD operations. Subsequent
  calls are ignored to prevent handler overwrite churn."
  []
  (if @handlers-registered?
    (log/debug "Template CRUD bridge events already registered; skipping")
    (do
      (reset! handlers-registered? true)
      (rf/reg-event-fx
        :app.template.frontend.events.list.crud/delete-entity
        (fn [cofx [_ entity-type id]]
          (run-bridge-operation :delete :request default-delete-request cofx entity-type [id])))

      (rf/reg-event-fx
        :app.template.frontend.events.list.crud/create-entity
        (fn [cofx [_ entity-type form-data]]
          (run-bridge-operation :create :request default-create-request cofx entity-type [form-data])))

      (rf/reg-event-fx
        :app.template.frontend.events.list.crud/update-entity
        (fn [cofx [_ entity-type id form-data]]
          (run-bridge-operation :update :request default-update-request cofx entity-type [id form-data])))

      (rf/reg-event-fx
        :app.template.frontend.events.list.crud/delete-success
        (fn [cofx [_ entity-type id]]
          (run-bridge-operation :delete :on-success default-crud-success cofx entity-type [id])))

      (rf/reg-event-fx
        :app.template.frontend.events.list.crud/create-success
        (fn [cofx [_ entity-type response]]
          (run-bridge-operation :create :on-success default-crud-success cofx entity-type [response])))

      (rf/reg-event-fx
        :app.template.frontend.events.list.crud/update-success
        (fn [cofx [_ entity-type id response]]
          (run-bridge-operation :update :on-success default-update-success cofx entity-type [id response])))

      (rf/reg-event-fx
        :app.template.frontend.events.list.crud/delete-failure
        (fn [cofx [_ entity-type error]]
          (run-bridge-operation :delete :on-failure default-delete-failure cofx entity-type [error])))

      (rf/reg-event-fx
        :app.template.frontend.events.list.crud/create-failure
        (fn [cofx [_ entity-type error]]
          (run-bridge-operation :create :on-failure default-create-failure cofx entity-type [error])))

      (rf/reg-event-fx
        :app.template.frontend.events.list.crud/update-failure
        (fn [cofx [_ entity-type error]]
          (run-bridge-operation :update :on-failure default-update-failure cofx entity-type [error])))

      (log/info "Template CRUD bridge events registered successfully"))))

;; ============================================================================
;; Utilities and Helpers
;; ============================================================================

(defn assoc-paths
  "Utility to assoc multiple `[path value]` pairs in a db map."
  [db path-value-pairs]
  (reduce (fn [acc [path value]]
            (assoc-in acc path value))
    db
    path-value-pairs))

(defn clear-all-bridges!
  "Clear all bridge registrations (primarily for testing)."
  []
  (reset! bridge-registry {})
  (log/info "All CRUD bridges cleared"))

(defn bridge-registry-summary
  "Get a summary of current bridge registrations for debugging."
  []
  (into {}
    (map (fn [[entity-key bridges]]
           [entity-key (mapv (juxt :bridge-id :priority (comp count :operations)) bridges)]))
    @bridge-registry))
