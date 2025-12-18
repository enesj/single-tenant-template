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

(deftest all-admin-routes-test
  (testing "all-admin-routes returns empty (admin routes imported directly)"
    ;; Admin routes are intentionally empty in the registry to avoid circular deps
    ;; They are imported directly by admin/frontend/routes.cljs
    (let [routes (domain-registry/all-admin-routes)]
      (is (empty? routes)))))

(deftest all-pages-test
  (testing "all-pages returns empty (pages imported directly)"
    ;; Pages are intentionally empty in the registry to avoid circular deps
    ;; They are imported directly by template/frontend/core.cljs
    (let [pages (domain-registry/all-pages)]
      (is (empty? pages)))))

(deftest all-admin-entities-test
  (testing "all-admin-entities returns map of entity registrations"
    (let [entities (domain-registry/all-admin-entities)]
      (is (map? entities))
      (is (pos? (count entities)))))

  (testing "admin entities include expected expense entities"
    (let [entity-keys (set (keys (domain-registry/all-admin-entities)))]
      (is (contains? entity-keys :expenses))
      (is (contains? entity-keys :receipts))
      (is (contains? entity-keys :suppliers))
      (is (contains? entity-keys :payers))
      (is (contains? entity-keys :articles))))

  (testing "each admin entity has init-fn"
    (let [entities (domain-registry/all-admin-entities)]
      (doseq [[k v] entities]
        (is (contains? v :init-fn) (str "Entity " k " missing :init-fn"))
        (is (fn? (:init-fn v)) (str "Entity " k " :init-fn is not a function"))))))

(deftest all-admin-domain-groups-test
  (testing "all-admin-domain-groups returns map of domain groups"
    (let [groups (domain-registry/all-admin-domain-groups)]
      (is (map? groups))
      (is (pos? (count groups)))))

  (testing "admin domain groups have correct structure"
    (let [groups (domain-registry/all-admin-domain-groups)]
      (doseq [[k v] groups]
        (is (contains? v :title) (str "Group " k " missing :title"))
        (is (contains? v :entities) (str "Group " k " missing :entities"))
        (is (set? (:entities v)) (str "Group " k " :entities is not a set")))))

  (testing "expenses-admin group exists with correct entities"
    (let [groups (domain-registry/all-admin-domain-groups)
          expenses-group (:expenses-admin groups)]
      (is (some? expenses-group))
      (is (= "Expenses Admin" (:title expenses-group)))
      (is (contains? (:entities expenses-group) :expenses))
      (is (contains? (:entities expenses-group) :suppliers)))))

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
