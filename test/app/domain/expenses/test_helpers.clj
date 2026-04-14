(ns app.domain.expenses.test-helpers
  "Test helpers for the expenses domain.

  These helpers smooth over common setup steps (e.g. payer types, tenants) so
  integration tests can focus on domain behavior rather than schema wiring."
  (:require
    [app.domain.backend.expenses.services.payer-types :as payer-types]
    [app.domain.backend.expenses.services.payers :as payers]
    [app.template.backend.security.email :as email-privacy]
    [app.template.backend.services.tenant :as tenant-svc]
    [clojure.string :as str]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.util UUID]))

(defn- payer-type-label
  "Derive a stable payer type label from legacy `:type` strings.

  The production system uses payer types as a table, while some tests still use
  enum-like strings such as \"cash\"/\"card\"/\"account\"."
  [type]
  (let [s (-> (or type "cash") str str/trim str/lower-case)]
    (if (str/blank? s)
      "Cash"
      (str/capitalize s))))

(defn ensure-test-tenant!
  "Create a tenant + owner membership for testing. Returns
  {:tenant <row> :membership <row> :tenant-id <uuid>}.

  Skips configured payer-types/custom expense-categories, but tenant
  provisioning still creates the required default expense category."
  [db user]
  (let [result (tenant-svc/provision-tenant!
                 db
                 {:tenant-defaults {:payer-types []
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

(defn ensure-payer-type!
  "Ensure a payer type exists (by label) and return the row.
  Accepts optional tenant-id for tenant-scoped payer types."
  ([db label] (ensure-payer-type! db label nil))
  ([db label tenant-id]
   (or (jdbc/execute-one!
         db
         (if tenant-id
           ["select * from payer_types where label = ? and tenant_id = ? limit 1" label tenant-id]
           ["select * from payer_types where label = ? limit 1" label])
         {:builder-fn rs/as-unqualified-lower-maps})
     (payer-types/create-payer-type!
       db
       (cond-> {:label label :is_default false}
         tenant-id (assoc :tenant_id tenant-id))))))

(defn- ensure-default-tenant-id!
  "Provision a lightweight tenant for tests that still create tenant-scoped
  payers without explicitly threading a tenant id."
  [db]
  (let [user (ensure-test-user! db {:email (str "payer-helper-" (UUID/randomUUID) "@example.com")})]
    (:tenant-id (ensure-test-tenant! db user))))

(defn create-payer!
  "Create a payer, accepting either:

  - `:payer_type_id` (preferred), or
  - legacy `:type` (string) which is mapped to a payer type label.

  Accepts optional `:tenant_id` in payer-data for tenant-scoped payers.
  When omitted, provisions a lightweight default tenant for backwards-compatible
  test setup.

  Returns the created payer row."
  [db {:keys [type payer_type_id tenant_id] :as payer-data}]
  (let [resolved-tenant-id (or tenant_id (ensure-default-tenant-id! db))
        payer-type-id (or payer_type_id
                        (:id (ensure-payer-type! db (payer-type-label type) resolved-tenant-id)))]
    (payers/create-payer!
      db
      (-> payer-data
        (dissoc :type)
        (assoc :tenant_id resolved-tenant-id
          :payer_type_id payer-type-id))
      {:tenant-id resolved-tenant-id})))
