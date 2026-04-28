(ns app.template.backend.security.email
  "Shared email privacy helpers.

   Centralizes normalization, lookup hashing, authenticated encryption,
   masking, and pseudonymous refs so auth,
   invitations, admin tools, and delivery paths all speak the same email
   dialect."
  (:require
    [app.shared.patterns.email :as email-patterns]
    [clojure.string :as str])
  (:import
    [java.security SecureRandom]
    [java.util Base64]
    [javax.crypto Cipher Mac]
    [javax.crypto.spec GCMParameterSpec SecretKeySpec]))

(def ^:private gcm-iv-length 12)
(def ^:private gcm-tag-bits 128)
(def ^:private secure-random (SecureRandom.))
(def ^:private encoder (Base64/getEncoder))
(def ^:private decoder (Base64/getDecoder))
(def ^:private default-dev-encryption-key-b64
  "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
(def ^:private default-dev-lookup-key-b64
  "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE=")

(defn- getenv*
  [k]
  (some-> (System/getenv k) str str/trim not-empty))

(defn- active-profile []
  (keyword (or (getenv* "AERO_PROFILE")
             (System/getProperty "app.environment")
             "dev")))

(defn- prod-profile? []
  (= :prod (active-profile)))

(defn- decode-base64
  [env-key raw]
  (try
    (.decode decoder raw)
    (catch IllegalArgumentException e
      (throw
        (ex-info (str env-key " must be base64-encoded")
          {:type :email-privacy/invalid-key
           :env-key env-key}
          e)))))

(defn- load-key-bytes
  [env-key default-b64]
  (let [raw (or (getenv* env-key)
              (when-not (prod-profile?) default-b64))]
    (when-not raw
      (throw
        (ex-info (str "Missing required email privacy key: " env-key)
          {:type :email-privacy/missing-key
           :env-key env-key})))
    (let [decoded (decode-base64 env-key raw)]
      (when-not (= 32 (alength decoded))
        (throw
          (ex-info (str env-key " must decode to exactly 32 bytes")
            {:type :email-privacy/invalid-key-length
             :env-key env-key
             :decoded-length (alength decoded)})))
      decoded)))

(defn current-key-version
  "Return the active email privacy key version string."
  []
  (or (getenv* "EMAIL_PRIVACY_KEY_VERSION") "v1"))

(defn normalize-email
  "Normalize an email to the canonical value used for lookup + storage."
  [email]
  (some-> email str str/trim str/lower-case not-empty))

(defn valid-email?
  "True when the value is a syntactically valid email."
  [email]
  (let [normalized (normalize-email email)]
    (boolean
      (and normalized
        (or (email-patterns/valid-email? normalized)
          (email-patterns/valid-email-simple? normalized))))))

(defn email->lookup-hash
  "Compute the blind lookup hash for an email using HMAC-SHA256."
  [email]
  (let [normalized (normalize-email email)
        mac (Mac/getInstance "HmacSHA256")]
    (.init mac (SecretKeySpec. (load-key-bytes "EMAIL_PRIVACY_LOOKUP_KEY_B64"
                                 default-dev-lookup-key-b64)
                 "HmacSHA256"))
    (->> (.doFinal mac (.getBytes normalized "UTF-8"))
      (.encodeToString encoder))))

(defn encrypt-email
  "Encrypt a normalized email with AES-256-GCM.

   Returns a base64 payload containing iv + ciphertext-tag bytes."
  ([email]
   (encrypt-email email (current-key-version)))
  ([email _key-version]
   (when-let [normalized (normalize-email email)]
     (let [iv (byte-array gcm-iv-length)
           cipher (Cipher/getInstance "AES/GCM/NoPadding")]
       (.nextBytes secure-random iv)
       (.init cipher
         Cipher/ENCRYPT_MODE
         (SecretKeySpec. (load-key-bytes "EMAIL_PRIVACY_ENCRYPTION_KEY_B64"
                           default-dev-encryption-key-b64)
           "AES")
         (GCMParameterSpec. gcm-tag-bits iv))
       (let [ciphertext (.doFinal cipher (.getBytes normalized "UTF-8"))
             payload (byte-array (+ (alength iv) (alength ciphertext)))]
         (System/arraycopy iv 0 payload 0 (alength iv))
         (System/arraycopy ciphertext 0 payload (alength iv) (alength ciphertext))
         (.encodeToString encoder payload))))))

(defn decrypt-email
  "Decrypt an email ciphertext previously produced by `encrypt-email`."
  [ciphertext]
  (when (some? ciphertext)
    (let [payload (.decode decoder (str ciphertext))
          iv (byte-array gcm-iv-length)
          cipher-bytes (byte-array (- (alength payload) gcm-iv-length))
          cipher (Cipher/getInstance "AES/GCM/NoPadding")]
      (System/arraycopy payload 0 iv 0 gcm-iv-length)
      (System/arraycopy payload gcm-iv-length cipher-bytes 0 (alength cipher-bytes))
      (.init cipher
        Cipher/DECRYPT_MODE
        (SecretKeySpec. (load-key-bytes "EMAIL_PRIVACY_ENCRYPTION_KEY_B64"
                          default-dev-encryption-key-b64)
          "AES")
        (GCMParameterSpec. gcm-tag-bits iv))
      (String. (.doFinal cipher cipher-bytes) "UTF-8"))))

(defn mask-email
  "Return a human-readable masked email hint."
  [email]
  (when-let [normalized (normalize-email email)]
    (let [[local domain] (str/split normalized #"@" 2)
          local-prefix (subs local 0 (min 2 (count local)))
          domain-parts (str/split (or domain "") #"\\.")
          domain-name (first domain-parts)
          domain-suffix (str/join "." (rest domain-parts))
          domain-prefix (subs domain-name 0 (min 1 (count domain-name)))]
      (str local-prefix "***@" domain-prefix "***"
        (when (seq domain-suffix)
          (str "." domain-suffix))))))

(defn email-storage
  "Return the canonical encrypted-at-rest email storage fields for persistence."
  [email]
  (when-let [normalized (normalize-email email)]
    {:email_ciphertext (encrypt-email normalized)
     :email_lookup_hash (email->lookup-hash normalized)
     :email_key_version (current-key-version)}))

(defn merge-email-storage
  "Merge canonical email storage fields into a persistence map."
  [m email]
  (merge m (email-storage email)))

(defn email-match-clause
  "Build a blind-index lookup clause for an email address.

   The second argument is retained for caller compatibility during the
   plaintext-column removal cutover."
  [lookup-hash-column _email-column email]
  [:= lookup-hash-column (email->lookup-hash email)])

(defn- row-value
  [row keys]
  (some #(get row %) keys))

(defn legacy-email-schema-missing?
  "True when a SQL exception indicates the new email privacy columns are not present yet."
  [t]
  (let [psql-ex (loop [e t]
                  (cond
                    (nil? e) nil
                    (instance? org.postgresql.util.PSQLException e) e
                    :else (recur (.getCause ^Throwable e))))
        sql-state (some-> psql-ex .getSQLState)
        message (some-> psql-ex .getMessage str/lower-case)]
    (and (= "42703" sql-state)
      (some #(str/includes? (or message "") %)
        ["email_lookup_hash" "email_ciphertext" "email_key_version"]))))

(defn resolve-email
  "Resolve the concrete email for a row.

   Accepts both explicit application-level email keys and DB ciphertext aliases
   used in JOIN projections."
  [row]
  (or (row-value row [:email
                      :users/email
                      :admins/email
                      :user_email
                      :user-email
                      :owner_email
                      :owner-email
                      :inviter_email
                      :inviter-email
                      :admin_email
                      :admin-email
                      :verification_email
                      :verification-email
                      :created_by_email
                      :created-by-email])
    (some-> (row-value row [:email_ciphertext
                            :users/email_ciphertext
                            :admins/email_ciphertext
                            :tenant_invitations/email_ciphertext
                            :admin_invitations/email_ciphertext
                            :user_email_ciphertext
                            :user-email-ciphertext
                            :owner_email_ciphertext
                            :owner-email-ciphertext
                            :inviter_email_ciphertext
                            :inviter-email-ciphertext
                            :admin_email_ciphertext
                            :admin-email-ciphertext
                            :verification_email_ciphertext
                            :verification-email-ciphertext
                            :created_by_email_ciphertext
                            :created-by-email-ciphertext])
      decrypt-email)))

(defn stable-ref
  "Return a short stable reference derived from a UUID-like identifier."
  [prefix entity-id]
  (when entity-id
    (let [raw (-> (str entity-id)
                (str/replace "-" "")
                str/upper-case)]
      (str prefix "-" (subs raw 0 (min 8 (count raw)))))))

(defn user-ref [user-id] (stable-ref "User" user-id))
(defn admin-ref [admin-id] (stable-ref "Admin" admin-id))
(defn tenant-ref [tenant-id] (stable-ref "Tenant" tenant-id))

(defn user-session-payload
  "Prepare user session payloads by stripping persistence-only email fields while
   preserving the resolved application email used by authenticated UI flows."
  [user]
  (when user
    (let [id (row-value user [:id :users/id])
          full-name (row-value user [:full_name :full-name :users/full_name])
          email (resolve-email user)]
      (cond-> (-> user
                (assoc :id id)
                (dissoc :users/email
                  :email_ciphertext :users/email_ciphertext
                  :email_lookup_hash :users/email_lookup_hash
                  :email_key_version :users/email_key_version
                  :password_hash :password-hash :users/password_hash))
        id (assoc :user_ref (user-ref id))
        email (assoc :email email)
        email (assoc :email_masked (mask-email email))
        (and (str/blank? (str full-name)) id) (assoc :full_name (user-ref id))))))

(defn routine-user-view
  "Remove raw email from a routine admin-facing user projection."
  [row]
  (let [id (row-value row [:id :users/id])
        email (resolve-email row)]
    (cond-> (-> row
              (dissoc :email :users/email
                :email_ciphertext :users/email_ciphertext
                :email_lookup_hash :users/email_lookup_hash
                :email_key_version :users/email_key_version
                :password_hash :password-hash :users/password_hash))
      id (assoc :user_ref (user-ref id))
      email (assoc :email_masked (mask-email email))
      (and id (str/blank? (str (row-value row [:full_name :full-name :users/full_name]))))
      (assoc :full_name (user-ref id)))))

(defn identity-user-view
  "Expose resolved user email for explicit admin identity-management screens.

  Use this only on user/tenant administration surfaces. Routine operational
  admin views should continue using `routine-user-view` or narrower privacy
  projections so receipts/expenses cannot be linked back to a real person."
  [row]
  (let [id (row-value row [:id :users/id])
        email (resolve-email row)]
    (cond-> (-> row
              (dissoc :email_ciphertext :users/email_ciphertext
                :email-ciphertext :users/email-ciphertext
                :email_lookup_hash :users/email_lookup_hash
                :email-lookup-hash :users/email-lookup-hash
                :email_key_version :users/email_key_version
                :email-key-version :users/email-key-version
                :password_hash :password-hash :users/password_hash))
      id (assoc :user_ref (user-ref id))
      email (assoc :email email
              :email_masked (mask-email email))
      (and id (str/blank? (str (row-value row [:full_name :full-name :users/full_name]))))
      (assoc :full_name email))))

(defn routine-admin-view
  "Remove raw email from a routine admin-facing admin projection."
  [row]
  (let [id (row-value row [:id :admins/id])
        email (resolve-email row)]
    (cond-> (-> row
              (dissoc :email :admins/email
                :email_ciphertext :admins/email_ciphertext
                :email_lookup_hash :admins/email_lookup_hash
                :email_key_version :admins/email_key_version
                :password_hash :password-hash :admins/password_hash))
      id (assoc :admin_ref (admin-ref id))
      email (assoc :email_masked (mask-email email))
      (and id (str/blank? (str (row-value row [:full_name :full-name :admins/full_name]))))
      (assoc :full_name (admin-ref id)))))

(defn routine-membership-view
  "Pseudonymize tenant member projections for admin browsing."
  [row]
  (let [user-id (row-value row [:user_id :user-id])]
    (cond-> (-> row
              (dissoc :user_id :user-id
                :user_email :user-email
                :user_email_ciphertext :user-email-ciphertext
                :user_full_name :user-full-name
                :full_name :full-name
                :email :owner_email :owner-email
                :owner_email_ciphertext :owner-email-ciphertext))
      user-id (assoc :user_ref (user-ref user-id))
      true (assoc :user_display_name (or (when user-id (user-ref user-id))
                                       "User")))))

(defn identity-membership-view
  "Expose member email/name for explicit admin tenant-management screens."
  [row]
  (let [user-id (row-value row [:user_id :user-id])
        user-name (row-value row [:user_full_name :user-full-name :full_name :full-name])
        user-email (resolve-email row)]
    (cond-> (-> row
              (dissoc :user_id :user-id
                :user_email_ciphertext :user-email-ciphertext
                :email_ciphertext :email-ciphertext
                :email_lookup_hash :email-lookup-hash
                :email_key_version :email-key-version))
      user-id (assoc :user_ref (user-ref user-id))
      user-email (assoc :user_email user-email
                   :email user-email
                   :email_masked (mask-email user-email))
      true (assoc :user_display_name (or user-name
                                       user-email
                                       (when user-id (user-ref user-id))
                                       "User")))))

(defn user-display-label
  "Return the preferred routine label for a user-linked record."
  [user-id user-name user-email]
  (or (some-> user-name str str/trim not-empty)
    (when user-id (user-ref user-id))
    (mask-email user-email)))

(defn routine-created-by-view
  "Pseudonymize created-by projections for routine admin browsing."
  [row]
  (let [user-id (row-value row [:created_by_user_id :created_by :created-by-user-id :created-by])
        user-name (row-value row [:created_by_full_name :created-by-full-name])
        user-email (resolve-email row)]
    (cond-> (dissoc row :created_by_email :created-by-email
              :created_by_email_ciphertext :created-by-email-ciphertext
              :created_by_full_name)
      true (assoc :created_by_name (user-display-label user-id user-name user-email)))))

(defn routine-payer-view
  "Pseudonymize user-linked payer projections for routine admin browsing."
  [row]
  (let [user-id (row-value row [:user_id :user-id])
        user-name (row-value row [:user_full_name :user-full-name])
        user-email (resolve-email row)]
    (cond-> (dissoc row :user_email :user-email
              :user_email_ciphertext :user-email-ciphertext
              :user_full_name)
      user-id (assoc :user_ref (user-ref user-id))
      true (assoc :user_display_name (user-display-label user-id user-name user-email)))))

(defn routine-tenant-view
  "Pseudonymize owner identity on tenant admin projections."
  [row]
  (let [owner-id (row-value row [:owner_id :owner_user_id])
        owner-name (row-value row [:owner_name])
        owner-email (resolve-email row)]
    (cond-> (-> row
              (dissoc :owner_email :owner-email
                :owner_email_ciphertext :owner-email-ciphertext
                :owner_id :owner_user_id))
      owner-id (assoc :owner_ref (user-ref owner-id))
      true (assoc :owner_name (or owner-name
                                (when owner-id (user-ref owner-id))
                                (mask-email owner-email))))))

(defn identity-tenant-view
  "Expose owner email/name for explicit admin tenant-management screens."
  [row]
  (let [owner-id (row-value row [:owner_id :owner_user_id :owner-id :owner-user-id])
        owner-name (row-value row [:owner_name :owner-name])
        owner-email (resolve-email row)]
    (cond-> (-> row
              (dissoc :owner_email_ciphertext :owner-email-ciphertext
                :owner_id :owner_user_id :owner-id :owner-user-id))
      owner-id (assoc :owner_ref (user-ref owner-id))
      owner-email (assoc :owner_email owner-email
                    :email_masked (mask-email owner-email))
      true (assoc :owner_name (or owner-name
                                owner-email
                                (when owner-id (user-ref owner-id)))))))

(defn redact-email-change
  "Return audit-safe email metadata."
  [email]
  (let [normalized (normalize-email email)]
    (cond-> {}
      normalized (assoc :email_masked (mask-email normalized)
                   :email_lookup_hash (email->lookup-hash normalized)))))

(defn reveal-email-payload
  "Construct the response payload for an explicit reveal action."
  [entity-type row]
  (let [email (resolve-email row)]
    {:entity_type entity-type
     :entity_id (row-value row [:id :users/id :admins/id])
     :entity_ref (case entity-type
                   :user (user-ref (row-value row [:id :users/id]))
                   :admin (admin-ref (row-value row [:id :admins/id]))
                   nil)
     :email email
     :email_masked (mask-email email)}))

(comment
  ;; (require 'app.template.backend.security.email :reload)
  ;; (normalize-email " User@Example.com ")
  ;; (mask-email "user@example.com")
  :rcf)
