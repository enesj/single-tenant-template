(ns app.domain.expenses.handlers.user-handlers-test
  "Regression tests for user-facing handler auth/role plumbing.

  These tests intentionally avoid hitting the DB. They verify that handlers return
  consistent JSON error responses and that auth/role extraction works for both
  session-based and :identity-based request shapes."
  (:require
    [app.domain.backend.expenses.handlers.user-articles :as user-articles]
    [app.domain.backend.expenses.handlers.user-raw-labels :as user-raw-labels]
    [app.domain.backend.expenses.handlers.user-receipts :as user-receipts]
    [cheshire.core :as json]
    [clojure.test :refer [deftest is testing]])
  (:import
    [java.util UUID]))

(defn- parse-json-body
  [resp]
  (when-let [body (:body resp)]
    (cond
      (map? body) body
      (string? body) (json/parse-string body true)
      :else (json/parse-string (slurp body) true))))

(deftest user-articles-unauthorized-when-no-user
  (testing "user articles handlers return 401 when request has no user"
    (let [handler (user-articles/list-articles-handler nil)
          resp (handler {})
          body (parse-json-body resp)]
      (is (= 401 (:status resp)))
      (is (= "Authentication required" (:error body))))))

(deftest user-articles-forbidden-when-role-missing-even-with-identity
  (testing "user articles handlers accept :identity but require admin/owner role"
    (let [handler (user-articles/list-articles-handler nil)
          resp (handler {:identity {:id (UUID/randomUUID)}})
          body (parse-json-body resp)]
      (is (= 403 (:status resp)))
      (is (= "Only admins and owners can access this page." (:error body))))))

(deftest user-receipts-forbidden-when-role-missing-even-with-identity
  (testing "user receipts handlers accept :identity for user-id extraction but still role-gate"
    (let [handler (user-receipts/list-receipts-handler nil)
          resp (handler {:identity {:id (UUID/randomUUID)}})
          body (parse-json-body resp)]
      (is (= 403 (:status resp)))
      (is (= "Role assignment required" (:error body))))))

(deftest user-raw-labels-unauthorized-when-no-user
  (testing "user raw labels handlers return 401 when request has no user"
    (let [handler (user-raw-labels/list-raw-labels-handler nil)
          resp (handler {})
          body (parse-json-body resp)]
      (is (= 401 (:status resp)))
      (is (= "Authentication required" (:error body))))))

(deftest user-raw-labels-forbidden-when-role-missing-even-with-identity
  (testing "user raw labels handlers accept :identity but require admin/owner role"
    (let [handler (user-raw-labels/list-raw-labels-handler nil)
          resp (handler {:identity {:id (UUID/randomUUID)}})
          body (parse-json-body resp)]
      (is (= 403 (:status resp)))
      (is (= "Only admins and owners can access this page." (:error body))))))
