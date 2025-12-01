(ns app.template.shared.auth
  "Generic SaaS authentication and authorization infrastructure.
   Provides core auth concepts that work across any multi-tenant SaaS application.")

;; -------------------------
;; Core User Roles (Generic)
;; -------------------------

(def role-owner "owner")
(def role-admin "admin")
(def role-member "member")
(def role-viewer "viewer")
(def role-unassigned "unassigned")

;; Core roles (domain-specific roles added separately)
(def core-roles #{role-owner role-admin role-member role-viewer role-unassigned})

;; Core role hierarchy (higher index = more permissions)
(def core-role-hierarchy [role-unassigned role-viewer role-member role-admin role-owner])

;; -------------------------
;; User Status Constants
;; -------------------------

(def status-active "active")
(def status-inactive "inactive")
(def status-pending "pending")
(def status-suspended "suspended")

(def valid-user-statuses #{status-active status-inactive status-pending status-suspended})

;; -------------------------
;; Authentication Providers
;; -------------------------

(def provider-google :google)
(def provider-github :github)

(def valid-providers #{provider-google provider-github})

;; -------------------------
;; Session Constants
;; -------------------------

(def session-timeout-hours 24)
(def session-timeout-ms (* session-timeout-hours 60 60 1000))

;; -------------------------
;; Core Permission Functions (Framework)
;; -------------------------

(defn get-user-permissions
  "Get permissions based on user role. Override in domain for specific role-permission mappings.
   Arity-1 returns empty set when no map is provided (single-tenant default)."
  ([user role-permissions-map]
   (when user
     (let [role (:role user)]
       (get role-permissions-map role #{}))))
  ([user]
   (get-user-permissions user {})))

(defn user-can?
  "Check if user has specific permission"
  [user permission permissions-map]
  (when user
    (contains? (get-user-permissions user permissions-map) permission)))

(defn has-role?
  "Check if user has specific role"
  [user role]
  (when user
    (= (:role user) role)))

(defn role-level
  "Get the hierarchy level of a role (higher = more permissions)"
  [role role-hierarchy]
  (when role
    (.indexOf role-hierarchy role)))

(defn role-at-least?
  "Check if user has at least the specified role level"
  [user min-role role-hierarchy]
  (when user
    (let [user-level (role-level (:role user) role-hierarchy)
          min-level (role-level min-role role-hierarchy)]
      (and user-level min-level (>= user-level min-level)))))

(defn is-owner?
  "Check if user is tenant owner"
  [user]
  (has-role? user role-owner))

(defn is-admin-or-higher?
  "Check if user is admin or higher"
  [user role-hierarchy]
  (role-at-least? user role-admin role-hierarchy))

(defn is-member-or-higher?
  "Check if user is member or higher"
  [user role-hierarchy]
  (role-at-least? user role-member role-hierarchy))

;; -------------------------
;; Role Display Functions
;; -------------------------

(defn core-role-display-name
  "Get user-friendly display name for core roles"
  [role]
  (case role
    "owner" "Owner"
    "admin" "Administrator"
    "member" "Member"
    "viewer" "Viewer"
    nil))

(defn core-role-description
  "Get description of core role capabilities"
  [role]
  (case role
    "owner" "Full access to all tenant resources and settings"
    "admin" "Administrative access to tenant resources"
    "member" "Standard member access"
    "viewer" "View-only access"
    nil))

;; -------------------------
;; Session helpers
;; -------------------------

(defn get-auth-status
  "Lightweight auth status helper for single-tenant template.
   Falls back to session data; includes optional legacy OAuth tokens."
  [auth-session oauth-tokens _get-user-info]
  (let [user (:user auth-session)
        tenant (:tenant auth-session)
        permissions (:permissions auth-session)
        legacy-session? (boolean (:legacy-session? auth-session))]
    {:authenticated (boolean user)
     :session-valid true
     :legacy-session legacy-session?
     :provider (:provider auth-session)
     :tokens oauth-tokens
     :user user
     :tenant tenant
     :permissions permissions}))

;; -------------------------
;; Validation Functions
;; -------------------------

(defn valid-core-role?
  "Check if role is a valid core role"
  [role]
  (contains? core-roles role))

(defn valid-user-status?
  "Check if user status is valid"
  [status]
  (contains? valid-user-statuses status))

(defn valid-provider?
  "Check if auth provider is valid"
  [provider]
  (contains? valid-providers provider))

;; -------------------------
;; User Status Functions
;; -------------------------

(defn user-active?
  "Check if user is active"
  [user]
  (when user
    (= (:status user) status-active)))

(defn user-pending?
  "Check if user is pending"
  [user]
  (when user
    (= (:status user) status-pending)))

(defn user-suspended?
  "Check if user is suspended"
  [user]
  (when user
    (= (:status user) status-suspended)))

;; -------------------------
;; Provider Functions
;; -------------------------

(defn provider-display-name
  "Get display name for auth provider"
  [provider]
  (case provider
    :google "Google"
    :github "GitHub"
    (str "Unknown (" provider ")")))

;; -------------------------
;; Role Badge Utilities (Frontend)
;; -------------------------

#?(:cljs
   (defn role-badge-class
     "Get CSS class for role badge"
     [_role is-owner?]
     (str "ds-badge ds-badge-sm "
       (if is-owner? "ds-badge-primary" "ds-badge-secondary"))))

;; -------------------------
;; Core Error Messages
;; -------------------------

(def core-error-messages
  {:unauthorized "Authentication required"
   :forbidden "Access denied - insufficient permissions"
   :invalid-role "Invalid user role"
   :invalid-status "Invalid user status"
   :invalid-provider "Invalid authentication provider"
   :session-expired "Session has expired, please log in again"
   :user-inactive "User account is inactive"
   :user-suspended "User account is suspended"
   :tenant-access-denied "Access to this tenant is denied"})
