(ns app.domain.backend.expenses.handlers.user-stores-test
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [app.domain.backend.expenses.handlers.user-stores :as user-stores]
    [app.domain.backend.expenses.services.stores :as stores]
    [cheshire.core :as json]
    [clojure.test :refer [deftest is testing]]))

(def db :test-db)

(defn- request
  ([method path] (request method path nil nil))
  ([method path session body]
   (cond-> {:request-method method
            :uri path
            :session session
            :headers {"content-type" "application/json"}}
     body (assoc :body (json/generate-string body)))))

(deftest create-store-authz
  (testing "401 when unauthenticated"
    (let [handler (user-stores/create-store-handler db)
          resp (handler (request :post "/api/v1/expenses/stores"))]
      (is (= 401 (:status resp)))))

  (testing "403 for non-admin roles"
    (let [handler (user-stores/create-store-handler db)
          req (request :post "/api/v1/expenses/stores"
                {:auth-session {:user {:id (java.util.UUID/randomUUID)
                                       :role "viewer"}}}
                {:supplier_id (str (java.util.UUID/randomUUID))
                 :display_name "Test Store"})
          resp (handler req)]
      (is (= 403 (:status resp))))))

(deftest create-store-validation
  (testing "400 when supplier_id missing"
    (with-redefs [h/json-response (fn [body & [status]] {:status (or status 200) :body body})]
      (let [handler (user-stores/create-store-handler db)
            req (request :post "/api/v1/expenses/stores"
                  {:auth-session {:user {:id (java.util.UUID/randomUUID)
                                         :role "admin"}}}
                  {:display_name "Store"})
            resp (handler req)]
        (is (= 400 (:status resp)))
        (is (= "supplier_id is required" (get-in resp [:body :error]))))))

  (testing "400 when supplier_id invalid"
    (with-redefs [h/json-response (fn [body & [status]] {:status (or status 200) :body body})]
      (let [handler (user-stores/create-store-handler db)
            req (request :post "/api/v1/expenses/stores"
                  {:auth-session {:user {:id (java.util.UUID/randomUUID)
                                         :role "admin"}}}
                  {:supplier_id "not-a-uuid"
                   :display_name "Store"})
            resp (handler req)]
        (is (= 400 (:status resp)))
        (is (= "Invalid supplier id" (get-in resp [:body :error]))))))

  (testing "400 when display_name missing"
    (with-redefs [h/json-response (fn [body & [status]] {:status (or status 200) :body body})]
      (let [handler (user-stores/create-store-handler db)
            req (request :post "/api/v1/expenses/stores"
                  {:auth-session {:user {:id (java.util.UUID/randomUUID)
                                         :role "admin"}}}
                  {:supplier_id (str (java.util.UUID/randomUUID))})
            resp (handler req)]
        (is (= 400 (:status resp)))
        (is (= "display_name is required" (get-in resp [:body :error]))))))

  (testing "201 when valid payload provided"
    (let [supplier-id (java.util.UUID/randomUUID)]
      (with-redefs [stores/find-or-create-store!
                    (fn [_db data]
                      (is (= supplier-id (:supplier_id data)))
                      (is (= "Mega Market" (:display_name data)))
                      (is (= "Somewhere" (:address data)))
                      (is (= "place123" (:place_id data)))
                      {:id (java.util.UUID/randomUUID)
                       :supplier_id (:supplier_id data)
                       :display_name (:display_name data)
                       :address (:address data)
                       :place_id (:place_id data)})
                    h/json-response (fn [body & [status]] {:status (or status 200) :body body})]
        (let [handler (user-stores/create-store-handler db)
              req (request :post "/api/v1/expenses/stores"
                    {:auth-session {:user {:id (java.util.UUID/randomUUID)
                                           :role "admin"}}}
                    {:supplier_id (str supplier-id)
                     :display_name "  Mega Market  "
                     :address "Somewhere"
                     :place_id "place123"})
              resp (handler req)]
          (is (= 201 (:status resp)))
          (is (= "Mega Market" (get-in resp [:body :data :display-name])))
          (is (= "Somewhere" (get-in resp [:body :data :address])))
          (is (= "place123" (get-in resp [:body :data :place-id])))
          (is (= supplier-id (get-in resp [:body :data :supplier-id]))))))))
