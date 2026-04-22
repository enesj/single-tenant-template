(ns app.domain.backend.expenses.services.expenses
  "Expense creation/update with line items."
  (:require
    [app.domain.backend.expenses.services.article-aliases :as aliases]
    [app.domain.backend.expenses.services.articles :as articles]
    [app.domain.backend.expenses.services.exchange-rates :as exchange-rates]
    [app.template.backend.security.email :as email-privacy]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [taoensso.timbre :as log])
  (:import
    [java.math RoundingMode]
    [java.time Instant LocalDate LocalDateTime OffsetDateTime ZoneId ZoneOffset]
    [java.util Date UUID]))

;; ============================================================================
;; Helpers
;; ============================================================================

(defn- require-keys!
  [m ks]
  (doseq [k ks]
    (when-not (get m k)
      (throw (ex-info (str (name k) " is required")
               {:status 400
                :field k
                :data m})))))

(defn- blank->nil
  [v]
  (cond
    (nil? v) nil
    (and (string? v) (str/blank? v)) nil
    :else v))

(defn- parse-uuid!
  [field v]
  (let [v (blank->nil v)]
    (cond
      (nil? v) nil
      (instance? UUID v) v
      :else
      (try
        (UUID/fromString (str v))
        (catch IllegalArgumentException _
          (throw (ex-info (str "Invalid " (name field))
                   {:status 400
                    :field field
                    :value v})))))))

(defn- parse-bigdec!
  [field v]
  (let [v (blank->nil v)]
    (cond
      (nil? v) nil
      (instance? java.math.BigDecimal v) v
      (number? v) (bigdec v)
      (string? v)
      (try
        (bigdec v)
        (catch Exception _
          (throw (ex-info (str "Invalid " (name field))
                   {:status 400
                    :field field
                    :value v}))))
      :else
      (throw (ex-info (str "Invalid " (name field))
               {:status 400
                :field field
                :value v})))))

(defn- parse-instant!
  [field v]
  (let [v (blank->nil v)]
    (cond
      (nil? v) nil
      (instance? Instant v) v
      (instance? Date v) (.toInstant ^Date v)
      (number? v) (Instant/ofEpochMilli (long v))
      (string? v)
      (or
        (try
          (Instant/parse v)
          (catch Exception _ nil))
        (try
          (-> (OffsetDateTime/parse v) .toInstant)
          (catch Exception _ nil))
        ;; Support HTML `datetime-local` values like "2025-12-12T12:34".
        (try
          (-> (LocalDateTime/parse v)
            (.atZone (ZoneId/systemDefault))
            .toInstant)
          (catch Exception _ nil))
        (try
          (-> (LocalDate/parse v) (.atStartOfDay ZoneOffset/UTC) .toInstant)
          (catch Exception _ nil))
        (throw (ex-info (str "Invalid " (name field))
                 {:status 400
                  :field field
                  :value v})))
      :else
      (throw (ex-info (str "Invalid " (name field))
               {:status 400
                :field field
                :value v})))))

(defn- update-if-present
  "Update map key only when it exists (avoids inserting optional columns with nil values)."
  [m k f]
  (if (contains? m k)
    (update m k f)
    m))

(defn- derive-unit-price
  "Derive a unit price from line total and quantity.

  Receipts often omit an explicit unit price; when qty is missing we treat the
  line total as the unit price (i.e. assume qty=1).
  Returns nil when qty is zero or negative (bad OCR data)."
  [qty line-total]
  (cond
    (nil? line-total) nil
    (nil? qty) line-total
    (pos? qty) (.divide ^java.math.BigDecimal line-total
                 ^java.math.BigDecimal qty
                 2
                 RoundingMode/HALF_UP)
    :else (do
            (log/warn "derive-unit-price: non-positive qty; unit price will be nil"
              {:qty qty :line-total line-total})
            nil)))

