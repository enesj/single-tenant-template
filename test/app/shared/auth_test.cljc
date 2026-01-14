(ns app.shared.auth-test
  #?(:clj  (:require
            [app.shared.auth :as auth]
            [clojure.test :refer [deftest is testing]])
     :cljs (:require
             [app.shared.auth :as auth]
             [cljs.test :refer-macros [deftest is testing]])))

(deftest role-normalization-test
  (testing "role->keyword normalizes strings/keywords"
    (is (= :admin (auth/role->keyword "admin")))
    (is (= :admin (auth/role->keyword "  ADMIN  ")))
    (is (= :viewer (auth/role->keyword :viewer)))
    (is (= :super-admin (auth/role->keyword "super_admin")))
    (is (nil? (auth/role->keyword nil)))
    (is (nil? (auth/role->keyword "")))
    (is (nil? (auth/role->keyword "   "))))

  (testing "role->string produces DB-friendly strings"
    (is (= "admin" (auth/role->string :admin)))
    (is (= "member" (auth/role->string "member")))
    (is (nil? (auth/role->string nil)))))

(deftest role-hierarchy-test
  (testing "valid-role? accepts keyword or string roles"
    (is (true? (auth/valid-role? :owner)))
    (is (true? (auth/valid-role? "member")))
    (is (false? (auth/valid-role? "super_admin"))))

  (testing "role-includes? compares privilege levels"
    (is (true? (auth/role-includes? :admin :member)))
    (is (true? (auth/role-includes? "owner" :admin)))
    (is (false? (auth/role-includes? :viewer :member)))
    (is (false? (auth/role-includes? "unknown" :viewer)))))

(deftest permissions-test
  (testing "permission lookup supports keyword and string keyed maps"
    (is (= #{:p1}
          (auth/get-permissions-for-role :admin {:admin #{:p1}})))
    (is (= #{:p1}
          (auth/get-permissions-for-role "admin" {"admin" #{:p1}})))
    (is (= #{}
          (auth/get-permissions-for-role :viewer {:admin #{:p1}}))))

  (testing "get-user-permissions is nil-safe and backward compatible"
    (is (nil? (auth/get-user-permissions nil)))
    (is (= #{:p1}
          (auth/get-user-permissions {:role "admin"} {"admin" #{:p1}}))))

  (testing "calculate-user-permissions delegates to role lookup"
    (is (= #{:p1}
          (auth/calculate-user-permissions {:role :admin} {:admin #{:p1}}))))

  (testing "permission predicates work on sets"
    (is (true? (auth/has-permission? #{:a :b} :a)))
    (is (false? (auth/has-permission? #{:a :b} :c)))
    (is (true? (auth/has-any-permission? #{:a :b} [:c :b])))
    (is (false? (auth/has-any-permission? #{} [:a])))
    (is (true? (auth/has-all-permissions? #{:a :b} [:a :b])))
    (is (false? (auth/has-all-permissions? #{:a} [:a :b])))))

(deftest auth-status-test
  (testing "get-auth-status returns a stable shape"
    (let [auth-session {:user {:id "u1" :role "member"}
                        :tenant {:id "t1"}
                        :permissions #{:p1}}
          status (auth/get-auth-status auth-session {:token "x"} nil)]
      (is (true? (:authenticated status)))
      (is (= {:id "u1" :role "member"} (:user status)))
      (is (= {:id "t1"} (:tenant status)))
      (is (= #{:p1} (:permissions status)))
      (is (= {:token "x"} (:tokens status))))))
