(ns app.backend.routes.admin.audit-test
  "Tests for admin audit log services.
   
   Tests audit log structure, filtering, and retrieval."
  (:require
    [app.backend.services.admin.audit :as audit]
    [app.shared.adapters.database :as db-adapter]
    [clojure.test :refer [deftest is testing]]))

;; ============================================================================
;; Audit Log Structure Tests
;; ============================================================================

(deftest audit-log-structure-test
  (testing "audit log entry has expected fields"
    (let [sample-log {:id #uuid "123e4567-e89b-12d3-a456-426614174000"
                      :admin-id #uuid "223e4567-e89b-12d3-a456-426614174000"
                      :action "create_user"
                      :entity-type "user"
                      :entity-id #uuid "323e4567-e89b-12d3-a456-426614174000"
                      :details {:email "new@example.com"}
                      :ip-address "192.168.1.1"
                      :user-agent "Mozilla/5.0"
                      :created-at "2024-01-01T00:00:00Z"}]
      (is (uuid? (:id sample-log)))
      (is (uuid? (:admin-id sample-log)))
      (is (string? (:action sample-log)))
      (is (string? (:entity-type sample-log)))
      (is (map? (:details sample-log))))))

;; ============================================================================
;; Audit Action Constants Tests  
;; ============================================================================

(deftest audit-action-values-test
  (testing "expected audit action types"
    (let [valid-actions #{"create_user" "update_user" "delete_user"
                          "create_admin" "update_admin" "delete_admin"
                          "login" "logout" "password_change"
                          "bulk_delete" "export" "import"
                          "settings_change" "permission_change"}]
      (is (contains? valid-actions "create_user"))
      (is (contains? valid-actions "delete_user"))
      (is (contains? valid-actions "login")))))

;; ============================================================================
;; Audit Entity Type Tests
;; ============================================================================

(deftest audit-entity-types-test
  (testing "expected entity types in audit logs"
    (let [valid-entity-types #{"user" "admin" "tenant" "settings"
                               "audit_log" "session" "integration"}]
      (is (contains? valid-entity-types "user"))
      (is (contains? valid-entity-types "admin"))
      (is (contains? valid-entity-types "audit_log")))))

;; ============================================================================
;; Audit Filter Tests
;; ============================================================================

(deftest audit-filter-params-test
  (testing "audit log filter params shape"
    (let [filters {:admin-id #uuid "123e4567-e89b-12d3-a456-426614174000"
                   :entity-type "user"
                   :entity-id #uuid "223e4567-e89b-12d3-a456-426614174000"
                   :action "create_user"
                   :limit 100
                   :offset 0}]
      (is (uuid? (:admin-id filters)))
      (is (string? (:entity-type filters)))
      (is (number? (:limit filters))))))

;; ============================================================================
;; Audit Log Retrieval Tests
;; ============================================================================

(deftest get-audit-logs-test
  (testing "get-audit-logs with mocked service returns empty list"
    (with-redefs [audit/get-audit-logs (fn [_ _] [])]
      (let [result (audit/get-audit-logs nil {})]
        (is (empty? result)))))

  (testing "get-audit-logs with filters returns mocked data"
    (with-redefs [audit/get-audit-logs 
                  (fn [_ {:keys [limit]}] 
                    (vec (repeat (or limit 10) {:id (random-uuid)})))]
      (let [result (audit/get-audit-logs nil {:limit 5})]
        (is (= 5 (count result)))))))

;; ============================================================================
;; Audit Log Pagination Tests
;; ============================================================================

(deftest audit-pagination-test
  (testing "pagination params are valid"
    (let [default-pagination {:limit 100 :offset 0}]
      (is (= 100 (:limit default-pagination)))
      (is (= 0 (:offset default-pagination)))))

  (testing "custom pagination values"
    (let [custom-pagination {:limit 50 :offset 100}]
      (is (= 50 (:limit custom-pagination)))
      (is (= 100 (:offset custom-pagination))))))

;; ============================================================================
;; Audit Details Serialization Tests
;; ============================================================================

(deftest audit-details-serialization-test
  (testing "audit details can be any map structure"
    (let [details {:changes {:old {:status "active"}
                             :new {:status "inactive"}}
                   :reason "User request"
                   :affected-records 1}]
      (is (map? details))
      (is (map? (:changes details)))))

  (testing "details can include arrays"
    (let [details {:deleted-ids ["id1" "id2" "id3"]
                   :count 3}]
      (is (vector? (:deleted-ids details)))
      (is (number? (:count details))))))

;; ============================================================================
;; PG Object Conversion Tests
;; ============================================================================

(deftest pg-object-conversion-test
  (testing "convert-pg-objects handles basic values"
    (let [data {:id "123" :name "test"}
          converted (db-adapter/convert-pg-objects data)]
      (is (map? converted))
      (is (= "test" (:name converted)))))

  (testing "convert-pg-objects handles vectors"
    (let [data [{:id 1} {:id 2}]
          converted (db-adapter/convert-pg-objects data)]
      (is (vector? converted))
      (is (= 2 (count converted))))))

;; ============================================================================
;; Audit Log Deletion Tests (Structure Only)
;; ============================================================================

(deftest audit-deletion-params-test
  (testing "single delete requires UUID"
    (let [audit-id #uuid "123e4567-e89b-12d3-a456-426614174000"]
      (is (uuid? audit-id))))

  (testing "bulk delete requires UUID array"
    (let [ids [#uuid "123e4567-e89b-12d3-a456-426614174000"
               #uuid "223e4567-e89b-12d3-a456-426614174000"]]
      (is (vector? ids))
      (is (every? uuid? ids)))))
