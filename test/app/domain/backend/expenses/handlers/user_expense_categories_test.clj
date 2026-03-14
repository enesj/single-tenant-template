(ns app.domain.backend.expenses.handlers.user-expense-categories-test
  (:require
    [app.domain.backend.expenses.handlers.user-expense-categories :as user-expense-categories]
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [app.domain.backend.expenses.services.expense-categories :as expense-categories]
    [cheshire.core :as json]
    [clojure.test :refer [deftest is testing]])
  (:import
    [java.util UUID]))

(def db :test-db)

(defn- request
  ([method path] (request method path nil nil))
  ([method path session body]
   (cond-> {:request-method method
            :uri path
            :session session
            :headers {"content-type" "application/json"}}
     body (assoc :body (json/generate-string body)))))

(defn- admin-session
  [tenant-id]
  {:auth-session {:user {:id (UUID/randomUUID)
                         :role "admin"}
                  :tenant {:id tenant-id}}})

(deftest list-expense-categories-scopes-to-current-tenant
  (let [tenant-id (UUID/randomUUID)
        list-opts (atom nil)
        count-opts (atom nil)]
    (with-redefs [expense-categories/service
                  {:list (fn [_db opts]
                           (reset! list-opts opts)
                           [{:id (UUID/randomUUID)
                             :tenant_id tenant-id
                             :name "Utilities"}])
                   :count (fn [_db opts]
                            (reset! count-opts opts)
                            9)}
                  h/json-response (fn [body & [status]] {:status (or status 200)
                                                         :body body})]
      (let [handler (user-expense-categories/list-expense-categories-handler db)
            resp (handler (assoc (request :get "/api/v1/expenses/expense-categories"
                                   (admin-session tenant-id)
                                   nil)
                            :query-params {:limit "2"
                                           :offset "3"
                                           :search "util"}))]
        (is (= 200 (:status resp)))
        (is (= 9 (get-in resp [:body :total])))
        (is (= {:limit 2
                :offset 3
                :search "util"
                :tenant-id tenant-id}
              @list-opts))
        (is (= {:search "util"
                :tenant-id tenant-id}
              @count-opts))))))

(deftest create-expense-category-includes-current-tenant-id
  (let [tenant-id (UUID/randomUUID)
        seen-payload (atom nil)]
    (with-redefs [expense-categories/service
                  {:create! (fn [_db payload]
                              (reset! seen-payload payload)
                              {:id (UUID/randomUUID)
                               :tenant_id (:tenant_id payload)
                               :name (:name payload)})}
                  h/json-response (fn [body & [status]] {:status (or status 200)
                                                         :body body})]
      (let [handler (user-expense-categories/create-expense-category-handler db)
            resp (handler (request :post "/api/v1/expenses/expense-categories"
                            (admin-session tenant-id)
                            {:name "Utilities"}))]
        (is (= 201 (:status resp)))
        (is (= {:name "Utilities"
                :tenant_id tenant-id}
              @seen-payload))
        (is (= tenant-id (get-in resp [:body :data :tenant-id])))))))

(deftest update-expense-category-uses-current-tenant-scope
  (let [tenant-id (UUID/randomUUID)
        expense-category-id (UUID/randomUUID)
        seen-args (atom nil)]
    (with-redefs [expense-categories/service
                  {:update! (fn [_db id updates opts]
                              (reset! seen-args {:id id :updates updates :opts opts})
                              {:id id
                               :tenant_id tenant-id
                               :name (:name updates)})}
                  h/json-response (fn [body & [status]] {:status (or status 200)
                                                         :body body})]
      (let [handler (user-expense-categories/update-expense-category-handler db)
            resp (handler (assoc (request :put (str "/api/v1/expenses/expense-categories/" expense-category-id)
                                   (admin-session tenant-id)
                                   {:name "Travel"})
                            :path-params {:id (str expense-category-id)}))]
        (is (= 200 (:status resp)))
        (is (= {:id expense-category-id
                :updates {:name "Travel"}
                :opts {:tenant-id tenant-id}}
              @seen-args))
        (is (= "Travel" (get-in resp [:body :data :name])))))))

(deftest batch-delete-expense-categories-uses-current-tenant-scope
  (let [tenant-id (UUID/randomUUID)
        category-id-a (UUID/randomUUID)
        category-id-b (UUID/randomUUID)
        delete-calls (atom [])]
    (with-redefs [expense-categories/service
                  {:delete! (fn [_db id opts]
                              (swap! delete-calls conj {:id id :opts opts})
                              true)}
                  h/json-response (fn [body & [status]] {:status (or status 200)
                                                         :body body})]
      (let [handler (user-expense-categories/batch-delete-expense-categories-handler db)
            resp (handler (request :delete "/api/v1/expenses/expense-categories/batch"
                            (admin-session tenant-id)
                            {:ids [(str category-id-a) (str category-id-b)]}))]
        (is (= 200 (:status resp)))
        (is (= [{:id category-id-a :opts {:tenant-id tenant-id}}
                {:id category-id-b :opts {:tenant-id tenant-id}}]
              @delete-calls))
        (is (= 2 (get-in resp [:body :data :deleted-count])))))))