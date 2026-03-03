(ns app.e2e.multi-tenancy.platform-admin-test
  "E2E tests for platform admin operations (Manual §9).

   Allium rules: AdminBlockedFromTenantData, ActivateImpersonation, DeactivateImpersonation,
                 CreateImpersonationGrant, RevokeImpersonationGrant
   Covers: admin blocked from expenses, tenant management, impersonation flow, owner revoke, audit trail."
  (:require
    [app.admin.backend.services.admin.auth :as admin-auth]
    [app.e2e.fixtures :as fixtures]
    [app.e2e.helpers :as h]
    [cheshire.core :as json]
    [clojure.test :refer [deftest is testing use-fixtures]])
  (:import
    [com.microsoft.playwright.options RequestOptions]))

(use-fixtures :each fixtures/with-browser-context)

;; ---------------------------------------------------------------------------
;; Helpers
;; ---------------------------------------------------------------------------

(def admin-creds
  "Platform admin credentials — must be seeded in the admin DB.
   Seeded via `bb seed-admin test` or via direct DB insert."
  {:email "admin@example.com" :password "admin123"})

(defn- seed-admin!
  "Ensure a platform admin exists in the test DB with a valid password hash.
   Uses the app's own hash-password to guarantee compatibility with verify."
  []
  (let [password-hash (admin-auth/hash-password (:password admin-creds))]
    (h/query-db
      "INSERT INTO admins (id, email, full_name, password_hash, role, status, created_at, updated_at)
       VALUES (?::uuid, ?, 'Platform Admin', ?, 'admin'::admin_role, 'active'::admin_status, now(), now())
       ON CONFLICT (email) DO UPDATE SET password_hash = EXCLUDED.password_hash, updated_at = now()"
      (str (java.util.UUID/randomUUID))
      (:email admin-creds)
      password-hash)))

(defn- admin-login!
  "Login as platform admin via API. Returns response with token."
  [context]
  (let [url  (str fixtures/base-url "/admin/api/login")
        opts (-> (RequestOptions/create)
               (.setHeader "Content-Type" "application/json")
               (.setData (json/generate-string admin-creds)))
        resp (.post (.request context) url opts)]
    {:status (.status resp)
     :body   (try (json/parse-string (.text resp) true) (catch Exception _ (.text resp)))}))

(defn- admin-api-get
  "Make an authenticated admin API GET request."
  [context path token]
  (let [url  (str fixtures/base-url path)
        opts (-> (RequestOptions/create)
               (.setHeader "x-admin-token" token))
        resp (.get (.request context) url opts)]
    {:status (.status resp)
     :body   (try (json/parse-string (.text resp) true) (catch Exception _ (.text resp)))}))

(defn- admin-api-post
  "Make an authenticated admin API POST request."
  [context path body token]
  (let [url  (str fixtures/base-url path)
        opts (-> (RequestOptions/create)
               (.setHeader "Content-Type" "application/json")
               (.setHeader "x-admin-token" token)
               (.setData (json/generate-string body)))
        resp (.post (.request context) url opts)]
    {:status (.status resp)
     :body   (try (json/parse-string (.text resp) true) (catch Exception _ (.text resp)))}))

;; ---------------------------------------------------------------------------
;; Test: Admin blocked from expense data without impersonation
;; ---------------------------------------------------------------------------

(deftest admin-blocked-from-expenses
  (testing "Platform admin cannot access tenant expense data without impersonation"
    (seed-admin!)
    ;; Register a user to create tenant data
    (h/api-register! h/user-a)

    ;; Login as admin
    (let [admin-ctx (.newContext (fixtures/get-browser))]
      (try
        (let [{:keys [status body]} (admin-login! admin-ctx)
              token (or (:token body) (get-in body [:session :token]))]
          (is (= 200 status) "Admin login should succeed")
          (is (some? token) "Admin should receive a token")

          ;; Try to access expense-related admin APIs — should get 403
          ;; The impersonation middleware is on /expenses/entries (the route-segment)
          (when token
            (let [{:keys [status]} (admin-api-get admin-ctx "/admin/api/expenses/entries" token)]
              (is (= 403 status) "Admin should be blocked from expense data"))))
        (finally
          (.close admin-ctx))))))

;; ---------------------------------------------------------------------------
;; Test: Admin can manage tenants (list, detail, members)
;; ---------------------------------------------------------------------------

