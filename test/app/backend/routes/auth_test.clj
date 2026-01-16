(ns app.backend.routes.auth-test
  (:require
    [app.template.backend.routes.auth :as routes.auth]
    [cheshire.core :as json]
    [clojure.test :refer [deftest is testing]]))

(deftest auth-status-auth-session-test
  (testing "auth-session marks user authenticated"
    (let [req {:session {:auth-session {:user {:id "user-1"
                                               :role "admin"
                                               :auth_provider "github"}}}}
          resp (routes.auth/auth-status-handler req)
          body (json/parse-string (:body resp) true)]
      (is (= 200 (:status resp)))
      (is (= true (:authenticated body)))
      (is (= "github" (:provider body)))
      (is (= "admin" (get-in body [:user :role])))))

  (testing "missing auth-session returns unauthenticated"
    (let [resp (routes.auth/auth-status-handler {})
          body (json/parse-string (:body resp) true)]
      (is (= 200 (:status resp)))
      (is (= false (:authenticated body))))))
