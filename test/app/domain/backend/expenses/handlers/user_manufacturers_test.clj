(ns app.domain.backend.expenses.handlers.user-manufacturers-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [app.domain.backend.expenses.handlers.user-manufacturers :as user-manu]
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [app.domain.backend.expenses.services.manufacturers :as manufacturers]
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

  (testing "200 for admin role with stubbed service and pagination envelope"
    (with-redefs [manufacturers/service
                  {:list (fn [_db opts]
                           (repeat (or (:limit opts) 1) {:id (java.util.UUID/randomUUID)
                                                         :display_name "Test Mfg"}))
                   :count (fn [_db opts]
                            (is (= "ac" (:search opts)))
                            17)}
                  h/json-response (fn [body & [status]] {:status (or status 200) :body body})]
      (let [handler (user-manu/list-manufacturers-handler db)
            req (-> (request :get "/api/v1/expenses/manufacturers"
                      {:auth-session {:user {:id (java.util.UUID/randomUUID)
                                             :role "admin"}}}
                      nil)
                  (assoc :query-params {:limit "2"
                                        :offset "3"
                                        :search "ac"}))
            resp (handler req)]
        (is (= 200 (:status resp)))
        (is (map? (:body resp)))
        (is (vector? (get-in resp [:body :data])))
        (is (= 17 (get-in resp [:body :total])))
        (is (= 2 (get-in resp [:body :limit])))
        (is (= 3 (get-in resp [:body :offset])))))))

(deftest create-manufacturer-validation
  (testing "400 when display_name missing"
    (with-redefs [manufacturers/service
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
    (with-redefs [manufacturers/service
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
        (is (= "ACME" (get-in resp [:body :data :display-name])))))))
