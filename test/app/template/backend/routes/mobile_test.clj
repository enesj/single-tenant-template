(ns app.template.backend.routes.mobile-test
  (:require
    [app.template.backend.routes.mobile :as mobile]
    [clojure.test :refer [deftest is testing]]))

(deftest render-mobile-page-test
  (testing "renders the mobile shell when the bundle is available"
    (with-redefs [mobile/mobile-bundle-present? (constantly true)]
      (let [resp (mobile/render-mobile-page {:uri "/m/t/enes-jakic/receipts"})]
        (is (= 200 (:status resp)))
        (is (= "text/html" (get-in resp [:headers "Content-Type"])))
        (is (re-find #"mobile-app" (:body resp)))
        (is (re-find #"/js/mobile/app.js" (:body resp))))))

  (testing "redirects to the desktop route when the mobile bundle is missing"
    (with-redefs [mobile/mobile-bundle-present? (constantly false)]
      (let [resp (mobile/render-mobile-page {:uri "/m/t/enes-jakic/receipts"
                                             :query-string "view=list"})]
        (is (= 302 (:status resp)))
        (is (= "/t/enes-jakic/receipts?view=list&mobile-fallback=1"
              (get-in resp [:headers "Location"])))
        (is (= "no-cache, no-store" (get-in resp [:headers "Cache-Control"])))
        (is (= "" (:body resp)))))))