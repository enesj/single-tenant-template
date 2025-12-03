(ns app.backend.services.admin.admins
  "Admin management service - allows owners to manage other admins.

   This namespace handles:
   - Listing and searching admins
   - Creating new admins
   - Updating admin information (excluding password)
   - Updating admin roles and status
   - Deleting/suspending admins (with safety checks)
   
   Security considerations:
   - Only owners can manage other admins
   - Admins cannot modify their own role/status
   - Cannot delete/suspend the last owner"
  (:require
    [app.backend.services.admin.audit :as audit]
    [app.backend.services.admin.auth :as auth]
    [app.shared.adapters.database :as db-adapter]
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
    (db-adapter/normalize-admin-result admin-config)
    (dissoc :password_hash :password-hash)))

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

(defn list-all-admins
  "List all admins with optional filtering and pagination"
  [db {:keys [search status role limit offset]
       :or {limit 50 offset 0}}]
  (let [base-query {:select [:a.*]
                    :from [[:admins :a]]
                    :order-by [[:created_at :desc]]}
        query (cond-> base-query
                search (update :where (fn [w]
                                        (let [clause [:or
                                                      [:ilike :a/email (str "%" search "%")]
                                                      [:ilike :a/full_name (str "%" search "%")]]]
                                          (if w [:and w clause] clause))))
                status (update :where (fn [w]
                                        (let [clause [:= :a/status (tc/cast-for-database :admin-status status)]]
                                          (if w [:and w clause] clause))))
                role (update :where (fn [w]
                                      (let [clause [:= :a/role (tc/cast-for-database :admin-role role)]]
                                        (if w [:and w clause] clause))))
                limit (assoc :limit limit)
                offset (assoc :offset offset))]
    (log/debug "Listing admins with filters" {:search search :status status :role role})
    (->> (jdbc/execute! db (hsql/format query))
      (map db-admin->app))))

(defn get-admin-count
  "Get total count of admins matching filters"
  [db {:keys [search status role]}]
  (let [base-query {:select [[:%count.* :count]]
                    :from [[:admins :a]]}
        query (cond-> base-query
                search (update :where (fn [w]
                                        (let [clause [:or
                                                      [:ilike :a/email (str "%" search "%")]
                                                      [:ilike :a/full_name (str "%" search "%")]]]
                                          (if w [:and w clause] clause))))
                status (update :where (fn [w]
                                        (let [clause [:= :a/status (tc/cast-for-database :admin-status status)]]
                                          (if w [:and w clause] clause))))
                role (update :where (fn [w]
                                      (let [clause [:= :a/role (tc/cast-for-database :admin-role role)]]
                                        (if w [:and w clause] clause)))))]
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
  [db {:keys [email password full_name role] :as admin-data} 
   current-admin-id ip-address user-agent]
  (log/info "Creating new admin" {:email email :role role :by-admin current-admin-id})
  
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
                              :role (tc/cast-for-database :admin-role (or role "admin"))
                              :status (tc/cast-for-database :admin-status "active")
                              :created_at now
                              :updated_at now}]
                    :returning [:*]}))]
    
    ;; Log the creation
    (audit/log-audit! db
      {:admin_id current-admin-id
       :action "create_admin"
       :entity-type "admin"
       :entity-id admin-id
       :changes {:email email :role (or role "admin")}
       :ip-address ip-address
       :user-agent user-agent})
    
    (log/info "Admin created successfully" {:admin-id admin-id :email email})
    (db-admin->app result)))

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
    
    (let [now (time/instant)
          result (jdbc/execute-one! db
                   (hsql/format {:update :admins
                                 :set (assoc clean-updates :updated_at now)
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
  
  ;; If changing FROM owner role, validate not last owner
  (let [current-admin (get-admin-details db admin-id)
        current-role (:role current-admin)]
    (when (and (= (str current-role) "owner") (not= (str new-role) "owner"))
      (validate-not-last-owner! db admin-id "change role of")))
  
  (let [now (time/instant)
        result (jdbc/execute-one! db
                 (hsql/format {:update :admins
                               :set {:role (tc/cast-for-database :admin-role new-role)
                                     :updated_at now}
                               :where [:= :id admin-id]
                               :returning [:*]}))]
    
    (when result
      (audit/log-audit! db
        {:admin_id current-admin-id
         :action "update_admin_role"
         :entity-type "admin"
         :entity-id admin-id
         :changes {:new-role new-role}
         :ip-address ip-address
         :user-agent user-agent})
      
      (log/info "Admin role updated" {:admin-id admin-id :new-role new-role}))
    
    (some-> result db-admin->app)))

(defn update-admin-status!
  "Update an admin's status (active/suspended)."
  [db admin-id new-status current-admin-id ip-address user-agent]
  (validate-not-self-modification! current-admin-id admin-id "change status of")
  
  ;; If suspending an owner, validate not last owner
  (when (= new-status "suspended")
    (let [current-admin (get-admin-details db admin-id)]
      (when (= (str (:role current-admin)) "owner")
        (validate-not-last-owner! db admin-id "suspend"))))
  
  (let [now (time/instant)
        result (jdbc/execute-one! db
                 (hsql/format {:update :admins
                               :set {:status (tc/cast-for-database :admin-status new-status)
                                     :updated_at now}
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
