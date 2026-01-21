(ns app.domain.backend.expenses.services.expenses
  "Expense creation/update with line items."
  (:require
    [app.domain.backend.expenses.services.article-aliases :as aliases]
    [app.domain.backend.expenses.services.articles :as articles]
    [app.domain.backend.expenses.services.price-history :as price-history]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
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

(defn- parse-bool
  "Parse boolean value. Named parse-bool to avoid shadowing clojure.core/parse-boolean."
  [v]
  (let [v (blank->nil v)]
    (cond
      (nil? v) nil
      (instance? Boolean v) v
      (string? v) (Boolean/parseBoolean v)
      :else (boolean v))))

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

(defn- normalize-expense-item
  [item]
  (-> item
    (update-if-present :id #(parse-uuid! :id %))
    (update-if-present :alias_id #(parse-uuid! :alias_id %))
    (update-if-present :raw_label #(some-> % str str/trim))
    (update-if-present :qty #(parse-bigdec! :qty %))
    (update-if-present :unit_price #(parse-bigdec! :unit_price %))
    (update-if-present :line_total #(parse-bigdec! :line_total %))))

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
  ([tx supplier-id {:keys [raw_label alias_id]} {:keys [allow-auto-link?] :or {allow-auto-link? true}}]
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
     (let [alias (aliases/find-or-create-alias! tx supplier-id (str/trim (str raw_label)))]
       alias)

     :else nil)))

(defn- normalize-expense-data
  [expense-data]
  (-> expense-data
    (update-if-present :supplier_id #(parse-uuid! :supplier_id %))
    (update-if-present :payer_id #(parse-uuid! :payer_id %))
    (update-if-present :user_id #(parse-uuid! :user_id %))
    (update-if-present :receipt_id #(parse-uuid! :receipt_id %))
    (update-if-present :purchased_at #(parse-instant! :purchased_at %))
    (update-if-present :total_amount #(parse-bigdec! :total_amount %))
    (update-if-present :currency #(some-> % str str/trim blank->nil))
    (update-if-present :is_posted parse-bool)
    (update-if-present :notes blank->nil)))

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
                         :status [:raw "CASE WHEN status = 'posted'::receipt_status THEN 'extracted'::receipt_status ELSE status END"]
                         :updated_at [:now]}
                   :where [:and
                           [:= :id receipt_id]
                           [:= :expense_id id]]
                   :returning [:*]})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn delete-expense!
  "Hard delete expense.

  Expense items are removed via FK ON DELETE CASCADE.

  If the expense is linked to a receipt (via :receipt_id and the receipt's :expense_id),
  the receipt is reverted to an approvable status so it can be deleted or processed again."
  [db id]
  (jdbc/with-transaction [tx db]
    (when-let [expense (jdbc/execute-one!
                         tx
                         (sql/format {:select [:*]
                                      :from [:expenses]
                                      :where [:= :id id]
                                      :limit 1})
                         {:builder-fn rs/as-unqualified-lower-maps})]
      (revert-linked-receipt-after-expense-delete! tx expense)
      (jdbc/execute-one!
        tx
        (sql/format {:delete-from :expenses
                     :where [:= :id id]
                     :returning [:*]})
        {:builder-fn rs/as-unqualified-lower-maps}))))

(defn get-expense-with-items
  [db id]
  (let [expense (jdbc/execute-one!
                  db
                  (sql/format {:select [[:e.*]
                                        [:s.display_name :supplier_display_name]
                                        [:s.normalized_key :supplier_normalized_key]
                                        [:p.label :payer_label]
                                        [:pt.label :payer_type]]
                               :from [[:expenses :e]]
                               :left-join [[:suppliers :s] [:= :s.id :e.supplier_id]
                                           [:payers :p] [:= :p.id :e.payer_id]
                                           [:payer_types :pt] [:= :pt.id :p.payer_type_id]]
                                     :where [:= :e.id id]})
                  {:builder-fn rs/as-unqualified-lower-maps})
        items (jdbc/execute!
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
                {:builder-fn rs/as-unqualified-lower-maps})]
    (when expense
      (assoc expense :items items))))

(defn get-expense
  "Get an expense (with items). Wrapper expected by the generic admin routes factory."
  [db id]
  (get-expense-with-items db id))

(defn list-expenses
  "List expenses with common filters.
   opts: :from, :to, :supplier-id, :payer-id, :is-posted?, :limit, :offset."
  [db {:keys [from to supplier-id payer-id is-posted? limit offset order-dir]
       :or {limit 50 offset 0 order-dir :desc}}]
  (let [base-where (cond-> [:and]
                     from (conj [:>= :e.purchased_at from])
                     to (conj [:<= :e.purchased_at to])
                     supplier-id (conj [:= :e.supplier_id supplier-id])
                     payer-id (conj [:= :e.payer_id payer-id])
                     (some? is-posted?) (conj [:= :e.is_posted (boolean is-posted?)]))
        query {:select [[:e.*]
                        [:s.display_name :supplier_display_name]
                        [:s.normalized_key :supplier_normalized_key]
                        [:p.label :payer_label]
                        [:pt.label :payer_type]]
               :from [[:expenses :e]]
               :left-join [[:suppliers :s] [:= :s.id :e.supplier_id]
                           [:payers :p] [:= :p.id :e.payer_id]
                           [:payer_types :pt] [:= :pt.id :p.payer_type_id]]
               :where base-where
               :order-by [[:e.purchased_at order-dir]]
               :limit limit
               :offset offset}]
    (jdbc/execute! db (sql/format query) {:builder-fn rs/as-unqualified-lower-maps})))

(defn count-expenses
  "Count total expenses with optional filters."
  [db {:keys [from to supplier-id payer-id is-posted?]}]
  (let [base-where (cond-> [:and]
                     from (conj [:>= :purchased_at from])
                     to (conj [:<= :purchased_at to])
                     supplier-id (conj [:= :supplier_id supplier-id])
                     payer-id (conj [:= :payer_id payer-id])
                     (some? is-posted?) (conj [:= :is_posted (boolean is-posted?)]))
        query {:select [[[:count :*] :total]]
               :from [:expenses]
               :where base-where}]
    (:total (jdbc/execute-one! db (sql/format query)
              {:builder-fn rs/as-unqualified-lower-maps}))))

(defn create-expense!
  "Create an expense and its line items.

  Supports two arities:
  - (create-expense! db {:supplier_id ... :items [...] ...})
  - (create-expense! db expense-data items)

  Behavior:
  - Stores each line item's raw label via `article_aliases` when the label is valid.
  - Skips alias creation for blank/short/punctuation-only labels.
  - Records a price observation for each inserted item whose alias is mapped to an article."
  ([db {:keys [items] :as body}]
   (create-expense! db (dissoc body :items) items))
  ([db expense-data items]
   (let [expense-data* (normalize-expense-data expense-data)
         items* (mapv normalize-expense-item (or items []))]
     (require-keys! expense-data* [:supplier_id :payer_id :purchased_at :total_amount])
     (when (empty? items*)
       (throw (ex-info "At least one line item is required" {:status 400 :field :items})))

     (jdbc/with-transaction [tx db]
       (let [expense-id (UUID/randomUUID)
             expense-row (-> expense-data*
                           (select-keys [:supplier_id
                                         :payer_id
                                         :user_id
                                         :receipt_id
                                         :purchased_at
                                         :total_amount
                                         :currency
                                         :notes
                                         :is_posted])
                           (update-if-present :currency #(when % [:cast % :currency]))
                           (assoc :id expense-id))
             expense (jdbc/execute-one!
                       tx
                       (sql/format {:insert-into :expenses
                                    :values [expense-row]
                                    :returning [:*]})
                       {:builder-fn rs/as-unqualified-lower-maps})
             supplier-id (:supplier_id expense)

             resolved-items (mapv (fn [item]
                                    (require-keys! item [:line_total])
                                    (let [alias (resolve-alias! tx supplier-id item)
                                          resolved-article-id (:article_id alias)]
                                      (assoc item
                                        :resolved_alias alias
                                        :resolved_alias_id (some-> alias :id)
                                        :resolved_article_id resolved-article-id)))
                              items*)
             item-rows (mapv (fn [{:keys [resolved_alias_id qty unit_price line_total]}]
                               {:id (UUID/randomUUID)
                                :expense_id expense-id
                                :alias_id resolved_alias_id
                                :qty qty
                                :unit_price unit_price
                                :line_total line_total})
                         resolved-items)
             inserted-items (jdbc/execute!
                              tx
                              (sql/format {:insert-into :expense_items
                                           :values item-rows
                                           :returning [:*]})
                              {:builder-fn rs/as-unqualified-lower-maps})]

         (doseq [[item resolved] (map vector inserted-items resolved-items)]
           (let [article-id (:resolved_article_id resolved)]
             (when (and (:supplier_id expense) article-id)
               (price-history/record-observation!
                 tx {:article_id article-id
                     :supplier_id (:supplier_id expense)
                     :expense_item_id (:id item)
                     :qty (:qty item)
                     :unit_price (:unit_price item)
                     :line_total (:line_total item)
                     :currency (:currency expense)
                     :observed_at (:purchased_at expense)}))))

         (get-expense-with-items tx expense-id))))))

(defn update-expense!
  "Update an expense and optionally upsert its items.

  If `:items` is present in the update body, the provided set becomes the new
  active set:
  - missing items are deleted
  - items with matching `:id` are updated
  - items without `:id` are inserted

  Auto-linking from aliases is applied only for newly inserted items (existing
  items are not retroactively auto-linked unless the alias is explicitly changed)."
  [db id body]
  (let [id* (parse-uuid! :id id)
        updates* (-> body
                   (dissoc :items)
                   normalize-expense-data
                   (select-keys [:supplier_id :payer_id :purchased_at :total_amount :currency :notes :is_posted])
                   (update-if-present :currency #(when % [:cast % :currency]))
                   (assoc :updated_at [:now]))]
    (jdbc/with-transaction [tx db]
      (when-let [expense (jdbc/execute-one!
                             tx
                             (sql/format {:update :expenses
                              :set updates*
                              :where [:= :id id*]
                              :returning [:*]})
                           {:builder-fn rs/as-unqualified-lower-maps})]

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
                    item* (cond-> (select-keys item [:qty :unit_price :line_total])
                            alias (assoc :alias_id (:id alias)))]
                (jdbc/execute!
                  tx
                    (sql/format {:update :expense_items
                           :set item*
                           :where [:and
                             [:= :id (:id item)]
                             [:= :expense_id id*]]}))

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
                  item-rows (mapv (fn [{:keys [resolved_alias_id qty unit_price line_total]}]
                                    {:id (UUID/randomUUID)
                                     :expense_id id*
                                     :alias_id resolved_alias_id
                                     :qty qty
                                     :unit_price unit_price
                                     :line_total line_total})
                              resolved-inserts)
                  inserted-items (if (seq item-rows)
                                   (jdbc/execute!
                                     tx
                                     (sql/format {:insert-into :expense_items
                                                  :values item-rows
                                                  :returning [:*]})
                                     {:builder-fn rs/as-unqualified-lower-maps})
                                   [])]

              ;; Record price observations for newly inserted items with an article.
              (doseq [[item resolved] (map vector inserted-items resolved-inserts)]
                (let [article-id (:resolved_article_id resolved)]
                  (when (and (:supplier_id expense) article-id)
                    (price-history/record-observation!
                      tx {:article_id article-id
                          :supplier_id (:supplier_id expense)
                          :expense_item_id (:id item)
                          :qty (:qty item)
                          :unit_price (:unit_price item)
                          :line_total (:line_total item)
                          :currency (:currency expense)
                          :observed_at (:purchased_at expense)})))))))

        (get-expense-with-items tx id*)))))

(defn create-from-receipt!
  "Create an expense tied to a receipt. Delegates to create-expense! then returns expense."
  [db receipt-id expense-data items]
  (create-expense! db (assoc expense-data :receipt_id receipt-id) items))))
