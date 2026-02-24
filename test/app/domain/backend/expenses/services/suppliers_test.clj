(ns app.domain.backend.expenses.services.suppliers-test
  (:require
    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [clojure.string :as str]
    [clojure.test :refer [deftest is testing]]
    [next.jdbc :as jdbc])
  (:import
    [java.util UUID]))

(deftest resolve-or-create-supplier-reuses-unique-descriptor-tail-match
  (testing "short OCR supplier name reuses an existing supplier with a descriptor tail"
    (let [existing-id (UUID/randomUUID)
          created? (atom false)
          descriptor-queries (atom 0)]
      (with-redefs [jdbc/execute-one! (fn [_db _sql-params _opts] nil)
                    jdbc/execute! (fn [_db sql-params _opts]
                                    (let [sql-lc (some-> sql-params first str str/lower-case)]
                                      (cond
                                        (str/includes? sql-lc "? like normalized_key || '-%'")
                                        []

                                        (str/includes? sql-lc "where normalized_key like ?")
                                        (do
                                          (swap! descriptor-queries inc)
                                          [{:id existing-id
                                            :display_name "Zavod za biomedicinsku dijagnostiku i ispitivanje Medicover BH"
                                            :normalized_key "zavod-za-biomedicinsku-dijagnostiku-i-ispitivanje-medicover-bh"}])

                                        :else
                                        [])))
                    suppliers/service {:create! (fn [& _]
                                                  (reset! created? true)
                                                  {:id (UUID/randomUUID)})
                                       :update! (fn [& _] nil)}]
        (let [res (suppliers/resolve-or-create-supplier-with-places!
                    :db
                    "Zavod za biomedicinsku dijagnostiku"
                    {:places-cfg {}})]
          (is (= :db (:source res)))
          (is (= existing-id (get-in res [:supplier :id])))
          (is (= 1 @descriptor-queries))
          (is (false? @created?)))))))

(deftest resolve-or-create-supplier-does-not-auto-link-ambiguous-descriptor-tail
  (testing "ambiguous descriptor-tail candidates fall back to creating supplier"
    (let [created-id (UUID/randomUUID)
          created-display-name (atom nil)]
      (with-redefs [jdbc/execute-one! (fn [_db _sql-params _opts] nil)
                    jdbc/execute! (fn [_db sql-params _opts]
                                    (let [sql-lc (some-> sql-params first str str/lower-case)]
                                      (cond
                                        (str/includes? sql-lc "? like normalized_key || '-%'")
                                        []

                                        (str/includes? sql-lc "where normalized_key like ?")
                                        [{:id (UUID/randomUUID)
                                          :display_name "Zavod za biomedicinsku dijagnostiku i ispitivanje Medicover BH"
                                          :normalized_key "zavod-za-biomedicinsku-dijagnostiku-i-ispitivanje-medicover-bh"}
                                         {:id (UUID/randomUUID)
                                          :display_name "Zavod za biomedicinsku dijagnostiku i laboratorijska ispitivanja"
                                          :normalized_key "zavod-za-biomedicinsku-dijagnostiku-i-laboratorijska-ispitivanja"}]

                                        :else
                                        [])))
                    suppliers/service {:create! (fn [_db {:keys [display_name]}]
                                                  (reset! created-display-name display_name)
                                                  {:id created-id
                                                   :display_name display_name
                                                   :normalized_key (suppliers/normalize-supplier-key display_name)})
                                       :update! (fn [& _] nil)}]
        (let [res (suppliers/resolve-or-create-supplier-with-places!
                    :db
                    "Zavod za biomedicinsku dijagnostiku"
                    {:places-cfg {}})]
          (is (= :ocr-fallback (:source res)))
          (is (= created-id (get-in res [:supplier :id])))
          (is (= "Zavod za biomedicinsku dijagnostiku" @created-display-name)))))))
