(ns app.template.frontend.filter-date-range-picker-test
  (:require
    [app.template.frontend.components.filter.date-range-picker :as date-range-picker]
    [app.template.frontend.components.filter.helpers :as filter-helpers]
    [cljs.test :refer [deftest is testing]]))

(defn- day-key
  [value]
  (filter-helpers/local-day-key value))

(deftest next-filter-value-test
  (let [today (js/Date. 2026 2 20 10 15 0)
        march-10 (js/Date. 2026 2 10 12 0 0)
        march-15 (js/Date. 2026 2 15 12 0 0)
        march-05 (js/Date. 2026 2 5 12 0 0)
        march-25 (js/Date. 2026 2 25 12 0 0)]
    (testing "first click creates a partial range through today"
      (let [result (date-range-picker/next-filter-value {:current-filter nil
                                                         :clicked-day march-10
                                                         :today today})]
        (is (= :partial (:selection-state result)))
        (is (= "2026-03-10" (day-key (:from result))))
        (is (= "2026-03-20" (day-key (:to result))))
        (is (= "2026-03-10" (day-key (:anchor result))))))

    (testing "clicking the same anchor again clears a partial range"
      (let [partial (date-range-picker/partial-filter-value march-10 today)]
        (is (nil? (date-range-picker/next-filter-value {:current-filter partial
                                                        :clicked-day march-10
                                                        :today today})))))

    (testing "second click completes the range and preserves ascending order"
      (let [partial (date-range-picker/partial-filter-value march-10 today)
            result (date-range-picker/next-filter-value {:current-filter partial
                                                         :clicked-day march-15
                                                         :today today})]
        (is (= :complete (:selection-state result)))
        (is (= "2026-03-10" (day-key (:from result))))
        (is (= "2026-03-15" (day-key (:to result))))))

    (testing "second click before the anchor reorders the completed range"
      (let [partial (date-range-picker/partial-filter-value march-10 today)
            result (date-range-picker/next-filter-value {:current-filter partial
                                                         :clicked-day march-05
                                                         :today today})]
        (is (= :complete (:selection-state result)))
        (is (= "2026-03-05" (day-key (:from result))))
        (is (= "2026-03-10" (day-key (:to result))))))

    (testing "clicking inside a completed range clears it"
      (let [complete {:from (filter-helpers/local-start-of-day march-10)
                      :to (filter-helpers/local-end-of-day march-15)
                      :selection-state :complete}]
        (is (nil? (date-range-picker/next-filter-value {:current-filter complete
                                                        :clicked-day march-15
                                                        :today today})))
        (is (nil? (date-range-picker/next-filter-value {:current-filter complete
                                                        :clicked-day march-10
                                                        :today today})))))

    (testing "clicking outside a completed range starts a new partial range"
      (let [complete {:from (filter-helpers/local-start-of-day march-10)
                      :to (filter-helpers/local-end-of-day march-15)
                      :selection-state :complete}
            result (date-range-picker/next-filter-value {:current-filter complete
                                                         :clicked-day march-05
                                                         :today today})]
        (is (= :partial (:selection-state result)))
        (is (= "2026-03-05" (day-key (:from result))))
        (is (= "2026-03-20" (day-key (:to result))))))

    (testing "future dates are ignored"
      (let [result (date-range-picker/next-filter-value {:current-filter nil
                                                         :clicked-day march-25
                                                         :today today})]
        (is (nil? result))))))

(deftest selected-picker-value-test
  (let [partial-filter {:from (filter-helpers/local-start-of-day (js/Date. 2026 2 3 0 0 0))
                        :to (filter-helpers/local-end-of-day (js/Date. 2026 2 18 0 0 0))
                        :anchor (filter-helpers/local-start-of-day (js/Date. 2026 2 3 0 0 0))
                        :selection-state :partial}
        complete-filter {:from (filter-helpers/local-start-of-day (js/Date. 2026 2 3 0 0 0))
                         :to (filter-helpers/local-end-of-day (js/Date. 2026 2 18 0 0 0))
                         :selection-state :complete}]
    (testing "partial selections expose only the anchor day to DayPicker"
      (is (= "2026-03-03"
            (day-key (date-range-picker/selected-picker-value partial-filter)))))

    (testing "completed selections normalize both endpoints to local day starts for rendering"
      (let [^js selected-range (date-range-picker/selected-picker-value complete-filter)
            from-date (.-from selected-range)
            to-date (.-to selected-range)]
        (is (= "2026-03-03" (day-key from-date)))
        (is (= "2026-03-18" (day-key to-date)))
        (is (= 0 (.getHours to-date)))))))

(deftest highlighted-days-test
  (let [items [{:id 1 :created_at (js/Date. 2026 2 10 9 30 0) :status "open" :category "A"}
               {:id 2 :created_at (js/Date. 2026 2 12 10 45 0) :status "closed" :category "A"}
               {:id 3 :created_at (js/Date. 2026 2 12 14 0 0) :status "open" :category "A"}
               {:id 4 :created_at (js/Date. 2026 2 18 8 15 0) :status "open" :category "B"}]
        active-filters {:status [{:value "open" :label "Open"}]
                        :category [{:value "A" :label "A"}]
                        :created_at {:from (filter-helpers/local-start-of-day (js/Date. 2026 2 12 0 0 0))
                                     :to (filter-helpers/local-end-of-day (js/Date. 2026 2 12 0 0 0))
                                     :selection-state :complete}}
        result (date-range-picker/local-highlighted-days {:items items
                                                          :active-filters active-filters
                                                          :field-id :created_at})]
    (testing "highlighting applies all non-date filters but ignores the current date filter"
      (is (= ["2026-03-10" "2026-03-12"]
            (sort (map day-key result)))))

    (testing "multiple matching rows on the same day produce one highlighted day"
      (is (= 2 (count result))))))
