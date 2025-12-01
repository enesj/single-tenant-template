(ns app.template.backend.auth.password-reset
  "Password reset service for users and admins.
   
   This namespace handles:
   - Password reset token generation and verification
   - Forgot password flow (request → email → reset)
   - Change password flow (authenticated users/admins)"
  (:require
    [app.template.backend.db.protocols :as db-protocols]
    [buddy.hashers :as hashers]
    [java-time.api :as time]
    [taoensso.timbre :as log])
  (:import
    [java.security SecureRandom]
    [java.util Base64 UUID]))

;; ============================================================================
;; Configuration
;; ============================================================================

(def reset-token-length 32)
(def reset-token-expiry-hours 1)  ; Tokens expire in 1 hour
(def min-password-length 10)

;; ============================================================================
;; Token Generation
;; ============================================================================

(defn generate-reset-token
  "Generate a secure random token for password reset"
  []
  (let [random (SecureRandom.)
        bytes (byte-array reset-token-length)]
    (.nextBytes random bytes)
    (-> (Base64/getUrlEncoder)
        (.withoutPadding)
        (.encodeToString bytes))))

;; ============================================================================
;; Password Hashing
;; ============================================================================

(defn hash-password
  "Hash password using bcrypt with SHA-512"
  [password]
  (hashers/derive password {:alg :bcrypt+sha512 :iterations 12}))

(defn verify-password
  "Verify password against hash"
  [password hash]
  (when (and password hash)
    (try
      (hashers/check password hash)
      (catch Exception _ false))))

;; ============================================================================
;; Helper Functions
;; ============================================================================

(defn- instant->instant
  "Normalize various timestamp types to java.time.Instant"
  [ts]
  (cond
    (instance? java.time.Instant ts)
    ts

    (instance? java.time.OffsetDateTime ts)
    (.toInstant ^java.time.OffsetDateTime ts)

    (instance? java.time.ZonedDateTime ts)
    (.toInstant ^java.time.ZonedDateTime ts)

    (instance? java.time.LocalDateTime ts)
    (-> ^java.time.LocalDateTime ts
        (.atZone (java.time.ZoneId/systemDefault))
        (.toInstant))

    (instance? java.util.Date ts)
    (.toInstant ^java.util.Date ts)

    (string? ts)
    (try
      (time/instant ts)
      (catch Exception _
        nil))

    :else
    nil))

(defn- token-expired?
  "Check if a token has expired"
  [expires-at]
  (if-let [expires-instant (instant->instant expires-at)]
    (time/after? (time/instant) expires-instant)
    ;; If we can't parse the timestamp, treat as expired for safety
    true))

;; ============================================================================
;; Reset Token Management
;; ============================================================================

(defn create-reset-token!
  "Create a password reset token for a user or admin.
   
   Args:
     db - database connection
     principal-type - :user or :admin  
     principal-id - UUID of the user or admin
   
   Returns: {:token <string> :expires-at <instant>}"
  [db principal-type principal-id]
  (let [token (generate-reset-token)
        expires-at (time/plus (time/instant) (time/hours reset-token-expiry-hours))
        token-id (UUID/randomUUID)
        principal-type-str (name principal-type)]
    
    ;; Invalidate any existing unused tokens for this principal
    (db-protocols/execute! db
      "DELETE FROM password_reset_tokens 
       WHERE principal_type = ?::login_principal_type 
         AND principal_id = ? 
         AND used_at IS NULL"
      [principal-type-str principal-id])
    
    ;; Create new token
    (db-protocols/execute! db
      "INSERT INTO password_reset_tokens 
       (id, principal_type, principal_id, token, expires_at, created_at)
       VALUES (?, ?::login_principal_type, ?, ?, ?, NOW())"
      [token-id principal-type-str principal-id token expires-at])
    
    (log/info "Created password reset token" 
              {:principal-type principal-type 
               :principal-id principal-id
               :expires-at expires-at})
    
    {:token token :expires-at expires-at}))

(defn find-reset-token
  "Find a password reset token by token string"
  [db token]
  (first (db-protocols/execute! db
           "SELECT * FROM password_reset_tokens WHERE token = ?"
           [token])))

(defn verify-reset-token
  "Verify a password reset token and return principal info.
   
   Returns: {:valid? boolean 
             :principal-type keyword 
             :principal-id uuid 
             :error string (if invalid)}"
  [db token]
  (if-let [token-record (find-reset-token db token)]
    (let [used-at (or (:used_at token-record)
                      (:password_reset_tokens/used_at token-record))
          expires-at (or (:expires_at token-record)
                         (:password_reset_tokens/expires_at token-record))
          principal-type (or (:principal_type token-record)
                             (:password_reset_tokens/principal_type token-record))
          principal-id (or (:principal_id token-record)
                           (:password_reset_tokens/principal_id token-record))]
      (cond
        ;; Token already used
        (some? used-at)
        {:valid? false :error "This reset link has already been used"}
        
        ;; Token expired
        (token-expired? expires-at)
        {:valid? false :error "This reset link has expired"}
        
        ;; Token valid
        :else
        {:valid? true
         :principal-type (keyword principal-type)
         :principal-id principal-id}))
    
    ;; Token not found
    {:valid? false :error "Invalid or expired reset link"}))

(defn mark-token-used!
  "Mark a reset token as used"
  [db token]
  (db-protocols/execute! db
    "UPDATE password_reset_tokens SET used_at = NOW() WHERE token = ?"
    [token]))

