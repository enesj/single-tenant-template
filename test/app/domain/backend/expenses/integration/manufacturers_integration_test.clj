(ns app.domain.backend.expenses.integration.manufacturers-integration-test
  (:require
    [clojure.test :refer [deftest is testing use-fixtures]]
    [app.backend.fixtures :as fixtures]
    [app.domain.backend.expenses.handlers.user-manufacturers :as user-manu]
    [cheshire.core :as json]))

(use-fixtures :each fixtures/with-transaction-rollback)

(defn- user-session
  ([role]
   {:auth-session {:user {:id (java.util.UUID/randomUUID)
                          :role role}}})
  ([]
   (user-session "admin")))

(defn- req
  "Build a minimal Ring request map for calling handlers directly.

  Note: these are handler-level integration tests (service + DB), not full
  router/middleware stack tests. (The full stack uses cookie-backed sessions.)"
  [{:keys [method uri session body query-params path-params]}]
  (cond-> {:request-method method
           :uri uri
           :session session
           :headers {"content-type" "application/json"}}
    (some? query-params) (assoc :query-params query-params)
    (some? path-params) (assoc :path-params path-params)
    (some? body) (assoc :body (json/generate-string body))))

(defn- parse-body
  [resp]
  (json/parse-string (:body resp) true))

(deftest manufacturers-user-endpoints-e2e
  (testing "End-to-end manufacturers user API (authz + CRUD)"
    (when-let [db fixtures/*test-db*]
      (let [list-handler (user-manu/list-manufacturers-handler db)
            create-handler (user-manu/create-manufacturer-handler db)
            update-handler (user-manu/update-manufacturer-handler db)
            delete-handler (user-manu/delete-manufacturer-handler db)
            uri "/api/v1/expenses/manufacturers"]

        ;; 403 for viewer on list
        (let [resp (list-handler (req {:method :get
                                       :uri uri
                                       :session (user-session "viewer")
                                       :query-params {:limit "2"}}))]
          (is (= 403 (:status resp))))

        ;; 200 for admin on list
        (let [resp (list-handler (req {:method :get
                                       :uri uri
                                       :session (user-session)
                                       :query-params {:limit "2"}}))
              body (parse-body resp)]
          (is (= 200 (:status resp)))
          (is (vector? (:data body))))

        ;; 201 create
        (let [resp (create-handler (req {:method :post
                                         :uri uri
                                         :session (user-session)
                                         :body {:display_name "TestCo"}}))
              body (parse-body resp)
              id (get-in body [:data :id])]
          (is (= 201 (:status resp)))
          (is (some? id))

          ;; 200 update
          (let [resp2 (update-handler (req {:method :put
                                            :uri (str uri "/" id)
                                            :session (user-session)
                                            :path-params {:id id}
                                            :body {:display_name "TestCo Updated"}}))
                body2 (parse-body resp2)]
            (is (= 200 (:status resp2)))
            (is (= "TestCo Updated" (get-in body2 [:data :display-name]))))

          ;; 200 delete
          (let [resp3 (delete-handler (req {:method :delete
                                            :uri (str uri "/" id)
                                            :session (user-session)
                                            :path-params {:id id}}))]
            (is (= 200 (:status resp3)))))))))