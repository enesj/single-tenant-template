(ns app.domain.backend.expenses.services.dashboard-test
  (:require
    [app.domain.backend.expenses.services.article-aliases :as article-aliases]
    [app.domain.backend.expenses.services.dashboard :as dashboard]
    [clojure.test :refer [deftest is]]))

(deftest get-unmapped-alias-count-delegates-tenant-scope-to-alias-service
  (let [tenant-id (java.util.UUID/randomUUID)
        seen-opts (atom nil)]
    (with-redefs [article-aliases/count-unmapped-aliases (fn [_db opts]
                                                           (reset! seen-opts opts)
                                                           17)]
      (is (= 17 (dashboard/get-unmapped-alias-count :mock-db tenant-id)))
      (is (= {:tenant-id tenant-id} @seen-opts)))))

(deftest get-dashboard-data-includes-tenant-scoped-unmapped-alias-count-for-power-users
  (let [tenant-id (java.util.UUID/randomUUID)]
    (with-redefs [dashboard/get-period-summary-30d (fn [_db _tenant-id] {:expense_count 1})
                  dashboard/get-period-summary-6m (fn [_db _tenant-id] {:expense_count 2})
                  dashboard/get-monthly-trend (fn [_db _tenant-id] [])
                  dashboard/get-top-suppliers (fn [_db _tenant-id] [])
                  dashboard/get-top-stores (fn [_db _tenant-id] [])
                  dashboard/get-top-articles (fn [_db _tenant-id] [])
                  dashboard/get-price-changes (fn [_db _tenant-id] [])
                  dashboard/get-category-breakdown (fn [_db _tenant-id] [])
                  dashboard/get-biggest-expense (fn [_db _tenant-id] nil)
                  dashboard/get-spending-averages (fn [_db _tenant-id] nil)
                  dashboard/get-team-overview (fn [_db _tenant-id] nil)
                  dashboard/get-unmapped-alias-count (fn [_db seen-tenant-id]
                                                       (is (= tenant-id seen-tenant-id))
                                                       11)]
      (let [data (dashboard/get-dashboard-data :mock-db tenant-id true)]
        (is (= 11 (:unmapped-alias-count data)))))))