(ns app.template.backend.routes.impersonation
  "User-side impersonation grant management routes.
   Mounted under /api/v1/tenant/impersonation-grants.
   All routes require owner role."
  (:require
    [app.shared.http :refer [json-response]]
    [app.template.backend.routes.utils :as route-utils]
    [app.template.backend.services.impersonation :as impersonation-svc]
    [app.template.backend.services.tenant :as tenant-svc]
    [clojure.walk :as walk]))

;; ============================================================================
;; Helpers
;; ============================================================================

(defn- sanitize
  "Walk a data structure and stringify Java types for JSON serialization."
  [obj]
  (walk/postwalk
    (fn [x]
      (cond
        (instance? java.util.UUID x) (str x)
        (instance? java.time.LocalDateTime x) (str x)
        (instance? java.time.OffsetDateTime x) (str x)
        (instance? java.time.Instant x) (str x)
        (instance? java.sql.Timestamp x) (str x)
        (instance? java.math.BigDecimal x) (str x)
        :else x))
    obj))

(defn- get-user [req]
  (or (get-in req [:session :auth-session :user])
    (get-in req [:session :user])))

(defn- get-tenant [req]
  (get-in req [:session :auth-session :tenant]))

(defn- require-owner!
  "Throw 403 unless the user is a tenant owner."
  [db tenant-id user-id]
  (let [membership (tenant-svc/get-membership db tenant-id user-id)]
    (when-not membership
      (throw (ex-info "Not a member of this tenant"
               {:type :forbidden
                :errors {:tenant ["You are not a member of this tenant"]}})))
    (let [role (or (:role membership) (:tenant_memberships/role membership))]
      (when-not (= role "owner")
        (throw (ex-info "Only tenant owners can manage impersonation grants"
                 {:type :forbidden
                  :errors {:role ["Requires owner role"]}}))))
    membership))

;; ============================================================================
;; Handlers
;; ============================================================================

(defn- list-grants-handler
  "GET /tenant/impersonation-grants — list all grants for the current tenant."
  [db]
  (fn [req]
    (route-utils/with-error-handling "impersonation-grants-list"
      (let [user      (get-user req)
            tenant    (get-tenant req)
            _         (when-not tenant
                        (throw (ex-info "No active tenant"
                                 {:type :validation-error
                                  :errors {:tenant ["No active tenant in session"]}})))
            tenant-id (or (:id tenant) (:tenants/id tenant))
            _         (require-owner! db tenant-id (:id user))
            grants    (impersonation-svc/list-grants-for-tenant db tenant-id)]
        (json-response {:grants (sanitize grants)})))))

(defn- create-grant-handler
  "POST /tenant/impersonation-grants — create a grant for a platform admin."
  [db]
  (fn [req]
    (route-utils/with-error-handling "impersonation-grant-create"
      (let [user      (get-user req)
            tenant    (get-tenant req)
            _         (when-not tenant
                        (throw (ex-info "No active tenant"
                                 {:type :validation-error
                                  :errors {:tenant ["No active tenant in session"]}})))
            tenant-id (or (:id tenant) (:tenants/id tenant))
            _         (require-owner! db tenant-id (:id user))
            {:keys [admin-email role]} (:body-params req)
            _         (when-not admin-email
                        (throw (ex-info "admin-email is required"
                                 {:type :validation-error
                                  :errors {:admin-email ["admin-email is required"]}})))
            grant     (impersonation-svc/create-grant!
                        db {:admin-email admin-email
                            :tenant-id   tenant-id
                            :role        (or role "viewer")
                            :granted-by  (:id user)})]
        (json-response {:success true :grant (sanitize grant)})))))

(defn- revoke-grant-handler
  "DELETE /tenant/impersonation-grants/:id — revoke a grant."
  [db]
  (fn [req]
    (route-utils/with-error-handling "impersonation-grant-revoke"
      (let [user      (get-user req)
            tenant    (get-tenant req)
            _         (when-not tenant
                        (throw (ex-info "No active tenant"
                                 {:type :validation-error
                                  :errors {:tenant ["No active tenant in session"]}})))
            tenant-id (or (:id tenant) (:tenants/id tenant))
            _         (require-owner! db tenant-id (:id user))
            grant-id  (get-in req [:path-params :id])]
        (impersonation-svc/revoke-grant-by-owner! db grant-id (:id user))
        (json-response {:success true})))))

;; ============================================================================
;; Route Tree
;; ============================================================================

(defn routes
  "Impersonation grant routes for tenant owners."
  [db]
  ["/impersonation-grants"
   [""    {:get  {:handler (list-grants-handler db)}
           :post {:handler (create-grant-handler db)}}]
   ["/:id" {:delete {:handler (revoke-grant-handler db)}}]])

(comment
  ;; (require 'app.template.backend.routes.impersonation :reload)
  :rcf)
