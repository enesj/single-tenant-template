(ns app.template.frontend.db.validation
  (:require
    [app.template.frontend.db.schemas :as schemas]
    [malli.core :as m]
    [malli.error :as me]))

(def ^:private validation-log-limit 5)
(def ^:private validation-log-window-ms 5000)
(defonce ^:private validation-log-state (volatile! {:window-start 0 :count 0}))

(defn- now-ms
  []
  (.now js/Date))

(defn- allow-validation-log?
  []
  (let [timestamp (now-ms)
        {:keys [window-start count]} @validation-log-state
        window-elapsed? (> (- timestamp window-start) validation-log-window-ms)]
    (if window-elapsed?
      (do (vreset! validation-log-state {:window-start timestamp :count 1})
        true)
      (if (< count validation-log-limit)
        (do (vswap! validation-log-state update :count inc)
          true)
        false))))

(defn log-validation-error!
  [strict? event exception]
  (when (allow-validation-log?)
    (let [event-id (when (vector? event) (first event))
          data (some-> (ex-data exception) (dissoc :db))
          schema-path (get-in data [:explanation :schema-path])
          payload (cond-> {:event event
                           :event-id event-id
                           :strict-mode? strict?
                           :message (ex-message exception)}
                    (:error data) (assoc :humanized (:error data))
                    schema-path (assoc :schema-path schema-path))
          log-fn (if strict? js/console.error js/console.warn)]
      (log-fn "app-db spec validation failed" (:event-id payload)))))

(def initialization-events
  #{:app.template.frontend.events.bootstrap/initialize-db
    :app.template.frontend.events.config/fetch-config-success
    :page/init-entities
    :page/init-login
    :page/init-logout})

(defn initialization-event?
  [event-id]
  (contains? initialization-events event-id))

(defn should-validate-event?
  [models-data event-id]
  (and models-data
    (or (nil? event-id)
      (not (initialization-event? event-id)))))

(defn debug-validate-critical-state
  [db]
  (when ^boolean goog.DEBUG
    (when-let [_error (m/explain schemas/critical-state-schema db)]
      nil))
  db)

(defn validate-db
  "Validates the db against the schema. Returns the db if valid, throws an error if not."
  ([db]
   (validate-db db nil))
  ([db models-data]
   (let [schema (if models-data
                  (schemas/make-app-db-schema models-data)
                  schemas/app-db-schema)]
     (if-let [error (m/explain schema db)]
       (let [humanized (me/humanize error)
             error-details (-> error
                             (dissoc :value)
                             (assoc :schema-path (->> error :errors (mapv :path))))]
         (when ^boolean goog.DEBUG
           nil)
         (throw (ex-info "app-db validation failed"
                  {:error humanized
                   :explanation error-details
                   :db db})))
       db))))
