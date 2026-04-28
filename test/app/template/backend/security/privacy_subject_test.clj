(ns app.template.backend.security.privacy-subject-test
  (:require
    [app.template.backend.security.privacy-subject :as subject]
    [clojure.test :refer [deftest is testing]]))

(def explicit-key-b64
  "BQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQU=")

(def alternate-key-b64
  "BgYGBgYGBgYGBgYGBgYGBgYGBgYGBgYGBgYGBgYGBgY=")

(defn- with-env
  [env f]
  (with-redefs-fn {#'subject/getenv* (fn [k] (get env k))}
    f))

(deftest user-subject-ref-is-stable-and-secret-derived
  (testing "same UUID/string input resolves to the same 64-char subject ref"
    (with-env {"AERO_PROFILE" "prod"
               "PRIVACY_SUBJECT_KEY_B64" explicit-key-b64}
      #(let [user-id #uuid "00000000-0000-0000-0000-000000000123"
             ref-a (subject/user-subject-ref user-id)
             ref-b (subject/user-subject-ref (str user-id))]
         (is (= ref-a ref-b))
         (is (re-matches #"[0-9a-f]{64}" ref-a)))))

  (testing "different secrets produce different refs"
    (let [user-id #uuid "00000000-0000-0000-0000-000000000123"
          ref-a (with-env {"AERO_PROFILE" "prod"
                           "PRIVACY_SUBJECT_KEY_B64" explicit-key-b64}
                  #(subject/user-subject-ref user-id))
          ref-b (with-env {"AERO_PROFILE" "prod"
                           "PRIVACY_SUBJECT_KEY_B64" alternate-key-b64}
                  #(subject/user-subject-ref user-id))]
      (is (not= ref-a ref-b)))))

(deftest user-subject-ref-handles-empty-input
  (with-env {"AERO_PROFILE" "dev"}
    #(do
       (is (nil? (subject/user-subject-ref nil)))
       (is (nil? (subject/user-subject-ref "")))
       (is (nil? (subject/user-subject-ref "   "))))))

(deftest local-default-key-is-local-only
  (testing "local/test profiles can use the bundled development key"
    (with-env {"AERO_PROFILE" "dev"}
      #(is (string? (subject/user-subject-ref #uuid "00000000-0000-0000-0000-000000000123"))))
    (with-env {"AERO_PROFILE" "test"}
      #(is (string? (subject/user-subject-ref #uuid "00000000-0000-0000-0000-000000000123")))))

  (testing "staging-like profiles require an explicit subject key"
    (let [ex (with-env {"AERO_PROFILE" "staging"}
               #(try
                  (subject/user-subject-ref #uuid "00000000-0000-0000-0000-000000000123")
                  nil
                  (catch clojure.lang.ExceptionInfo e e)))]
      (is (= :privacy-subject/missing-key (-> ex ex-data :type)))
      (is (= :staging (-> ex ex-data :profile))))))

(deftest user-match-clause-supports-migration-window
  (with-env {"AERO_PROFILE" "prod"
             "PRIVACY_SUBJECT_KEY_B64" explicit-key-b64}
    #(let [user-id #uuid "00000000-0000-0000-0000-000000000123"
           clause (subject/user-match-clause :e.subject_ref :e.user_id user-id)]
       (is (= :or (first clause)))
       (is (= [:= :e.user_id user-id] (nth clause 2)))
       (is (re-matches #"[0-9a-f]{64}" (get-in clause [1 2]))))))
