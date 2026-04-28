(ns app.template.backend.auth.password-reset-test
  (:require
    [app.template.backend.auth.password-reset :as password-reset]
    [app.template.backend.db.protocols :as db-protocols]
    [app.template.backend.security.tokens :as token-security]
    [clojure.test :refer [deftest is testing]]))

(deftest create-reset-token-stores-hash
  (testing "create-reset-token! returns raw token but stores only its hash"
    (let [principal-id (java.util.UUID/randomUUID)
          raw-token "reset-token-1"
          calls (atom [])]
      (with-redefs [password-reset/generate-reset-token (constantly raw-token)
                    db-protocols/execute! (fn [_db sql params]
                                            (swap! calls conj {:sql sql :params params})
                                            [])]
        (let [result (password-reset/create-reset-token! :db :user principal-id)
              insert-call (second @calls)
              params (:params insert-call)]
          (is (= raw-token (:token result)))
          (is (some #{(token-security/hash-token raw-token)} params)
            "hashed token is persisted")
          (is (not (some #{raw-token} params))
            "raw token is not persisted"))))))

(deftest reset-token-lookups-use-token-hash
  (testing "find-reset-token hashes the incoming raw token before querying"
    (let [raw-token "reset-token-2"]
      (with-redefs [db-protocols/execute! (fn [_db sql params]
                                            (is (string? sql))
                                            (is (= [(token-security/hash-token raw-token)] params))
                                            [])]
        (is (nil? (password-reset/find-reset-token :db raw-token))))))

  (testing "mark-token-used! hashes the incoming raw token before updating"
    (let [raw-token "reset-token-3"]
      (with-redefs [db-protocols/execute! (fn [_db sql params]
                                            (is (string? sql))
                                            (is (= [(token-security/hash-token raw-token)] params))
                                            [])]
        (is (= [] (password-reset/mark-token-used! :db raw-token)))))))