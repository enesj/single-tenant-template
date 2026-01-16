(ns app.backend.routes-smoke-test
  (:require
   [app.template.backend.routes :as routes]
   [app.template.backend.routes.admin-api :as admin-api]
   [app.admin.backend.services.admin.dashboard :as admin-dashboard]
   [app.template.backend.services.monitoring.login-events :as login-monitoring]
   [app.template.backend.webserver :as webserver]
   [cheshire.core :as json]
    [clojure.string :as str]
   [clojure.test :refer [deftest is]]
   [ring.mock.request :as mock]))

(defn- stub-service-container
  ([] (stub-service-container {}))
  ([overrides]
   (let [base
         {:models-data {}
          ;; crud-routes must be in the format from DI container: [["/:entity" config subroutes...]]
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
          :config {:base-url "http://localhost:8086"}}

         merged (merge base overrides)]
     ;; Merge nested :config (common override use-case in route tests).
     (update merged :config #(merge (:config base) %)))))

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
      (is (str/includes? (get-in resp [:headers "Content-Type"]) "text/html"))
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

(deftest legacy-admin-settings-routes-serve-spa-when-enabled
  (with-redefs [admin-api/admin-api-routes (fn [_ _] ["/admin/api" {:get {:handler (constantly {:status 200})}}])
                admin-dashboard/get-dashboard-stats (fn [_] {:total-admins 0})
                login-monitoring/count-recent-login-events (fn [_ _] 0)]
    (let [handler (build-handler (stub-service-container))]
      (doseq [path ["/admin/settings" "/admin/amin-settings"]]
        (let [resp (handler (mock/request :get path))]
          (is (= 200 (:status resp)) (str "expected 200 for " path))
          (is (str/includes? (get-in resp [:headers "Content-Type"]) "text/html")
            (str "expected HTML for " path)))))))

(deftest legacy-admin-settings-routes-return-410-when-disabled
  (with-redefs [admin-api/admin-api-routes (fn [_ _] ["/admin/api" {:get {:handler (constantly {:status 200})}}])
                admin-dashboard/get-dashboard-stats (fn [_] {:total-admins 0})
                login-monitoring/count-recent-login-events (fn [_ _] 0)]
    (let [svc (stub-service-container
                {:config {:legacy {:routes {:admin-settings {:enabled? false}}}}})
          handler (build-handler svc)]
      (doseq [path ["/admin/settings" "/admin/amin-settings"]]
        (let [resp (handler (mock/request :get path))
              body (slurp-body resp)]
          (is (= 410 (:status resp)) (str "expected 410 for " path))
          (is (str/includes? (get-in resp [:headers "Content-Type"]) "text/plain")
            (str "expected text/plain for " path))
          (is (str/includes? body "Use /admin/admin-settings")
            (str "expected migration guidance for " path)))))))
