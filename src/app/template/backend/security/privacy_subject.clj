(ns app.template.backend.security.privacy-subject
  "Secret-derived pseudonymous subject references for operational data.

   These refs let operational tables group data by authenticated user without
   storing a directly joinable users.id value. The mapping is derived with an
   application secret and is not stored in the database."
  (:require
    [clojure.string :as str])
  (:import
    [java.util Base64 UUID]
    [javax.crypto Mac]
    [javax.crypto.spec SecretKeySpec]))

(def default-dev-subject-key-b64
  "BAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQ=")

(def decoder
  (Base64/getDecoder))

(defn- getenv*
  [k]
  (some-> (System/getenv k) str str/trim not-empty))

(defn- active-profile
  []
  (keyword (or (getenv* "AERO_PROFILE")
             (System/getProperty "app.environment")
             "dev")))

(defn- local-key-default-profile?
  []
  (contains? #{:dev :local :test} (active-profile)))

(defn- decode-base64
  [env-key raw]
  (try
    (.decode decoder ^String raw)
    (catch IllegalArgumentException e
      (throw (ex-info (str "Invalid base64 for " env-key)
               {:type :privacy-subject/invalid-key
                :env-key env-key}
               e)))))

(defn- validate-key-bytes
  [env-key decoded]
  (when (< (alength ^bytes decoded) 32)
    (throw (ex-info (str env-key " must decode to at least 32 bytes")
             {:type :privacy-subject/invalid-key
              :env-key env-key
              :bytes (alength ^bytes decoded)})))
  decoded)

(defn- load-key-bytes
  []
  (let [env-key "PRIVACY_SUBJECT_KEY_B64"
        profile (active-profile)
        raw (or (getenv* env-key)
              (when (local-key-default-profile?) default-dev-subject-key-b64))]
    (when-not raw
      (throw (ex-info (str "Missing required privacy subject key: " env-key)
               {:type :privacy-subject/missing-key
                :env-key env-key
                :profile profile})))
    (->> raw
      (decode-base64 env-key)
      (validate-key-bytes env-key))))

(defn- hex
  [bytes]
  (format "%064x" (java.math.BigInteger. 1 ^bytes bytes)))

(defn user-subject-ref
  "Return the deterministic pseudonymous subject ref for `user-id`.

   The returned value is an HMAC-SHA256 hex string. A DB dump can group rows
   for the same subject, but cannot derive the users.id value without the
   application secret. Nil/blank input returns nil so optional ownership paths
   keep their existing behavior."
  [user-id]
  (let [id (cond
             (nil? user-id) nil
             (instance? UUID user-id) (str user-id)
             :else (some-> user-id str str/trim not-empty))]
    (when id
      (let [mac (Mac/getInstance "HmacSHA256")]
        (.init mac (SecretKeySpec. (load-key-bytes) "HmacSHA256"))
        (->> (.doFinal mac (.getBytes (str "privacy-subject:v1:user:" id) "UTF-8"))
          hex)))))

(defn user-match-clause
  "HoneySQL predicate matching a user-owned row by secret-derived subject ref."
  [subject-column user-id]
  (when-let [subject-ref (user-subject-ref user-id)]
    [:= subject-column subject-ref]))

(defn subject-only-match-clause
  "HoneySQL predicate matching only the subject-ref column."
  [subject-column user-id]
  (user-match-clause subject-column user-id))

(comment
  (user-subject-ref (UUID/randomUUID))
  :rcf)
