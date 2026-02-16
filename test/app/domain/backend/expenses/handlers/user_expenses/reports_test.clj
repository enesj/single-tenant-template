(ns app.domain.backend.expenses.handlers.user-expenses.reports-test
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.reports :as reports]
    [app.domain.backend.expenses.services.user-expense-reports :as report-services]
    [cheshire.core :as json]
    [clojure.test :refer [deftest is testing]])
  (:import
    (java.time Instant)
    (java.util UUID)))

(defn- req
  [{:keys [user-id role query-params]}]
  (cond-> {:query-params (or query-params {})}
    user-id (assoc :session {:auth-session {:user {:id user-id :role role}}})))

(defn- parse-body [resp]
  (json/parse-string (:body resp) true))

(deftest reports-require-authentication
  (let [handler (reports/day-of-week-spending-handler nil)
        resp (handler {:query-params {}})]
    (is (= 401 (:status resp)))))

(deftest reports-forbidden-for-invalid-role
  (let [handler (reports/top-items-spending-handler nil)
        resp (handler (req {:user-id (UUID/randomUUID)
                            :role "guest"
                            :query-params {}}))]
    (is (= 403 (:status resp)))))

(deftest monthly-comparison-validates-month-format
  (let [handler (reports/monthly-comparison-handler nil)
        resp (handler (req {:user-id (UUID/randomUUID)
                            :role "member"
                            :query-params {:month_a "2026-13"
                                           :month_b "2026-01"}}))
        body (parse-body resp)]
    (is (= 400 (:status resp)))
    (is (= "Invalid month_a format (expected YYYY-MM)" (:error body)))))

(deftest top-items-handler-validates-manufacturer-id
  (let [handler (reports/top-items-spending-handler nil)
        resp (handler (req {:user-id (UUID/randomUUID)
                            :role "member"
                            :query-params {:manufacturer_id "not-a-uuid"}}))
        body (parse-body resp)]
    (is (= 400 (:status resp)))
    (is (= "Invalid manufacturer_id" (:error body)))))

(deftest supplier-deep-dive-requires-supplier-id
  (let [handler (reports/supplier-deep-dive-handler nil)
        resp (handler (req {:user-id (UUID/randomUUID)
                            :role "member"
                            :query-params {}}))
        body (parse-body resp)]
    (is (= 400 (:status resp)))
    (is (= "supplier_id is required" (:error body)))))

(deftest top-items-handler-parses-shared-options
  (let [captured (atom nil)
        user-id (UUID/randomUUID)
        supplier-id (UUID/randomUUID)
        payer-id (UUID/randomUUID)
        category-id (UUID/randomUUID)
        subcategory-id (UUID/randomUUID)
        expense-category-id (UUID/randomUUID)
        manufacturer-id (UUID/randomUUID)
        from "2026-01-01"
        to "2026-01-31T23:59:59Z"
        handler (reports/top-items-spending-handler nil)]
    (with-redefs [report-services/get-user-top-item-spending
                  (fn [_db passed-user-id opts]
                    (reset! captured {:user-id passed-user-id :opts opts})
                    [{:alias_label "MILK"
                      :article_canonical_name "Milk"
                      :currency "BAM"
                      :total_amount 10M
                      :line_count 1}])]
      (let [resp (handler (req {:user-id user-id
                                :role "member"
                                :query-params {:from from
                                               :to to
                                               :currency "bam"
                                               :supplier_id (str supplier-id)
                                               :payer_id (str payer-id)
                                               :category_id (str category-id)
                                               :subcategory_id (str subcategory-id)
                                               :expense_category_id (str expense-category-id)
                                               :manufacturer_id (str manufacturer-id)
                                               :limit "7"}}))
            body (parse-body resp)
            row (first (:data body))]
        (testing "response shape"
          (is (= 200 (:status resp)))
          (is (= "MILK" (:alias_label row)))
          (is (= "Milk" (:article_canonical_name row)))
          (is (= "BAM" (:currency row))))

        (testing "parsed opts sent to service"
          (is (= user-id (:user-id @captured)))
          (is (instance? Instant (get-in @captured [:opts :from])))
          (is (instance? Instant (get-in @captured [:opts :to])))
          (is (= "BAM" (get-in @captured [:opts :currency])))
          (is (= supplier-id (get-in @captured [:opts :supplier-id])))
          (is (= payer-id (get-in @captured [:opts :payer-id])))
          (is (= category-id (get-in @captured [:opts :category-id])))
          (is (= subcategory-id (get-in @captured [:opts :subcategory-id])))
          (is (= expense-category-id (get-in @captured [:opts :expense-category-id])))
          (is (= manufacturer-id (get-in @captured [:opts :manufacturer-id])))
          (is (= 7 (get-in @captured [:opts :limit]))))))))

(deftest category-allocation-handler-returns-representative-shape
  (let [handler (reports/category-allocation-handler nil)
        user-id (UUID/randomUUID)]
    (with-redefs [report-services/get-user-category-allocation
                  (fn [_db _user-id _opts]
                    [{:category_key "food"
                      :category_name "Food"
                      :currency "BAM"
                      :total_amount 12M
                      :line_count 2
                      :allocation_pct 70.5882}
                     {:category_key "uncategorized"
                      :category_name "Uncategorized"
                      :currency "BAM"
                      :total_amount 5M
                      :line_count 1
                      :allocation_pct 29.4118}])]
      (let [resp (handler (req {:user-id user-id
                                :role "member"
                                :query-params {:currency "BAM"}}))
            body (parse-body resp)
            rows (:data body)]
        (is (= 200 (:status resp)))
        (is (vector? rows))
        (is (= 2 (count rows)))
        (is (some #(= "Uncategorized" (:category_name %)) rows))))))

(deftest supplier-deep-dive-handler-returns-expected-sections
  (let [handler (reports/supplier-deep-dive-handler nil)
        user-id (UUID/randomUUID)
        supplier-id (UUID/randomUUID)]
    (with-redefs [report-services/get-user-supplier-deep-dive
                  (fn [_db _user-id _opts]
                    {:supplier-id supplier-id
                     :supplier-name "Test Supplier"
                     :summary [{:currency "BAM" :total_amount 20M :expense_count 2}]
                     :trend [{:month "2026-01" :currency "BAM" :total_amount 20M :expense_count 2}]
                     :top-aliases [{:alias_label "MILK" :currency "BAM" :total_amount 20M :line_count 2}]})]
      (let [resp (handler (req {:user-id user-id
                                :role "member"
                                :query-params {:supplier_id (str supplier-id)}}))
            body (parse-body resp)
            data (:data body)]
        (is (= 200 (:status resp)))
        (is (map? data))
        (is (vector? (:summary data)))
        (is (vector? (:trend data)))
        (is (vector? (:top-aliases data)))))))
