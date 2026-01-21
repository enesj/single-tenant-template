(ns app.domain.backend.expenses.services.service-configs
  "Service configuration maps for expenses domain entities."
  (:require
    [app.domain.backend.expenses.services.articles :as articles]
    [app.domain.backend.expenses.services.price-history :as price-history]
    [app.domain.backend.expenses.services.services-factory :as factory]
    [clojure.string :as str])
  (:import
    [java.util UUID]))

;; ============================================================================
;; Shared Normalization Functions
;; ============================================================================

(defn normalize-supplier-key
  "Normalize supplier name to lowercase, replace spaces with hyphens, remove special chars.
   Used for deduplication and fuzzy matching."
  [name]
  (when name
    (-> name
      str/trim
      str/lower-case
      (str/replace #"[^a-z0-9\s-]" "")
      (str/replace #"\s+" "-"))))

;; ============================================================================
;; Simple Entity Configs
;; ============================================================================

(def article-alias-config
  {:table-name "article_aliases"
   :table-alias :aa
   :primary-key :aa/id
   :required-fields [:supplier_id :raw_label :raw_label_normalized]
   :allowed-order-by {:created-at :aa/created_at
          :raw-label :aa/raw_label
          :raw-label-normalized :aa/raw_label_normalized
          :supplier-display-name :s/display_name
          :article-canonical-name :a/canonical_name}
   :default-order-by :aa/created_at
   :search-fields [:aa/raw_label :aa/raw_label_normalized :s/display_name :a/canonical_name]
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
   :table-alias :po
   :primary-key :po/id
   :required-fields [:article_id :supplier_id :observed_at :qty :unit_price]
   :allowed-order-by {:article-canonical-name :a/canonical_name
                      :supplier-display-name :s/display_name
                      :observed-at :po/observed_at
                      :created-at :po/created_at
                      :unit-price :po/unit_price
                      :line-total :po/line_total
                      :qty :po/qty
                      :currency :po/currency}
   :default-order-by :po/observed_at
   :search-fields [:a/canonical_name :s/display_name]
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
   :field-transformers {:normalized_key normalize-supplier-key}
   :before-insert (fn [data]
                    (let [display-name (:display_name data)]
                      (assoc data
                        :normalized_key (normalize-supplier-key display-name)
                        :id (UUID/randomUUID))))
   :before-update (fn [_id updates]
                    (if (:display_name updates)
                      (assoc updates
                        :normalized_key (normalize-supplier-key (:display_name updates))
                        :updated_at [:now])
                      (assoc updates :updated_at [:now])))
   :has-search? true
   :has-count? true})

(def payer-config
  {:table-name "payers"
   :table-alias :p
   :primary-key :id
   :required-fields [:payer_type_id :label]
   :allowed-order-by {:label :p/label
                      :payer-type :pt/label
                      :created-at :p/created_at
                      :updated-at :p/updated_at}
   :default-order-by :p/label
   :search-fields [:p/label]
   :joins [[:payer_types :pt] [:= :pt/id :p/payer_type_id]]
   :select-fields [[:p/*]
                   [:pt/label :payer_type_label]]
   :before-insert (fn [data]
                    (when-not (get data :payer_type_id)
                      (throw (ex-info "payer_type_id is required" {:data data})))
                    (-> data
                      (assoc :id (UUID/randomUUID))
                      (update :is_default #(boolean %))))
   :before-update (fn [_id updates]
                    (-> updates
                      (assoc :updated_at [:now])))
   :has-search? true
   :has-count? true})

(def payer-type-config
  {:table-name "payer_types"
   :primary-key :id
   :required-fields [:label]
   :allowed-order-by {:label :label
                      :created-at :created_at
                      :updated-at :updated_at}
   :default-order-by :label
   :search-fields [:label]
   :before-insert (fn [data]
                    (-> data
                      (assoc :id (UUID/randomUUID))
                      (update :is_default #(boolean %))))
   :before-update (fn [_id updates]
                    (-> updates (assoc :updated_at [:now])))
   :has-search? true
   :has-count? true})

(def article-config
  {:table-name "articles"
   :primary-key :id
   :required-fields [:canonical_name]
   :allowed-order-by {:canonical-name :canonical_name
                      :normalized-key :normalized_key
                      :created-at :created_at
                      :updated-at :updated_at}
   :default-order-by :canonical_name
   :search-fields [:canonical_name :normalized_key]
   :field-transformers {:normalized_key articles/normalize-article-key}
   :before-insert (fn [data]
                    (let [canonical-name (:canonical_name data)]
                      (-> data
                        (assoc :id (UUID/randomUUID))
                        (assoc :normalized_key (articles/normalize-article-key canonical-name)))))
   :before-update (fn [_id updates]
                    (cond-> updates
                      (:canonical_name updates)
                      (assoc :normalized_key (articles/normalize-article-key (:canonical_name updates)))
                      true (assoc :updated_at [:now])))
   :has-search? true
   :has-count? true})

(def expense-config
  {:table-name "expenses"
   :primary-key :id
   :required-fields [:supplier_id :payer_id :purchased_at :total_amount]
   :allowed-order-by {:expense-date :purchased_at
                      :purchased-at :purchased_at
                      :created-at :created_at
                      :updated-at :updated_at
                      :total-amount :total_amount}
   :default-order-by :purchased_at
   :search-fields [:s/display_name :p/label]
   :joins [[:suppliers :s] [:= :s/id :e/supplier_id]
           [:payers :p] [:= :p/id :e/payer_id]]
   :select-fields [[:e.*]
                   [:s/display_name :supplier_display_name]
                   [:s/normalized_key :supplier_normalized_key]
                   [:p/label :payer_label]
                   [:p/type :payer_type]]
   :before-insert (fn [data]
                    (-> data
                      (assoc :id (UUID/randomUUID))
                      (update :currency #(when % [:cast % :currency]))
                      (update :is_posted #(if (nil? %) true (boolean %)))))
   :before-update (fn [_id updates]
                    (-> updates
                      (update :currency #(when % [:cast % :currency]))
                      (assoc :updated_at [:now])))
   :has-count? true})

(def expense-item-config
  {:table-name "expense_items"
   :table-alias :ei
   :primary-key :ei/id
   ;; We accept :raw_label (string) or :alias_id (uuid). The DB stores :alias_id.
   :required-fields [:expense_id :line_total]
   :allowed-order-by {:expense-id :ei/expense_id
                      :raw-label :aa/raw_label
                      :alias-id :ei/alias_id
                      :created-at :ei/created_at
                      :qty :ei/qty
                      :unit-price :ei/unit_price
                      :line-total :ei/line_total
                      :expense-purchased-at :e/purchased_at
                      :article-canonical-name :a/canonical_name}
   :default-order-by :ei/created_at
   :search-fields [:aa/raw_label :a/canonical_name]
   :joins [[:expenses :e] [:= :e/id :ei/expense_id]
           [:article_aliases :aa] [:= :aa/id :ei/alias_id]
           [:articles :a] [:= :a/id :aa/article_id]]
   :select-fields [[:ei.*]
                   [:aa/raw_label :raw_label]
                   [:aa/raw_label_normalized :raw_label_normalized]
                   [:e/purchased_at :expense_purchased_at]
                   [:a/canonical_name :article_canonical_name]]
   :before-insert (fn [db data]
                    (let [alias-svc (requiring-resolve 'app.domain.backend.expenses.services.article-aliases/find-or-create-alias!)
                          raw-label (some-> (:raw_label data) str str/trim)
                          supplier-id (:supplier_id data)]
                      (cond
                        (some? (:alias_id data))
                        (dissoc data :raw_label :supplier_id)

                        (some? raw-label)
                        (let [alias (alias-svc db supplier-id raw-label)]
                          (-> data
                            (dissoc :raw_label :supplier_id)
                            (assoc :alias_id (:id alias))))

                        :else
                        (throw (ex-info "raw_label or alias_id is required"
                                 {:entity "expense_items"
                                  :missing-field :raw_label})))))
   :before-update (fn [db _id updates]
                    (let [alias-svc (requiring-resolve 'app.domain.backend.expenses.services.article-aliases/find-or-create-alias!)
                          raw-label (some-> (:raw_label updates) str str/trim)
                          supplier-id (:supplier_id updates)]
                      (cond
                        (some? raw-label)
                        (let [alias (alias-svc db supplier-id raw-label)]
                          (-> updates
                            (dissoc :raw_label :supplier_id)
                            (assoc :alias_id (:id alias))))

                        :else
                        (dissoc updates :raw_label :supplier_id))))
   :has-search? true
   :has-count? true})

(def receipt-config
  {:table-name "receipts"
   :primary-key :id
   :required-fields [:storage_key]
   :allowed-order-by {:created-at :created_at
                      :updated-at :updated_at
                      :status :status}
   :default-order-by :created_at
   :search-fields [:original_filename :storage_key]
   :before-insert (fn [data]
                    (-> data
                      (assoc :id (UUID/randomUUID))
                      (assoc :status "uploaded")
                      (update :status #(vector :cast % :receipt_status))))
   :has-count? true
   ;; Note: receipts have many custom operations (upload, status transitions, etc)
   ;; that are kept in receipts.clj - this config is for basic CRUD only
   :custom-service? true})

(def price-history-config
  {:table-name "price_observations"
   :table-alias :po
   :primary-key :id
   :required-fields [:article_id :supplier_id :observed_at :line_total]
   :allowed-order-by {:observed-at :observed_at
                      :created-at :created_at
                      :unit-price :unit_price
                      :line-total :line_total}
   :default-order-by :observed_at
   :search-fields [:a/canonical_name :s/display_name]
   :joins [[:articles :a] [:= :a/id :po/article_id]
           [:suppliers :s] [:= :s/id :po/supplier_id]]
   :select-fields [[:po.*]
                   [:a/canonical_name :article_canonical_name]
                   [:s/display_name :supplier_display_name]]
   :before-insert (fn [data]
                    (-> data
                      (assoc :id (UUID/randomUUID))
                      (update :currency #(when % [:cast % :currency]))
                      (update :observed_at #(or % [:now]))))
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

;; ==========================================================================
;; Registry
;; ==========================================================================

(def ^:private entity-configs
  {:article-alias article-alias-config
   :price-observation price-observation-config
   :supplier supplier-config
   :payer-type payer-type-config
   :payer payer-config
   :article article-config
   :expense expense-config
   :expense-item expense-item-config
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

;; ==========================================================================
;; Service Registration
;; ==========================================================================

(defn register-all-entity-services!
  "Register all entity services with the factory."
  []
  (doseq [[entity-key config] entity-configs]
    (factory/register-entity-service!
      (assoc config :entity-key entity-key))))

;; Auto-register all services on require
(register-all-entity-services!)
