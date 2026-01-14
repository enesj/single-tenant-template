(ns app.domain.backend.registry-test
  "Tests for the backend domain registry.
   Verifies that the registry provides correct domain manifests and aggregated data."
  (:require
    [clojure.test :refer [deftest is testing]]
    [clojure.java.io :as io]
    [app.domain.backend.registry :as domain-registry]))

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
      (is (contains? manifest :ui-config))
      (is (contains? manifest :redirects))
      (is (contains? manifest :spa-routes)))))

(deftest get-domain-test
  (testing "get-domain returns manifest for valid domain"
    (let [manifest (domain-registry/get-domain :expenses)]
      (is (some? manifest))
      (is (= :expenses (:id manifest)))))

  (testing "get-domain returns nil for unknown domain"
    (is (nil? (domain-registry/get-domain :unknown-domain)))))

(deftest all-admin-api-routes-test
  (testing "all-admin-api-routes returns vector of routes"
    (let [routes (domain-registry/all-admin-api-routes nil nil)]
      (is (vector? routes))
      (is (pos? (count routes)))))

  (testing "admin routes start with expected paths"
    (let [routes (domain-registry/all-admin-api-routes nil nil)
          first-path (first (first routes))]
      (is (string? first-path))
      (is (= "/expenses" first-path)))))

(deftest all-user-api-routes-test
  (testing "all-user-api-routes returns vector of routes (no app-config)"
    (let [routes (domain-registry/all-user-api-routes nil nil)]
      (is (vector? routes))
      (is (pos? (count routes)))))

  (testing "all-user-api-routes accepts optional app-config"
    (let [routes (domain-registry/all-user-api-routes nil nil {:some :config})
          first-path (first (first routes))]
      (is (vector? routes))
      (is (string? first-path))
      (is (= "/expenses" first-path)))))

(deftest get-ui-config-paths-test
  (testing "get-ui-config-paths returns map with domain config paths"
    (let [paths (domain-registry/get-ui-config-paths)]
      (is (map? paths))
      (is (contains? paths :expenses))))

  (testing "expenses config paths include all required files"
    (let [paths (domain-registry/get-ui-config-paths)
          expenses-paths (get paths :expenses)]
      (is (map? expenses-paths))
      (is (contains? expenses-paths :entities))
      (is (contains? expenses-paths :view-options))
      (is (contains? expenses-paths :form-fields))
      (is (contains? expenses-paths :table-columns))))

  (testing "all config paths are strings ending in .edn"
    (let [paths (domain-registry/get-ui-config-paths)
          expenses-paths (get paths :expenses)]
      (doseq [[_ path] expenses-paths]
        (is (string? path))
        (is (.endsWith path ".edn"))))))

(deftest primary-user-ui-config-paths-test
  (testing "primary-user-ui-config-paths returns paths map for single-domain setups"
    (let [paths (domain-registry/primary-user-ui-config-paths)]
      (is (map? paths))
      (is (contains? paths :entities))
      (is (contains? paths :view-options))
      (is (contains? paths :form-fields))
      (is (contains? paths :table-columns))))

  (testing "primary-user-ui-config-paths returns nil when no domains are enabled"
    (clojure.core/with-redefs-fn {#'domain-registry/enabled-domains []}
      (fn []
        (is (nil? (domain-registry/primary-user-ui-config-paths)))))))

(deftest get-admin-ui-config-paths-test
  (testing "get-admin-ui-config-paths returns vector of path maps"
    (let [paths (domain-registry/get-admin-ui-config-paths)]
      (is (vector? paths))
      (is (pos? (count paths)))
      (is (every? map? paths))))

  (testing "admin config path maps include expected keys"
    (let [paths (domain-registry/get-admin-ui-config-paths)
          first-domain-paths (first paths)]
      (is (contains? first-domain-paths :view-options))
      (is (contains? first-domain-paths :form-fields))
      (is (contains? first-domain-paths :table-columns))))

  (testing "admin config paths are .edn files and exist on disk"
    (let [paths (domain-registry/get-admin-ui-config-paths)]
      (doseq [domain-paths paths
              [_k path] domain-paths]
        (is (string? path))
        (is (.endsWith path ".edn"))
        (is (.exists (io/file path)) (str "Missing admin config EDN: " path))))))

(deftest get-post-login-path-test
  (testing "get-post-login-path returns valid path"
    (let [path (domain-registry/get-post-login-path)]
      (is (string? path))
      (is (.startsWith path "/")))))

(deftest all-spa-routes-test
  (testing "all-spa-routes returns collection of path strings"
    (let [routes (domain-registry/all-spa-routes)]
      (is (seq routes))
      (is (every? string? routes))))

  (testing "spa-routes include expected expense paths"
    (let [routes (set (domain-registry/all-spa-routes))]
      (is (contains? routes "/expenses"))
      (is (contains? routes "/expenses/list"))
      (is (contains? routes "/expenses/new"))
      (is (contains? routes "/expenses/upload")))))

(deftest manifest-routes-are-functions-test
  (testing "admin-api route is a function"
    (let [manifest (domain-registry/get-domain :expenses)
          admin-api-fn (get-in manifest [:routes :admin-api])]
      (is (fn? admin-api-fn))))

  (testing "user-api route is a function"
    (let [manifest (domain-registry/get-domain :expenses)
          user-api-fn (get-in manifest [:routes :user-api])]
      (is (fn? user-api-fn)))))
