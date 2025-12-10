(ns app.template.backend.auth.email-verification
  "Email verification service for single-tenant authentication"
  (:require
    [app.template.backend.db.protocols :as db-protocols]
    [java-time :as time]
    [taoensso.timbre :as log])
  (:import
    [java.security SecureRandom]
    [java.util Base64]))

(def verification-token-length 32)
(def verification-expiry-hours 24)
(def max-verification-attempts 5)

(defn generate-verification-token
  "Generate a secure random token for email verification"
  []
  (let [random (SecureRandom.)
        bytes (byte-array verification-token-length)]
    (.nextBytes random bytes)
    (-> (Base64/getUrlEncoder)
      (.withoutPadding)
      (.encodeToString bytes))))

(defn create-verification-token!
  "Create a new email verification token for a user"
  [db user-id]
  (let [token (generate-verification-token)
        expires-at (time/plus (time/local-date-time) (time/hours verification-expiry-hours))
        token-data {:id (java.util.UUID/randomUUID)
                    :user_id user-id
                    :token token
                    :expires_at expires-at
                    :attempts 0}]

    ;; Clean up any existing tokens for this user
    (db-protocols/execute! db
      "DELETE FROM email_verification_tokens WHERE user_id = ?"
      [user-id])

    ;; Create new token
    (db-protocols/create db nil :email_verification_tokens token-data)

    (log/info "Created email verification token for user" user-id)
    token))

(defn find-verification-token
  "Find a verification token by token string.

   In the single-tenant template we don't need RLS bypass or tenant joins."
  [db token]
  (first (db-protocols/execute! db
           "SELECT evt.*, u.email, u.full_name
            FROM email_verification_tokens evt
            JOIN users u ON evt.user_id = u.id
            WHERE evt.token = ? AND evt.used_at IS NULL"
           [token])))

(defn- token-expired?
  "Return true if the token has expired given the raw expires_at value from the DB."
  [expires-at]
  (let [current-time (time/instant)
        expires-time (cond
                       ;; Already an Instant (from database timestamptz)
                       (instance? java.time.Instant expires-at)
                       expires-at

                       ;; Offset/Zoned date-times – normalize to Instant
                       (instance? java.time.OffsetDateTime expires-at)
                       (.toInstant ^java.time.OffsetDateTime expires-at)

                       (instance? java.time.ZonedDateTime expires-at)
                       (.toInstant ^java.time.ZonedDateTime expires-at)

                       ;; LocalDateTime (older code paths) – assume default zone
                       (instance? java.time.LocalDateTime expires-at)
                       (-> ^java.time.LocalDateTime expires-at
                         (.atZone (time/zone-id))
                         (.toInstant))

                       ;; java.util.Date
                       (instance? java.util.Date expires-at)
                       (.toInstant ^java.util.Date expires-at)

                       ;; String timestamp
                       (string? expires-at)
                       (try
                         (time/instant expires-at)
                         (catch Exception e
                           (log/warn "Failed to parse expires_at timestamp:" expires-at "error:" (.getMessage e))
                           nil))

                       :else
                       (do
                         (log/warn "Unknown expires_at format:" (type expires-at) expires-at)
                         nil))]
    (if expires-time
      (time/after? current-time expires-time)
      ;; If we cannot interpret the timestamp, treat token as expired for safety.
      true)))

(defn verify-email-token!
  "Verify an email verification token and mark user as verified, progressing onboarding"
  [db token]
  (log/info "Attempting to verify email token" token)

  (if-let [{:email_verification_tokens/keys [used_at expires_at attempts user_id]
            :keys [email]} (find-verification-token db token)]
    (cond
      used_at
      (do
        (log/warn "Verification token already used:" token)
        {:success false :error :token-already-used :message "This verification link has already been used"})

      (token-expired? expires_at)
      (do
        (log/warn "Verification token expired:" token)
        {:success false :error :token-expired :message "Verification link has expired"})

      (>= attempts max-verification-attempts)
      (do
        (log/warn "Too many verification attempts for token:" token)
        {:success false :error :too-many-attempts :message "Too many verification attempts"})

      :else
      (try
        ;; Mark token as used
        (db-protocols/execute! db
          "UPDATE email_verification_tokens
           SET used_at = NOW(),
               attempts = attempts + 1,
               last_attempted_at = NOW()
           WHERE token = ?"
          [token])

        ;; Mark user as verified
        (db-protocols/execute! db
          "UPDATE users SET email_verified = true WHERE id = ?"
          [user_id])

        (log/info "Successfully verified email for user" user_id)

        {:success true
         :user-id user_id
         :email email
         :message "Email successfully verified"}

        (catch Exception e
          (log/error e "Error verifying email token:" token)
          {:success false :error :database-error :message "Failed to verify email"})))
    ;; else case for if-let
    (do
      (log/warn "Verification token not found:" token)
      {:success false :error :token-not-found :message "Invalid verification token"})))

(defn resend-verification-token!
  "Create a new verification token, invalidating any existing ones"
  [db user-id]
  (log/info "Resending verification token for user" user-id)

  ;; Clean up existing tokens
  (db-protocols/execute! db
    "DELETE FROM email_verification_tokens WHERE user_id = ?"
    [user-id])

  ;; Create new token
  (create-verification-token! db user-id))

(defn user-needs-verification?
  "Check if a user needs email verification"
  [user]
  (and user
    (not (:email_verified user))))

(defn mark-user-verification-pending!
  "Mark user's verification status as pending after sending email"
  [db user-id]
  ;; In the single-tenant template we only track a boolean email_verified flag.
  ;; Keeping this function for API compatibility; it simply ensures the flag is false.
  (db-protocols/execute! db
    "UPDATE users SET email_verified = false WHERE id = ?"
    [user-id])
  (log/info "Marked user" user-id "verification state as pending (email_verified = false)"))

(defn get-user-verification-status
  "Get the current verification status for a user"
  [db user-id]
  (let [result (first (db-protocols/execute! db
                        "SELECT email_verified FROM users WHERE id = ?"
                        [user-id]))]
    {:email-verified (:email_verified result)
     ;; Derive a human/status string even though we only persist a boolean.
     :verification-status (if (:email_verified result) "verified" "unverified")}))

(defprotocol EmailService
  "Protocol for sending verification-related emails in the single-tenant app."
  (send-verification-email [service user token])
  (send-verification-success-email [service user]))

;; Mock email service for development/testing
(defrecord MockEmailService [base-url]
  EmailService
  (send-verification-email [_service user token]
    (let [verify-url (str base-url "/verify-email?token=" token)]
      (log/info "Mock email sent to" (:email user) "with verification URL:" verify-url)
      {:success true :url verify-url}))

  (send-verification-success-email [_service user]
    (log/info "Mock verification success email sent to" (:email user))
    {:success true}))

(defn create-mock-email-service
  "Create a mock email service for development"
  [base-url]
  (->MockEmailService base-url))
