(ns app.template.di.container
  "Generic dependency injection container for service lifecycle management.

   Provides:
   - Service registration and dependency resolution
   - Lifecycle management (init/start/stop)
   - Configuration-based service activation
   - Service discovery and retrieval
   - Monitoring hooks for observability
   - Error handling and graceful degradation"
  (:require
    [clojure.set :as set]
    [taoensso.timbre :as log]))

;; ============================================================================
;; Protocols
;; ============================================================================

(defprotocol Lifecycle
  "Service lifecycle management protocol"
  (init [svc container]
    "Initialize service, returning initialized instance")
  (start [svc container]
    "Start service, returning running instance")
  (stop [svc container]
    "Stop service, returning stopped instance or nil"))

;; ============================================================================
;; Records
;; ============================================================================

(defrecord ServiceDef
  [id         ; keyword - unique service identifier
   ctor       ; fn [container] -> service instance
   deps       ; set of service-ids this service depends on
   active?    ; fn [config] -> boolean
   hooks])    ; {:on-init fn, :on-start fn, :on-stop fn}

(defrecord DIContainer
  [config     ; application configuration map
   defs       ; map of id -> ServiceDef
   instances  ; atom holding {id -> {:instance obj, :status keyword, :error ex}}
   listeners]) ; atom holding lifecycle event listeners

;; ============================================================================
;; Container Creation and Registration
;; ============================================================================

(defn create-container
  "Create a new dependency injection container"
  [config]
  (->DIContainer config {} (atom {}) (atom [])))

(defn register-service!
  "Register a service definition with the container"
  [container service-def]
  (when (get-in container [:defs (:id service-def)])
    (throw (ex-info "Duplicate service registration"
             {:service-id (:id service-def)
              :existing-def (get-in container [:defs (:id service-def)])})))
  (assoc-in container [:defs (:id service-def)] service-def))

(defn add-lifecycle-listener!
  "Add a listener function that will be called on lifecycle events"
  [container listener-fn]
  (swap! (:listeners container) conj listener-fn)
  container)

;; ============================================================================
;; Dependency Resolution
;; ============================================================================

(defn- get-active-services
  "Get set of service ids that should be activated based on config"
  [container]
  (->> (:defs container)
    (filter (fn [[_id service-def]]
              (let [active-fn (or (:active? service-def)
                                (constantly true))]
                (active-fn (:config container)))))
    (map first)
    set))

(defn- build-dependency-graph
  "Build dependency graph for active services"
  [container active-service-ids]
  (->> active-service-ids
    (map (fn [id]
           (let [def (get-in container [:defs id])
                 deps (set/intersection (:deps def) active-service-ids)]
             [id deps])))
    (into {})))

