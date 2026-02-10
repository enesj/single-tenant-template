(ns app.domain.backend.expenses.services.service-configs
  "Service configuration maps for expenses domain entities."
  (:require
    [app.domain.backend.expenses.services.articles :as articles]
    [app.domain.backend.expenses.services.price-history :as price-history]
    [app.domain.backend.expenses.services.services-factory :as factory]
    [clojure.string :as str])
  (:import
    [java.text Normalizer Normalizer$Form]
    [java.util UUID]))

;; ============================================================================
;; Shared Normalization Functions
;; ============================================================================

(defn- join-single-letter-tokens
  [value]
  (let [tokens (->> (str/split (or value "") #"\s+")
                 (remove str/blank?))]
    (if (seq tokens)
      (let [{:keys [acc run]}
            (reduce
              (fn [{:keys [acc run]} token]
                (if (= 1 (count token))
                  {:acc acc
                   :run (conj run token)}
                  {:acc (cond-> acc
                          (seq run) (conj (apply str run))
                          true (conj token))
                   :run []}))
              {:acc [] :run []}
              tokens)
            acc (cond-> acc (seq run) (conj (apply str run)))]
        (str/join " " acc))
      value)))

(def ^:private html-entity->text
  {"amp" "&"
   "lt" "<"
   "gt" ">"
   "quot" "\""
   "apos" "'"
   "nbsp" " "})

(defn- codepoint->str
  "Convert a Unicode codepoint (0..0x10FFFF) to a string.

  Returns nil when the codepoint is invalid."
  [n]
  (try
    (when (and (number? n)
            (<= 0 (long n) 1114111))
      (String. (Character/toChars (int n))))
    (catch Exception _
      nil)))

(defn- unescape-html-entities-once
  "Best-effort decode of common HTML entities.

  Supports:
  - Named: &amp;, &lt;, &gt;, &quot;, &apos;, &nbsp; (semicolon optional)
  - Numeric: &#38;, &#x26; (semicolon optional)

  Unknown entities are preserved as-is."
  [s]
  (let [s (some-> s str)]
    (when s
      (-> s
        (str/replace
          #"&#x([0-9A-Fa-f]{1,6});?"
          (fn [[m hex]]
            (try
              (let [n (Long/parseLong hex 16)]
                (or (codepoint->str n) m))
              (catch Exception _
                m))))
        (str/replace
          #"&#([0-9]{1,7});?"
          (fn [[m dec]]
            (try
              (let [n (Long/parseLong dec 10)]
                (or (codepoint->str n) m))
              (catch Exception _
                m))))
        (str/replace
          #"&([A-Za-z]+);?"
          (fn [[m ent]]
            (or (get html-entity->text (str/lower-case ent)) m)))))))

(defn unescape-html-entities
  "Decode HTML entities, with a small bounded repeat to handle double-encoding.

  Example: `&amp;amp;` -> `&amp;` -> `&`"
  [s]
  (let [s (some-> s str)]
    (when s
      (loop [i 0
             cur s]
        (let [nxt (unescape-html-entities-once cur)]
          (if (or (>= i 2) (= nxt cur))
            nxt
            (recur (inc i) nxt)))))))

(defn normalize-supplier-key
  "Normalize supplier name to lowercase, replace spaces with hyphens, remove special chars.
   Used for deduplication and fuzzy matching."
  [name]
  (when name
    (let [legal-suffix-tokens
          ;; Common legal entity suffixes. We intentionally treat these as “end of
          ;; canonical company name” for dedupe, and drop anything after them
          ;; (e.g. location like "Sarajevo").
          ;;
          ;; NOTE: We include both dotted and undotted forms, but after our
          ;; punctuation-stripping step most dotted forms collapse ("d.o.o." -> "doo").
          #{"doo" "dd" "ad" "llc" "ltd" "inc" "gmbh" "ag"}
          canonicalize-tokens
          (fn [tokens]
            (if (seq tokens)
              (let [idx (->> tokens
                          (map-indexed vector)
                          (keep (fn [[i t]] (when (contains? legal-suffix-tokens t) i)))
                          first)]
                ;; Only strip when the suffix appears after at least one “real” token.
                (if (and (some? idx) (pos? (long idx)))
                  (subvec (vec tokens) 0 (long idx))
                  (vec tokens)))
              []))]
      (-> name
        unescape-html-entities
        str/trim
        (Normalizer/normalize Normalizer$Form/NFD)
        (str/replace #"\p{M}+" "")
        (str/replace #"Đ" "D")
        (str/replace #"đ" "d")
        str/lower-case
        ;; Keep hyphens/spaces for tokenization, remove everything else.
        (str/replace #"[^a-z0-9\s-]" "")
        ;; Treat hyphens as whitespace for dedupe.
        (str/replace #"-" " ")
        join-single-letter-tokens
        (str/replace #"\s+" " ")
        str/trim
        (str/split #"\s+")
        (->> (remove str/blank?) vec canonicalize-tokens)
        (->> (str/join "-"))))))

(defn normalize-store-key
  "Normalize store/address strings into a stable key for dedupe."
  [value]
  (when value
    (-> value
      unescape-html-entities
      str/trim
      (Normalizer/normalize Normalizer$Form/NFD)
      (str/replace #"\p{M}+" "")
      (str/replace #"Đ" "D")
      (str/replace #"đ" "d")
      str/lower-case
      (str/replace #"[^a-z0-9\s-]" "")
      (str/replace #"-" " ")
      join-single-letter-tokens
      (str/replace #"\s+" " ")
      str/trim
      (str/split #"\s+")
      (->> (remove str/blank?)
        (str/join "-")))))

(defn normalize-manufacturer-key
  "Normalize manufacturer name to lowercase, replace spaces with hyphens, remove special chars.
   Used for deduplication and fuzzy matching."
  [name]
  (when name
    (-> name
      unescape-html-entities
      str/trim
      (Normalizer/normalize Normalizer$Form/NFD)
      (str/replace #"\p{M}+" "")
      (str/replace #"Đ" "D")
      (str/replace #"đ" "d")
      str/lower-case
      ;; Keep hyphens/spaces for tokenization, remove everything else.
      (str/replace #"[^a-z0-9\s-]" "")
      ;; Treat hyphens as whitespace for dedupe.
      (str/replace #"-" " ")
      join-single-letter-tokens
      (str/replace #"\s+" " ")
      str/trim
      (str/split #"\s+")
      (->> (remove str/blank?)
        (str/join "-")))))

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

(def supplier-alias-config
  {:table-name "supplier_aliases"
   :table-alias :sa
   :primary-key :sa/id
   :required-fields [:raw_label :raw_label_normalized]
   :allowed-order-by {:created-at :sa/created_at
                      :updated-at :sa/updated_at
                      :raw-label :sa/raw_label
                      :raw-label-normalized :sa/raw_label_normalized
                      :supplier-display-name :s/display_name
                      :confidence :sa/confidence}
   :default-order-by :sa/created_at
   :search-fields [:sa/raw_label :sa/raw_label_normalized :s/display_name]
   :joins [[:suppliers :s] [:= :s/id :sa/supplier_id]]
   :select-fields [[:sa.*]
                   [:s/display_name :supplier_display_name]]
   :field-transformers {:raw_label_normalized normalize-supplier-key}
   :has-search? true
   :has-count? true})

(def store-alias-config
  {:table-name "store_aliases"
   :table-alias :sta
   :primary-key :sta/id
   :required-fields [:raw_label :raw_label_normalized]
   :allowed-order-by {:created-at :sta/created_at
                      :updated-at :sta/updated_at
                      :raw-label :sta/raw_label
                      :raw-label-normalized :sta/raw_label_normalized
                      :store-display-name :st/display_name
                      :supplier-display-name :s/display_name
                      :confidence :sta/confidence}
   :default-order-by :sta/created_at
   :search-fields [:sta/raw_label :sta/raw_label_normalized :st/display_name :st/address :s/display_name]
   :joins [[:stores :st] [:= :st/id :sta/store_id]
           [:suppliers :s] [:= :s/id :st/supplier_id]]
   :select-fields [[:sta.*]
                   [:st/display_name :store_display_name]
                   [:st/address :store_address]
                   [:st/supplier_id :supplier_id]
                   [:s/display_name :supplier_display_name]]
   :field-transformers {:raw_label_normalized normalize-store-key}
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
                    (let [display-name (unescape-html-entities (:display_name data))]
                      (assoc data
                        :display_name display-name
                        :normalized_key (normalize-supplier-key display-name)
                        :id (UUID/randomUUID))))
   :before-update (fn [_id updates]
                    (if (:display_name updates)
                      (let [display-name (unescape-html-entities (:display_name updates))]
                        (assoc updates
                          :display_name display-name
                          :normalized_key (normalize-supplier-key display-name)
                          :updated_at [:now]))
                      (assoc updates :updated_at [:now])))
   :has-search? true
   :has-count? true})

(def store-config
  {:table-name "stores"
   :primary-key :id
   :required-fields [:supplier_id :display_name]
   :allowed-order-by {:display-name :display_name
                      :normalized-key :normalized_key
                      :created-at :created_at
                      :updated-at :updated_at}
   :default-order-by :display_name
   :search-fields [:display_name :normalized_key :address :place_id]
   :field-transformers {:normalized_key normalize-store-key}
   :before-insert (fn [data]
                    (let [display-name (unescape-html-entities (:display_name data))
                          address (unescape-html-entities (:address data))
                          key-src (str (or display-name "") " " (or address ""))]
                      (-> data
                        (assoc :display_name display-name)
                        (assoc :address address)
                        (assoc :normalized_key (normalize-store-key key-src))
                        (assoc :id (UUID/randomUUID)))))
   :before-update (fn [_id updates]
                    (let [display-name (when (contains? updates :display_name)
                                         (unescape-html-entities (:display_name updates)))
                          address (when (contains? updates :address)
                                    (unescape-html-entities (:address updates)))
                          updates (cond-> (assoc updates :updated_at [:now])
                                    (contains? updates :display_name)
                                    (assoc :display_name display-name)
                                    (contains? updates :address)
                                    (assoc :address address))]
                      (cond-> updates
                        (or (contains? updates :display_name)
                          (contains? updates :address))
                        (assoc :normalized_key (normalize-store-key (str (or display-name "") " " (or address "")))))))
   :has-search? true
   :has-count? true})

(def manufacturer-config
  {:table-name "manufacturers"
   :primary-key :id
   :required-fields [:display_name]
   :allowed-order-by {:display-name :display_name
                      :normalized-key :normalized_key
                      :created-at :created_at
                      :updated-at :updated_at}
   :default-order-by :display_name
   :search-fields [:display_name :normalized_key]
   :field-transformers {:normalized_key normalize-manufacturer-key}
   :before-insert (fn [data]
                    (let [display-name (unescape-html-entities (:display_name data))]
                      (-> data
                        (assoc :display_name display-name)
                        (assoc :id (UUID/randomUUID))
                        (assoc :normalized_key (normalize-manufacturer-key display-name)))))
   :before-update (fn [_id updates]
                    (if (:display_name updates)
                      (let [display-name (unescape-html-entities (:display_name updates))]
                        (assoc updates
                          :display_name display-name
                          :normalized_key (normalize-manufacturer-key display-name)
                          :updated_at [:now]))
                      (assoc updates :updated_at [:now])))
   :has-search? true
   :has-count? true})

(def category-config
  {:table-name "categories"
   :primary-key :id
   :required-fields [:name]
   :allowed-order-by {:name :name
                      :created-at :created_at
                      :updated-at :updated_at}
   :default-order-by :name
   :search-fields [:name :description]
   :before-insert (fn [data]
                    (-> data
                      (assoc :id (UUID/randomUUID))))
   :before-update (fn [_id updates]
                    (-> updates
                      (assoc :updated_at [:now])))
   :has-search? true
   :has-count? true})

(def subcategory-config
  {:table-name "subcategories"
   :primary-key :id
   :required-fields [:category_id :name]
   :allowed-order-by {:name :name
                      :category-id :category_id
                      :created-at :created_at
                      :updated-at :updated_at}
   :default-order-by :name
   :search-fields [:name :description]
   :before-insert (fn [data]
                    (when-not (:category_id data)
                      (throw (ex-info "category_id is required" {:data data})))
                    (-> data
                      (assoc :id (UUID/randomUUID))))
   :before-update (fn [_id updates]
                    (-> updates
                      (assoc :updated_at [:now])))
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
   :supplier-alias supplier-alias-config
   :store-alias store-alias-config
   :price-observation price-observation-config
   :supplier supplier-config
   :store store-config
   :manufacturer manufacturer-config
   :category category-config
   :subcategory subcategory-config
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