;; ============================================================================
;; Principal Lookup
;; ============================================================================

(defn find-principal-by-email
  "Find a user or admin by email"
  [db principal-type email]
  (let [table (if (= principal-type :admin) "admins" "users")
        query (str "SELECT id, email, full_name, password_hash FROM " table " WHERE email = ?")]
    (first (db-protocols/execute! db query [email]))))

(defn find-principal-by-id
  "Find a user or admin by ID"
  [db principal-type principal-id]
  (let [table (if (= principal-type :admin) "admins" "users")
        query (str "SELECT id, email, full_name, password_hash FROM " table " WHERE id = ?")]
    (first (db-protocols/execute! db query [principal-id]))))

;; ============================================================================
;; Password Reset Operations
;; ============================================================================

(defn request-password-reset!
  "Request a password reset for a user or admin.
   
   Args:
     db - database connection
     email - email address
     principal-type - :user or :admin
     send-email-fn - function to send reset email (fn [to-email token reset-url full-name])
     base-url - base URL for reset links
   
   Returns: {:success boolean :message string}"
  [db email principal-type send-email-fn base-url]
  (let [principal (find-principal-by-email db principal-type email)
        ;; Normalize keys from namespaced or plain
        principal-id (or (:id principal)
                         (:users/id principal)
                         (:admins/id principal))
        principal-email (or (:email principal)
                            (:users/email principal)
                            (:admins/email principal))
        full-name (or (:full_name principal)
                      (:users/full_name principal)
                      (:admins/full_name principal))]
    
    ;; Always return success to prevent email enumeration
    (if principal-id
      (let [{:keys [token]} (create-reset-token! db principal-type principal-id)
            reset-path (if (= principal-type :admin)
                         "/admin/reset-password"
                         "/reset-password")
            reset-url (str base-url reset-path "?token=" token)]
        
        ;; Send reset email
        (when send-email-fn
          (try
            (send-email-fn principal-email token reset-url full-name)
            (log/info "Password reset email sent" {:email email :type principal-type})
            (catch Exception e
              (log/error e "Failed to send reset email" {:email email}))))
        
        {:success true 
         :message "If an account exists with this email, you will receive password reset instructions."})
      
      ;; No account found - return same message to prevent enumeration
      (do
        (log/warn "Password reset requested for unknown email" {:email email :type principal-type})
        {:success true 
         :message "If an account exists with this email, you will receive password reset instructions."}))))

(defn reset-password!
  "Reset password using a valid token.
   
   Args:
     db - database connection
     token - reset token from email
     new-password - new password to set
   
   Returns: {:success boolean :error string (if failed)}"
  [db token new-password]
  (let [verification (verify-reset-token db token)]
    (cond
      ;; Token invalid
      (not (:valid? verification))
      {:success false :error (:error verification)}
      
      ;; Password too short
      (< (count (or new-password "")) min-password-length)
      {:success false :error (str "Password must be at least " min-password-length " characters")}
      
      ;; Process reset
      :else
      (let [{:keys [principal-type principal-id]} verification
            table (if (= principal-type :admin) "admins" "users")
            password-hash (hash-password new-password)]
        
        ;; Update password
        (db-protocols/execute! db
          (str "UPDATE " table " SET password_hash = ?, updated_at = NOW() WHERE id = ?")
          [password-hash principal-id])
        
        ;; Mark token as used
        (mark-token-used! db token)
        
        (log/info "Password reset successful" 
                  {:principal-type principal-type 
                   :principal-id principal-id})
        
        {:success true
         :principal-type principal-type
         :principal-id principal-id}))))

;; ============================================================================
;; Change Password (Authenticated)
;; ============================================================================

(defn change-password!
  "Change password for an authenticated user or admin.
   
   Args:
     db - database connection
     principal-type - :user or :admin
     principal-id - UUID of the user or admin
     current-password - current password for verification
     new-password - new password to set
   
   Returns: {:success boolean :error string (if failed)}"
  [db principal-type principal-id current-password new-password]
  (let [principal (find-principal-by-id db principal-type principal-id)
        password-hash (or (:password_hash principal)
                          (:users/password_hash principal)
                          (:admins/password_hash principal))]
    (cond
      ;; Principal not found
      (nil? principal)
      {:success false :error "Account not found"}
      
      ;; Current password incorrect
      (not (verify-password current-password password-hash))
      {:success false :error "Current password is incorrect"}
      
      ;; New password same as current
      (verify-password new-password password-hash)
      {:success false :error "New password must be different from current password"}
      
      ;; New password too short
      (< (count (or new-password "")) min-password-length)
      {:success false :error (str "Password must be at least " min-password-length " characters")}
      
      ;; Process change
      :else
      (let [table (if (= principal-type :admin) "admins" "users")
            new-hash (hash-password new-password)]
        
        (db-protocols/execute! db
          (str "UPDATE " table " SET password_hash = ?, updated_at = NOW() WHERE id = ?")
          [new-hash principal-id])
        
        (log/info "Password changed successfully" 
                  {:principal-type principal-type 
                   :principal-id principal-id})
        
        {:success true}))))

;; ============================================================================
;; Cleanup
;; ============================================================================

(defn cleanup-expired-tokens!
  "Remove expired password reset tokens"
  [db]
  (let [result (db-protocols/execute! db
                 "DELETE FROM password_reset_tokens WHERE expires_at < NOW()"
                 [])]
    (log/info "Cleaned up expired password reset tokens" {:count (count result)})
    {:removed (count result)}))
