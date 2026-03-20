(ns app.admin.frontend.events.users.template.form-interceptors
  "Form submission interceptors for admin context.
   
   This module intercepts form submissions in the admin context
   and routes them through the bridge CRUD system instead of the template's
   direct HTTP calls. This ensures consistent success handling and highlighting.
   
   This interceptor works for any entity in the admin context - it uses
   a registry to determine which entities should be routed through the bridge."
  (:require
    [app.shared.model-naming :as model-naming]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.events.form]     ;; Ensure template form events are loaded first
    [clojure.string :as str]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; Registry of entities that should be routed through bridge system in admin context
(defonce ^:private bridge-entities (atom #{:users}))

(defn register-bridge-entity!
  "Register an entity to be routed through the bridge system in admin context.
   Domain adapters should call this during initialization for their entities."
  [entity-key]
  (swap! bridge-entities conj entity-key)
  (log/debug "Registered bridge entity" {:entity entity-key}))

(defn- convert-keys-to-db
  "Convert all keys in a map from kebab-case to snake_case for database/API compatibility"
  [m]
  (model-naming/app-map-keys->db m))

(def ^:private backlog-type-aliases
  {"issue" "Issue"
   "feature" "Feature"
   "refactoring" "Refactoring"
   "review" "Review"
   "improvment" "Improvement"
   "improvement" "Improvement"})

(def ^:private backlog-status-aliases
  {"waiting" "Waiting"
   "in progres" "In progress"
   "in progress" "In progress"
   "completed" "Completed"
   "need improvments" "Need improvements"
   "need improvements" "Need improvements"})

(defn- backlog-option-value
  [value]
  (if (map? value)
    (or (:value value)
      (get value "value")
      value)
    value))

(defn- canonical-backlog-value
  [aliases value]
  (let [normalized (some-> value backlog-option-value str str/trim)
        lowered (some-> normalized str/lower-case)]
    (or (get aliases lowered) normalized)))

(defn- normalize-backlog-field
  [request-params field aliases]
  (let [field-name (name field)]
    (cond
      (contains? request-params field)
      (if-let [canonical (canonical-backlog-value aliases (get request-params field))]
        (assoc request-params field canonical)
        (dissoc request-params field))

      (contains? request-params field-name)
      (if-let [canonical (canonical-backlog-value aliases (get request-params field-name))]
        (assoc request-params field-name canonical)
        (dissoc request-params field-name))

      :else request-params)))

(defn- normalize-backlog-request-params
  [entity-k request-params]
  (if (= :backlog entity-k)
    (-> request-params
      (normalize-backlog-field :type backlog-type-aliases)
      (normalize-backlog-field :status backlog-status-aliases))
    request-params))

(rf/reg-event-fx
  :app.template.frontend.events.form/submit-form
  (fn [{:keys [db]} [_ {:keys [entity-name editing values] :as _form-data}]]
    (let [entity-k (keyword entity-name)
          ;; Safe check for admin context - handles Node tests where window may not exist
          in-admin? (and (exists? js/window)
                      (some? (.-location js/window))
                      (str/includes? (or (.-pathname js/window.location) "") "/admin"))
          ;; Convert keys to snake_case for API
          db-values (convert-keys-to-db values)
          db-values (normalize-backlog-request-params entity-k db-values)
          ;; For PUT requests, exclude :id from request body since it's in URL
          request-params (if editing
                           (dissoc db-values :id)
                           db-values)
          ;; Check if this entity should use bridge system
          use-bridge? (contains? @bridge-entities entity-k)]
      ;; Debug logging for form submission
      (log/info "📝 Form submit interceptor:"
        {:entity-name entity-name
         :entity-k entity-k
         :editing editing
         :in-admin? in-admin?
         :use-bridge? use-bridge?
         :values values
         :db-values db-values})
      (cond
        ;; Admin bridge entity edit
        (and use-bridge? editing in-admin?)
        {:db (assoc-in db (paths/form-submitting? entity-k) true)
         :dispatch [:app.template.frontend.events.list.crud/update-entity entity-k (:id values) (dissoc db-values :id)]}

        ;; Admin bridge entity create
        (and use-bridge? (not editing) in-admin?)
        {:db (assoc-in db (paths/form-submitting? entity-k) true)
         :dispatch [:app.template.frontend.events.list.crud/create-entity entity-k db-values]}

        ;; Fallback to template default - inline the logic directly 
        ;; to avoid dispatch timing issues in tests
        :else
        (let [request (if editing
                        ((if in-admin?
                           http/update-entity-admin
                           http/update-entity)
                         {:entity-name (name entity-name)
                          :id (:id values)
                          :data request-params
                          :on-success [:app.template.frontend.events.form/update-success entity-name]
                          :on-failure [:app.template.frontend.events.form/update-failure entity-name]})
                        ((if in-admin?
                           http/create-entity-admin
                           http/create-entity)
                         {:entity-name (name entity-name)
                          :data request-params
                          :on-success [:app.template.frontend.events.form/create-success entity-name]
                          :on-failure [:app.template.frontend.events.form/create-failure entity-name]}))]
          {:db (assoc-in db (paths/form-submitting? entity-name) true)
           :http-xhrio request})))))

;; Note: The separate :admin.template.form/submit-user-edit and :admin.template.form/submit-user-create
;; events are no longer needed since we now route through the bridge system.
;; The bridge system handles admin-specific HTTP endpoints via the registered adapter.
