(ns app.backend.routes.api-test
  "Tests for public API endpoints.

   Tests metrics, config, and health endpoints."
  (:require
    [app.backend.test-helpers :as h]
    [app.domain.backend.registry :as domain-registry]
    [app.shared.frontend-config.io :as frontend-config-io]
    [app.shared.frontend-config.template-user :as template-user]
    [app.template.backend.routes.admin.settings-io :as settings-io]
    [app.template.backend.routes.api :as api-routes]
    [clojure.string :as str]
    [clojure.test :refer [deftest is testing]]
    [ring.mock.request :as mock]))

;; ============================================================================
;; Metrics Endpoint Tests
;; ============================================================================

(deftest metrics-endpoint-test
  (testing "metrics endpoint returns JSON with expected structure"
    (let [handler (h/build-handler)
          resp (handler (mock/request :get "/api/v1/metrics"))]
      (is (h/ok? resp))
      (is (h/json-content-type? resp))
      (let [body (h/parse-json-body resp)]
        (is (= "ok" (:status body)))
        (is (contains? body :login-metrics)))))

  (testing "metrics endpoint includes login metrics"
    (let [handler (h/build-handler (h/stub-service-container)
                    {:count-recent-login-events (fn [_ _] 42)})
          resp (handler (mock/request :get "/api/v1/metrics"))
          body (h/parse-json-body resp)]
      (is (= 42 (get-in body [:login-metrics :last-24h :total]))))))

;; ============================================================================
;; Config Endpoint Tests
;; ============================================================================

(deftest config-endpoint-test
  (let [runtime-ui-config {:entities {:expenses {:title "Runtime Expenses"}}
                           :view-options {:expenses {:display-defaults {:show-filtering? true}}}
                           :form-fields {:expenses {:create-fields [:amount]}}
                           :table-columns {:expenses {:available-columns [:amount]}}}]
    (clojure.core/with-redefs-fn
      {#'api-routes/single-domain-runtime-ui-config (fn [_db] runtime-ui-config)}
      (fn []
        (testing "config endpoint returns 200"
          (let [handler (h/build-handler)
                resp (handler (mock/request :get "/api/v1/config"))]
            (is (h/ok? resp))))

        (testing "config endpoint returns models data"
          (let [service-container (h/stub-service-container
                                    {:models-data {:items {:fields {:id :uuid
                                                                    :name :string}}}})
                handler (h/build-handler service-container)
                resp (handler (mock/request :get "/api/v1/config"))]
            (is (h/ok? resp))
            (let [body (h/parse-json-body resp)]
              (is (map? body)))))))))

(deftest config-endpoint-loads-runtime-user-ui-config-in-single-domain-mode
  (testing "config endpoint returns runtime-aware user UI config on refresh"
    (clojure.core/with-redefs-fn
      {#'domain-registry/get-ui-config-paths (constantly {:expenses {:entities "ignored"}})
       #'settings-io/read-user-entities (fn [_db] {:expenses {:title "Runtime Expenses"}})
       #'settings-io/read-user-view-options (fn [_db] {:unmapped-aliases {:display-locks {}
                                                                          :display-defaults {}
                                                                          :list-config {:form-display :modal}}})
       #'settings-io/read-user-form-fields (fn [_db] {:expenses {:create-fields [:amount]}})
       #'settings-io/read-user-table-columns (fn [_db] {:expenses {:available-columns [:amount]}})}
      (fn []
        (let [handler (h/build-handler)
              resp (handler (mock/request :get "/api/v1/config"))
              body (h/parse-json-body resp)]
          (is (h/ok? resp))
          (is (= {:expenses {:title "Runtime Expenses"}}
                (get-in body [:domain-ui-config :entities])))
          (is (= {}
                (get-in body [:domain-ui-config :view-options :unmapped-aliases :display-locks])))
          (is (= {:form-display "modal"}
                (get-in body [:domain-ui-config :view-options :unmapped-aliases :list-config]))))))))

