(ns app.template.frontend.utils.date-test
  (:require
    [app.shared.date :as date]
    [cljs.test :refer-macros [deftest is testing]]))

(deftest test-format-date
  (testing "format-date handles Date objects"
    (let [date-obj (js/Date. "2024-03-15T10:30:00")]
      (is (= "2024-03-15" (date/format-date date-obj)))))

  (testing "format-date handles ISO strings"
    (is (= "2024-03-15" (date/format-date "2024-03-15T10:30:00")))
    (is (= "2024-12-25" (date/format-date "2024-12-25"))))

  (testing "format-date handles nil and invalid inputs"
    (is (nil? (date/format-date nil)))
    (is (nil? (date/format-date "")))
    (is (nil? (date/format-date "invalid-date")))))

(deftest test-format-date-for-api
  (testing "format-date-for-api formats dates for API consumption"
    (let [date-obj (js/Date. "2024-03-15T10:30:00")]
      (is (= "2024-03-15" (date/format-date-for-api date-obj))))
    (is (= "2024-12-25" (date/format-date-for-api "2024-12-25T00:00:00"))))

  (testing "format-date-for-api handles nil"
    (is (nil? (date/format-date-for-api nil)))))

(deftest test-format-date-display
  (testing "format-date-display formats dates for UI display"
    (let [date-obj (js/Date. "2024-03-15")]
      (is (re-matches #"\d{1,2}/\d{1,2}/\d{4}" (date/format-date-display date-obj)))))

  (testing "format-date-display handles various input formats"
    (is (string? (date/format-date-display "2024-03-15")))
    (is (string? (date/format-date-display "2024-03-15T10:30:00"))))

  (testing "format-date-display handles nil"
    (is (= "Select a date" (date/format-date-display nil)))))

(deftest test-parse-date
  (testing "parse-date handles various date formats"
    ;; ISO format
    (let [result (date/parse-date "2024-03-15")]
      (is (instance? js/Date result))
      (is (= 2024 (.getFullYear result)))
      (is (= 2 (.getMonth result)))                         ; March is month 2 (0-indexed)
      (is (= 15 (.getDate result))))

    ;; ISO with time
    (let [result (date/parse-date "2024-03-15T10:30:00")]
      (is (instance? js/Date result)))

    ;; US format
    (let [result (date/parse-date "03/15/2024")]
      (is (instance? js/Date result))
      (is (= 2024 (.getFullYear result)))
      (is (= 2 (.getMonth result)))
      (is (= 15 (.getDate result))))

    ;; JS Date string
    (let [result (date/parse-date "Fri Mar 15 2024")]
      (is (instance? js/Date result))))

  (testing "parse-date handles invalid inputs"
    (is (nil? (date/parse-date nil)))
    (is (nil? (date/parse-date "")))
    (is (nil? (date/parse-date "invalid-date")))
    (is (nil? (date/parse-date "13/32/2024")))))            ; Invalid date

(deftest test-is-valid-date?
  (testing "is-valid-date? validates Date objects"
    (is (true? (date/is-valid-date? (js/Date.))))
    (is (true? (date/is-valid-date? (js/Date. "2024-03-15"))))
    (is (false? (date/is-valid-date? (js/Date. "invalid")))))

  (testing "is-valid-date? validates date strings"
    (is (true? (date/is-valid-date? "2024-03-15")))
    (is (true? (date/is-valid-date? "03/15/2024")))
    (is (false? (date/is-valid-date? "invalid-date")))
    (is (false? (date/is-valid-date? nil)))
    (is (false? (date/is-valid-date? "")))))

(deftest test-add-days
  (testing "add-days adds days to a date"
    (let [base-date (js/Date. "2024-03-15")
          result (date/add-days base-date 5)]
      (is (= 20 (.getDate result))))

    (let [base-date (js/Date. "2024-03-30")
          result (date/add-days base-date 5)]
      (is (= 4 (.getDate result)))                          ; Should roll over to April
      (is (= 3 (.getMonth result)))))                       ; April is month 3

  (testing "add-days handles negative days"
    (let [base-date (js/Date. "2024-03-15")
          result (date/add-days base-date -5)]
      (is (= 10 (.getDate result)))))

  (testing "add-days handles nil"
    (is (nil? (date/add-days nil 5)))))

(deftest test-date-range
  (testing "date-range generates correct range"
    (let [start (js/Date. "2024-03-15")
          end (js/Date. "2024-03-18")
          range (date/date-range start end)]
      (is (= 4 (count range)))                              ; Should include both start and end
      (is (= 15 (.getDate (first range))))
      (is (= 18 (.getDate (last range))))))

  (testing "date-range handles same date"
    (let [date (js/Date. "2024-03-15")
          range (date/date-range date date)]
      (is (= 1 (count range)))))

  (testing "date-range handles nil"
    (is (= [] (date/date-range nil (js/Date.))))
    (is (= [] (date/date-range (js/Date.) nil)))
    (is (= [] (date/date-range nil nil)))))

(deftest test-format-date-range
  (testing "format-date-range formats date ranges"
    (let [start (js/Date. "2024-03-15")
          end (js/Date. "2024-03-20")]
      (is (re-matches #".+ - .+" (date/format-date-range start end)))))

  (testing "format-date-range handles nil values"
    (is (string? (date/format-date-range nil (js/Date.))))
    (is (string? (date/format-date-range (js/Date.) nil)))
    (is (= "Select date range" (date/format-date-range nil nil)))))

(deftest test-start-of-month
  (testing "start-of-month returns first day of month"
    (let [date (js/Date. "2024-03-15")
          result (date/start-of-month date)]
      (is (= 1 (.getDate result)))
      (is (= 2 (.getMonth result)))
      (is (= 2024 (.getFullYear result)))))

  (testing "start-of-month handles nil"
    (is (nil? (date/start-of-month nil)))))

(deftest test-end-of-month
  (testing "end-of-month returns last day of month"
    ;; March has 31 days
    (let [date (js/Date. "2024-03-15")
          result (date/end-of-month date)]
      (is (= 31 (.getDate result))))

    ;; February 2024 is a leap year, so 29 days
    (let [date (js/Date. "2024-02-15")
          result (date/end-of-month date)]
      (is (= 29 (.getDate result))))

    ;; April has 30 days
    (let [date (js/Date. "2024-04-15")
          result (date/end-of-month date)]
      (is (= 30 (.getDate result)))))

  (testing "end-of-month handles nil"
    (is (nil? (date/end-of-month nil)))))

(deftest test-days-between
  (testing "days-between calculates correct difference"
    (let [date1 (js/Date. "2024-03-15")
          date2 (js/Date. "2024-03-20")]
      (is (= 5 (date/days-between date1 date2)))
      (is (= 5 (date/days-between date2 date1))))           ; Should be absolute

    (let [date1 (js/Date. "2024-03-15")
          date2 (js/Date. "2024-03-15")]
      (is (= 0 (date/days-between date1 date2)))))

  (testing "days-between handles nil"
    (is (nil? (date/days-between nil (js/Date.))))
    (is (nil? (date/days-between (js/Date.) nil)))
    (is (nil? (date/days-between nil nil)))))
