(ns app.backend.services.admin.auth
  "Admin authentication and session management.

   This namespace handles:
   - Password hashing and verification (bcrypt with SHA-256 migration)
   - Admin user authentication
   - Session creation and management
   - Security token generation"
  (:require
    [app.shared.type-conversion :as tc]
    [buddy.core.codecs :as codecs]
    [buddy.core.hash :as hash]
    [buddy.hashers :as hashers]
    [honey.sql :as hsql]
    [java-time.api :as time]
    [next.jdbc :as jdbc]
    [next.jdbc.sql :as sql]
    [taoensso.timbre :as log])
  (:import
    [java.util UUID]))

;; ============================================================================
;; Token and Password Management
;; ============================================================================

(defn generate-session-token
  "Generate a unique session token"
  []
  (str (UUID/randomUUID)))

(defn hash-password
  "Hash a password using bcrypt+sha512 for secure storage.
   Uses 12 iterations (2^12 rounds) for good security vs performance balance."
  [password]
  (hashers/derive password {:alg :bcrypt+sha512 :iterations 12}))

(defn verify-password
  "Verify a password against a stored bcrypt hash.
   Returns true if the password matches the stored hash, false otherwise."
  [password stored-hash]
  (hashers/check password stored-hash))

(defn verify-bcrypt-password
  "Verify password using bcrypt securely."
  [password password-hash]
  (try
    (hashers/check password password-hash)
    (catch Exception _ false)))

(defn verify-sha256-password
  "Verify password using SHA-256 (legacy support)."
  [password password-hash]
  (= (-> (hash/sha256 password)
       codecs/bytes->hex)
    password-hash))

;; ============================================================================
;; Admin User Management
;; ============================================================================

(defn find-admin-by-email
  "Find an admin by email address"
  [db email]
  (jdbc/execute-one! db
    (hsql/format {:select [:*]
                  :from [:admins]
                  :where [:= :email email]})))

(defn find-admin-by-id
  "Find an admin by id"
  [db admin-id]
  (jdbc/execute-one! db
    (hsql/format {:select [:*]
                  :from [:admins]
                  :where [:= :id admin-id]})))

(defn create-admin!
  "Creates a new admin user with hashed password"
  [db {:keys [email password full_name role] :as admin-data}]
  (let [admin-id (UUID/randomUUID)
        now (time/instant)]
    (jdbc/execute! db (hsql/format
                        {:insert-into :admins
                         :values [{:id admin-id
                                   :email email
                                   :password_hash (hash-password password)
                                   :full_name full_name
                                   :role (tc/cast-for-database :admin-role (or role "admin"))
                                   :status (tc/cast-for-database :admin-status "active")
                                   :created_at now
                                   :updated_at now}]}))))

(defn migrate-admin-password!
  "Helper function to migrate an admin's password from SHA-256 to bcrypt.
   Should be called after successful login with old hash to upgrade security."
  [db admin-id password]
  (let [new-hash (hash-password password)
        now (time/instant)]
    (jdbc/execute-one! db
      (hsql/format {:update :admins
                    :set {:password_hash new-hash
                          :updated_at now}
                    :where [:= :id admin-id]}))
    (log/info "Migrated admin password to bcrypt" {:admin-id admin-id})
    true))

;; ============================================================================
;; Authentication
;; ============================================================================

(defn authenticate-admin
  "Authenticate an admin with email and password.
   Automatically migrates from SHA-256 to bcrypt when old format is detected."
  [db email password]
  (some-> (find-admin-by-email db email)
    (as-> admin
      (when (= (str (or (:status admin) (:admins/status admin))) "active")
        (let [password-hash (or (:password_hash admin) (:admins/password_hash admin))
              admin-id (or (:id admin) (:admins/id admin))]
          (cond
                  ;; Try bcrypt verification first (new secure format)
            (verify-bcrypt-password password password-hash)
            (do
              (log/info "Admin authentication successful" {:email email})
              (dissoc admin :password_hash :admins/password_hash))

                  ;; Fallback to SHA-256 for backwards compatibility
            (verify-sha256-password password password-hash)
            (do
                    ;; Automatically migrate to bcrypt
              (log/info "Migrating admin password from SHA-256 to bcrypt" {:email email})
              (migrate-admin-password! db admin-id password)
              (dissoc admin :password_hash :admins/password_hash))

            :else
            (do
              (log/warn "Admin authentication failed - invalid password" {:email email})
              nil)))))))

;; ============================================================================
;; Session Management
;; ============================================================================

(defonce session-store (atom {}))

(defn create-admin-session!
  "Create a new admin session"
  [db admin-id ip-address user-agent]
  (let [session-id (UUID/randomUUID)
        token (generate-session-token)
        now (time/instant)
        expires-at (time/plus now (time/hours 8))]
    (swap! session-store assoc token {:id session-id
                                      :admin-id admin-id
                                      :created-at now
                                      :last-activity now
                                      :expires-at expires-at
                                      :ip-address ip-address
                                      :user-agent user-agent})
    {:id session-id
     :token token
     :expires_at expires-at}))

(defn get-admin-by-session
  "Get admin by session token"
  [db token]
  (when-let [{:keys [admin-id expires-at]} (@session-store token)]
    (when (time/after? expires-at (time/instant))
      (find-admin-by-id db admin-id))))

(defn update-session-activity!
  "Update last activity timestamp for a session"
  [db token]
  (swap! session-store update token (fn [session]
                                      (when session
                                        (assoc session :last-activity (time/instant))))))

(defn invalidate-session!
  "Invalidate an admin session"
  [db token]
  (swap! session-store dissoc token))

(defn invalidate-all-admin-sessions!
  "Invalidate all sessions for an admin"
  [db admin-id]
  (swap! session-store
    (fn [sessions]
      (into {} (remove (fn [[_ {:keys [admin-id]}]]
                         (= admin-id admin-id))
                 sessions)))))
