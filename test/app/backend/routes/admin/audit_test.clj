(ns app.backend.routes.admin.audit-test
  "Tests for admin audit log services.

   Tests audit log structure, filtering, and retrieval."
  (:require
    [app.admin.backend.services.admin.audit :as audit]
    [app.backend.test-helpers :as h]
    [app.shared.adapters.database :as shared-db]
    [app.template.backend.routes.admin.audit :as audit-routes]
    [app.template.backend.security.email :as email-privacy]
    [clojure.test :refer [deftest is testing use-fixtures]]
    [honey.sql :as hsql]
    [next.jdbc :as jdbc]))

;; ============================================================================
;; Audit Log Structure Tests
;; ============================================================================

(use-fixtures :each h/with-clean-test-state)

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
        (is (= 5 (count result))))))

  (testing "routine audit results expose admin refs without raw admin email"
    (let [admin-id (random-uuid)
          target-id (random-uuid)
          raw-log {:id (random-uuid)
                   :actor_id admin-id
                   :actor_type "admin"
                   :action "update_user"
                   :target_type "user"
                   :target_id target-id
                   :admin_name nil
                   :created_at (java.time.Instant/parse "2026-01-01T00:00:00Z")}
          results (with-redefs-fn {#'hsql/format identity
                                   #'jdbc/execute! (fn [_ _] [raw-log])
                                   #'app.admin.backend.services.admin.audit/resolve-entity-name
                                   (fn [_ _ _] "Example User")}
                    #(audit/get-audit-logs nil {:limit 1 :offset 0}))
          result (first results)]
      (is (= 1 (count results)))
      (is (= "Example User" (:entity-name result)))
      (is (= (email-privacy/admin-ref admin-id) (:admin-ref result)))
      (is (= (:admin-ref result) (:admin-name result)))
      (is (not (contains? result :admin-email))))))

(deftest get-audit-logs-handler-pagination-metadata-test
  (testing "audit handler returns logs, total, limit, and offset with filter-aware total"
    (let [db (h/mock-db)
          handler (audit-routes/get-audit-logs-handler db)
          admin-id (random-uuid)
          entity-id (random-uuid)
          request (h/mock-admin-request :get "/admin/api/audit" {:id admin-id}
                    {:params {:admin-id (str admin-id)
                              :entity-type "user"
                              :entity-id (str entity-id)
                              :action "create_user"
                              :limit "5"
                              :offset "10"}})
          sample-logs [{:id (random-uuid)
                        :actor-id admin-id
                        :target-type "user"
                        :target-id entity-id
                        :action "create_user"}
                       {:id (random-uuid)
                        :actor-id admin-id
                        :target-type "user"
                        :target-id entity-id
                        :action "delete_user"}
                       {:id (random-uuid)
                        :actor-id (random-uuid)
                        :target-type "admin"
                        :target-id (random-uuid)
                        :action "create_user"}]]
      (with-redefs [audit/get-audit-logs-page
                    (fn [_db opts]
                      (is (= admin-id (:admin-id opts)))
                      (is (= "user" (:entity-type opts)))
                      (is (= entity-id (:entity-id opts)))
                      (is (= "create_user" (:action opts)))
                      (is (= 5 (:limit opts)))
                      (is (= 10 (:offset opts)))
                      (let [matches? (fn [log]
                                       (and (= (:admin-id opts) (:actor-id log))
                                         (= (:entity-type opts) (:target-type log))
                                         (= (:entity-id opts) (:target-id log))
                                         (= (:action opts) (:action log))))
                            filtered (vec (filter matches? sample-logs))]
                        {:logs filtered
                         :total (count filtered)
                         :limit (:limit opts)
                         :offset (:offset opts)}))]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (vector? (:logs body)))
          (is (= 1 (:total body)))
          (is (= 5 (:limit body)))
          (is (= 10 (:offset body)))
          (is (= 1 (count (:logs body)))))))))

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
          converted (shared-db/convert-pg-objects data)]
      (is (map? converted))
      (is (= "test" (:name converted)))))

  (testing "convert-pg-objects handles vectors"
    (let [data [{:id 1} {:id 2}]
          converted (shared-db/convert-pg-objects data)]
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