(defn- topological-sort
  "Topologically sort services based on dependencies"
  [dep-graph]
  (loop [sorted []
         graph dep-graph]
    (if (empty? graph)
      sorted
      (let [no-deps (filter #(empty? (second %)) graph)]
        (if (empty? no-deps)
          (throw (ex-info "Circular dependency detected"
                   {:remaining-services (keys graph)}))
          (let [resolved (map first no-deps)
                new-graph (reduce (fn [g [id deps]]
                                    (if (contains? (set resolved) id)
                                      g
                                      (assoc g id (set/difference deps (set resolved)))))
                            {}
                            graph)]
            (recur (concat sorted resolved) new-graph)))))))

;; ============================================================================
;; Lifecycle Management
;; ============================================================================

(defn- notify-listeners
  "Notify all listeners of a lifecycle event"
  [container event-type service-id service-instance error]
  (doseq [listener @(:listeners container)]
    (try
      (listener {:type event-type
                 :service-id service-id
                 :instance service-instance
                 :error error
                 :timestamp (System/currentTimeMillis)})
      (catch Exception e
        (log/error e "Error in lifecycle listener")))))

(defn- init-service
  "Initialize a single service"
  [container service-id]
  (try
    (let [service-def (get-in container [:defs service-id])
          service-instance ((or (:ctor service-def)
                              (throw (ex-info "No constructor for service"
                                       {:service-id service-id})))
                            container)
          ;; If service implements Lifecycle, call init
          initialized (if (satisfies? Lifecycle service-instance)
                        (init service-instance container)
                        service-instance)
          _ (swap! (:instances container)
              assoc service-id {:instance initialized
                                :status :initialized})
          ;; Call init hook if provided
          _ (when-let [hook (get-in service-def [:hooks :on-init])]
              (hook service-id initialized))
          _ (notify-listeners container :initialized service-id initialized nil)
          _ (log/info "Initialized service" service-id)]
      initialized)

    (catch Exception e
      (log/error e "Failed to initialize service" service-id)
      (swap! (:instances container)
        assoc service-id {:instance nil
                          :status :failed
                          :error e})
      (notify-listeners container :init-failed service-id nil e)
      (throw e))))

(defn initialize-services!
  "Initialize all services in dependency order"
  [container]
  (let [active-ids (get-active-services container)
        dep-graph (build-dependency-graph container active-ids)
        init-order (topological-sort dep-graph)]

    ;;(log/info "Initializing services in order:" init-order)

    (doseq [service-id init-order]
      (try
        (init-service container service-id)
        (catch Exception e
          (log/error e "Service initialization failed, continuing with remaining services"))))

    container))

(defn start-services!
  "Start all initialized services"
  [container]
  (let [instances @(:instances container)
        init-order (->> instances
                     (filter #(= :initialized (get-in % [1 :status])))
                     (map first))]

    ;;(log/info "Starting services:" init-order)

    (doseq [service-id init-order]
      (try
        (let [service-def (get-in container [:defs service-id])
              service-instance (get-in instances [service-id :instance])
              ;; If service implements Lifecycle, call start
              started (if (satisfies? Lifecycle service-instance)
                        (start service-instance container)
                        service-instance)
              _ (swap! (:instances container)
                  assoc-in [service-id :instance] started)
              _ (swap! (:instances container)
                  assoc-in [service-id :status] :started)
              ;; Call start hook if provided
              _ (when-let [hook (get-in service-def [:hooks :on-start])]
                  (hook service-id started))
              _ (notify-listeners container :started service-id started nil)
              _ (log/info "Started service" service-id)])

        (catch Exception e
          (log/error e "Failed to start service" service-id)
          (swap! (:instances container)
            assoc-in [service-id :status] :start-failed)
          (notify-listeners container :start-failed service-id nil e))))

    container))

(defn stop-services!
  "Stop all running services in reverse order"
  [container]
  (let [instances @(:instances container)
        started-ids (->> instances
                      (filter #(= :started (get-in % [1 :status])))
                      (map first)
                      reverse)]

    (log/info "Stopping services:" started-ids)

    (doseq [service-id started-ids]
      (try
        (let [service-def (get-in container [:defs service-id])
              service-instance (get-in instances [service-id :instance])]

          ;; If service implements Lifecycle, call stop
          (when (satisfies? Lifecycle service-instance)
            (stop service-instance container))

          (swap! (:instances container)
            assoc-in [service-id :status] :stopped)

          ;; Call stop hook if provided
          (when-let [hook (get-in service-def [:hooks :on-stop])]
            (hook service-id service-instance))

          (notify-listeners container :stopped service-id service-instance nil)

          (log/info "Stopped service" service-id))

        (catch Exception e
          (log/error e "Error stopping service" service-id)
          (notify-listeners container :stop-failed service-id nil e))))

    container))

;; ============================================================================
;; Service Discovery and Retrieval
;; ============================================================================

(defn list-services
  "List all registered service IDs"
  [container]
  (keys (:defs container)))

(defn list-active-services
  "List service IDs that are configured as active"
  [container]
  (get-active-services container))

(defn service-status
  "Get the current status of a service"
  [container service-id]
  (get-in @(:instances container) [service-id :status] :not-initialized))

(defn service-statuses
  "Get status map for all services"
  [container]
  (->> @(:instances container)
    (map (fn [[id info]] [id (:status info)]))
    (into {})))

(defn get-service
  "Get a service instance by ID"
  ([container service-id]
   (get-service container service-id nil))
  ([container service-id default]
   (get-in @(:instances container) [service-id :instance] default)))

(defn get-service!
  "Get a service instance by ID, throwing if not found or failed"
  [container service-id]
  (let [service-info (get @(:instances container) service-id)]
    (cond
      (nil? service-info)
      (throw (ex-info "Service not found" {:service-id service-id}))

      (= :failed (:status service-info))
      (throw (ex-info "Service initialization failed"
               {:service-id service-id
                :error (:error service-info)}))

      (not= :started (:status service-info))
      (throw (ex-info "Service not started"
               {:service-id service-id
                :status (:status service-info)}))

      :else (:instance service-info))))

;; ============================================================================
;; Convenience Functions
;; ============================================================================

(defn create-and-boot-system
  "Create container, register services, and boot the system"
  [config service-registration-fn]
  (-> (create-container config)
    service-registration-fn
    initialize-services!
    start-services!))

(defn shutdown-system!
  "Gracefully shutdown the system"
  [container]
  (stop-services! container))

;; ============================================================================
;; Configuration Helpers
;; ============================================================================

(defn config-based-active?
  "Default active? predicate based on config"
  [service-id]
  (fn [config]
    (get-in config [:services service-id :enabled?] true)))

(defn create-simple-service
  "Create a simple service definition"
  [id ctor & {:keys [deps active? hooks]
              :or {deps #{} active? (config-based-active? id) hooks {}}}]
  (->ServiceDef id ctor deps active? hooks))
