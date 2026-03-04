(ns app.domain.backend.expenses.services.countries-test
  (:require
    [app.backend.fixtures :as fixtures]
    [app.domain.backend.expenses.services.countries :as countries]
    [clojure.test :refer [deftest is testing use-fixtures]]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.util UUID]))

(use-fixtures :each fixtures/with-transaction-rollback)

(def ^:private ^:const alphabet
  "ABCDEFGHIJKLMNOPQRSTUVWXYZ")

(defonce ^:private code-counter
  ;; Start near the end of the 2-letter space so we avoid common real country codes.
  (atom 600))

(defn- n->code
  "Deterministic base-26 two-letter code (AA..ZZ)."
  [n]
  (let [n (mod (long n) (* 26 26))
        a (quot n 26)
        b (mod n 26)]
    (str (.charAt alphabet a)
      (.charAt alphabet b))))

(defn- code-exists?
  [db code]
  (let [row (jdbc/execute-one!
              db
              ["select 1 as ok from countries where code = ? limit 1" code]
              {:builder-fn rs/as-unqualified-lower-maps})]
    (boolean row)))

(defn- unique-code!
  "Returns a 2-char code that does not exist in the current DB snapshot.

  This keeps tests deterministic and avoids collisions if `countries.code` is unique."
  [db]
  (loop [attempt 0]
    (when (> attempt 1000)
      (throw (ex-info "Could not find a unique country code for test" {:status 500})))
    (let [n (swap! code-counter inc)
          code (n->code n)]
      (if (code-exists? db code)
        (recur (inc attempt))
        code))))

(defn- unique-country
  [prefix]
  (str prefix "-" (UUID/randomUUID)))

(defn- ex-status
  [^clojure.lang.ExceptionInfo e]
  (:status (ex-data e)))

(deftest create-and-get-country-computes-id
  (testing "create + get returns row and includes computed :id == :country"
    (let [db fixtures/*test-db*
          country (unique-country "Testland")
          code (unique-code! db)
          created (countries/create-country! db {:country country :code code})
          fetched (countries/get-country db country)]
      (is (= country (:country created)))
      (is (= code (:code created)))
      (is (= country (:id created)) ":id should be computed from :country")

      (is (= country (:country fetched)))
      (is (= code (:code fetched)))
      (is (= country (:id fetched)) ":id should be computed from :country"))))

(deftest list-countries-includes-id
  (testing "list returns rows with :id present"
    (let [db fixtures/*test-db*
          c1 (unique-country "Listland")
          c2 (unique-country "Listland")
          code1 (unique-code! db)
          code2 (unique-code! db)
          _ (countries/create-country! db {:country c1 :code code1})
          _ (countries/create-country! db {:country c2 :code code2})
          rows (countries/list-countries db {:limit 500 :offset 0 :order-by :country :order-dir :asc})
          ids (set (map :id rows))]
      (is (contains? ids c1) "Created country should be present in list with :id")
      (is (contains? ids c2) "Created country should be present in list with :id")
      (is (every? some? (map :id rows)) "Every row should include computed :id"))))

(deftest count-countries-returns-number-and-increments
  (testing "count returns a number and increments after create"
    (let [db fixtures/*test-db*
          before (countries/count-countries db {})
          country (unique-country "Countland")
          code (unique-code! db)
          _ (countries/create-country! db {:country country :code code})
          after (countries/count-countries db {})]
      (is (number? before))
      (is (= (inc before) after)))))

(deftest update-country-supports-rename-and-code-change
  (testing "update supports changing :code and/or :country (rename) and returns updated :id"
    (let [db fixtures/*test-db*
          old-country (unique-country "Oldname")
          old-code (unique-code! db)
          _created (countries/create-country! db {:country old-country :code old-code})

          new-country (unique-country "Newname")
          new-code (unique-code! db)
          updated (countries/update-country! db old-country {:country new-country
                                                             :code new-code})
          fetched-new (countries/get-country db new-country)
          fetched-old (countries/get-country db old-country)]
      (is (= new-country (:country updated)))
      (is (= new-code (:code updated)))
      (is (= new-country (:id updated)) ":id should be recomputed after rename")

      (is (= new-country (:country fetched-new)))
      (is (= new-code (:code fetched-new)))
      (is (= new-country (:id fetched-new)))

      (is (nil? fetched-old) "Old PK should no longer resolve after rename"))))

(deftest delete-country-boolean
  (testing "delete returns true for existing, false for missing"
    (let [db fixtures/*test-db*
          country (unique-country "Deletia")
          code (unique-code! db)
          _ (countries/create-country! db {:country country :code code})]
      (is (true? (countries/delete-country! db country)) "Existing delete should return true")
      (is (false? (countries/delete-country! db country)) "Second delete should return false")
      (is (false? (countries/delete-country! db (unique-country "Missing"))) "Missing delete should return false"))))

(deftest validation-errors-are-400
  (testing "blank country throws ex-info with {:status 400}"
    (let [db fixtures/*test-db*]
      (try
        (countries/create-country! db {:country "   " :code "AB"})
        (is false "Expected create-country! to throw")
        (catch clojure.lang.ExceptionInfo e
          (is (= 400 (ex-status e)))))))

  (testing "invalid code length throws ex-info with {:status 400}"
    (let [db fixtures/*test-db*
          country (unique-country "Badcode")]
      (try
        (countries/create-country! db {:country country :code "ABC"})
        (is false "Expected create-country! to throw")
        (catch clojure.lang.ExceptionInfo e
          (is (= 400 (ex-status e))))))))
