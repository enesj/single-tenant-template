(ns app.template.backend.db.users
  "Template user database operations - extracted from domain-specific code"
  (:require
   [app.shared.field-casting :as field-casting]
   [app.template.backend.db.protocols :as db-protocols]
   [honey.sql :as sql]
   [next.jdbc :as jdbc]))

;; User lookup functions using database adapter

(defn find-user-by-email
  "Find user by email within specific tenant.

  This function looks up a user by email address within the context of a
  specific tenant. Used for tenant-specific authentication and user operations."
  [db-adapter tenant-id email]
  (db-protocols/list-with-filters db-adapter :users
    {:tenant_id tenant-id :email email}))

(defn find-user-by-email-global
  "Find user by email across all tenants (for tenant switching).

  This function performs a global lookup of a user by email address across
  all tenants. Used for tenant switching and cross-tenant operations."
  [db-adapter email]
  (first (db-protocols/list-with-filters db-adapter :users {:email email})))

(defn find-user-by-id
  "Find user by ID.

  Primary key lookup for user records."
  [db-adapter user-id]
  (db-protocols/find-by-id db-adapter :users user-id))

;; User creation and modification

(defn create-user
  "Create new user with default values.

  This function creates a new user record with sensible defaults for
  required fields. Uses centralized field casting for proper data types."
  [db-adapter metadata user-data]
  (let [;; Add defaults for required fields
        data-with-defaults (-> user-data
                             (update :role #(or % "member"))
                             (update :status #(or % "active"))
                             (update :auth_provider #(or % "google")))]
    (db-protocols/create db-adapter metadata :users data-with-defaults)))

(defn update-user
  "Update user data.

  Updates user information using centralized field casting to ensure
  proper data types for all fields."
  [db-adapter metadata user-id updates]
  (db-protocols/update-record db-adapter metadata :users user-id updates))

;; Tenant relationship functions

(defn find-tenants-for-user
  "Find all tenants user has access to.

  This function returns all tenants that a user has access to, enabling
  tenant switching and multi-tenant user management."
  [db-adapter user-email]
  ;; This requires a more complex query with joins
  ;; For now, we'll use the raw connection approach
  (let [connection (:connection db-adapter)]
    (jdbc/execute! connection
      (sql/format {:select [:t.*]
                   :from [[:tenants :t]]
                   :join [[:users :u] [:= :t.id :u.tenant_id]]
                   :where [:= :u.email user-email]}))))

(defn find-user-in-tenant
  "Find user's record in specific tenant.

  This function looks up a user's record within a specific tenant context,
  ensuring proper tenant isolation."
  [db-adapter user-id tenant-id]
  (first (db-protocols/list-with-filters db-adapter :users
           {:id user-id :tenant_id tenant-id})))

(defn find-users-for-tenant
  "Find all users for a specific tenant.

  This function returns all users that belong to a specific tenant,
  ordered by creation date (newest first)."
  [db-adapter tenant-id]
  ;; For ordered results, we need to use raw SQL for now
  (let [connection (:connection db-adapter)]
    (jdbc/execute! connection
      (sql/format {:select [:*]
                   :from [:users]
                   :where [:= :tenant_id tenant-id]
                   :order-by [[:created_at :desc]]}))))

;; Helper functions with ! convention

(defn create-user!
  "Create new user (alias for create-user with ! convention).

  This is an alias for the create-user function following the ! convention
  for functions that modify state."
  [db-adapter metadata user-data]
  (create-user db-adapter metadata user-data))

(defn add-user-to-tenant!
  "Add user to tenant relationship (for multi-tenant user access).

  This function adds a user to a tenant relationship by updating the user's
  tenant_id and role. In a more complex multi-tenant setup, this might
  insert into a user_tenants table."
  [db-adapter metadata params]
  (let [{:keys [user_id tenant_id role]} params]
    ;; For now, this updates the user's tenant_id and role
    ;; In a more complex multi-tenant setup, this might insert into a user_tenants table
    (update-user db-adapter metadata user_id {:tenant_id tenant_id :role role})))

;; Legacy function compatibility (using raw db connection for compatibility)

(defn find-user-by-email-legacy
  "Legacy function for backward compatibility - uses raw database connection"
  [db tenant-id email]
  (jdbc/execute-one! db
    (sql/format {:select [:*]
                 :from [:users]
                 :where [:and
                         [:= :tenant_id tenant-id]
                         [:= :email email]]})))

(defn find-user-by-email-global-legacy
  "Legacy function for backward compatibility - uses raw database connection"
  [db email]
  (jdbc/execute-one! db
    (sql/format {:select [:*]
                 :from [:users]
                 :where [:= :email email]
                 :limit 1})))

(defn find-user-by-id-legacy
  "Legacy function for backward compatibility - uses raw database connection"
  [db user-id]
  (jdbc/execute-one! db
    (sql/format {:select [:*]
                 :from [:users]
                 :where [:= :id user-id]})))

(defn create-user-legacy
  "Legacy function for backward compatibility - uses raw database connection"
  [db md user-data]
  (let [;; Add defaults for required fields
        data-with-defaults (-> user-data
                             (update :role #(or % "member"))
                             (update :status #(or % "active"))
                             (update :auth_provider #(or % "google")))
        ;; Use centralized field casting
        processed-data (field-casting/prepare-insert-data md :users data-with-defaults)]
    (jdbc/execute-one! db
      (sql/format {:insert-into [:users]
                   :values [processed-data]
                   :returning [:*]}))))

(defn update-user-legacy
  "Legacy function for backward compatibility - uses raw database connection"
  [db md user-id updates]
  (let [;; Use centralized field casting for updates
        processed-updates (field-casting/prepare-update-data md :users updates)]
    (jdbc/execute-one! db
      (sql/format {:update [:users]
                   :set processed-updates
                   :where [:= :id user-id]
                   :returning [:*]}))))
