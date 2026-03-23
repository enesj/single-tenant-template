(ns app.template.backend.middleware.device-detection-test
  (:require
    [app.template.backend.middleware.device-detection :as dd]
    [clojure.test :refer [deftest is testing]]))

(def iphone-ua "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1")
(def android-phone-ua "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
(def ipad-ua "Mozilla/5.0 (iPad; CPU OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1")
(def android-tablet-ua "Mozilla/5.0 (Linux; Android 14; SM-X810) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
(def desktop-ua "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")

(deftest phone-ua?-test
  (testing "phones are detected"
    (is (true? (dd/phone-ua? iphone-ua)) "iPhone")
    (is (true? (dd/phone-ua? android-phone-ua)) "Android phone"))

  (testing "tablets are NOT classified as phones"
    (is (not (dd/phone-ua? ipad-ua)) "iPad")
    (is (not (dd/phone-ua? android-tablet-ua)) "Android tablet"))

  (testing "desktop is NOT classified as phone"
    (is (not (dd/phone-ua? desktop-ua)) "desktop Chrome"))

  (testing "nil and blank are not phones"
    (is (not (dd/phone-ua? nil)))
    (is (not (dd/phone-ua? "")))
    (is (not (dd/phone-ua? "   ")))))

(def passthrough-handler
  (fn [_] {:status 200 :body "ok"}))

(def wrapped (dd/wrap-mobile-redirect passthrough-handler))

(defn- make-request [path ua & {:keys [method] :or {method :get}}]
  {:uri path
   :request-method method
   :headers {"user-agent" ua}})

(deftest wrap-mobile-redirect-test
  (testing "phone GET to /dashboard → 302 to /m/dashboard"
    (let [resp (wrapped (make-request "/dashboard" iphone-ua))]
      (is (= 302 (:status resp)))
      (is (= "/m/dashboard" (get-in resp [:headers "Location"])))))

  (testing "phone GET to / → 302 to /m/dashboard"
    (let [resp (wrapped (make-request "/" iphone-ua))]
      (is (= 302 (:status resp)))
      (is (= "/m/dashboard" (get-in resp [:headers "Location"])))))

  (testing "phone GET to /login → 302 to /m/login"
    (let [resp (wrapped (make-request "/login" iphone-ua))]
      (is (= 302 (:status resp)))
      (is (= "/m/login" (get-in resp [:headers "Location"])))))

  (testing "phone GET to /expenses/list → 302 to /m/expenses/list"
    (let [resp (wrapped (make-request "/expenses/list" iphone-ua))]
      (is (= 302 (:status resp)))
      (is (= "/m/expenses/list" (get-in resp [:headers "Location"])))))

  (testing "tablet GET to /dashboard → passthrough (200)"
    (let [resp (wrapped (make-request "/dashboard" ipad-ua))]
      (is (= 200 (:status resp)))))

  (testing "desktop GET to /dashboard → passthrough (200)"
    (let [resp (wrapped (make-request "/dashboard" desktop-ua))]
      (is (= 200 (:status resp)))))

  (testing "phone GET to /api/v1/expenses → passthrough (API never redirected)"
    (let [resp (wrapped (make-request "/api/v1/expenses" iphone-ua))]
      (is (= 200 (:status resp)))))

  (testing "phone GET to /admin/dashboard → passthrough (admin never redirected)"
    (let [resp (wrapped (make-request "/admin/dashboard" iphone-ua))]
      (is (= 200 (:status resp)))))

  (testing "phone GET to /m/dashboard → passthrough (already mobile)"
    (let [resp (wrapped (make-request "/m/dashboard" iphone-ua))]
      (is (= 200 (:status resp)))))

  (testing "phone POST to /dashboard → passthrough (only GET redirected)"
    (let [resp (wrapped (make-request "/dashboard" iphone-ua :method :post))]
      (is (= 200 (:status resp)))))

  (testing "phone GET to /health → passthrough"
    (let [resp (wrapped (make-request "/health" iphone-ua))]
      (is (= 200 (:status resp)))))

  (testing "phone GET to /js/main/app.js → passthrough (static asset)"
    (let [resp (wrapped (make-request "/js/main/app.js" iphone-ua))]
      (is (= 200 (:status resp))))))

(deftest desktop-path->mobile-test
  (testing "known path mappings"
    (is (= "/m/dashboard" (dd/desktop-path->mobile "/")))
    (is (= "/m/dashboard" (dd/desktop-path->mobile "/home")))
    (is (= "/m/login" (dd/desktop-path->mobile "/login")))
    (is (= "/m/forgot-password" (dd/desktop-path->mobile "/forgot-password")))
    (is (= "/m/tenant-select" (dd/desktop-path->mobile "/tenant-select"))))

  (testing "unknown paths get /m prefix"
    (is (= "/m/expenses/list" (dd/desktop-path->mobile "/expenses/list")))
    (is (= "/m/receipts" (dd/desktop-path->mobile "/receipts")))
    (is (= "/m/suppliers" (dd/desktop-path->mobile "/suppliers")))))
