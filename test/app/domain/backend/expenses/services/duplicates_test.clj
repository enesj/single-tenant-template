(ns app.domain.backend.expenses.services.duplicates-test
  "Tests for duplicate detection service."
  (:require
    [app.domain.backend.expenses.services.duplicates :as duplicates]
    [clojure.test :refer [deftest is testing]]
    [next.jdbc :as jdbc])
  (:import
    [java.util UUID]))

;; ============================================================================
;; Prefix Detection
;; ============================================================================

(deftest detect-prefix-duplicates-groups-by-shared-prefix-test
  (testing "entities sharing the same 2-word prefix are grouped together"
    (let [id-a (UUID/randomUUID)
          id-b (UUID/randomUUID)
          id-c (UUID/randomUUID)
          rows [{:id id-a :display_name "Coca Cola" :normalized_key "coca-cola" :created_at #inst "2024-01-01"}
                {:id id-b :display_name "Coca Cola 330ml" :normalized_key "coca-cola-330ml" :created_at #inst "2024-01-02"}
                {:id id-c :display_name "Pepsi" :normalized_key "pepsi" :created_at #inst "2024-01-03"}]]
      (with-redefs [jdbc/execute! (fn [_db _sql _opts] rows)]
        (let [clusters (duplicates/detect-prefix-duplicates :db :suppliers {:prefix-words 2})]
          (is (= 1 (count clusters)) "Only one cluster expected (coca-cola prefix)")
          (is (= 2 (:count (first clusters))))
          (is (= #{id-a id-b}
                (set (map :id (:members (first clusters)))))))))))

(deftest detect-prefix-duplicates-no-duplicates-test
  (testing "entities with unique prefixes produce no clusters"
    (let [rows [{:id (UUID/randomUUID) :display_name "A" :normalized_key "alpha-beta" :created_at #inst "2024-01-01"}
                {:id (UUID/randomUUID) :display_name "B" :normalized_key "gamma-delta" :created_at #inst "2024-01-02"}]]
      (with-redefs [jdbc/execute! (fn [_db _sql _opts] rows)]
        (let [clusters (duplicates/detect-prefix-duplicates :db :suppliers {:prefix-words 2})]
          (is (empty? clusters)))))))

(deftest detect-prefix-duplicates-single-word-keys-test
  (testing "single-word keys with prefix-words=1 are grouped"
    (let [id-a (UUID/randomUUID)
          id-b (UUID/randomUUID)
          rows [{:id id-a :display_name "Bingo" :normalized_key "bingo" :created_at #inst "2024-01-01"}
                {:id id-b :display_name "Bingo Store" :normalized_key "bingo-store" :created_at #inst "2024-01-02"}]]
      (with-redefs [jdbc/execute! (fn [_db _sql _opts] rows)]
        (let [clusters (duplicates/detect-prefix-duplicates :db :suppliers {:prefix-words 1})]
          (is (= 1 (count clusters)))
          (is (= #{id-a id-b}
                (set (map :id (:members (first clusters)))))))))))

;; ============================================================================
;; Usage Count Enrichment
;; ============================================================================

(deftest enrich-with-usage-counts-adds-counts-test
  (testing "usage counts are summed across FK tables"
    (let [id-a (UUID/randomUUID)
          id-b (UUID/randomUUID)
          clusters [{:members [{:id id-a} {:id id-b}] :count 2}]
          call-count (atom 0)]
      (with-redefs [jdbc/execute! (fn [_db _sql-params _opts]
                                    (swap! call-count inc)
                                    ;; Return different counts for different FK tables
                                    [{:entity_id id-a :cnt 5}
                                     {:entity_id id-b :cnt 2}])]
        (let [enriched (duplicates/enrich-with-usage-counts :db :suppliers clusters)
              members (:members (first enriched))
              count-a (:usage-count (first (filter #(= id-a (:id %)) members)))
              count-b (:usage-count (first (filter #(= id-b (:id %)) members)))]
          ;; Suppliers have 5 FK tables, so each entity gets 5 * its per-table count
          (is (pos? count-a))
          (is (pos? count-b))
          (is (> count-a count-b)))))))

(deftest enrich-with-usage-counts-empty-clusters-test
  (testing "empty clusters pass through unchanged"
    (let [enriched (duplicates/enrich-with-usage-counts :db :suppliers [])]
      (is (empty? enriched)))))

;; ============================================================================
;; Invalid Entity Type
;; ============================================================================

;; ============================================================================
;; Cluster ID and Ignore Filtering
;; ============================================================================

(deftest cluster-id-deterministic-order-insensitive-test
  (testing "same members in different order produce the same cluster-id"
    (let [id-a (UUID/randomUUID)
          id-b (UUID/randomUUID)
          id-c (UUID/randomUUID)
          cluster-id-a (duplicates/cluster-id :suppliers [{:id id-a} {:id id-b} {:id id-c}])
          cluster-id-b (duplicates/cluster-id :suppliers [{:id id-c} {:id id-a} {:id id-b}])]
      (is (string? cluster-id-a))
      (is (= cluster-id-a cluster-id-b)))))

(deftest cluster-id-changes-when-members-change-test
  (testing "different member sets produce different cluster IDs"
    (let [id-a (UUID/randomUUID)
          id-b (UUID/randomUUID)
          id-c (UUID/randomUUID)
          cluster-id-a (duplicates/cluster-id :suppliers [id-a id-b])
          cluster-id-b (duplicates/cluster-id :suppliers [id-a id-c])]
      (is (not= cluster-id-a cluster-id-b)))))

(deftest filter-ignored-clusters-removes-only-matching-clusters-test
  (testing "only clusters present in ignored-id set are removed"
    (let [clusters [{:cluster-id "cid-1" :members [{:id (UUID/randomUUID)}]}
                    {:cluster-id "cid-2" :members [{:id (UUID/randomUUID)}]}
                    {:cluster-id "cid-3" :members [{:id (UUID/randomUUID)}]}]
          filtered (duplicates/filter-ignored-clusters #{"cid-2"} clusters)]
      (is (= 2 (count filtered)))
      (is (= #{"cid-1" "cid-3"}
            (set (map :cluster-id filtered)))))))

(deftest filter-ignored-clusters-supports-snake-case-key-test
  (testing "filtering also supports :cluster_id maps"
    (let [clusters [{:cluster_id "cid-1" :members []}
                    {:cluster_id "cid-2" :members []}]
          filtered (duplicates/filter-ignored-clusters #{"cid-2"} clusters)]
      (is (= [{:cluster_id "cid-1" :members []}] filtered)))))

(deftest detect-duplicates-invalid-entity-type-throws-test
  (testing "unknown entity type throws ex-info"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Unknown entity type"
          (duplicates/detect-duplicates :db :widgets :prefix {})))))

(deftest detect-duplicates-invalid-strategy-throws-test
  (testing "unknown strategy throws ex-info"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Unknown strategy"
          (duplicates/detect-duplicates :db :suppliers :magic {})))))
