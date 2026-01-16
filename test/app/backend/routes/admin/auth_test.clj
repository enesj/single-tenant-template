(ns app.backend.routes.admin.auth-test
  "Tests for admin authentication services.
   
   Tests password hashing, verification, session management,
   and token generation."
  (:require
    [app.admin.backend.services.admin.auth :as auth]
    [buddy.core.codecs :as codecs]
    [buddy.core.hash :as hash]
    [clojure.string :as str]
    [clojure.test :refer [deftest is testing]]))

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

;; ============================================================================
;; SHA-256 Legacy Format Tests
;; ============================================================================

(deftest sha256-verification-test
  (testing "verify-sha256-password works with hex-encoded hash"
    ;; SHA-256 of "password" = 5e884898da28047d64c...
    ;; We can verify by hashing a known password
    (let [password "test123"
          ;; Generate expected SHA-256 hex
          expected-hash (-> (hash/sha256 password)
                             codecs/bytes->hex)]
      (is (auth/verify-sha256-password password expected-hash))))

  (testing "verify-sha256-password rejects wrong password"
    (let [correct-hash (-> (hash/sha256 "correct")
                codecs/bytes->hex)]
      (is (not (auth/verify-sha256-password "wrong" correct-hash))))))

;; ==========================================================================
;; Legacy SHA-256 fallback gating (admin login)
;; ==========================================================================

(deftest authenticate-admin-sha256-fallback-gating-test
  (testing "authenticate-admin migrates SHA-256 hashes when legacy support is enabled"
    (let [email "admin@example.com"
          password "legacy-pass"
          sha-hash (-> (hash/sha256 password) codecs/bytes->hex)
          migrate-called? (atom false)
          fake-admin {:id (java.util.UUID/randomUUID)
                      :email email
                      :status "active"
                      :password_hash sha-hash}]
      (with-redefs [auth/find-admin-by-email (fn [_db _email] fake-admin)
                    auth/migrate-admin-password! (fn [& _]
                                                 (reset! migrate-called? true)
                                                 true)]
        (is (some? (auth/authenticate-admin nil email password {:legacy-sha256-enabled? true})))
        (is @migrate-called? "Expected migration to be triggered for SHA-256"))))

  (testing "authenticate-admin rejects SHA-256 hashes when legacy support is disabled"
    (let [email "admin@example.com"
          password "legacy-pass"
          sha-hash (-> (hash/sha256 password) codecs/bytes->hex)
          migrate-called? (atom false)
          fake-admin {:id (java.util.UUID/randomUUID)
                      :email email
                      :status "active"
                      :password_hash sha-hash}]
      (with-redefs [auth/find-admin-by-email (fn [_db _email] fake-admin)
                    auth/migrate-admin-password! (fn [& _]
                                                 (reset! migrate-called? true)
                                                 true)]
        (is (nil? (auth/authenticate-admin nil email password {:legacy-sha256-enabled? false})))
        (is (false? @migrate-called?) "Should not migrate when legacy SHA-256 is disabled")))))

(deftest authenticate-admin-bcrypt-still-works-test
  (testing "authenticate-admin accepts bcrypt hashes regardless of legacy flag"
    (let [email "admin@example.com"
          password "secure-pass"
          bcrypt-hash (auth/hash-password password)
          fake-admin {:id (java.util.UUID/randomUUID)
                      :email email
                      :status "active"
                      :password_hash bcrypt-hash}]
      (with-redefs [auth/find-admin-by-email (fn [_db _email] fake-admin)]
        (is (some? (auth/authenticate-admin nil email password {:legacy-sha256-enabled? false})))
        (is (some? (auth/authenticate-admin nil email password {:legacy-sha256-enabled? true})))))))

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
