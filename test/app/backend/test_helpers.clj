(ns app.backend.test-helpers
  "Shared test utilities and helpers for backend testing.
   
   Provides:
   - Service container stubs for isolated testing
   - Handler builders with mocked dependencies
   - Request/response helpers for Ring mock testing
   - JSON parsing utilities"
  (:refer-clojure :exclude [random-uuid])
  (:require
    [app.template.backend.routes :as routes]
    [app.template.backend.routes.admin-api :as admin-api]
    [app.admin.backend.services.admin.dashboard :as admin-dashboard]
    [app.template.backend.services.monitoring.login-events :as login-monitoring]
    [app.template.backend.webserver :as webserver]
    [cheshire.core :as json]
    [clojure.string :as str]
    [ring.mock.request :as mock]))

;; ============================================================================
;; Service Container Stubs
;; ============================================================================

(defn stub-service-container
  "Create a minimal service container for testing.
   
   The stub provides minimal implementations of required services
   so routes can be exercised without real database connections.
   
   Options can override any key in the default stub:
   (stub-service-container {:models-data {:users {...}}})"
  ([] (stub-service-container {}))
  ([overrides]
   (merge
     {:models-data {}
      ;; crud-routes must match the DI container shape from app.template.backend.routes.crud
      :crud-routes [["/api/:entity"
                     {:swagger {:tags ["CRUD Operations"]}}
                     ["" {:get {:handler (fn [_] {:status 200 :body "stub list"})}
                          :post {:handler (fn [_] {:status 201 :body "stub create"})}}]
                     ["/:id" {:get {:handler (fn [_] {:status 200 :body "stub get"})}
                              :put {:handler (fn [_] {:status 200 :body "stub update"})}
                              :delete {:handler (fn [_] {:status 204})}}]]]
      :auth-routes {:login-handler (fn [req]
                                     {:status 200
                                      :headers {"Content-Type" "application/json"}
                                      :body (json/generate-string
                                              {:ok true
                                               :has-service-container (boolean (:service-container req))})})
                    :register-handler (fn [_req]
                                        {:status 200
                                         :headers {"Content-Type" "application/json"}
                                         :body (json/generate-string {:ok true})})}
      :password-routes {:forgot-password-handler (fn [_] {:status 200})
                        :verify-reset-token-handler (fn [_] {:status 200})
                        :reset-password-handler (fn [_] {:status 200})
                        :change-password-handler (fn [_] {:status 200})}
      :config {:base-url "http://localhost:8086"}}
     overrides)))

;; ============================================================================
;; Handler Builders
;; ============================================================================

(defn build-handler
  "Build a test handler with stubbed dependencies.
   
   This creates a full Ring handler stack that wraps its execution in with-redefs
   to ensure that database-hitting functions are always stubbed.
   
   Optional `overrides` map allows replacing default stubs:
   {:get-dashboard-stats (fn [db] ...)
    :count-recent-login-events (fn [db opts] ...)}
   
   Usage:
   (let [handler (build-handler)]
     (handler (mock/request :get \"/\")))"
  ([]
   (build-handler (stub-service-container) {}))
  ([service-container]
   (build-handler service-container {}))
  ([service-container overrides]
   (let [actual-handler (with-redefs [admin-api/admin-api-routes
                                      (fn [_ _] ["/admin/api" {:get {:handler (constantly {:status 200})}}])]
                          (-> (routes/app-routes {} service-container)
                            (webserver/wrap-service-container service-container)))]
     (fn [request]
       (with-redefs [admin-dashboard/get-dashboard-stats
                     (get overrides :get-dashboard-stats
                       (fn [_] {:total-admins 0
                                :active-sessions 0
                                :recent-activity 0
                                :recent-events []}))

                     login-monitoring/count-recent-login-events
                     (get overrides :count-recent-login-events
                       (fn [_ _] 0))]
         (actual-handler request))))))

;; ============================================================================
;; Request Helpers
;; ============================================================================

(defn json-request
  "Create a JSON request with proper content-type.
   
   Usage:
   (json-request :post \"/api/v1/auth/login\" {:email \"test@example.com\"})"
  [method path & [body]]
  (-> (mock/request method path)
    (mock/content-type "application/json")
    (cond-> body (mock/body (json/generate-string body)))))

;; ============================================================================
;; Response Helpers
;; ============================================================================

(defn slurp-body
  "Safely slurp response body regardless of type.
   Handles string, InputStream, and nil bodies."
  [resp]
  (let [body (:body resp)]
    (cond
      (string? body) body
      (nil? body) ""
      :else (slurp body))))

(defn parse-json-body
  "Parse JSON response body into Clojure data.
   Uses keywords for map keys."
  [resp]
  (let [body-str (slurp-body resp)]
    (when (seq body-str)
      (json/parse-string body-str true))))

;; ============================================================================
;; Response Assertions
;; ============================================================================

(defn status?
  "Check if response has expected status code."
  [resp expected-status]
  (= expected-status (:status resp)))

(defn ok?
  "Check if response is 200 OK."
  [resp]
  (status? resp 200))

(defn not-found?
  "Check if response is 404 Not Found."
  [resp]
  (status? resp 404))

(defn json-content-type?
  "Check if response has JSON content type."
  [resp]
  (some-> (get-in resp [:headers "Content-Type"])
    (str/includes? "application/json")))

(defn html-content-type?
  "Check if response has HTML content type."
  [resp]
  (some-> (get-in resp [:headers "Content-Type"])
    (str/includes? "text/html")))

;; ============================================================================
;; Test Data Generators
;; ============================================================================

(defn random-uuid
  "Generate a random UUID."
  []
  (java.util.UUID/randomUUID))

;; ============================================================================
;; Fixtures for Unit Tests
;; ============================================================================

(defn with-clean-test-state
  "Fixture that ensures clean state for each test.
   Does not require database connection - for unit tests with mocks."
  [f]
  (f))

;; ============================================================================
;; Mock Helpers for Unit Tests
;; ============================================================================

(defn mock-db
  "Create a mock database placeholder.
   Used in unit tests where handlers are tested with with-redefs."
  []
  :mock-db)

(defn mock-admin-request
  "Create a mock Ring request with admin context.
   
   Usage:
   (mock-admin-request :get \"/admin/api/users\" mock-admin {:params {:search \"test\"}})"
  [method uri admin & [{:keys [params path-params body]}]]
  (cond-> {:request-method method
           :uri uri
           :admin admin
           :headers {"content-type" "application/json"
                     "x-admin-token" "test-token"}
           :params (or params {})}
    path-params (assoc :path-params path-params)
    body (assoc :body body)))

(defn parse-response-body
  "Parse JSON response body to Clojure map."
  [response]
  (when-let [body (:body response)]
    (cond
      (string? body) (json/parse-string body true)
      (map? body) body
      :else (json/parse-string (slurp body) true))))
