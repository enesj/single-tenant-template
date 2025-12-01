(ns app.shared.model-naming-test
  (:require
    [app.shared.model-naming :as model-naming]
    [clojure.test :refer [deftest is testing]]))

(deftest keyword-roundtrip-test
  (testing "db-keyword->app converts snake to kebab"
    (is (= :tenant-id (model-naming/db-keyword->app :tenant_id)))
    (is (= :email-verification-tokens
          (model-naming/db-keyword->app :email_verification_tokens)))
    (is (= :tenants/property-id
          (model-naming/db-keyword->app :tenants/property_id)))
    (is (= "tenant-id"
          (model-naming/db-keyword->app "tenant_id"))))
  (testing "app-keyword->db converts kebab to snake"
    (is (= :tenant_id (model-naming/app-keyword->db :tenant-id)))
    (is (= :email_verification_tokens
          (model-naming/app-keyword->db :email-verification-tokens)))
    (is (= :transaction_types_v2/id
          (model-naming/app-keyword->db :transaction-types-v2/id)))
    (is (= "tenant_id"
          (model-naming/app-keyword->db "tenant-id")))))

(def sample-models
  {:email_verification_tokens
   {:fields [[:id :uuid {:primary-key true}]
             [:tenant_id :uuid {:foreign-key :tenants/id}]
             [:created_at :timestamptz]
             [:last_attempted_at :timestamptz {:null true}]]
    :indexes [[:idx_email_tokens_tenant :btree {:fields [:tenant_id]}]]
    :constraints {:check [:> :tenant_id 0]}}
   :transaction_types_v2
   {:fields [[:id :uuid {:primary-key true}]
             [:parent_category_id :uuid {:foreign-key :transaction_types_v2/id}]
             [:tenant_id :uuid {:foreign-key :tenants/id}]]}})

(deftest convert-models-test
  (let [converted (model-naming/convert-models sample-models)
        token-entity (:email-verification-tokens converted)
        types-entity (:transaction-types-v2 converted)]
    (testing "entity keys converted to kebab case"
      (is (contains? converted :email-verification-tokens))
      (is (contains? converted :transaction-types-v2)))
    (testing "field definitions converted"
      (is (= [:tenant-id :uuid {:foreign-key :tenants/id}]
            (second (:fields token-entity))))
      (is (= [:created-at :timestamptz]
            (nth (:fields token-entity) 2)))
      (is (= [:parent-category-id :uuid {:foreign-key :transaction-types-v2/id}]
            (second (:fields types-entity)))))
    (testing "index field names converted"
      (is (= [:idx-email-tokens-tenant :btree {:fields [:tenant-id]}]
            (first (:indexes token-entity)))))
    (testing "constraint field references converted"
      (is (= {:check [:> :tenant-id 0]}
            (:constraints token-entity))))
    (testing "alias maps recorded"
      (is (= {:id :id
              :tenant_id :tenant-id
              :created_at :created-at
              :last_attempted_at :last-attempted-at}
            (:db/field-aliases token-entity)))
      (is (= {:id :id
              :tenant-id :tenant_id
              :created-at :created_at
              :last-attempted-at :last_attempted_at}
            (:app/field-aliases token-entity))))
    (testing "entity alias metadata available"
      (is (= :email_verification_tokens (:db/entity token-entity)))
      (is (= :email_verification_tokens
            (model-naming/app-entity->db converted :email-verification-tokens)))
      (is (= :email-verification-tokens
            (model-naming/db-entity->app converted :email_verification_tokens))))
    (testing "field alias helpers roundtrip"
      (is (= :tenant_id (model-naming/app-field->db token-entity :tenant-id)))
      (is (= :tenant-id (model-naming/db-field->app token-entity :tenant_id)))
      (is (= :transaction-types-v2/id
            (get-in types-entity [:fields 1 2 :foreign-key]))))
    (testing "attach-app-models stores converted metadata"
      (let [attached (model-naming/attach-app-models sample-models)
            meta-app (:model-naming/app-models (meta attached))]
        (is (map? attached))
        (is (= (model-naming/convert-models sample-models) meta-app))))
    (testing "entity-definition resolves both naming styles"
      (is (= token-entity (model-naming/entity-definition converted :email_verification_tokens)))
      (is (= token-entity (model-naming/entity-definition converted :email-verification-tokens))))
    (testing "app-map->db and db-map->app roundtrip"
      (let [app-map {:tenant-id "tenant" :created-at :ts}
            db-map (model-naming/app-map->db converted :email-verification-tokens app-map)]
        (is (= {:tenant_id "tenant" :created_at :ts} db-map))
        (is (= app-map (model-naming/db-map->app converted :email-verification-tokens db-map)))))
    (testing "app-filters->db converts filters"
      (is (= {:tenant_id "tenant"}
            (model-naming/app-filters->db converted :email-verification-tokens {:tenant-id "tenant"}))))
    (testing "db-rows->app converts result collections"
      (let [rows [{:tenant_id "tenant" :created_at :ts}
                  {:tenant_id "tenant-2" :created_at :ts2}]]
        (is (= [{:tenant-id "tenant" :created-at :ts}
                {:tenant-id "tenant-2" :created-at :ts2}]
              (model-naming/db-rows->app converted :email-verification-tokens rows)))))
    (testing "app-models retrieves converted representation"
      (is (= converted (model-naming/app-models sample-models)))
      (let [attached (model-naming/attach-app-models sample-models)]
        (is (= (:model-naming/app-models (meta attached))
              (model-naming/app-models attached)))))))
