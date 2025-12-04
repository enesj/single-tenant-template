(ns app.backend.routes-smoke-test
  (:require
    [app.backend.routes :as routes]
    [app.backend.routes.admin-api :as admin-api]
    [app.backend.webserver :as webserver]
    [app.backend.services.admin.dashboard :as admin-dashboard]
    [app.backend.services.monitoring.login-events :as login-monitoring]
    [cheshire.core :as json]
    [clojure.test :refer [deftest is testing]]
    [ring.mock.request :as mock]))

(defn- stub-service-container []
  {:models-data {}
   ;; crud-routes must be in the format from DI container: [[\"/:entity\" config subroutes...]]
   :crud-routes [["/:entity" 
                  {:middleware []
                   "" {:get {:handler (fn [_] {:status 200 :body "stub list"})}
                       :post {:handler (fn [_] {:status 201 :body "stub create"})}}}
                  ["/:id" {:get {:handler (fn [_] {:status 200 :body "stub get"})}
                           :put {:handler (fn [_] {:status 200 :body "stub update"})}
                           :delete {:handler (fn [_] {:status 204})}}]]]
   ;; Auth route stubs to keep /api/v1/auth/* handlers happy
   :auth-routes {:login-handler (fn [req]
                                  {:status 200
                                   :headers {"Content-Type" "application/json"}
                                   :body (json/generate-string
                                           {:ok true
                                            :has-service-container (boolean (:service-container req))})})}
   :password-routes {}
   :config {:base-url "http://localhost:8086"}})

(defn- build-handler [service-container]
  (-> (routes/app-routes {} service-container)
      (webserver/wrap-service-container service-container)))

(defn- slurp-body [resp]
  (let [body (:body resp)]
    (cond
      (string? body) body
      (nil? body) ""
      :else (slurp body))))

(deftest home-route-serves-html
  (with-redefs [admin-api/admin-api-routes (fn [_ _] ["/admin/api" {:get {:handler (constantly {:status 200})}}])
                admin-dashboard/get-dashboard-stats (fn [_] {:total-admins 0})
                login-monitoring/count-recent-login-events (fn [_ _] 0)]
    (let [handler (build-handler (stub-service-container))
          resp (handler (mock/request :get "/"))]
      (is (= 200 (:status resp)))
      (is (clojure.string/includes? (get-in resp [:headers "Content-Type"]) "text/html"))
      (is (re-find #"Test route works|<" (slurp-body resp))))))

(deftest metrics-endpoint-returns-json
  (with-redefs [admin-api/admin-api-routes (fn [_ _] ["/admin/api" {:get {:handler (constantly {:status 200})}}])
                admin-dashboard/get-dashboard-stats (fn [_] {:total-admins 2
                                                             :active-sessions 1
                                                             :recent-activity 0
                                                             :recent-events []})
                login-monitoring/count-recent-login-events (fn [_ _] 5)]
    (let [handler (build-handler (stub-service-container))
          resp (handler (mock/request :get "/api/v1/metrics"))
          body (json/parse-string (slurp-body resp) true)]
      (is (= 200 (:status resp)))
      (is (= "v1" (get-in resp [:headers "X-API-Version"])))
      (is (= "ok" (:status body)))
      (is (= 5 (get-in body [:login-metrics :last-24h :total]))))))

(deftest login-route-uses-stub-handler
  (with-redefs [admin-api/admin-api-routes (fn [_ _] ["/admin/api" {:get {:handler (constantly {:status 200})}}])
                admin-dashboard/get-dashboard-stats (fn [_] {:total-admins 0})
                login-monitoring/count-recent-login-events (fn [_ _] 0)]
    (let [svc (stub-service-container)
          handler (build-handler svc)
          resp (handler (mock/request :post "/api/v1/auth/login"))
          body (json/parse-string (slurp-body resp) true)]
      (is (= 200 (:status resp)))
      (is (:ok body))
      (is (:has-service-container body)))))
