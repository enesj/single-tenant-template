(ns app.shared.auth
  "Cross-platform authentication/authorization helpers.

  This namespace intentionally stays small and data-oriented so it can be
  reused by both the backend (Clojure) and the frontend (ClojureScript).

  NOTE: This project uses *string* roles in DB rows (e.g. \"admin\"), while UI
  code often prefers *keyword* roles (e.g. :admin). Helpers here accept either."
  (:require
    [clojure.string :as str]))

;; ---------------------------------------------------------------------------
;; Core roles (single-tenant template)
;; ---------------------------------------------------------------------------

;; String constants (DB-friendly)
(def role-owner "owner")
(def role-admin "admin")
(def role-member "member")
(def role-viewer "viewer")
(def role-unassigned "unassigned")

;; Keyword roles (UI-friendly)
(def core-roles
  #{:owner :admin :member :viewer :unassigned})

;; Lower index = fewer permissions, higher index = more permissions
(def core-role-hierarchy
  [:unassigned :viewer :member :admin :owner])

(defn role->keyword
  "Normalize a role value to a keyword.

  Accepts keywords/strings/symbols. Strings are lower-cased and normalized so
  both \"super_admin\" and \"super-admin\" become :super-admin.

  Returns nil for nil/blank inputs." 
  [role]
  (cond
    (nil? role) nil
    (keyword? role) role
    (symbol? role) (role->keyword (name role))
    (string? role) (let [s (-> role str/trim str/lower-case)]
                    (when-not (str/blank? s)
                      (-> s
                        (str/replace "_" "-")
                        keyword)))
    :else (role->keyword (str role))))

(defn role->string
  "Normalize a role value to a DB-friendly string.

  Returns nil for nil/blank inputs." 
  [role]
  (let [k (role->keyword role)]
    (when k
      (name k))))

(defn valid-role?
  "True when `role` is one of the supported core roles." 
  [role]
  (contains? core-roles (role->keyword role)))

(defn role-level
  "Return the hierarchy index of `role` or nil when unknown." 
  [role]
  (let [k (role->keyword role)
        idx (.indexOf core-role-hierarchy k)]
    (when (>= idx 0) idx)))

(defn role-includes?
  "True when `role` is at least as privileged as `min-role`.

  Examples:
  - (role-includes? :admin :member) => true
  - (role-includes? \"viewer\" :member) => false" 
  [role min-role]
  (let [a (role-level role)
        b (role-level min-role)]
    (and (some? a)
      (some? b)
      (>= a b))))

;; ---------------------------------------------------------------------------
;; Permissions (optional mapping)
;; ---------------------------------------------------------------------------

(defn get-permissions-for-role
  "Lookup permissions for a role.

  `role-permissions-map` may use either keyword keys (:admin) or string keys
  (\"admin\"). Missing roles return an empty set." 
  [role role-permissions-map]
  (let [k (role->keyword role)
        s (role->string role)]
    (or (get role-permissions-map k)
      (get role-permissions-map s)
      #{})))

(defn get-user-permissions
  "Get permissions for a user record.

  Backward-compatible with older template helpers:
  - (get-user-permissions user)
  - (get-user-permissions user role-permissions-map)

  Returns nil when user is nil." 
  [user & more]
  (when user
    (let [role-permissions-map (or (first more) {})]
      (get-permissions-for-role (:role user) role-permissions-map))))

(defn calculate-user-permissions
  "Derive a permission set from a user record.

  Expects user shape like {:role \"admin\"} or {:role :admin}." 
  ([user] (calculate-user-permissions user {}))
  ([user role-permissions-map]
   (when user
     (get-permissions-for-role (:role user) role-permissions-map))))

(defn has-permission?
  "True when `permissions` contains `permission`." 
  [permissions permission]
  (contains? (set permissions) permission))

(defn has-any-permission?
  "True when `permissions` contains at least one permission in `required`." 
  [permissions required]
  (boolean
    (some (set permissions) required)))

(defn has-all-permissions?
  "True when `permissions` contains every permission in `required`." 
  [permissions required]
  (every? (set permissions) required))

;; ---------------------------------------------------------------------------
;; Session/auth status helper
;; ---------------------------------------------------------------------------

(defn get-auth-status
  "Normalize auth/session state into a stable, portable shape.

  This is intentionally lightweight (data-only). Callers may attach extra
  fields (e.g. :provider, :tokens) as needed." 
  ([auth-session] (get-auth-status auth-session nil nil))
  ([auth-session oauth-tokens _get-user-info]
   (let [user (:user auth-session)
         tenant (:tenant auth-session)
         permissions (:permissions auth-session)]
     {:authenticated (boolean user)
      :session-valid true
      :legacy-session (boolean (:legacy-session? auth-session))
      :provider (:provider auth-session)
      :tokens oauth-tokens
      :user user
      :tenant tenant
      :permissions permissions})))
