(ns app.shared.adapters.database-test
  (:require
    [app.shared.adapters.database :as db-adapter]
    [clojure.test :refer [deftest is testing]]))

(deftest convert-app-keys->camel-keys-test
  (testing "Converts kebab keywords to camelCase"
    (is (= {:firstName "Ada" :lastName "Lovelace"}
          (db-adapter/convert-app-keys->camel-keys
            {:first-name "Ada" :last-name "Lovelace"}))))
  (testing "Handles nested structures and namespaces"
    (is (= {:id "1" :profile {:createdAt "2025-01-01"}}
          (db-adapter/convert-app-keys->camel-keys
            {:user/id "1" :profile {:created_at "2025-01-01"}}))))
  (testing "Leaves non keyword keys untouched"
    (is (= {"rawKey" 1 :camelKey 2}
          (db-adapter/convert-app-keys->camel-keys
            {"rawKey" 1 :camel-key 2})))))
