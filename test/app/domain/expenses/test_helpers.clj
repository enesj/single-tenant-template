(ns app.domain.expenses.test-helpers
  "Test helpers for the expenses domain.

  These helpers smooth over common setup steps (e.g. payer types, tenants) so
  integration tests can focus on domain behavior rather than schema wiring."
  (:require
    [app.domain.backend.expenses.services.payer-types :as payer-types]
    [app.domain.backend.expenses.services.payers :as payers]
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

  Skips seeding payer-types/expense-categories so tests control their own data."
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
     (or (jdbc/execute-one! db
           ["select * from users where email = ? limit 1" email]
           {:builder-fn rs/as-unqualified-lower-maps})
       (jdbc/execute-one! db
         ["insert into users (id, email, full_name, password_hash) values (?, ?, ?, ?) returning *"
          id email name "$2a$11$fakehashfortesting000000000000000000000000000000"]
         {:builder-fn rs/as-unqualified-lower-maps})))))

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

(defn create-payer!
  "Create a payer, accepting either:

  - `:payer_type_id` (preferred), or
  - legacy `:type` (string) which is mapped to a payer type label.

  Accepts optional `:tenant_id` in payer-data for tenant-scoped payers.

  Returns the created payer row."
  [db {:keys [type payer_type_id tenant_id] :as payer-data}]
  (let [payer-type-id (or payer_type_id
                        (:id (ensure-payer-type! db (payer-type-label type) tenant_id)))]
    (payers/create-payer!
      db
      (-> payer-data
        (dissoc :type)
        (assoc :payer_type_id payer-type-id))
      (when tenant_id {:tenant-id tenant_id}))))