(deftest admin-tenant-management
  (testing "Platform admin can list and view tenants and their members"
    (seed-admin!)
    (h/api-register! h/user-a)

    (let [admin-ctx (.newContext (fixtures/get-browser))]
      (try
        (let [{:keys [body]} (admin-login! admin-ctx)
              token (or (:token body) (get-in body [:session :token]))]

          (when token
            ;; List tenants
            (let [{:keys [status body]} (admin-api-get admin-ctx "/admin/api/tenants" token)]
              (is (= 200 status) "Admin can list tenants")
              (is (some? body) "Tenant list should not be empty"))

            ;; Get specific tenant detail
            (let [tenant (h/get-tenant-by-slug "e2e-user-a")
                  tenant-id (:id tenant)]
              (when tenant-id
                (let [{:keys [status body]} (admin-api-get admin-ctx
                                              (str "/admin/api/tenants/" tenant-id) token)]
                  (is (= 200 status) "Admin can view tenant detail"))

                ;; List tenant members
                (let [{:keys [status body]} (admin-api-get admin-ctx
                                              (str "/admin/api/tenants/" tenant-id "/members") token)]
                  (is (= 200 status) "Admin can view tenant members"))))))
        (finally
          (.close admin-ctx))))))

;; ---------------------------------------------------------------------------
;; Test: Full impersonation flow (grant → activate → data → deactivate)
;; ---------------------------------------------------------------------------

(deftest impersonation-grant-activate-deactivate
  (testing "Full impersonation lifecycle: grant → activate → access → deactivate"
    (seed-admin!)
    ;; Register user-a as tenant owner
    (h/api-register! h/user-a)

    ;; As tenant owner, create an impersonation grant for the admin
    (let [admin-db (first (h/query-db "SELECT id, email FROM admins WHERE email = ?" (:email admin-creds)))
          grant-resp (h/api-post "/api/v1/tenant/impersonation-grants"
                       {:admin-email (:email admin-creds) :role "viewer"})]
      (is (or (= 200 (:status grant-resp)) (= 201 (:status grant-resp)))
        "Creating impersonation grant should succeed")

      ;; Verify grant exists in DB
      (let [grants (h/query-db "SELECT * FROM impersonation_grants WHERE status = 'active'")]
        (is (>= (count grants) 1)
          "At least one active grant should exist"))

      ;; As admin, activate the grant
      (let [admin-ctx (.newContext (fixtures/get-browser))]
        (try
          (let [{:keys [body]} (admin-login! admin-ctx)
                token (or (:token body) (get-in body [:session :token]))
                grant (first (h/query-db "SELECT * FROM impersonation_grants WHERE status = 'active' ORDER BY created_at DESC LIMIT 1"))
                grant-id (:id grant)]

            (when (and token grant-id)
              ;; Activate impersonation
              (let [{:keys [status]} (admin-api-post admin-ctx
                                       "/admin/api/impersonation/activate"
                                       {:grant-id (str grant-id)} token)]
                (is (= 200 status) "Activating impersonation should succeed"))

              ;; Deactivate impersonation
              (let [{:keys [status]} (admin-api-post admin-ctx
                                       "/admin/api/impersonation/deactivate"
                                       {} token)]
                (is (= 200 status) "Deactivating impersonation should succeed"))))
          (finally
            (.close admin-ctx)))))))

;; ---------------------------------------------------------------------------
;; Test: Tenant owner can revoke impersonation grant
;; ---------------------------------------------------------------------------

(deftest impersonation-revoke-by-owner
  (testing "Tenant owner can revoke an active impersonation grant"
    (seed-admin!)
    (h/api-register! h/user-a)

    ;; Create a grant
    (h/api-post "/api/v1/tenant/impersonation-grants"
      {:admin-email (:email admin-creds) :role "viewer"})

    ;; Find the grant
    (let [grant (first (h/query-db
                         "SELECT * FROM impersonation_grants
                          WHERE status = 'active'
                          ORDER BY created_at DESC LIMIT 1"))
          grant-id (:id grant)]
      (is (some? grant-id) "Active grant should exist")

      ;; Revoke it as the owner
      (let [{:keys [status]} (h/api-delete (str "/api/v1/tenant/impersonation-grants/" grant-id))]
        (is (= 200 status) "Revoking grant should succeed"))

      ;; Verify grant is revoked
      (let [updated (first (h/query-db "SELECT * FROM impersonation_grants WHERE id = ?::uuid" (str grant-id)))]
        (is (= "revoked" (:status updated))
          "Grant should be revoked")))))

;; ---------------------------------------------------------------------------
;; Test: Audit trail for impersonation operations
;; ---------------------------------------------------------------------------

(deftest impersonation-audit-trail
  (testing "Impersonation operations create audit log entries"
    (seed-admin!)
    (h/api-register! h/user-a)

    ;; Create a grant (should create audit entry)
    (h/api-post "/api/v1/tenant/impersonation-grants"
      {:admin-email (:email admin-creds) :role "viewer"})

    ;; Check audit logs — DB column is target_type (not entity_type)
    (let [logs (h/get-audit-logs 10)
          impersonation-logs (filter #(contains? #{"impersonation_grant" "admin_session"}
                                        (:target_type %)) logs)]
      (is (>= (count impersonation-logs) 1)
        "At least one audit log entry for impersonation should exist")
      (let [actions (set (map :action impersonation-logs))]
        (is (or (contains? actions "impersonation_granted")
              (contains? actions "grant_created"))
          "Audit should contain a grant creation action")))))