(deftest config-endpoint-preserves-multi-domain-shape
  (testing "config endpoint keeps the nested multi-domain response shape"
    (clojure.core/with-redefs-fn
      {#'domain-registry/get-ui-config-paths (constantly {:expenses {:entities "expenses-entities"
                                                                     :view-options "expenses-view-options"}
                                                          :sales {:entities "sales-entities"
                                                                  :table-columns "sales-table-columns"}})
       #'template-user/paths {:entities "template-entities"}
       #'frontend-config-io/read-edn-or-empty+validate
       (fn [{:keys [path]}]
         (case path
           "expenses-entities" {:expenses {:title "Expenses"}}
           "expenses-view-options" {:expenses {:display-defaults {:show-filtering? true}}}
           "sales-entities" {:sales {:title "Sales"}}
           "sales-table-columns" {:sales {:available-columns ["id" "total_amount"]}}
           "template-entities" {:tenant-members {:title "Tenant Members"}}
           {}))
       #'settings-io/read-user-entities (fn [_db] (throw (ex-info "should not read runtime store in multi-domain mode" {})))
       #'settings-io/read-user-view-options (fn [_db] (throw (ex-info "should not read runtime store in multi-domain mode" {})))
       #'settings-io/read-user-form-fields (fn [_db] (throw (ex-info "should not read runtime store in multi-domain mode" {})))
       #'settings-io/read-user-table-columns (fn [_db] (throw (ex-info "should not read runtime store in multi-domain mode" {})))}
      (fn []
        (let [handler (h/build-handler)
              resp (handler (mock/request :get "/api/v1/config"))
              body (h/parse-json-body resp)]
          (is (h/ok? resp))
          (is (= {:expenses {:title "Expenses"}}
                (get-in body [:domain-ui-config :expenses :entities])))
          (is (= {:show-filtering? true}
                (get-in body [:domain-ui-config :expenses :view-options :expenses :display-defaults])))
          (is (= {:sales {:title "Sales"}}
                (get-in body [:domain-ui-config :sales :entities])))
          (is (= ["id" "total_amount"]
                (get-in body [:domain-ui-config :sales :table-columns :sales :available-columns])))
          (is (= {:tenant-members {:title "Tenant Members"}}
                (get-in body [:domain-ui-config :template :entities]))))))))

;; ============================================================================
;; Home Route Tests
;; ============================================================================

(deftest home-route-test
  (testing "home route returns HTML"
    (let [handler (h/build-handler)
          resp (handler (mock/request :get "/"))]
      (is (h/ok? resp))
      (is (h/html-content-type? resp))))

  (testing "home route HTML contains expected content"
    (let [handler (h/build-handler)
          resp (handler (mock/request :get "/"))
          body (h/slurp-body resp)]
      (is (or (str/includes? body "<!DOCTYPE")
            (str/includes? body "<html")
            (str/includes? body "<")
            (str/includes? body "Test route works"))))))

;; ============================================================================
;; Auth Endpoint Tests  
;; ============================================================================

(deftest auth-login-endpoint-test
  (testing "login endpoint accepts POST"
    (let [handler (h/build-handler)
          resp (handler (h/json-request :post "/api/v1/auth/login"
                          {:email "test@example.com"
                           :password "password123"}))]
      (is (h/ok? resp))))

  (testing "login endpoint uses stub handler"
    (let [handler (h/build-handler)
          resp (handler (h/json-request :post "/api/v1/auth/login"))
          body (h/parse-json-body resp)]
      (is (:ok body))
      (is (:has-service-container body)))))

;; ============================================================================
;; API Version Header Tests
;; ============================================================================

(deftest api-version-header-test
  (testing "API endpoints include version header"
    (let [handler (h/build-handler)
          resp (handler (mock/request :get "/api/v1/metrics"))]
      (is (= "v1" (get-in resp [:headers "X-API-Version"])))))

  (testing "version header present on config endpoint"
    (let [handler (h/build-handler)
          resp (handler (mock/request :get "/api/v1/config"))]
      (is (= "v1" (get-in resp [:headers "X-API-Version"]))))))

;; ============================================================================
;; 404 Tests
;; ============================================================================

(deftest not-found-test
  (testing "unknown API path returns 404"
    (let [handler (h/build-handler)
          resp (handler (mock/request :get "/api/v1/unknown-endpoint"))]
      (is (h/not-found? resp))))

  (testing "malformed API path returns 404"
    (let [handler (h/build-handler)
          resp (handler (mock/request :get "/api/v999/metrics"))]
      (is (h/not-found? resp)))))
