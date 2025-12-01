 (ns app.admin.frontend.adapters.core-test
   (:require
     [app.admin.frontend.adapters.core :as core]
     [cljs.test :refer [deftest is testing]]
     [re-frame.db :as rf-db]))

;; Note: admin-context? is covered implicitly via adapter behavior tests

(deftest admin-token-from-storage
  (testing "admin-token returns db or localStorage value"
    (reset! rf-db/app-db {:admin/token "x"})
    (is (= "x" (core/admin-token @rf-db/app-db)))
    (reset! rf-db/app-db {})
    (.setItem js/localStorage "admin-token" "y")
    (is (= "y" (core/admin-token @rf-db/app-db)))))