(defn- inconsistent-item-unit-price?
  [{:keys [qty unit_price line_total]}]
  (let [expected (when (and qty unit_price)
                   (.multiply ^java.math.BigDecimal qty
                     ^java.math.BigDecimal unit_price))
        diff (when (and expected line_total)
               (.abs (.subtract ^java.math.BigDecimal expected
                       ^java.math.BigDecimal line_total)))]
    (and qty
      unit_price
      line_total
      (pos? (.signum ^java.math.BigDecimal qty))
      diff
      (pos? (.compareTo ^java.math.BigDecimal diff (bigdec "0.01"))))))

(defn- normalize-expense-item
  [item]
  (let [item* (-> item
                (update-if-present :id #(parse-uuid! :id %))
                (update-if-present :alias_id #(parse-uuid! :alias_id %))
                (update-if-present :raw_label #(some-> % str str/trim))
                (update-if-present :unit #(some-> % str str/trim str/lower-case blank->nil))
                (update-if-present :qty #(parse-bigdec! :qty %))
                (update-if-present :unit_price #(parse-bigdec! :unit_price %))
                (update-if-present :line_total #(parse-bigdec! :line_total %)))
        derived-unit-price (when (nil? (:unit_price item*))
                             (derive-unit-price (:qty item*) (:line_total item*)))
        repaired-unit-price (when (and (:unit_price item*)
                                    (inconsistent-item-unit-price? item*))
                              (derive-unit-price (:qty item*) (:line_total item*)))]
    (cond-> item*
      derived-unit-price (assoc :unit_price derived-unit-price)
      repaired-unit-price (assoc :unit_price repaired-unit-price))))

(def ^:private min-alias-normalized-length
  "Minimum length of a normalized alias label to be considered valid.

  This prevents creating/looking up aliases for blank/garbage labels like \" \", \"A\", or \"##\"."
  2)

(defn- valid-alias-label?
  [raw-label]
  (let [raw-label* (some-> raw-label str str/trim)
        normalized (articles/normalize-alias-label raw-label*)]
    (and (not (str/blank? raw-label*))
      (>= (count (or normalized "")) min-alias-normalized-length))))

(defn- resolve-alias!
  "Resolve or create an article alias for an expense item.

  Accepts either:
  - :alias_id (UUID)
  - :raw_label (string) (upserted into article_aliases)

  Options:
  - :allow-auto-link? (default true) controls whether raw_label can be used to
    auto-link existing items on update.

  Returns the alias row or nil when both are missing/invalid."
  ([tx supplier-id item]
   (resolve-alias! tx supplier-id item {}))
  ([tx supplier-id {:keys [raw_label alias_id unit]} {:keys [allow-auto-link?] :or {allow-auto-link? true}}]
   (cond
     alias_id
     (when-let [alias (jdbc/execute-one!
                        tx
                        (sql/format {:select [:*]
                                     :from [:article_aliases]
                                     :where [:= :id alias_id]
                                     :limit 1})
                        {:builder-fn rs/as-unqualified-lower-maps})]
       alias)

     (and allow-auto-link?
       (valid-alias-label? raw_label))
     (let [alias (aliases/find-or-create-alias! tx supplier-id (str/trim (str raw_label)) unit)]
       alias)

     :else nil)))

(defn- normalize-expense-data
  [expense-data]
  (-> expense-data
    (update-if-present :supplier_id #(parse-uuid! :supplier_id %))
    (update-if-present :payer_id #(parse-uuid! :payer_id %))
    (update-if-present :user_id #(parse-uuid! :user_id %))
    (update-if-present :created_by #(parse-uuid! :created_by %))
    (update-if-present :receipt_id #(parse-uuid! :receipt_id %))
    (update-if-present :store_id #(parse-uuid! :store_id %))
    (update-if-present :expense_category_id #(parse-uuid! :expense_category_id %))
    (update-if-present :article_id #(parse-uuid! :article_id %))
    (update-if-present :purchased_at #(parse-instant! :purchased_at %))
    (update-if-present :total_amount #(parse-bigdec! :total_amount %))
    (update-if-present :currency #(some-> % str str/trim blank->nil))
    (update-if-present :notes blank->nil)))

(defn- expense-rate-date
  "Derive the LocalDate used for exchange-rate lookup."
  [{:keys [purchased_at]}]
  (if (instance? Instant purchased_at)
    (LocalDate/ofInstant purchased_at ZoneOffset/UTC)
    (LocalDate/now)))

(defn- compute-bam-amount
  [total-amount rate]
  (some-> (.multiply ^java.math.BigDecimal total-amount
            ^java.math.BigDecimal rate)
    (.setScale 2 RoundingMode/HALF_UP)))

(defn normalize-expense-amounts
  "Populate derived monetary fields so all expense write paths persist
   consistent values.

   - BAM expenses persist original_amount = bam_amount = total_amount.
   - Non-BAM expenses use the cached daily exchange rate when available.
   - If no rate is cached yet, bam_amount falls back to total_amount so it is
     never left nil in reporting queries."
  ([db expense-data]
   (normalize-expense-amounts db expense-data nil))
  ([db expense-data {:keys [app-config ensure-rates?]}]
   (let [total-amount (:total_amount expense-data)
         currency (or (:currency expense-data) "BAM")]
     (if-not total-amount
       (assoc expense-data :currency currency)
       (if (= "BAM" currency)
         (assoc expense-data
           :currency currency
           :original_amount total-amount
           :bam_amount total-amount
           :exchange_rate nil
           :rate_fetched_at nil)
         (let [rate-date (expense-rate-date expense-data)
               _ (when (and ensure-rates? app-config)
                   (try
                     (exchange-rates/ensure-daily-rates!
                       db
                       (exchange-rates/build-config app-config))
                     (catch Exception e
                       (log/warn e "Failed to ensure daily rates"
                         {:currency currency
                          :rate-date (str rate-date)}))))
               rate (or (:exchange_rate expense-data)
                      (exchange-rates/get-conversion-rate db currency rate-date))
               bam-amount (or (when rate
                                (compute-bam-amount total-amount rate))
                            total-amount)]
           (assoc expense-data
             :currency currency
             :original_amount total-amount
             :bam_amount bam-amount
             :exchange_rate rate
             :rate_fetched_at (when rate
                                (or (:rate_fetched_at expense-data)
                                  (Instant/now))))))))))

(defn- revert-linked-receipt-after-expense-delete!
  "If the deleted expense was created from a receipt, revert the receipt so it can be
  deleted or approved+posted again.

  Rules:
  - Only applies when the receipt is currently linked to this expense.
  - Always clears the receipt's :expense_id link.
  - Only reverts receipts in status \"posted\" back to \"extracted\".

  NOTE: We revert to \"extracted\" (approvable); there is no \"exported\" receipt status
  (\"exported\" refers to the local file storage subdir used during posting)."
  [tx {:keys [id receipt_id]}]
  (when (and id receipt_id)
    (jdbc/execute-one!
      tx
      (sql/format {:update :receipts
                   :set {:expense_id nil
                         ;; If the receipt was posted, revert it to an approvable state.
                         ;; If it was already reverted/reset to a different status, keep it
                         ;; but still clear the now-invalid expense link.
                         :status [:raw "CASE WHEN status = 'posted'::receipt_status THEN 'extracted'::receipt_status ELSE status END"]}
                   :where [:and
                           [:= :id receipt_id]
                           [:= :expense_id id]]
                   :returning [:*]})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn delete-expense!
  "Hard delete expense.

  Expense items are removed via FK ON DELETE CASCADE.

  If the expense is linked to a receipt (via :receipt_id and the receipt's :expense_id),
  the receipt is reverted to an approvable status so it can be deleted or processed again.

  Optional `tenant-id` scopes the delete to a specific tenant."
  ([db id] (delete-expense! db id nil))
  ([db id tenant-id]
   (jdbc/with-transaction [tx db]
     (let [where (if tenant-id
                   [:and [:= :id id] [:= :tenant_id tenant-id]]
                   [:= :id id])]
       (when-let [expense (jdbc/execute-one!
                            tx
                            (sql/format {:select [:*]
                                         :from [:expenses]
                                         :where where
                                         :limit 1})
                            {:builder-fn rs/as-unqualified-lower-maps})]
         (revert-linked-receipt-after-expense-delete! tx expense)
         (jdbc/execute-one!
           tx
           (sql/format {:delete-from :expenses
                        :where where
                        :returning [:*]})
           {:builder-fn rs/as-unqualified-lower-maps}))))))

(defn get-expense-with-items
  "Get an expense with its items. Optional `tenant-id` scopes to a specific tenant."
  ([db id] (get-expense-with-items db id nil))
  ([db id tenant-id]
   (let [where (if tenant-id
                 [:and [:= :e.id id] [:= :e.tenant_id tenant-id]]
                 [:= :e.id id])
         expense (jdbc/execute-one!
                   db
                   (sql/format {:select [[:e.*]
                                         [:s.display_name :supplier_display_name]
                                         [:s.normalized_key :supplier_normalized_key]
                                         [:p.label :payer_label]
                                         [:p.type :payer_type]
                                         [:ec.name :expense_category_name]]
                                :from [[:expenses :e]]
                                :left-join [[:suppliers :s] [:= :s.id :e.supplier_id]
                                            [:payers :p] [:= :p.id :e.payer_id]
                                            [:expense_categories :ec] [:= :ec.id :e.expense_category_id]]
                                :where where})
                   {:builder-fn rs/as-unqualified-lower-maps})
         items (when expense
                 (jdbc/execute!
                   db
                   (sql/format {:select [[:ei.*]
                                         [:aa.raw_label :raw_label]
                                         [:aa.raw_label_normalized :raw_label_normalized]
                                         [:a.canonical_name :article_canonical_name]]
                                :from [[:expense_items :ei]]
                                :left-join [[:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                            [:articles :a] [:= :a.id :aa.article_id]]
                                :where [:= :ei.expense_id id]
                                :order-by [[:ei.created_at :asc]]})
                   {:builder-fn rs/as-unqualified-lower-maps}))]
     (when expense
       (assoc expense :items items)))))

(defn get-expense
  "Get an expense (with items). Wrapper expected by the generic admin routes factory."
  [db id]
  (get-expense-with-items db id))

(defn- source-clause
  "Build a WHERE fragment for the :source filter.
   \"manual\"  → receipt_id IS NULL
   \"receipt\" → receipt_id IS NOT NULL
   \"none\"    → always false (both toggles off)
   other/nil  → no filter"
  [col source]
  (case (some-> source str)
    "manual"  [:is col nil]
    "receipt" [:is-not col nil]
    "none"    [:= 1 0]
    nil))

(defn list-expenses
  "List expenses with common filters.
   opts: :from, :to, :supplier-id, :payer-id, :tenant-id, :source, :limit, :offset."
  [db {:keys [from to supplier-id payer-id tenant-id source limit offset order-dir]
       :or {limit 50 offset 0 order-dir :desc}}]
  (let [from (try (parse-instant! :from from) (catch Exception _ nil))
        to (try (parse-instant! :to to) (catch Exception _ nil))
        source-where (source-clause :e.receipt_id source)
        base-where (cond-> [:and]
                     tenant-id (conj [:= :e.tenant_id tenant-id])
                     from (conj [:>= :e.purchased_at from])
                     to (conj [:<= :e.purchased_at to])
                     supplier-id (conj [:= :e.supplier_id supplier-id])
                     payer-id (conj [:= :e.payer_id payer-id])
                     source-where (conj source-where))
        query {:select [[:e.*]
                        [:s.display_name :supplier_display_name]
                        [:s.normalized_key :supplier_normalized_key]
                        [:p.label :payer_label]
                        [:p.type :payer_type]
                        [:ec.name :expense_category_name]
                        [:cb.id :created_by_user_id]
                        [:cb.full_name :created_by_full_name]]
               :from [[:expenses :e]]
               :left-join [[:suppliers :s] [:= :s.id :e.supplier_id]
                           [:payers :p] [:= :p.id :e.payer_id]
                           [:expense_categories :ec] [:= :ec.id :e.expense_category_id]
                           [:users :cb] [:= :cb.id :e.created_by]]
               :where base-where
               :order-by [[:e.purchased_at order-dir]]
               :limit limit
               :offset offset}]
    (->> (jdbc/execute! db (sql/format query) {:builder-fn rs/as-unqualified-lower-maps})
      (mapv email-privacy/routine-created-by-view))))

(defn count-expenses
  "Count expenses with the same filters as `list-expenses`.
   opts: :from, :to, :supplier-id, :payer-id, :tenant-id, :source."
  [db {:keys [from to supplier-id payer-id tenant-id source]}]
  (let [from (try (parse-instant! :from from) (catch Exception _ nil))
        to (try (parse-instant! :to to) (catch Exception _ nil))
        source-where (source-clause :receipt_id source)
        base-where (cond-> [:and]
                     tenant-id (conj [:= :tenant_id tenant-id])
                     from (conj [:>= :purchased_at from])
                     to (conj [:<= :purchased_at to])
                     supplier-id (conj [:= :supplier_id supplier-id])
                     payer-id (conj [:= :payer_id payer-id])
                     source-where (conj source-where))
        row (jdbc/execute-one!
              db
              (sql/format {:select [[[:count :*] :total]]
                           :from [:expenses]
                           :where base-where})
              {:builder-fn rs/as-unqualified-lower-maps})]
    {:total (long (or (:total row) 0))}))

(defn- lookup-tenant-id
  [db table-name entity-id]
  (when entity-id
    (some-> (jdbc/execute-one!
              db
              (sql/format {:select [:tenant_id]
                           :from [table-name]
                           :where [:= :id entity-id]
                           :limit 1})
              {:builder-fn rs/as-unqualified-lower-maps})
      :tenant_id)))

(defn- infer-expense-tenant-id
  [db {:keys [tenant_id receipt_id payer_id]}]
  (or tenant_id
    (lookup-tenant-id db :receipts receipt_id)
    (lookup-tenant-id db :payers payer_id)))

(defn- existing-expense-id-for-receipt
  "Return the already-linked expense id for a receipt, if any.
   Throws 404 when the referenced receipt does not exist."
  [tx receipt-id]
  (let [receipt (jdbc/execute-one!
                  tx
                  (sql/format {:select [:id :expense_id]
                               :from [:receipts]
                               :where [:= :id receipt-id]
                               :limit 1})
                  {:builder-fn rs/as-unqualified-lower-maps})]
    (when-not receipt
      (throw (ex-info "Receipt not found"
               {:status 404
                :field :receipt_id
                :receipt-id receipt-id})))
    (or (:expense_id receipt)
      (:id (jdbc/execute-one!
             tx
             (sql/format {:select [:id]
                          :from [:expenses]
                          :where [:= :receipt_id receipt-id]
                          :limit 1})
             {:builder-fn rs/as-unqualified-lower-maps})))))

(declare throw-receipt-link-conflict!)

(defn- ensure-receipt-link-available!
  "Prevent creating a second expense for the same receipt.
   Callers must explicitly unlink first (for example by deleting the linked expense)."
  [tx {:keys [receipt_id]}]
  (when receipt_id
    (when-let [expense-id (existing-expense-id-for-receipt tx receipt_id)]
      (throw-receipt-link-conflict! receipt_id expense-id))))

(defn- throw-receipt-link-conflict!
  [receipt-id expense-id]
  (throw (ex-info "Receipt already linked to an expense. Unlink it first before creating another expense"
           {:status 409
            :field :receipt_id
            :receipt-id receipt-id
            :expense-id expense-id})))

(defn create-expense!
  "Create an expense and its line items.

  Supports two arities:
  - (create-expense! db {:supplier_id ... :items [...] ...})
  - (create-expense! db expense-data items)

  Behavior:
  - Stores each line item's raw label via `article_aliases` when the label is valid.
  - Skips alias creation for blank/short/punctuation-only labels.
  - Falls back to inferring `:tenant_id` from the linked receipt or payer when
    older callers omit it.
  - Persists normalized monetary fields (`original_amount`, `bam_amount`,
    `exchange_rate`, `rate_fetched_at`) for every create path."
  ([db {:keys [items] :as body}]
   (create-expense! db (dissoc body :items) items))
  ([db expense-data items]
   (let [expense-data* (normalize-expense-data expense-data)
         normalized-items (mapv normalize-expense-item (or items []))
         article-id (:article_id expense-data*)
         items* (if (and article-id (empty? normalized-items))
                  (let [article (jdbc/execute-one!
                                  db
                                  (sql/format {:select [:canonical_name]
                                               :from [:articles]
                                               :where [:= :id article-id]
                                               :limit 1})
                                  {:builder-fn rs/as-unqualified-lower-maps})
                        article-name (or (:canonical_name article) "Item")]
                    [{:raw_label article-name
                      :qty (bigdec 1)
                      :unit_price (:total_amount expense-data*)
                      :line_total (:total_amount expense-data*)}])
                  normalized-items)]

     (jdbc/with-transaction [tx db]
       (let [expense-data* (if (and (nil? (:supplier_id expense-data*))
                                 (:store_id expense-data*))
                             (if-let [store (jdbc/execute-one!
                                              tx
                                              (sql/format {:select [:supplier_id]
                                                           :from [:stores]
                                                           :where [:= :id (:store_id expense-data*)]
                                                           :limit 1})
                                              {:builder-fn rs/as-unqualified-lower-maps})]
                               (assoc expense-data* :supplier_id (:supplier_id store))
                               expense-data*)
                             expense-data*)
             expense-data* (if-let [inferred-tenant-id (infer-expense-tenant-id tx expense-data*)]
                             (assoc expense-data* :tenant_id inferred-tenant-id)
                             expense-data*)
             _ (require-keys! expense-data* [:payer_id :purchased_at :total_amount])
             _ (ensure-receipt-link-available! tx expense-data*)
             expense-data* (normalize-expense-amounts tx expense-data*)
             ;; Context validation: at least one of supplier, store, category, or article
             _ (when-not (or (:supplier_id expense-data*)
                           (:store_id expense-data*)
                           (:expense_category_id expense-data*)
                           article-id)
                 (throw (ex-info "At least one context is required: supplier, store, category, or article"
                          {:status 400 :field :context})))
             expense-id (UUID/randomUUID)
             expense-row (-> expense-data*
                           (select-keys [:tenant_id
                                         :store_id
                                         :supplier_id
                                         :payer_id
                                         :expense_category_id
                                         :user_id
                                         :created_by
                                         :receipt_id
                                         :purchased_at
                                         :total_amount
                                         :original_amount
                                         :bam_amount
                                         :exchange_rate
                                         :rate_fetched_at
                                         :currency
                                         :notes])
                           (update-if-present :currency #(when % [:cast % :currency]))
                           (assoc :id expense-id))
             expense (try
                       (jdbc/execute-one!
                         tx
                         (sql/format {:insert-into :expenses
                                      :values [expense-row]
                                      :returning [:*]})
                         {:builder-fn rs/as-unqualified-lower-maps})
                       (catch org.postgresql.util.PSQLException e
                         (let [message (or (.getMessage e) "")]
                           (if (and (:receipt_id expense-row)
                                 (= "23505" (.getSQLState e))
                                 (or (str/includes? message "uniq_expenses_receipt_id")
                                   (str/includes? message "expenses_receipt_id")))
                             (throw-receipt-link-conflict! (:receipt_id expense-row)
                               (existing-expense-id-for-receipt tx (:receipt_id expense-row)))
                             (throw e)))))
             supplier-id (:supplier_id expense)]

         ;; Insert line items (if any — empty items is valid for minimal entry)
         (when (seq items*)
           (let [resolved-items (mapv (fn [item]
                                        (require-keys! item [:line_total])
                                        (let [alias (resolve-alias! tx supplier-id item)
                                              resolved-article-id (:article_id alias)]
                                          (assoc item
                                            :resolved_alias alias
                                            :resolved_alias_id (some-> alias :id)
                                            :resolved_article_id resolved-article-id)))
                                  items*)
                 tenant-id (:tenant_id expense)
                 item-rows (mapv (fn [{:keys [resolved_alias_id qty unit unit_price line_total price_modified]}]
                                   (cond-> {:id (UUID/randomUUID)
                                            :expense_id expense-id
                                            :alias_id resolved_alias_id
                                            :qty qty
                                            :unit_price unit_price
                                            :line_total line_total}
                                     tenant-id (assoc :tenant_id tenant-id)
                                     (some? price_modified) (assoc :price_modified price_modified)
                                     unit (assoc :unit unit)))
                             resolved-items)]
             (jdbc/execute!
               tx
               (sql/format {:insert-into :expense_items
                            :values item-rows
                            :returning [:*]})
               {:builder-fn rs/as-unqualified-lower-maps})))

         (get-expense-with-items tx expense-id))))))

(defn- ensure-direct-expense-edit-allowed!
  "Reject direct edits for receipt-linked expenses unless the caller explicitly opts in.

  Receipt-linked expenses must be changed through receipt editing so the expense
  row, receipt metadata, and receipt status stay in sync."
  [{:keys [id receipt_id]} {:keys [allow-linked-expense-update?]}]
  (when (and receipt_id (not allow-linked-expense-update?))
    (throw (ex-info "Only manually entered expenses can be edited directly. Edit the linked receipt instead."
             {:status 409
              :field :receipt_id
              :expense-id id
              :receipt-id receipt_id}))))

(defn update-expense!
  "Update an expense and optionally upsert its items.

  If `:items` is present in the update body, the provided set becomes the new
  active set:
  - missing items are deleted
  - items with matching `:id` are updated
  - items without `:id` are inserted

  Auto-linking from aliases is applied only for newly inserted items (existing
  items are not retroactively auto-linked unless the alias is explicitly changed).

  Receipt-linked expenses (`:receipt_id`) are read-only for direct expense edits
  by default. Use `:allow-linked-expense-update?` only from receipt workflows
  that keep the linked receipt in sync.

  Optional `tenant-id` scopes the update to a specific tenant."
  ([db id body] (update-expense! db id body nil nil))
  ([db id body tenant-id] (update-expense! db id body tenant-id nil))
  ([db id body tenant-id opts]
   (let [id* (parse-uuid! :id id)
         parsed-updates (-> body
                          (dissoc :items)
                          normalize-expense-data)
         amount-keys [:purchased_at :total_amount :currency]
         base-keys [:store_id :supplier_id :payer_id :expense_category_id :purchased_at :total_amount :currency :notes]
         where (if tenant-id
                 [:and [:= :id id*] [:= :tenant_id tenant-id]]
                 [:= :id id*])]
     (jdbc/with-transaction [tx db]
       (when-let [existing (jdbc/execute-one!
                             tx
                             (sql/format {:select [:*]
                                          :from [:expenses]
                                          :where where
                                          :limit 1})
                             {:builder-fn rs/as-unqualified-lower-maps})]
         (ensure-direct-expense-edit-allowed! existing opts)
         (let [updates* (if (seq parsed-updates)
                          (let [base-updates (select-keys parsed-updates base-keys)]
                            (if (some #(contains? base-updates %) amount-keys)
                              (let [amount-input (merge (select-keys existing [:purchased_at :total_amount :currency :original_amount :bam_amount :exchange_rate :rate_fetched_at])
                                                   base-updates)
                                    normalized-amounts (select-keys (normalize-expense-amounts tx amount-input)
                                                         [:original_amount :bam_amount :exchange_rate :rate_fetched_at])]
                                (merge base-updates normalized-amounts))
                              base-updates))
                          {})
               updates* (update-if-present updates* :currency #(when % [:cast % :currency]))
               expense (if (seq updates*)
                         (jdbc/execute-one!
                           tx
                           (sql/format {:update :expenses
                                        :set updates*
                                        :where where
                                        :returning [:*]})
                           {:builder-fn rs/as-unqualified-lower-maps})
                         existing)]

           (when (contains? body :items)
             (let [existing-ids (->> (jdbc/execute!
                                       tx
                                       (sql/format {:select [:id]
                                                    :from [:expense_items]
                                                    :where [:= :expense_id id*]})
                                       {:builder-fn rs/as-unqualified-lower-maps})
                                  (map :id)
                                  (into #{}))
                   items* (mapv normalize-expense-item (or (:items body) []))
                   _ (when (empty? items*)
                       (throw (ex-info "At least one line item is required" {:status 400 :field :items})))

                   update-items (filterv (fn [{item-id :id}]
                                           (and item-id (contains? existing-ids item-id)))
                                  items*)
                   keep-ids (into #{} (map :id update-items))
                   insert-items (filterv (fn [{item-id :id}]
                                           (not (and item-id (contains? existing-ids item-id))))
                                  items*)
                   supplier-id (:supplier_id expense)
                   delete-where (if (seq keep-ids)
                                  [:and
                                   [:= :expense_id id*]
                                   [:not [:in :id keep-ids]]]
                                  [:= :expense_id id*])]

              ;; Delete removed items.
               (jdbc/execute!
                 tx
                 (sql/format {:delete-from :expense_items
                              :where delete-where}))

              ;; Update existing items (no retroactive auto-linking).
               (doseq [item update-items]
                 (require-keys! item [:line_total])
                 (let [alias (resolve-alias! tx supplier-id item)
                       item* (cond-> (select-keys item [:qty :unit :unit_price :line_total])
                               alias (assoc :alias_id (:id alias)))]
                   (jdbc/execute!
                     tx
                     (sql/format {:update :expense_items
                                  :set item*
                                  :where [:and
                                          [:= :id (:id item)]
                                          [:= :expense_id id*]]}))))

              ;; Insert new items (auto-link from alias when possible).
               (let [resolved-inserts (mapv (fn [item]
                                              (require-keys! item [:line_total])
                                              (let [alias (resolve-alias! tx supplier-id item)
                                                    resolved-article-id (:article_id alias)]
                                                (assoc item
                                                  :resolved_alias alias
                                                  :resolved_alias_id (some-> alias :id)
                                                  :resolved_article_id resolved-article-id)))
                                        insert-items)
                     exp-tenant-id (:tenant_id expense)
                     item-rows (mapv (fn [{:keys [resolved_alias_id qty unit unit_price line_total]}]
                                       (cond-> {:id (UUID/randomUUID)
                                                :expense_id id*
                                                :alias_id resolved_alias_id
                                                :qty qty
                                                :unit_price unit_price
                                                :line_total line_total}
                                         exp-tenant-id (assoc :tenant_id exp-tenant-id)
                                         unit (assoc :unit unit)))
                                 resolved-inserts)
                     _inserted-items (if (seq item-rows)
                                       (jdbc/execute!
                                         tx
                                         (sql/format {:insert-into :expense_items
                                                      :values item-rows
                                                      :returning [:*]})
                                         {:builder-fn rs/as-unqualified-lower-maps})
                                       [])])))

           (get-expense-with-items tx id*)))))))


