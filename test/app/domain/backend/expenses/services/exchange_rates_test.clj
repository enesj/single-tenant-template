(ns app.domain.backend.expenses.services.exchange-rates-test
  (:require
    [app.backend.fixtures :as fixtures]
    [app.domain.backend.expenses.services.exchange-rates :as exchange-rates]
    [clj-http.client :as http]
    [clojure.test :refer [deftest is use-fixtures]]
    [next.jdbc :as jdbc])
  (:import
    [java.time LocalDate]
    [java.util UUID]))

(use-fixtures :each fixtures/with-transaction-rollback)

(defn- insert-rate!
  [db {:keys [currency-code rate-date rate is-fallback]}]
  (jdbc/execute-one!
    db
    ["insert into daily_exchange_rates (id, currency_code, rate_date, rate, fetched_at, is_fallback, created_at) values (?, ?, ?, ?, now(), ?, now())"
     (UUID/randomUUID) currency-code rate-date rate (boolean is-fallback)]))

(deftest ensure-daily-rates-uses-existing-cache
  (when-let [db fixtures/*test-db*]
    (let [today (LocalDate/now)
          _ (insert-rate! db {:currency-code "EUR"
                              :rate-date today
                              :rate 1.95583M
                              :is-fallback false})
          result (with-redefs [exchange-rates/fetch-and-cache-daily-rates!
                               (fn [& _]
                                 (throw (ex-info "should not fetch" {})))]
                   (exchange-rates/ensure-daily-rates! db {}))]
      (is (= :ok (:status result)))
      (is (= 1 (count (:rates result))))
      (is (= 1.95583M (:rate (first (:rates result))))))))

(deftest fetch-and-cache-daily-rates-stores-rates-from-html-fetch
  (when-let [db fixtures/*test-db*]
    (let [today (LocalDate/now)
          html-body (str "<div class=\"currcircle\">EUR</div><td class=\"middle-column\">1.955830</td>"
                      "<div class=\"currcircle\">USD</div><td class=\"middle-column\">1.804210</td>")
          result (with-redefs [http/get (fn [_url _opts]
                                          {:status 200
                                           :body html-body})]
                   (exchange-rates/fetch-and-cache-daily-rates! db {} today))
          eur-rate (exchange-rates/get-rate-for-currency db "EUR" today)]
      (is (= :ok (:status result)))
      (is (= 2 (count (:rates result))))
      (is (= 1.955830M (:rate eur-rate))))))
