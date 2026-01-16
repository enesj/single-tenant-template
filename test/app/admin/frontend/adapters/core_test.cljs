 (ns app.admin.frontend.adapters.core-test
   (:require
     [app.admin.frontend.adapters.core :as core]
     [cljs.test :refer [deftest is testing]]
     [re-frame.db :as rf-db]))

;; Note: admin-context? is covered implicitly via adapter behavior tests

(deftest admin-token-from-db
  (testing "admin-token returns db value when present"
    (reset! rf-db/app-db {:admin/token "x"})
    (is (= "x" (core/admin-token @rf-db/app-db))))

  (testing "admin-token is nil when missing"
    (reset! rf-db/app-db {})
    (is (nil? (core/admin-token @rf-db/app-db)))))

(deftest admin-context-detection
  (testing "admin-context? keys off the active route"
    (reset! rf-db/app-db {:current-route {:data {:name :admin-dashboard}}})
    (is (true? (core/admin-context? @rf-db/app-db)))
    (reset! rf-db/app-db {:admin/token "x"
                          :current-route {:data {:name :receipts}}})
    (is (false? (core/admin-context? @rf-db/app-db)))))
