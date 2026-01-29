(ns app.domain.backend.expenses.handlers.user-manufacturers-test
  (:require
    [clojure.test :refer [deftest is testing use-fixtures]]
    [app.domain.backend.expenses.handlers.user-manufacturers :as user-manu]
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [cheshire.core :as json]))

(def db :test-db)

(defn- request
  ([method path] (request method path nil nil))
  ([method path session body]
   (cond-> {:request-method method
            :uri path
            :session session
            :headers {"content-type" "application/json"}}
     body (assoc :body (json/generate-string body)))))

(deftest list-manufacturers-authz
  (testing "401 when unauthenticated"
    (let [handler (user-manu/list-manufacturers-handler db)
          resp (handler (request :get "/api/v1/expenses/manufacturers"))]
      (is (= 401 (:status resp)))))

  (testing "403 for non-admin roles"
    (let [handler (user-manu/list-manufacturers-handler db)
          req (request :get "/api/v1/expenses/manufacturers"
                       {:auth-session {:user {:id (java.util.UUID/randomUUID)
                                              :role "viewer"}}}
                       nil)
          resp (handler req)]
      (is (= 403 (:status resp)))))

  (testing "200 for admin role with stubbed service"
    (with-redefs [app.domain.backend.expenses.services.manufacturers/service
                  {:list (fn [_db opts]
                           ;; Return a simple vector obeying opts
                           (repeat (or (:limit opts) 1) {:id (java.util.UUID/randomUUID)
                                                         :display_name "Test Mfg"}))}
                  h/json-response (fn [body & [status]] {:status (or status 200) :body body})]
      (let [handler (user-manu/list-manufacturers-handler db)
            req (request :get "/api/v1/expenses/manufacturers"
                         {:auth-session {:user {:id (java.util.UUID/randomUUID)
                                                :role "admin"}}}
                         nil)
            resp (handler req)]
        (is (= 200 (:status resp)))
        (is (map? (:body resp)))
        (is (vector? (get-in resp [:body :data])))))))

(deftest create-manufacturer-validation
  (testing "400 when display_name missing"
    (with-redefs [app.domain.backend.expenses.services.manufacturers/service
                  {:create! (fn [& _] (throw (ex-info "Missing display name" {:type :validation})))}
                  h/json-response (fn [body & [status]] {:status (or status 200) :body body})]
      (let [handler (user-manu/create-manufacturer-handler db)
            req (request :post "/api/v1/expenses/manufacturers"
                         {:auth-session {:user {:id (java.util.UUID/randomUUID)
                                                :role "admin"}}}
                         {:bogus true})
            resp (handler req)]
        (is (= 400 (:status resp))))))

  (testing "201 when valid payload provided"
    (with-redefs [app.domain.backend.expenses.services.manufacturers/service
                  {:create! (fn [_db {:keys [display_name]}]
                              {:id (java.util.UUID/randomUUID)
                               :display_name display_name})}
                  h/json-response (fn [body & [status]] {:status (or status 200) :body body})]
      (let [handler (user-manu/create-manufacturer-handler db)
            req (request :post "/api/v1/expenses/manufacturers"
                         {:auth-session {:user {:id (java.util.UUID/randomUUID)
                                                :role "admin"}}}
                         {:display_name "ACME"})
            resp (handler req)]
        (is (= 201 (:status resp)))
        (is (= "ACME" (get-in resp [:body :data :display_name])))))))
