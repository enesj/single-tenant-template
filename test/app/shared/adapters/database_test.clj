(ns app.shared.adapters.database-test
  (:require
    [app.shared.adapters.database :as db]
    [app.shared.adapters.normalization :as norm]
    [clojure.test :refer [deftest is testing]])
  (:import
    (org.postgresql.util PGobject)))

(deftest convert-app-keys->camel-keys-test
  (testing "Converts kebab keywords to camelCase"
    (is (= {:firstName "Ada" :lastName "Lovelace"}
          (norm/convert-app-keys->camel-keys
            {:first-name "Ada" :last-name "Lovelace"}))))
  (testing "Handles nested structures and namespaces"
    (is (= {:id "1" :profile {:createdAt "2025-01-01"}}
          (norm/convert-app-keys->camel-keys
            {:user/id "1" :profile {:created_at "2025-01-01"}}))))
  (testing "Leaves non keyword keys untouched"
    (is (= {"rawKey" 1 :camelKey 2}
          (norm/convert-app-keys->camel-keys
            {"rawKey" 1 :camel-key 2})))))

(deftest convert-pg-objects-test
  (testing "Converts PGobject json/jsonb values to data"
    (let [pg (doto (PGobject.)
               (.setType "jsonb")
               (.setValue "{\"a\":1,\"b\":[2,3]}"))]
      (is (= {:payload {:a 1 :b [2 3]}}
             (db/convert-pg-objects {:payload pg}))))
    (let [pg (doto (PGobject.)
               (.setType "json")
               (.setValue "{\"ok\":true}"))]
      (is (= {:payload {:ok true}}
             (db/convert-pg-objects {:payload pg}))))))

(deftest to-app-test
  (testing "Normalizes snake_case DB keys to kebab-case"
    (is (= {:first-name "Ada" :last-name "Lovelace"}
           (db/to-app {:first_name "Ada" :last_name "Lovelace"}))))
  (testing "Works on collections"
    (is (= [{:first-name "Ada"}]
           (db/to-app [{:first_name "Ada"}])))))
