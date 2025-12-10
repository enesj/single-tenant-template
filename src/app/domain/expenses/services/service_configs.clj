(ns app.domain.expenses.services.service-configs
  "Service configuration maps for expenses domain entities."
  (:require
    [app.domain.expenses.services.services-factory :as factory]
    [app.domain.expenses.services.articles :as articles]
    [app.domain.expenses.services.price-history :as price-history]))

;; ============================================================================
;; Simple Entity Configs
;; ============================================================================

(def article-alias-config
  {:table-name "article_aliases"
   :primary-key :aa/id
   :required-fields [:supplier_id :raw_label]
   :allowed-order-by {:created-at :aa/created_at
                      :raw-label-normalized :aa/raw_label_normalized
                      :confidence :aa/confidence
                      :supplier-display-name :s/display_name
                      :article-canonical-name :a/canonical_name}
   :default-order-by :aa/created_at
   :search-fields [:aa/raw_label_normalized :s/display_name :a/canonical_name]
   :joins [[:suppliers :s] [:= :s/id :aa/supplier_id]
           [:articles :a] [:= :a/id :aa/article_id]]
   :select-fields [[:aa.*]
                   [:s/display_name :supplier_display_name]
                   [:a/canonical_name :article_canonical_name]]
   :field-transformers {:raw_label_normalized articles/normalize-alias-label}
   :has-search? true
   :has-count? true})

(def price-observation-config
  {:table-name "price_observations"
   :primary-key :po/id
   :required-fields [:article_id :supplier_id :observed_at :qty :unit_price]
   :allowed-order-by {:article-canonical-name :articles.canonical_name
                      :supplier-display-name :suppliers.display_name
                      :observed-at :po.observed_at
                      :created-at :po.created_at
                      :unit-price :po.unit_price
                      :line-total :po.line_total
                      :qty :po.qty
                      :currency :po.currency}
   :default-order-by :po.observed_at
   :search-fields [:articles.canonical_name :suppliers.display_name]
   :joins [[:articles :a] [:= :a/id :po/article_id]
           [:suppliers :s] [:= :s/id :po/supplier_id]]
   :select-fields [[:po.*]
                   [:a/canonical_name :article_canonical_name]
                   [:s/display_name :supplier_display_name]]
   :has-count? true
   :custom-create-fn (fn [db data]
                       (price-history/record-observation! db data))})

