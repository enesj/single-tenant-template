(ns app.shared.http-public-api-test
  #?(:clj  (:require
            [app.shared.http :as http]
            [clojure.test :refer [deftest is testing]])
     :cljs (:require
             [app.shared.http :as http]
             [cljs.test :refer-macros [deftest is testing]])))

(deftest status-predicates-test
  (testing "success?"
    (is (true? (http/success? {:status 200})))
    (is (true? (http/success? {:response {:status 204}})))
    (is (false? (http/success? {:status 404}))))

  (testing "client-error?"
    (is (true? (http/client-error? {:status 400})))
    (is (false? (http/client-error? {:status 500}))))

  (testing "server-error?"
    (is (true? (http/server-error? {:status 500})))
    (is (false? (http/server-error? {:status 401})))) )

(deftest header-helpers-test
  (testing "create-json-headers"
    (is (= {http/header-content-type http/content-type-json-utf8}
          (http/create-json-headers)))
    (is (= {http/header-content-type http/content-type-json-utf8
            "X" "y"}
          (http/create-json-headers {"X" "y"}))))

  (testing "create-auth-headers"
    (is (= {http/header-x-admin-token "t"}
          (http/create-auth-headers "t")))
    (is (= {}
          (http/create-auth-headers nil)))
    (is (= {"Authorization" "t"}
          (http/create-auth-headers "t" "Authorization"))))

  (testing "merge-headers"
    (is (= {"a" 1}
          (http/merge-headers nil {"a" 1})))
    (is (= {"a" 1 "b" 2}
          (http/merge-headers {"a" 1} {"b" 2}))))

  (testing "is-json?"
    (is (true? (http/is-json? "application/json; charset=utf-8")))
    (is (true? (http/is-json? {:headers {http/header-content-type "application/json"}})))
    (is (false? (http/is-json? "text/plain")))))

(deftest response-builders-test
  (testing "create-success-response aliases json-response"
    (is (= (http/json-response {:ok true})
          (http/create-success-response {:ok true})))
    (is (= (http/json-response 201 {:ok true})
          (http/create-success-response 201 {:ok true}))))

  (testing "create-error-response aliases error-response"
    (is (= (http/error-response "Nope")
          (http/create-error-response "Nope")))
    (is (= (http/error-response 400 "Nope")
          (http/create-error-response 400 "Nope")))
    (is (= (http/error-response 400 "Nope" {:why "bad"})
          (http/create-error-response 400 "Nope" {:why "bad"})))) )
