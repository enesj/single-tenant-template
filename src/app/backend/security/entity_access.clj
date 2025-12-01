(ns app.backend.security.entity-access
  "Entity access control for generic CRUD routes.

   This module defines which entities are allowed through generic CRUD endpoints
   vs. which must be accessed only through specific admin or protected routes."
  (:require
    [clojure.set :as set]
    [taoensso.timbre :as log]))

;; ================================================================================
;; Entity Access Control Configuration
;; ================================================================================

(def ^:private admin-only-entities
  "Entities that can ONLY be accessed through admin API routes.
   These entities contain sensitive system data and should never be exposed
   through generic CRUD endpoints."
  #{:admins
    :admin-sessions
    :audit-logs})

(def ^:private protected-entities
  "Entities with special access rules that require custom authorization."
  #{:users})

(def ^:private public-entities
  "Entities allowed through generic CRUD (none in single-tenant admin)."
  #{})

;; ================================================================================
;; Access Control Functions
;; ================================================================================

(defn admin-only-entity?
  "Check if an entity can only be accessed through admin routes."
  [entity-key]
  (contains? admin-only-entities entity-key))

(defn protected-entity?
  "Check if an entity has special access rules."
  [entity-key]
  (contains? protected-entities entity-key))

(defn public-entity?
  "Check if an entity is safe for normal CRUD operations."
  [entity-key]
  (contains? public-entities entity-key))

(defn entity-allowed-for-generic-crud?
  "Check if an entity is allowed through generic CRUD endpoints.

   Args:
   - entity-key: Keyword representing the entity (e.g., :users, :properties)
   - is-admin?: Boolean indicating if this is an admin request

   Returns:
   - true if the entity can be accessed through generic CRUD
   - false if it should be blocked"
  [entity-key is-admin?]
  (cond
    ;; Admin-only entities are NEVER allowed through generic CRUD
    (admin-only-entity? entity-key)
    (do
      (log/warn "🚫 BLOCKED: Admin-only entity access attempt"
        {:entity entity-key :is-admin is-admin?})
      false)

    ;; Protected entities are allowed but will have special handling
    (protected-entity? entity-key)
    (do
      (log/info "⚠️  PROTECTED: Entity requires special authorization"
        {:entity entity-key :is-admin is-admin?})
      true)

    ;; Public tenant entities are allowed for authenticated users
    (public-entity? entity-key)
    (do
      (log/debug "✅ ALLOWED: Public entity access"
        {:entity entity-key :is-admin is-admin?})
      true)

    ;; Unknown entities are blocked by default (fail-safe)
    :else
    (do
      (log/warn "🚫 BLOCKED: Unknown entity - blocking for security"
        {:entity entity-key :is-admin is-admin?})
      false)))

(defn get-blocked-entity-response
  "Generate appropriate response for blocked entity access."
  [entity-key reason]
  {:status 403
   :headers {"Content-Type" "application/json"}
   :body {:error "Entity access not allowed"
          :entity (name entity-key)
          :reason reason
          :suggestion (case reason
                        :admin-only "This entity can only be accessed through admin API"
                        :unknown "Entity not found in allowed list"
                        :security "Access blocked for security reasons"
                        "Contact administrator for access")}})

;; ================================================================================
;; Entity List Functions
;; ================================================================================

(defn get-allowed-entities-for-user
  "Get list of entities a regular user can access through generic CRUD."
  []
  (set/union public-entities protected-entities))

(defn get-all-known-entities
  "Get all entities known to the access control system."
  []
  (set/union admin-only-entities protected-entities public-entities))

;; ================================================================================
;; Validation Functions
;; ================================================================================

(defn validate-entity-access-config
  "Validate that entity access configuration is consistent.

   This should be called during application startup to ensure no entities
   are accidentally in multiple categories."
  []
  (let [admin-count (count admin-only-entities)
        protected-count (count protected-entities)
        public-count (count public-entities)
        total-defined (+ admin-count protected-count public-count)]

    (log/info "Entity access control configuration:"
      {:admin-only-entities admin-count
       :protected-entities protected-count
       :public-entities public-count
       :total-entities total-defined})

    ;; Check for overlaps between categories
    (let [overlaps (set/intersection admin-only-entities
                     (set/union protected-entities public-entities))]
      (when (seq overlaps)
        (log/error "❌ Entity access config error: Overlapping entity definitions"
          {:overlapping-entities overlaps})
        (throw (ex-info "Entity access configuration has overlapping definitions"
                 {:overlapping-entities overlaps}))))

    (log/info "✅ Entity access control configuration validated successfully")
    true))

;; ================================================================================
;; Development and Debugging
;; ================================================================================

(defn log-entity-access-attempt
  "Log entity access attempts for security monitoring."
  [entity-key allowed? request-info]
  (log/info (if allowed? "🔓 ENTITY ACCESS GRANTED" "🔒 ENTITY ACCESS DENIED")
    (merge {:entity entity-key :allowed allowed?} request-info)))

(comment
  ;; Example usage:
  (entity-allowed-for-generic-crud? :properties false)  ; => true
  (entity-allowed-for-generic-crud? :admins false)      ; => false

  (get-allowed-entities-for-user)

  (validate-entity-access-config))
  ; => true (logs validation results)