(def supplier-config
  {:table-name "suppliers"
   :primary-key :id
   :required-fields [:display_name]
   :allowed-order-by {:display-name :display_name
                      :normalized-key :normalized_key
                      :created-at :created_at
                      :updated-at :updated_at}
   :default-order-by :display_name
   :search-fields [:display_name :normalized_key]
   :field-transformers {:normalized_key (fn [name]
                                          (when name
                                            (-> name
                                                clojure.string/trim
                                                clojure.string/lower-case
                                                (clojure.string/replace #"[^a-z0-9\s-]" "")
                                                (clojure.string/replace #"\s+" "-"))))}
   :before-insert (fn [data]
                    (let [display-name (:display_name data)]
                      (assoc data
                             :normalized_key ((:field-transformers supplier-config) :normalized-key display-name)
                             :id (java.util.UUID/randomUUID))))
   :before-update (fn [id updates]
                    (if (:display_name updates)
                      (assoc updates :normalized_key
                                   ((:field-transformers supplier-config) :normalized-key (:display_name updates))
                                   :updated_at [:now])
                      updates))
   :has-search? true
   :has-count? true})

(def payer-config
  {:table-name "payers"
   :primary-key :id
   :required-fields [:display_name]
   :allowed-order-by {:display-name :display_name
                      :payer-type :payer_type
                      :created-at :created_at
                      :updated-at :updated_at}
   :default-order-by :display_name
   :search-fields [:display_name :payer_type]
   :before-insert (fn [data]
                    (assoc data :id (java.util.UUID/randomUUID)))
   :has-search? true
   :has-count? true})

(def article-config
  {:table-name "articles"
   :primary-key :id
   :required-fields [:canonical_name]
   :allowed-order-by {:canonical-name :canonical_name
                      :created-at :created_at
                      :updated-at :updated_at}
   :default-order-by :canonical_name
   :search-fields [:canonical_name :description]
   :field-transformers {:canonical_name articles/normalize-article-key}
   :before-insert (fn [data]
                    (assoc data :id (java.util.UUID/randomUUID)))
   :has-search? true
   :has-count? true})

(def expense-config
  {:table-name "expenses"
   :primary-key :id
   :required-fields [:supplier_id :payer_id :expense_date]
   :allowed-order-by {:expense-date :expense_date
                      :created-at :created_at
                      :updated-at :updated_at
                      :total-amount :total_amount}
   :default-order-by :expense_date
   :search-fields [:s/display_name :p/display_name]
   :joins [[:suppliers :s] [:= :s/id :supplier_id]
           [:payers :p] [:= :p/id :payer_id]]
   :select-fields [[:e.*]
                   [:s/display_name :supplier_display_name]
                   [:p/display_name :payer_display_name]]
   :before-insert (fn [data]
                    (assoc data :id (java.util.UUID/randomUUID)))
   :has-count? true})

(def receipt-config
  {:table-name "receipts"
   :primary-key :id
   :required-fields [:supplier_id :receipt_date :total_amount]
   :allowed-order-by {:receipt-date :receipt_date
                      :created-at :created_at
                      :total-amount :total_amount}
   :default-order-by :receipt_date
   :search-fields [:s/display_name :receipt_number]
   :joins [[:suppliers :s] [:= :s/id :supplier_id]]
   :select-fields [[:r.*]
                   [:s/display_name :supplier_display_name]]
   :before-insert (fn [data]
                    (assoc data :id (java.util.UUID/randomUUID)))
   :has-count? true})

(def price-history-config
  {:table-name "price_history"
   :primary-key :id
   :required-fields [:article_id :supplier_id :price_date :unit_price]
   :allowed-order-by {:price-date :price_date
                      :created-at :created_at
                      :unit-price :unit_price}
   :default-order-by :price_date
   :search-fields [:a/canonical_name :s/display_name]
   :joins [[:articles :a] [:= :a/id :article_id]
           [:suppliers :s] [:= :s/id :supplier_id]]
   :select-fields [[:ph.*]
                   [:a/canonical_name :article_canonical_name]
                   [:s/display_name :supplier_display_name]]
   :before-insert (fn [data]
                    (assoc data :id (java.util.UUID/randomUUID)))
   :has-count? true})

(def report-config
  {:table-name "reports"
   :primary-key :id
   :required-fields [:report_type :report_data]
   :allowed-order-by {:created-at :created_at
                      :report-type :report_type}
   :default-order-by :created_at
   :search-fields [:report_type]
   :before-insert (fn [data]
                    (assoc data :id (java.util.UUID/randomUUID)
                           :created_at [:now]))
   :has-count? true})

;; ============================================================================
;; Registry
;; ============================================================================

(def ^:private entity-configs
  {:article-alias article-alias-config
   :price-observation price-observation-config
   :supplier supplier-config
   :payer payer-config
   :article article-config
   :expense expense-config
   :receipt receipt-config
   :price-history price-history-config
   :report report-config})

(defn get-entity-config
  "Get configuration for an entity by key."
  [entity-key]
  (when-let [config (get entity-configs entity-key)]
    (factory/register-entity-service! config)))

(defn list-entity-configs
  "List all available entity configurations."
  []
  (keys entity-configs))

;; ============================================================================
;; Service Registration
;; ============================================================================

(defn register-all-entity-services!
  "Register all entity services with the factory."
  []
  (doseq [[entity-key config] entity-configs]
    (factory/register-entity-service!
      (assoc config :entity-key entity-key))))

;; Auto-register all services on require
(register-all-entity-services!)