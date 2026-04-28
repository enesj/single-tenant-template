(ns app.template.backend.security.email-test
  (:require
    [app.template.backend.security.email :as email]
    [clojure.test :refer [deftest is testing]]))

(def v1-key-b64
  "AgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgI=")

(def v2-key-b64
  "AwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwM=")

(defn- with-env
  [env f]
  (with-redefs-fn {#'email/getenv* (fn [k] (get env k))}
    f))

(deftest email-storage-uses-active-key-version
  (testing "new writes use the active version and active encryption key"
    (with-env {"EMAIL_PRIVACY_KEY_VERSION" "v2"
               "EMAIL_PRIVACY_ENCRYPTION_KEY_B64" v2-key-b64}
      #(let [storage (email/email-storage " User@Example.COM ")]
         (is (= "v2" (:email_key_version storage)))
         (is (= "user@example.com"
               (email/decrypt-email (:email_ciphertext storage)
                 (:email_key_version storage))))))))

(deftest decrypt-email-uses-stored-key-version-from-keyring
  (testing "old ciphertext decrypts with the key matching stored email_key_version"
    (with-env {"EMAIL_PRIVACY_KEY_VERSION" "v2"
               "EMAIL_PRIVACY_ENCRYPTION_KEY_B64" v2-key-b64
               "EMAIL_PRIVACY_ENCRYPTION_KEYRING_B64" (str "v1:" v1-key-b64 ",v2:" v2-key-b64)}
      #(let [old-ciphertext (email/encrypt-email "Old@Example.COM" "v1")]
         (is (= "old@example.com"
               (email/decrypt-email old-ciphertext "v1")))
         (is (= "old@example.com"
               (email/resolve-email {:email_ciphertext old-ciphertext
                                     :email_key_version "v1"})))))))

(deftest decrypt-email-uses-per-version-env-key
  (testing "per-version env vars can supply retired read keys"
    (with-env {"EMAIL_PRIVACY_KEY_VERSION" "v2"
               "EMAIL_PRIVACY_ENCRYPTION_KEY_B64" v2-key-b64
               "EMAIL_PRIVACY_ENCRYPTION_KEY_V1_B64" v1-key-b64}
      #(let [old-ciphertext (email/encrypt-email "EnvKey@Example.COM" "v1")]
         (is (= "envkey@example.com"
               (email/resolve-email {:email_ciphertext old-ciphertext
                                     :email_key_version "v1"})))))))

(deftest missing-retired-key-fails-in-prod
  (testing "prod-like config must retain old keys for old rows"
    (let [old-ciphertext (with-env {"EMAIL_PRIVACY_KEY_VERSION" "v1"
                                    "EMAIL_PRIVACY_ENCRYPTION_KEY_B64" v1-key-b64}
                           #(email/encrypt-email "Missing@Example.COM" "v1"))
          ex (with-env {"AERO_PROFILE" "prod"
                        "EMAIL_PRIVACY_KEY_VERSION" "v2"
                        "EMAIL_PRIVACY_ENCRYPTION_KEY_B64" v2-key-b64}
               #(try
                  (email/decrypt-email old-ciphertext "v1")
                  nil
                  (catch clojure.lang.ExceptionInfo e e)))]
      (is (= :email-privacy/missing-key (-> ex ex-data :type)))
      (is (= "v1" (-> ex ex-data :key-version))))))

(deftest default-dev-keys-are-local-only
  (testing "local/test profiles can use bundled development defaults"
    (with-env {"AERO_PROFILE" "dev"}
      #(is (= "dev@example.com"
             (email/decrypt-email (email/encrypt-email "Dev@Example.COM" "v1")
               "v1"))))
    (with-env {"AERO_PROFILE" "test"}
      #(is (string? (email/email->lookup-hash "test@example.com")))))

  (testing "staging-like profiles must configure explicit encryption and lookup keys"
    (let [encryption-ex (with-env {"AERO_PROFILE" "staging"
                                   "EMAIL_PRIVACY_KEY_VERSION" "v1"}
                          #(try
                             (email/encrypt-email "Stage@Example.COM" "v1")
                             nil
                             (catch clojure.lang.ExceptionInfo e e)))
          lookup-ex (with-env {"AERO_PROFILE" "staging"}
                      #(try
                         (email/email->lookup-hash "stage@example.com")
                         nil
                         (catch clojure.lang.ExceptionInfo e e)))]
      (is (= :email-privacy/missing-key (-> encryption-ex ex-data :type)))
      (is (= :staging (-> encryption-ex ex-data :profile)))
      (is (= :email-privacy/missing-key (-> lookup-ex ex-data :type)))
      (is (= :staging (-> lookup-ex ex-data :profile))))))

(deftest raw-email-takes-precedence-over-ciphertext
  (testing "explicit email values are returned without decrypting ciphertext"
    (with-env {"AERO_PROFILE" "prod"
               "EMAIL_PRIVACY_KEY_VERSION" "v2"
               "EMAIL_PRIVACY_ENCRYPTION_KEY_B64" v2-key-b64}
      #(is (= "plain@example.com"
             (email/resolve-email {:email "plain@example.com"
                                   :email_ciphertext "not-base64"
                                   :email_key_version "missing"}))))))

(deftest invalid-ciphertext-is-rejected
  (testing "malformed ciphertext is not silently accepted"
    (with-env {"EMAIL_PRIVACY_KEY_VERSION" "v1"
               "EMAIL_PRIVACY_ENCRYPTION_KEY_B64" v1-key-b64}
      #(is (thrown? IllegalArgumentException
             (email/decrypt-email "not-base64" "v1"))))))
