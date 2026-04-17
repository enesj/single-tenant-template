(ns app.domain.expenses.test-helpers
  "Test helpers for the expenses domain.

  These helpers smooth over common setup steps (e.g. payers, tenants) so
  integration tests can focus on domain behavior rather than schema wiring."
  (:require
    [app.domain.backend.expenses.services.payers :as payers]
    [app.template.backend.security.email :as email-privacy]
    [app.template.backend.services.tenant :as tenant-svc]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.util UUID]))

(defn- normalize-payer-type
  [value]
  (if (= "system" (some-> value str))
    "system"
    "custom"))

(defn ensure-test-tenant!
  "Create a tenant + owner membership for testing. Returns
  {:tenant <row> :membership <row> :tenant-id <uuid>}.

  Skips configured starter payers/custom expense-categories, but tenant
  provisioning still creates the required default expense category."
  [db user]
  (let [result (tenant-svc/provision-tenant!
                 db
                 {:tenant-defaults {:payers []
                                    :expense-categories []}}
                 user)]
    (assoc result :tenant-id (or (:id (:tenant result))
                               (:tenants/id (:tenant result))))))

(defn ensure-test-user!
  "Insert a minimal users row and return the map. Idempotent on email."
  ([db] (ensure-test-user! db {}))
  ([db {:keys [email name] :or {email "test@example.com" name "Test User"}}]
   (let [id (UUID/randomUUID)]
     (or (some-> (jdbc/execute-one! db
                   ["select * from users where email_lookup_hash = ? limit 1"
                    (email-privacy/email->lookup-hash email)]
                   {:builder-fn rs/as-unqualified-lower-maps})
           (as-> row
             (cond-> row
               (email-privacy/resolve-email row)
               (assoc :email (email-privacy/resolve-email row)))))
       (some-> (jdbc/execute-one! db
                 ["insert into users (id, email_ciphertext, email_lookup_hash, email_key_version, full_name, password_hash) values (?, ?, ?, ?, ?, ?) returning *"
                  id
                  (email-privacy/encrypt-email email)
                  (email-privacy/email->lookup-hash email)
                  (email-privacy/current-key-version)
                  name
                  "$2a$11$fakehashfortesting000000000000000000000000000000"]
                 {:builder-fn rs/as-unqualified-lower-maps})
         (as-> row
           (cond-> row
             (email-privacy/resolve-email row)
             (assoc :email (email-privacy/resolve-email row)))))))))

(defn- ensure-default-tenant-id!
  "Provision a lightweight tenant for tests that still create tenant-scoped
  payers without explicitly threading a tenant id."
  [db]
  (let [user (ensure-test-user! db {:email (str "payer-helper-" (UUID/randomUUID) "@example.com")})]
    (:tenant-id (ensure-test-tenant! db user))))

(defn create-payer!
  "Create a payer for tests.

  Accepts either direct payer-type values such as system/custom or older
  payment-method-like strings such as cash/card, which normalize to custom.

  Accepts optional `:tenant_id` in payer-data for tenant-scoped payers.
  When omitted, provisions a lightweight default tenant for backwards-compatible
  test setup.

  Returns the created payer row."
  [db {:keys [type tenant_id] :as payer-data}]
  (let [resolved-tenant-id (or tenant_id (ensure-default-tenant-id! db))]
    (payers/create-payer!
      db
      (-> payer-data
        (assoc :type (normalize-payer-type type))
        (assoc :tenant_id resolved-tenant-id
          :type (normalize-payer-type type)))
      {:tenant-id resolved-tenant-id})))
