(ns app.domain.backend.expenses.integration.manufacturers-integration-test
  (:require
    [clojure.test :refer [deftest is testing use-fixtures]]
    [app.backend.fixtures :as fixtures]
    [app.backend.test-helpers :as h]
    [ring.mock.request :as mock]
    [cheshire.core :as json]))

(use-fixtures :each fixtures/with-transaction-rollback)

(defn- user-session
  ([role] {:auth-session {:user {:id (java.util.UUID/randomUUID)
                                 :role role}}})
  ([] (user-session "admin")))

(deftest manufacturers-user-endpoints-e2e
  (testing "End-to-end manufacturers user API (authz + CRUD)"
    (let [handler (h/build-handler-with-db)
          ;; Helper to hit endpoint with session + optional body
          req-json (fn [method path session & [body]]
                     (-> (h/json-request method path body)
                       (assoc :session session)))
          parse h/parse-json-body]
      ;; 403 for viewer on list
      (let [resp (handler (req-json :get "/api/v1/expenses/manufacturers" (user-session "viewer")))]
        (is (= 403 (:status resp))))
      ;; 200 for admin on list
      (let [resp (handler (req-json :get "/api/v1/expenses/manufacturers" (user-session)))]
        (is (= 200 (:status resp))))
      ;; 201 create
      (let [resp (handler (req-json :post "/api/v1/expenses/manufacturers" (user-session)
                            {:display_name "TestCo"}))
            body (parse resp)
            id (:id (:data body))]
        (is (= 201 (:status resp)))
        (is (some? id))
        ;; 200 update
        (let [resp2 (handler (req-json :put (str "/api/v1/expenses/manufacturers/" id) (user-session)
                               {:display_name "TestCo Updated"}))
              body2 (parse resp2)]
          (is (= 200 (:status resp2)))
          (is (= "TestCo Updated" (get-in body2 [:data :display_name]))))
        ;; 200 delete
        (let [resp3 (handler (req-json :delete (str "/api/v1/expenses/manufacturers/" id) (user-session)))]
          (is (= 200 (:status resp3))))))))