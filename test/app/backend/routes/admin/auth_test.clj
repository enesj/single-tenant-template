(ns app.backend.routes.admin.auth-test
  "Tests for admin authentication services.
   
   Tests password hashing, verification, session management,
   and token generation."
  (:require
    [app.admin.backend.services.admin.auth :as auth]
    [clojure.string :as str]
    [clojure.test :refer [deftest is testing]]
    [next.jdbc :as jdbc]))

;; ============================================================================
;; Password Hashing Tests
;; ============================================================================

(deftest password-hashing-test
  (testing "hash-password creates valid bcrypt hash"
    (let [password "test-password-123"
          hash (auth/hash-password password)]
      (is (string? hash))
      (is (> (count hash) 50) "bcrypt hashes should be ~60+ chars")
      (is (str/starts-with? hash "bcrypt+sha512$")
        "should use bcrypt+sha512 algorithm")))

  (testing "hash-password creates different hashes for same password"
    (let [password "same-password"
          hash1 (auth/hash-password password)
          hash2 (auth/hash-password password)]
      (is (not= hash1 hash2) "hashes should include random salt"))))

(deftest password-verification-test
  (testing "verify-password validates correct password"
    (let [password "test-password-123"
          hash (auth/hash-password password)]
      (is (auth/verify-password password hash))))

  (testing "verify-password rejects wrong password"
    (let [hash (auth/hash-password "correct-password")]
      (is (not (auth/verify-password "wrong-password" hash)))))

  (testing "verify-password handles empty password"
    (let [hash (auth/hash-password "some-password")]
      (is (not (auth/verify-password "" hash)))))

  (testing "verify-password handles nil password gracefully"
    (let [hash (auth/hash-password "some-password")]
      ;; buddy.hashers/check returns false for nil input
      (is (not (auth/verify-password nil hash)))))

  (testing "verify-bcrypt-password handles invalid hash format"
    (is (not (auth/verify-bcrypt-password "password" "not-a-valid-hash")))))

;; ============================================================================
;; Session Token Tests
;; ============================================================================

(deftest session-token-generation-test
  (testing "generates UUID format tokens"
    (let [token (auth/generate-session-token)]
      (is (string? token))
      (is (= 36 (count token)) "UUID should be 36 chars")
      (is (re-matches #"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}" token)
        "should be valid UUID format")))

  (testing "generates unique tokens"
    (let [tokens (repeatedly 100 auth/generate-session-token)]
      (is (= 100 (count (set tokens))) "all tokens should be unique"))))

(deftest session-token-storage-hashing-test
  (testing "hash-session-token derives a deterministic non-bearer storage value"
    (let [token "admin-token-1"
          storage (auth/hash-session-token token)]
      (is (string? storage))
      (is (= 64 (count storage)) "SHA-256 hex should be 64 chars")
      (is (re-matches #"[0-9a-f]{64}" storage) "storage value should be lowercase hex")
      (is (not= token storage) "storage value must not be the raw bearer token")
      (is (= storage (auth/hash-session-token token)) "same token should hash consistently")))

  (testing "hash-session-token preserves nil as no-match input"
    (is (nil? (auth/hash-session-token nil)))))

(deftest create-admin-session-stores-token-hash-test
  (testing "create-admin-session! returns raw token but stores only its hash"
    (let [admin-id (java.util.UUID/randomUUID)
          raw-token "00000000-0000-0000-0000-000000000001"
          storage-token (auth/hash-session-token raw-token)
          calls (atom [])]
      (with-redefs [auth/generate-session-token (constantly raw-token)
                    jdbc/execute-one! (fn [_db statement opts]
                                        (swap! calls conj {:statement statement :opts opts})
                                        {:id (java.util.UUID/randomUUID)})]
        (let [session (auth/create-admin-session! :db admin-id "127.0.0.1" "ua")
              [_sql & params] (:statement (first @calls))]
          (is (= raw-token (:token session)) "raw bearer token is still returned to the caller")
          (is (some #{storage-token} params) "hashed token is persisted")
          (is (not (some #{raw-token} params)) "raw bearer token is not persisted"))))))

(deftest authenticate-admin-bcrypt-still-works-test
  (testing "authenticate-admin accepts bcrypt hashes"
    (let [email "admin@example.com"
          password "secure-pass"
          bcrypt-hash (auth/hash-password password)
          fake-admin {:id (java.util.UUID/randomUUID)
                      :email email
                      :status "active"
                      :password_hash bcrypt-hash}]
      (with-redefs [auth/find-admin-by-email (fn [_db _email] fake-admin)]
        (is (some? (auth/authenticate-admin nil email password)))))))

;; ============================================================================
;; Integration-like Tests (with stubs)
;; ============================================================================

(deftest password-strength-test
  (testing "bcrypt handles various password lengths"
    (doseq [length [1 8 16 32 64 128]]
      (let [password (apply str (repeat length "a"))
            hash (auth/hash-password password)]
        (is (auth/verify-password password hash)
          (str "should handle " length " char password"))))))

(deftest password-special-chars-test
  (testing "bcrypt handles special characters"
    (doseq [password ["pass!@#$%^&*()"
                      "パスワード"
                      "пароль"
                      "🔐🔑🔒"
                      "with\nnewline"
                      "with\ttab"]]
      (let [hash (auth/hash-password password)]
        (is (auth/verify-password password hash)
          (str "should handle password: " (pr-str password)))))))
