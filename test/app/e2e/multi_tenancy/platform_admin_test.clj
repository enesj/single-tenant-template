(ns app.e2e.multi-tenancy.platform-admin-test
  "E2E tests for platform admin operations (Manual §9).

   Covers: direct privacy-scrubbed admin access to expenses/receipts and
   tenant management without impersonation."
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

;; ---------------------------------------------------------------------------
;; Test: Admin blocked from expense data without impersonation
;; ---------------------------------------------------------------------------

(deftest admin-can-access-expense-data-without-impersonation
  (testing "Platform admin can access admin expenses and receipts data without impersonation"
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

          ;; Expenses and receipts are now available directly to platform admins.
          (when token
            (let [{expense-status :status} (admin-api-get admin-ctx "/admin/api/expenses/entries" token)
                  {receipt-status :status} (admin-api-get admin-ctx "/admin/api/expenses/receipts" token)]
              (is (= 200 expense-status)
                "Admin should be able to access expense data without impersonation")
              (is (= 200 receipt-status)
                "Admin should be able to access receipt data without impersonation"))))
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
                (let [{:keys [status]} (admin-api-get admin-ctx
                                         (str "/admin/api/tenants/" tenant-id) token)]
                  (is (= 200 status) "Admin can view tenant detail"))

                ;; List tenant members
                (let [{:keys [status]} (admin-api-get admin-ctx
                                         (str "/admin/api/tenants/" tenant-id "/members") token)]
                  (is (= 200 status) "Admin can view tenant members"))))))
        (finally
          (.close admin-ctx))))))

