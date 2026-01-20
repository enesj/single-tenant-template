(ns app.domain.frontend.registry-test
  "Tests for the frontend domain registry.
   Verifies that the registry provides correct domain manifests and aggregated data."
  (:require
    [cljs.test :refer [deftest is testing]]
    [app.domain.frontend.registry :as domain-registry]))

(deftest enabled-domains-test
  (testing "enabled-domains returns non-empty vector of manifests"
    (let [domains domain-registry/enabled-domains]
      (is (vector? domains))
      (is (pos? (count domains)))
      (is (every? map? domains)))))

(deftest expenses-manifest-structure-test
  (testing "expenses manifest has required keys"
    (let [manifest (domain-registry/get-domain :expenses)]
      (is (some? manifest))
      (is (= :expenses (:id manifest)))
      (is (contains? manifest :routes))
      (is (contains? manifest :init!))
      (is (contains? manifest :admin-entities))
      (is (contains? manifest :admin-domain-groups))
      (is (contains? manifest :user-domain-groups)))))

(deftest get-domain-test
  (testing "get-domain returns manifest for valid domain"
    (let [manifest (domain-registry/get-domain :expenses)]
      (is (some? manifest))
      (is (= :expenses (:id manifest)))))

  (testing "get-domain returns nil for unknown domain"
    (is (nil? (domain-registry/get-domain :unknown-domain)))))

(deftest all-user-routes-test
  (testing "all-user-routes returns collection of routes"
    (let [routes (domain-registry/all-user-routes)]
      (is (seq routes))
      (is (pos? (count routes)))))

  (testing "user routes have correct structure"
    (let [routes (vec (domain-registry/all-user-routes))
          [path data] (first routes)]
      (is (string? path))
      (is (map? data))
      (is (contains? data :name))
      (is (contains? data :view))
      (is (contains? data :controllers))))

  (testing "user routes include expected expense paths"
    (let [paths (set (map first (domain-registry/all-user-routes)))]
      (is (contains? paths "/expenses"))
      (is (contains? paths "/expenses/list"))
      (is (contains? paths "/expenses/new"))
      (is (contains? paths "/expenses/upload")))))

(deftest all-admin-entities-test
  (testing "all-admin-entities returns a map (may be empty)"
    (let [entities (domain-registry/all-admin-entities)]
      (is (map? entities))))

  (testing "expenses domain currently does not register admin entities"
    (is (empty? (domain-registry/all-admin-entities)))))

(deftest all-admin-domain-groups-test
  (testing "all-admin-domain-groups returns a map (may be empty)"
    (let [groups (domain-registry/all-admin-domain-groups)]
      (is (map? groups))))

  (testing "expenses domain currently does not register admin domain groups"
    (is (empty? (domain-registry/all-admin-domain-groups)))))

(deftest all-user-domain-groups-test
  (testing "all-user-domain-groups returns map of domain groups"
    (let [groups (domain-registry/all-user-domain-groups)]
      (is (map? groups))
      (is (pos? (count groups)))))

  (testing "user domain groups have correct structure"
    (let [groups (domain-registry/all-user-domain-groups)]
      (doseq [[k v] groups]
        (is (contains? v :title) (str "Group " k " missing :title"))
        (is (contains? v :entities) (str "Group " k " missing :entities")))))

  (testing "expenses-user group exists"
    (let [groups (domain-registry/all-user-domain-groups)
          expenses-group (:expenses-user groups)]
      (is (some? expenses-group))
      (is (= "User Expenses" (:title expenses-group))))))

(deftest init-all-domains-test
  (testing "init-all-domains! completes without error"
    (is (nil? (domain-registry/init-all-domains!)))))

(deftest manifest-init-is-function-test
  (testing "manifest :init! is a function"
    (let [manifest (domain-registry/get-domain :expenses)
          init-fn (:init! manifest)]
      (is (fn? init-fn)))))

(deftest manifest-routes-user-is-function-test
  (testing "manifest :routes :user is a function"
    (let [manifest (domain-registry/get-domain :expenses)
          user-routes-fn (get-in manifest [:routes :user])]
      (is (fn? user-routes-fn)))))
