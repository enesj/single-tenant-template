(ns app.template.shared.auth
    "Generic SaaS authentication and authorization infrastructure.

    NOTE: This namespace is kept for backward compatibility with older template
    code. The canonical single-tenant helpers now live in `app.shared.auth`."
    (:require
        [app.shared.auth :as shared-auth]))

;; -------------------------
;; Core User Roles (Generic)
;; -------------------------
(def role-admin shared-auth/role-admin)
(def role-member shared-auth/role-member)

;; -------------------------
;; User Status Constants
;; -------------------------

;; -------------------------
;; Authentication Providers
;; -------------------------

;; -------------------------
;; Session Constants
;; -------------------------

;; -------------------------
;; Core Permission Functions (Framework)
;; -------------------------

(def get-user-permissions shared-auth/get-user-permissions)

;; -------------------------
;; Role Badge Utilities (Frontend)
;; -------------------------

;; Commented out - unused
;; #?(:cljs
;;    (defn role-badge-class
;;      "Get CSS class for role badge"
;;      [_role is-owner?]
;;      (str "ds-badge ds-badge-sm "
;;        (if is-owner? "ds-badge-primary" "ds-badge-secondary"))))

;; -------------------------
;; Core Error Messages
;; -------------------------

