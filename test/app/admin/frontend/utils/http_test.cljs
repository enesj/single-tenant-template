 (ns app.admin.frontend.utils.http-test
   (:require
     [app.admin.frontend.test-setup :as setup]
     [app.admin.frontend.utils.http :as http]
     [cljs.test :refer [deftest is testing]]))

(deftest admin-request-includes-headers-and-token
  (testing "admin-request merges defaults and x-admin-token"
    (setup/reset-db!)
    (setup/install-http-stub!)
    (setup/put-token! "test-token-123")
    (let [req (http/admin-request {:method :get
                                   :uri "/admin/api/ping"
                                   :on-success [:ok]
                                   :on-failure [:err]})]
      (is (= :get (:method req)))
      (is (= "/admin/api/ping" (:uri req)))
      (is (= [:ok] (:on-success req)))
      (is (= [:err] (:on-failure req)))
      (is (= "application/json" (get-in req [:headers "Content-Type"])))
      (is (= "test-token-123" (get-in req [:headers "x-admin-token"])))
      (is (some? (:format req)))
      (is (some? (:response-format req))))))

(deftest http-method-helpers
  (testing "admin-get sets method"
    (is (= :get (:method (http/admin-get {:uri "/u"})))))
  (testing "admin-post sets method"
    (is (= :post (:method (http/admin-post {:uri "/u" :params {:a 1}})))))
  (testing "admin-put sets method"
    (is (= :put (:method (http/admin-put {:uri "/u" :params {:b 2}})))))
  (testing "admin-delete sets method"
    (is (= :delete (:method (http/admin-delete {:uri "/u"})))))
  (testing "admin-patch sets method"
    (is (= :patch (:method (http/admin-patch {:uri "/u" :params {:p 1}}))))))

(deftest extract-error-message-cases
  (testing "extract-error-message variants"
    (is (= "X" (http/extract-error-message {:response {:error "X"}})))
    (is (= "Y" (http/extract-error-message {:response {:message "Y"}})))
    (is (= "Z" (http/extract-error-message {:error "Z"})))
    (is (= "An unexpected error occurred" (http/extract-error-message {})))))

(deftest helper-utilities
  (testing "with-loading-state returns augmented request (no-op keys present)"
    (let [req {:method :get :uri "/u"}
          wrapped (http/with-loading-state req [:some :path])]
      (is (= :get (:method wrapped)))
      (is (= "/u" (:uri wrapped)))))
  (testing "create-standard-handlers builds success/failure vecs"
    (is (= {:on-success [:base :success]
            :on-failure [:base :failure]}
          (http/create-standard-handlers :base)))))
