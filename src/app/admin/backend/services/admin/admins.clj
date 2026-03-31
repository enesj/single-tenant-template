(ns app.admin.backend.services.admin.admins
  "Admin account management; update owner/admin flows here."
  (:require
    [app.admin.backend.services.admin.audit :as audit]
    [app.admin.backend.services.admin.auth :as auth]
    [app.shared.adapters.normalization :as norm]
    [app.shared.type-conversion :as tc]
    [honey.sql :as hsql]
    [java-time.api :as time]
    [next.jdbc :as jdbc]
    [taoensso.timbre :as log])
  (:import
    [java.util UUID]))

;; ============================================================================
;; Configuration
;; ============================================================================

(def ^:private admin-config
  "Configuration for normalizing admin database results to application format"
  {:prefixes ["admins-" "admin-"]
   :namespaces #{"admins" "admin" "a"}
   :id-fields #{:id}})

(defn- db-admin->app
  "Normalize an admin row from the database using shared utilities"
  [admin]
  (-> admin
    (norm/normalize-admin-result admin-config)
    (dissoc :password_hash :password-hash)))

(def ^:private valid-admin-roles
  #{"admin" "support" "owner"})

(defn- validate-admin-role!
  "Ensure admin role is one of the supported enum values."
  [role]
  (when-not (contains? valid-admin-roles role)
    (throw (ex-info "Invalid role. Must be one of: admin, support, owner"
             {:status 400
              :field :role
              :allowed ["admin" "support" "owner"]
              :reason :invalid-role}))))

;; ============================================================================
;; Validation Helpers
;; ============================================================================

(defn- count-owners
  "Count the number of admins with owner role"
  [db]
  (-> (jdbc/execute-one! db
        (hsql/format {:select [[:%count.* :count]]
                      :from [:admins]
                      :where [:and
                              [:= :role (tc/cast-for-database :admin-role "owner")]
                              [:= :status (tc/cast-for-database :admin-status "active")]]}))
    :count))

(defn- validate-owner-assignment!
  "Ensure there can be at most one active global owner."
  [db role]
  (when (and (= role "owner") (pos? (count-owners db)))
    (throw (ex-info "Cannot assign the owner role because an active global owner already exists"
             {:status 400
              :field :role
              :reason :owner-already-exists}))))

(defn- is-last-owner?
  "Check if the given admin is the last active owner"
  [db admin-id]
  (let [admin (auth/find-admin-by-id db admin-id)
        role (or (:role admin) (:admins/role admin))]
    (and (= (str role) "owner")
      (= 1 (count-owners db)))))

(defn- validate-not-self-modification!
  "Ensure admin is not trying to modify their own sensitive fields"
  [current-admin-id target-admin-id operation]
  (when (= (str current-admin-id) (str target-admin-id))
    (throw (ex-info (str "Cannot " operation " your own account")
             {:status 400
              :field operation
              :reason :self-modification}))))

(defn- validate-not-last-owner!
  "Ensure we're not removing the last owner"
  [db admin-id operation]
  (when (is-last-owner? db admin-id)
    (throw (ex-info (str "Cannot " operation " the last owner account")
             {:status 400
              :field operation
              :reason :last-owner-protection}))))

;; ============================================================================
;; Admin Listing and Search
;; ============================================================================

(defn- build-admin-list-filter-clauses
  [{:keys [search status role email full-name
           last-login-at-from last-login-at-to]}]
  (cond-> []
    search (conj [:or
                  [:ilike :a/email (str "%" search "%")]
                  [:ilike :a/full_name (str "%" search "%")]])
    status (conj [:= :a/status (tc/cast-for-database :admin-status status)])
    role (conj [:= :a/role (tc/cast-for-database :admin-role role)])
    email (conj [:ilike :a/email (str "%" email "%")])
    full-name (conj [:ilike :a/full_name (str "%" full-name "%")])
    last-login-at-from (conj [:>= :a/last_login_at last-login-at-from])
    last-login-at-to (conj [:<= :a/last_login_at last-login-at-to])))

(defn- build-admin-list-where-clause
  [filters]
  (let [clauses (build-admin-list-filter-clauses filters)]
    (when (seq clauses)
      (into [:and] clauses))))

(defn list-all-admins
  "List all admins with optional filtering and pagination"
  [db {:keys [limit offset]
       :or {limit 50 offset 0}
       :as filters}]
  (let [where-clause (build-admin-list-where-clause filters)
        query (cond-> {:select [:a.*]
                       :from [[:admins :a]]
                       :order-by [[:created_at :desc]]}
                where-clause (assoc :where where-clause)
                limit (assoc :limit limit)
                offset (assoc :offset offset))]
    (log/debug "Listing admins with filters" (select-keys filters [:search :status :role :email :full-name]))
    (->> (jdbc/execute! db (hsql/format query))
      (map db-admin->app))))

(defn get-admin-count
  "Get total count of admins matching filters"
  [db filters]
  (let [where-clause (build-admin-list-where-clause filters)
        query (cond-> {:select [[:%count.* :count]]
                       :from [[:admins :a]]}
                where-clause (assoc :where where-clause))]
    (-> (jdbc/execute-one! db (hsql/format query))
      :count)))

;; ============================================================================
;; Admin Details
;; ============================================================================

(defn get-admin-details
  "Get detailed admin information by ID"
  [db admin-id]
  (let [query {:select [:a.*]
               :from [[:admins :a]]
               :where [:= :a.id admin-id]}]
    (some-> (jdbc/execute-one! db (hsql/format query))
      db-admin->app)))

;; ============================================================================
;; Admin Creation
;; ============================================================================

(defn create-admin!
  "Create a new admin with full audit logging.
   Wraps auth/create-admin! with additional validation and audit."
  [db {:keys [email password full_name role] :as _admin-data}
   current-admin-id ip-address user-agent]
  (let [requested-role (or (some-> role str) "admin")]
    (log/info "Creating new admin" {:email email :role requested-role :by-admin current-admin-id})

    (validate-admin-role! requested-role)
    (validate-owner-assignment! db requested-role)

    ;; Check if email already exists
    (when (auth/find-admin-by-email db email)
      (throw (ex-info "An admin with this email already exists"
               {:status 400
                :field :email
                :reason :duplicate-email})))

    (let [admin-id (UUID/randomUUID)
          now (time/instant)
          result (jdbc/execute-one! db
                   (hsql/format
                     {:insert-into :admins
                      :values [{:id admin-id
                                :email email
                                :password_hash (auth/hash-password password)
                                :full_name full_name
                                :role (tc/cast-for-database :admin-role requested-role)
                                :status (tc/cast-for-database :admin-status "active")
                                :created_at now}]
                      :returning [:*]}))]

      ;; Log the creation
      (audit/log-audit! db
        {:admin_id current-admin-id
         :action "create_admin"
         :entity-type "admin"
         :entity-id admin-id
         :changes {:email email :role requested-role}
         :ip-address ip-address
         :user-agent user-agent})

      (log/info "Admin created successfully" {:admin-id admin-id :email email :role requested-role})
      (db-admin->app result))))

;; ============================================================================
;; Admin Updates
;; ============================================================================

(defn update-admin!
  "Update admin information (excluding role, status, and password).
   For role/status changes, use dedicated functions."
  [db admin-id updates current-admin-id ip-address user-agent]
  (let [allowed-fields #{:email :full_name :full-name}
        clean-updates (-> (select-keys updates allowed-fields)
                        ;; Normalize full-name to full_name
                        (as-> u
                          (if (:full-name u)
                            (-> u (assoc :full_name (:full-name u)) (dissoc :full-name))
                            u)))]

    (when (empty? clean-updates)
      (throw (ex-info "No valid fields to update"
               {:status 400
                :reason :no-valid-fields})))

    ;; Check for email uniqueness if email is being changed
    (when-let [new-email (:email clean-updates)]
      (when-let [existing (auth/find-admin-by-email db new-email)]
        (when (not= (str (or (:id existing) (:admins/id existing))) (str admin-id))
          (throw (ex-info "An admin with this email already exists"
                   {:status 400
                    :field :email
                    :reason :duplicate-email})))))

    (let [result (jdbc/execute-one! db
                   (hsql/format {:update :admins
                                 :set clean-updates
                                 :where [:= :id admin-id]
                                 :returning [:*]}))]

      (when result
        (audit/log-audit! db
          {:admin_id current-admin-id
           :action "update_admin"
           :entity-type "admin"
           :entity-id admin-id
           :changes clean-updates
           :ip-address ip-address
           :user-agent user-agent})

        (log/info "Admin updated" {:admin-id admin-id :changes clean-updates}))

      (some-> result db-admin->app))))

(defn update-admin-role!
  "Update an admin's role. Only owners can do this."
  [db admin-id new-role current-admin-id ip-address user-agent]
  (validate-not-self-modification! current-admin-id admin-id "change role of")
  (let [normalized-new-role (some-> new-role str)
        current-admin (get-admin-details db admin-id)]
    (when-not current-admin
      (throw (ex-info "Admin not found"
               {:status 404
                :reason :not-found})))

    (validate-admin-role! normalized-new-role)

    ;; Guard: only one active global owner can exist at a time.
    (when (and (= normalized-new-role "owner")
            (not= (str (:role current-admin)) "owner"))
      (validate-owner-assignment! db normalized-new-role))

    ;; If changing FROM owner role, validate not last owner
    (when (and (= (str (:role current-admin)) "owner")
            (not= normalized-new-role "owner"))
      (validate-not-last-owner! db admin-id "change role of"))

    (let [result (jdbc/execute-one! db
                   (hsql/format {:update :admins
                                 :set {:role (tc/cast-for-database :admin-role normalized-new-role)}
                                 :where [:= :id admin-id]
                                 :returning [:*]}))]

      (when result
        (audit/log-audit! db
          {:admin_id current-admin-id
           :action "update_admin_role"
           :entity-type "admin"
           :entity-id admin-id
           :changes {:new-role normalized-new-role}
           :ip-address ip-address
           :user-agent user-agent})

        (log/info "Admin role updated" {:admin-id admin-id :new-role normalized-new-role}))

      (some-> result db-admin->app))))

(defn transfer-admin-ownership!
  "Transfer global ownership from the current owner to a target active admin.
   Atomically swaps: actor -> admin, target -> owner."
  [db target-admin-id current-admin-id ip-address user-agent]
  (validate-not-self-modification! current-admin-id target-admin-id "transfer ownership to")
  (jdbc/with-transaction [tx db]
    (let [current-owner (get-admin-details tx current-admin-id)
          target-admin  (get-admin-details tx target-admin-id)]
      (when-not current-owner
        (throw (ex-info "Current owner not found"
                 {:status 404
                  :reason :current-owner-not-found})))
      (when-not target-admin
        (throw (ex-info "Target admin not found"
                 {:status 404
                  :reason :not-found})))

      (when-not (= (str (:role current-owner)) "owner")
        (throw (ex-info "Only the current owner can transfer ownership"
                 {:status 403
                  :field :role
                  :reason :insufficient-permissions})))

      (when-not (= (str (:status current-owner)) "active")
        (throw (ex-info "Current owner must be active"
                 {:status 400
                  :field :status
                  :reason :current-owner-inactive})))

      (when-not (= (str (:status target-admin)) "active")
        (throw (ex-info "Target admin must be active"
                 {:status 400
                  :field :status
                  :reason :target-inactive})))

      (when-not (= (str (:role target-admin)) "admin")
        (throw (ex-info "Ownership can only be transferred to an active admin"
                 {:status 400
                  :field :role
                  :reason :target-must-be-admin})))

      ;; Demote the current owner first so the unique owner index always remains valid.
      (jdbc/execute-one! tx
        (hsql/format {:update :admins
                      :set {:role (tc/cast-for-database :admin-role "admin")}
                      :where [:= :id current-admin-id]}))

      (let [updated-target (jdbc/execute-one! tx
                             (hsql/format {:update :admins
                                           :set {:role (tc/cast-for-database :admin-role "owner")}
                                           :where [:= :id target-admin-id]
                                           :returning [:*]}))]
        (audit/log-audit! tx
          {:admin_id current-admin-id
           :action "transfer_admin_ownership"
           :entity-type "admin"
           :entity-id target-admin-id
           :changes {:previous-owner-id current-admin-id
                     :previous-owner-email (:email current-owner)
                     :new-owner-id target-admin-id
                     :new-owner-email (:email target-admin)
                     :previous-owner-new-role "admin"
                     :new-owner-role "owner"}
           :ip-address ip-address
           :user-agent user-agent})

        (log/info "Admin ownership transferred" {:from current-admin-id :to target-admin-id})
        (db-admin->app updated-target)))))

(defn update-admin-status!
  "Update an admin's status (active/suspended)."
  [db admin-id new-status current-admin-id ip-address user-agent]
  (validate-not-self-modification! current-admin-id admin-id "change status of")

  ;; If suspending an owner, validate not last owner
  (when (= new-status "suspended")
    (let [current-admin (get-admin-details db admin-id)]
      (when (= (str (:role current-admin)) "owner")
        (validate-not-last-owner! db admin-id "suspend"))))

  (let [result (jdbc/execute-one! db
                 (hsql/format {:update :admins
                               :set {:status (tc/cast-for-database :admin-status new-status)}
                               :where [:= :id admin-id]
                               :returning [:*]}))]

    (when result
      (audit/log-audit! db
        {:admin_id current-admin-id
         :action "update_admin_status"
         :entity-type "admin"
         :entity-id admin-id
         :changes {:new-status new-status}
         :ip-address ip-address
         :user-agent user-agent})

      (log/info "Admin status updated" {:admin-id admin-id :new-status new-status}))

    (some-> result db-admin->app)))

;; ============================================================================
;; Admin Deletion
;; ============================================================================

(defn delete-admin!
  "Delete an admin account. Cannot delete self or last owner."
  [db admin-id current-admin-id ip-address user-agent]
  (validate-not-self-modification! current-admin-id admin-id "delete")
  (validate-not-last-owner! db admin-id "delete")

  (let [admin (get-admin-details db admin-id)]
    (when-not admin
      (throw (ex-info "Admin not found"
               {:status 404
                :reason :not-found})))

    ;; Delete the admin
    (jdbc/execute-one! db
      (hsql/format {:delete-from :admins
                    :where [:= :id admin-id]}))

    ;; Invalidate all sessions for this admin
    (auth/invalidate-all-admin-sessions! db admin-id)

    ;; Log the deletion
    (audit/log-audit! db
      {:admin_id current-admin-id
       :action "delete_admin"
       :entity-type "admin"
       :entity-id admin-id
       :changes {:deleted-email (:email admin)}
       :ip-address ip-address
       :user-agent user-agent})

    (log/info "Admin deleted" {:admin-id admin-id :email (:email admin)})

    {:success true
     :message "Admin deleted successfully"
     :admin admin}))
