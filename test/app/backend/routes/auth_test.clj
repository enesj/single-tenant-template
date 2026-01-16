(ns app.backend.routes.auth-test
  (:require
    [app.template.backend.routes.auth :as routes.auth]
    [cheshire.core :as json]
    [clojure.test :refer [deftest is testing]]))

(deftest auth-status-legacy-oauth-fallback-gating-test
  (testing "legacy OAuth session tokens are treated as authenticated when legacy support is enabled"
    (let [req {:session {:ring.middleware.oauth2/access-tokens
                         {:github {:access-token "tok"}}}}
          resp (routes.auth/auth-status-handler req)
          body (json/parse-string (:body resp) true)]
      (is (= 200 (:status resp)))
      (is (= true (:authenticated body)))
      (is (not (contains? body :legacy-session)))
      (is (= "github" (:provider body)))
      (is (nil? (:user body)))))

  (testing "legacy OAuth session tokens are ignored when legacy support is disabled"
    (let [req {:service-container {:config {:legacy {:oauth {:token-format-enabled? false}}}}
               :session {:ring.middleware.oauth2/access-tokens
                         {:github {:access-token "tok"}}}}
          resp (routes.auth/auth-status-handler req)
          body (json/parse-string (:body resp) true)]
      (is (= 200 (:status resp)))
      (is (= false (:authenticated body)))
      (is (not (contains? body :legacy-session)))
      (is (not (contains? body :provider))))))
