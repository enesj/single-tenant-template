(ns app.domain.backend.expenses.handlers.search
  "Cross-entity search handlers.

  User search is tenant-scoped; admin search is global.
  Both fan out across all relevant tables in parallel via pmap."
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [taoensso.timbre :as log]))

;; ---------------------------------------------------------------------------
;; Search helpers
;; ---------------------------------------------------------------------------

(defn- pattern [term] (str "%" term "%"))

(def ^:private ^:const fuzzy-threshold
  "Minimum word_similarity score for fuzzy matching (0.0–1.0).
   0.5 balances typo tolerance against noise — ILIKE still catches exact substrings."
  0.5)

(defn- fuzzy-text-where
  "Build a WHERE clause combining ILIKE substring match and pg_trgm fuzzy match.
   Matches if any column contains the term as a substring (ILIKE)
   OR has word_similarity above the threshold."
  [columns term pattern]
  (into [:or]
    (mapcat (fn [col]
              [[:ilike col pattern]
               [:> [:word_similarity term col] fuzzy-threshold]])
      columns)))

(defn- relevance-score-expr
  "HoneySQL expression: best word_similarity across `columns` for `term`.
   Used in SELECT + ORDER BY to rank results by relevance."
  [term columns]
  (if (= 1 (count columns))
    [:word_similarity term (first columns)]
    (into [:greatest] (map #(vector :word_similarity term %) columns))))

(defn- search-entity
  "Run `entity-fn` safely, returning [] on any exception."
  [entity-fn]
  (try (entity-fn) (catch Exception e (log/warn "Search entity failed" {:error (.getMessage e)}) [])))

(defn- parse-search-limit
  [request default-limit]
  (let [raw (h/get-param (:query-params request) :limit)
        parsed (some-> raw str parse-long)]
    (-> (or parsed default-limit)
      long
      (max 1)
      (min 25))))

(defn- parse-entity-type
  [request]
  (some-> (h/get-param (:query-params request) :type)
    str
    str/trim
    str/lower-case
    not-empty))

(defn- parse-query
  [request]
  (some-> (h/get-param (:query-params request) :q)
    str
    str/trim
    not-empty))

;; ---------------------------------------------------------------------------
;; Per-entity search queries
;; ---------------------------------------------------------------------------

(defn- search-expenses
  [db term limit tenant-id]
  (let [p (pattern term)
        text-where (fuzzy-text-where [:s.display_name :p.label :e.notes] term p)
        where (if tenant-id
                [:and text-where [:= :e.tenant_id tenant-id]]
                text-where)]
    (jdbc/execute!
      db
      (sql/format {:select [[:e.id :id]
                            [:e.total_amount :total_amount]
                            [:e.currency :currency]
                            [:e.purchased_at :purchased_at]
                            [:e.is_posted :is_posted]
                            [:e.notes :notes]
                            [:s.display_name :supplier_display_name]
                            [:p.label :payer_label]]
                   :from [[:expenses :e]]
                   :left-join [[:suppliers :s] [:= :s.id :e.supplier_id]
                               [:payers :p] [:= :p.id :e.payer_id]]
                   :where where
                   :order-by [[:e.purchased_at :desc]]
                   :limit limit})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- search-receipts
  [db term limit tenant-id]
  (let [p (pattern term)
        text-where (fuzzy-text-where [:original_filename :supplier_guess :store_guess] term p)
        where (if tenant-id
                [:and text-where [:= :tenant_id tenant-id]]
                text-where)]
    (jdbc/execute!
      db
      (sql/format {:select [:id :original_filename :supplier_guess :store_guess :status :created_at]
                   :from [:receipts]
                   :where where
                   :order-by [[:created_at :desc]]
                   :limit limit})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- search-payers
  [db term limit tenant-id]
  (let [p (pattern term)
        text-where (fuzzy-text-where [:label] term p)
        where (if tenant-id
                [:and text-where [:= :tenant_id tenant-id]]
                text-where)]
    (jdbc/execute!
      db
      (sql/format {:select [:id :label :payer_type_id]
                   :from [:payers]
                   :where where
                   :order-by [[:label :asc]]
                   :limit limit})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- search-expense-cats
  [db term limit tenant-id]
  (let [p (pattern term)
        text-where (fuzzy-text-where [:name] term p)
        where (if tenant-id
                [:and text-where [:= :tenant_id tenant-id]]
                text-where)]
    (jdbc/execute!
      db
      (sql/format {:select [:id :name]
                   :from [:expense_categories]
                   :where where
                   :order-by [[:name :asc]]
                   :limit limit})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- search-suppliers
  [db term limit tenant-id]
  (let [p (pattern term)]
    (jdbc/execute!
      db
      (sql/format
        (if tenant-id
          {:select [[:s.id :id]
                    [:s.display_name :display_name]
                    [:s.normalized_key :normalized_key]
                    [:s.address :address]]
           :from [[:suppliers :s]]
           :where [:and
                   (fuzzy-text-where [:s.display_name :s.address] term p)
                   [:or
                    [:exists {:select [1]
                              :from [[:expenses :e]]
                              :where [:and
                                      [:= :e.tenant_id tenant-id]
                                      [:= :e.supplier_id :s.id]]}]
                    [:exists {:select [1]
                              :from [[:receipts :r]]
                              :join [[:supplier_aliases :sa] [:= :sa.id :r.supplier_alias_id]]
                              :where [:and
                                      [:= :r.tenant_id tenant-id]
                                      [:= :sa.supplier_id :s.id]]}]
                    [:exists {:select [1]
                              :from [[:receipts :r]]
                              :join [[:store_aliases :sta] [:= :sta.id :r.store_alias_id]
                                     [:stores :st_receipt] [:= :st_receipt.id :sta.store_id]]
                              :where [:and
                                      [:= :r.tenant_id tenant-id]
                                      [:= :st_receipt.supplier_id :s.id]]}]]]
           :order-by [[:s.display_name :asc]]
           :limit limit}
          {:select [:id :display_name :normalized_key :address]
           :from [:suppliers]
           :where (fuzzy-text-where [:display_name :address] term p)
           :order-by [[:display_name :asc]]
           :limit limit}))
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- search-stores
  [db term limit tenant-id]
  (let [p (pattern term)]
    (jdbc/execute!
      db
      (sql/format
        (if tenant-id
          {:select [[:st.id :id]
                    [:st.display_name :display_name]
                    [:st.normalized_key :normalized_key]
                    [:st.address :address]]
           :from [[:stores :st]]
           :where [:and
                   (fuzzy-text-where [:st.display_name :st.address] term p)
                   [:or
                    [:exists {:select [1]
                              :from [[:expenses :e]]
                              :where [:and
                                      [:= :e.tenant_id tenant-id]
                                      [:= :e.store_id :st.id]]}]
                    [:exists {:select [1]
                              :from [[:receipts :r]]
                              :join [[:store_aliases :sta] [:= :sta.id :r.store_alias_id]]
                              :where [:and
                                      [:= :r.tenant_id tenant-id]
                                      [:= :sta.store_id :st.id]]}]]]
           :order-by [[:st.display_name :asc]]
           :limit limit}
          {:select [:id :display_name :normalized_key :address]
           :from [:stores]
           :where (fuzzy-text-where [:display_name :address] term p)
           :order-by [[:display_name :asc]]
           :limit limit}))
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- search-articles
  [db term limit tenant-id]
  (let [p (pattern term)]
    (jdbc/execute!
      db
      (sql/format
        (if tenant-id
          {:select [[:a.id :id]
                    [:a.canonical_name :canonical_name]]
           :from [[:articles :a]]
           :where [:and
                   (fuzzy-text-where [:a.canonical_name] term p)
                   [:or
                    [:exists {:select [1]
                              :from [[:expense_items :ei]]
                              :where [:and
                                      [:= :ei.tenant_id tenant-id]
                                      [:= :ei.article_id :a.id]]}]
                    [:exists {:select [1]
                              :from [[:expense_items :ei]]
                              :join [[:article_aliases :aa] [:= :aa.id :ei.alias_id]]
                              :where [:and
                                      [:= :ei.tenant_id tenant-id]
                                      [:= :aa.article_id :a.id]]}]]]
           :order-by [[:a.canonical_name :asc]]
           :limit limit}
          {:select [:id :canonical_name]
           :from [:articles]
           :where (fuzzy-text-where [:canonical_name] term p)
           :order-by [[:canonical_name :asc]]
           :limit limit}))
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- search-categories
  [db term limit tenant-id]
  (let [p (pattern term)]
    (jdbc/execute!
      db
      (sql/format
        (if tenant-id
          {:select [[:c.id :id]
                    [:c.name :name]
                    [:c.description :description]]
           :from [[:categories :c]]
           :where [:and
                   (fuzzy-text-where [:c.name] term p)
                   [:or
                    [:exists {:select [1]
                              :from [[:expense_items :ei]]
                              :join [[:articles :a_item] [:= :a_item.id :ei.article_id]
                                     [:subcategories :sub_item] [:= :sub_item.id :a_item.subcategory_id]]
                              :where [:and
                                      [:= :ei.tenant_id tenant-id]
                                      [:= :sub_item.category_id :c.id]]}]
                    [:exists {:select [1]
                              :from [[:expense_items :ei]]
                              :join [[:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                     [:articles :a_alias] [:= :a_alias.id :aa.article_id]
                                     [:subcategories :sub_alias] [:= :sub_alias.id :a_alias.subcategory_id]]
                              :where [:and
                                      [:= :ei.tenant_id tenant-id]
                                      [:= :sub_alias.category_id :c.id]]}]]]
           :order-by [[:c.name :asc]]
           :limit limit}
          {:select [:id :name :description]
           :from [:categories]
           :where (fuzzy-text-where [:name] term p)
           :order-by [[:name :asc]]
           :limit limit}))
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- search-subcategories
  [db term limit tenant-id]
  (let [p (pattern term)]
    (jdbc/execute!
      db
      (sql/format
        (if tenant-id
          {:select [[:sub.id :id]
                    [:sub.name :name]
                    [:c.name :category_name]]
           :from [[:subcategories :sub]]
           :join [[:categories :c] [:= :c.id :sub.category_id]]
           :where [:and
                   (fuzzy-text-where [:sub.name] term p)
                   [:or
                    [:exists {:select [1]
                              :from [[:expense_items :ei]]
                              :join [[:articles :a_item] [:= :a_item.id :ei.article_id]]
                              :where [:and
                                      [:= :ei.tenant_id tenant-id]
                                      [:= :a_item.subcategory_id :sub.id]]}]
                    [:exists {:select [1]
                              :from [[:expense_items :ei]]
                              :join [[:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                     [:articles :a_alias] [:= :a_alias.id :aa.article_id]]
                              :where [:and
                                      [:= :ei.tenant_id tenant-id]
                                      [:= :a_alias.subcategory_id :sub.id]]}]]]
           :order-by [[:c.name :asc] [:sub.name :asc]]
           :limit limit}
          {:select [[:s.id :id] [:s.name :name] [:c.name :category_name]]
           :from [[:subcategories :s]]
           :join [[:categories :c] [:= :c.id :s.category_id]]
           :where (fuzzy-text-where [:s.name] term p)
           :order-by [[:c.name :asc] [:s.name :asc]]
           :limit limit}))
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- search-manufacturers
  [db term limit tenant-id]
  (let [p (pattern term)]
    (jdbc/execute!
      db
      (sql/format
        (if tenant-id
          {:select [[:m.id :id]
                    [:m.display_name :display_name]
                    [:m.normalized_key :normalized_key]]
           :from [[:manufacturers :m]]
           :where [:and
                   (fuzzy-text-where [:m.display_name :m.normalized_key] term p)
                   [:or
                    [:exists {:select [1]
                              :from [[:expense_items :ei]]
                              :join [[:articles :a_item] [:= :a_item.id :ei.article_id]]
                              :where [:and
                                      [:= :ei.tenant_id tenant-id]
                                      [:= :a_item.manufacturer_id :m.id]]}]
                    [:exists {:select [1]
                              :from [[:expense_items :ei]]
                              :join [[:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                     [:articles :a_alias] [:= :a_alias.id :aa.article_id]]
                              :where [:and
                                      [:= :ei.tenant_id tenant-id]
                                      [:= :a_alias.manufacturer_id :m.id]]}]]]
           :order-by [[:m.display_name :asc]]
           :limit limit}
          {:select [:id :display_name :normalized_key]
           :from [:manufacturers]
           :where (fuzzy-text-where [:display_name :normalized_key] term p)
           :order-by [[:display_name :asc]]
           :limit limit}))
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- search-cities
  [db term limit tenant-id]
  (let [p (pattern term)]
    (jdbc/execute!
      db
      (sql/format
        (if tenant-id
          {:select [[:city.id :id]
                    [:city.name :name]
                    [:city.zip :zip]
                    [:city.country :country]]
           :from [[:cities :city]]
           :where [:and
                   (fuzzy-text-where [:city.name :city.zip] term p)
                   [:or
                    [:exists {:select [1]
                              :from [[:expenses :e]]
                              :join [[:stores :st_exp] [:= :st_exp.id :e.store_id]]
                              :where [:and
                                      [:= :e.tenant_id tenant-id]
                                      [:= :st_exp.city_id :city.id]]}]
                    [:exists {:select [1]
                              :from [[:receipts :r]]
                              :join [[:store_aliases :sta] [:= :sta.id :r.store_alias_id]
                                     [:stores :st_receipt] [:= :st_receipt.id :sta.store_id]]
                              :where [:and
                                      [:= :r.tenant_id tenant-id]
                                      [:= :st_receipt.city_id :city.id]]}]]]
           :order-by [[:city.name :asc]]
           :limit limit}
          {:select-distinct [[:c.id :id] [:c.name :name] [:c.zip :zip] [:c.country :country]]
           :from [[:cities :c]]
           :join [[:stores :st] [:= :st.city_id :c.id]
                  [:expenses :e] [:= :e.store_id :st.id]]
           :where (fuzzy-text-where [:c.name :c.zip] term p)
           :order-by [[:c.name :asc]]
           :limit limit}))
      {:builder-fn rs/as-unqualified-lower-maps})))

;; ---------------------------------------------------------------------------
;; Handler factories
;; ---------------------------------------------------------------------------

(defn user-search-handler
  "Handler for user search — results are scoped to the user's tenant."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/expenses-read-roles "Role required")]
        forbidden
        (let [tenant-id (h/get-tenant-id request)
              q (get-in request [:query-params "q"])
              limit 5]
          (if (and q (>= (count q) 2))
            (try
              (let [results (into {}
                              (pmap
                                (fn [[k f]] [k (search-entity f)])
                                {:payers         #(search-payers db q limit tenant-id)
                                 :expense-cats   #(search-expense-cats db q limit tenant-id)
                                 :suppliers      #(search-suppliers db q limit tenant-id)
                                 :stores         #(search-stores db q limit tenant-id)
                                 :articles       #(search-articles db q limit tenant-id)
                                 :categories     #(search-categories db q limit tenant-id)
                                 :subcategories  #(search-subcategories db q limit tenant-id)
                                 :manufacturers  #(search-manufacturers db q limit tenant-id)
                                 :cities         #(search-cities db q limit tenant-id)}))]
                (h/json-response {:results results}))
              (catch Exception e
                (log/error e "Error executing user search" {:q q :tenant-id tenant-id})
                (h/json-response {:error "Search failed"} 500)))
            (h/json-response {:results {}}))))
      (h/unauthorized-response))))

(defn admin-search-handler
  "Handler for admin search — results are global (no tenant filter)."
  [db]
  (fn [request]
    (let [q (get-in request [:query-params "q"])
          limit 5]
      (if (and q (>= (count q) 2))
        (try
          (let [results (into {}
                          (pmap
                            (fn [[k f]] [k (search-entity f)])
                            {:payers         #(search-payers db q limit nil)
                             :expense-cats   #(search-expense-cats db q limit nil)
                             :suppliers      #(search-suppliers db q limit nil)
                             :stores         #(search-stores db q limit nil)
                             :articles       #(search-articles db q limit nil)
                             :categories     #(search-categories db q limit nil)
                             :subcategories  #(search-subcategories db q limit nil)
                             :manufacturers  #(search-manufacturers db q limit nil)
                             :cities         #(search-cities db q limit nil)}))]
            (h/json-response {:results results}))
          (catch Exception e
            (log/error e "Error executing admin search" {:q q})
            (h/json-response {:error "Search failed"} 500)))
        (h/json-response {:results {}})))))

;; ---------------------------------------------------------------------------
;; Quick-search — lightweight scored search for the smart expense form
;; ---------------------------------------------------------------------------

(defn- quick-search-suppliers
  "Global supplier search with word_similarity score."
  [db term limit]
  (let [p (pattern term)]
    (jdbc/execute!
      db
      (sql/format {:select [:id
                            [:display_name :label]
                            [[:greatest
                              [:word_similarity term :display_name]
                              [:word_similarity term [:coalesce :address [:inline ""]]]]
                             :score]]
                   :from [:suppliers]
                   :where (fuzzy-text-where [:display_name :address] term p)
                   :order-by [[:score :desc] [:display_name :asc]]
                   :limit limit})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- quick-search-stores
  "Global store search with score. Includes supplier info for auto-fill."
  [db term limit]
  (let [p (pattern term)]
    (jdbc/execute!
      db
      (sql/format {:select [[:st.id :id]
                            [:st.display_name :label]
                            [:st.supplier_id :supplier_id]
                            [:s.display_name :supplier_display_name]
                            [[:greatest
                              [:word_similarity term :st.display_name]
                              [:word_similarity term [:coalesce :st.address [:inline ""]]]]
                             :score]]
                   :from [[:stores :st]]
                   :left-join [[:suppliers :s] [:= :s.id :st.supplier_id]]
                   :where (fuzzy-text-where [:st.display_name :st.address] term p)
                   :order-by [[:score :desc] [:st.display_name :asc]]
                   :limit limit})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- quick-search-articles
  "Global article search with score."
  [db term limit]
  (let [p (pattern term)]
    (jdbc/execute!
      db
      (sql/format {:select [:id
                            [:canonical_name :label]
                            [[:word_similarity term :canonical_name] :score]]
                   :from [:articles]
                   :where (fuzzy-text-where [:canonical_name] term p)
                   :order-by [[:score :desc] [:canonical_name :asc]]
                   :limit limit})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- quick-search-expense-cats
  "Tenant-scoped expense category search with score."
  [db term limit tenant-id]
  (let [p (pattern term)
        text-where (fuzzy-text-where [:name] term p)
        where (if tenant-id
                [:and text-where [:= :tenant_id tenant-id]]
                text-where)]
    (jdbc/execute!
      db
      (sql/format {:select [:id
                            [:name :label]
                            [[:word_similarity term :name] :score]]
                   :from [:expense_categories]
                   :where where
                   :order-by [[:score :desc] [:name :asc]]
                   :limit limit})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn quick-search-handler
  "Lightweight global catalog search for the smart expense form.
   Searches suppliers, stores, articles globally and expense_categories by tenant.
   Returns a flat scored list for cross-type ranking."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/expenses-read-roles "Role required")]
        forbidden
        (let [tenant-id (h/get-tenant-id request)
              q (get-in request [:query-params "q"])
              limit 5]
          (if (and q (>= (count q) 2))
            (try
              (let [raw (into {}
                          (pmap
                            (fn [[k f]] [k (search-entity f)])
                            {:supplier  #(quick-search-suppliers db q limit)
                             :store     #(quick-search-stores db q limit)
                             :article   #(quick-search-articles db q limit)
                             :category  #(quick-search-expense-cats db q limit tenant-id)}))
                    tagged (mapcat
                             (fn [[entity-type results]]
                               (map #(assoc % :entity_type (name entity-type)) results))
                             raw)
                    sorted (->> tagged
                             (sort-by :score >)
                             (take 12)
                             vec)]
                (h/json-response {:results sorted}))
              (catch Exception e
                (log/error e "Error executing quick search" {:q q})
                (h/json-response {:error "Search failed"} 500)))
            (h/json-response {:results []}))))
      (h/unauthorized-response))))

;; ---------------------------------------------------------------------------
;; Quick Add search — dedicated filtered search for /expenses/new
;; ---------------------------------------------------------------------------

(defn- quick-add-search-suppliers
  [db term limit]
  (let [p (pattern term)
        rel (relevance-score-expr term [:display_name :address])]
    (jdbc/execute!
      db
      (sql/format {:select [:id
                            [:display_name :label]
                            [rel :relevance]]
                   :from [:suppliers]
                   :where (fuzzy-text-where [:display_name :address] term p)
                   :order-by [[rel :desc] [:display_name :asc]]
                   :limit limit})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- quick-add-search-stores
  [db term limit supplier-id]
  (let [p (pattern term)
        rel (relevance-score-expr term [:st.display_name :st.address :s.display_name])
        base-where [:and
                    [:is-not :st.supplier_id nil]
                    (fuzzy-text-where [:st.display_name :st.address :s.display_name] term p)]
        where (cond-> base-where
                supplier-id (conj [:= :st.supplier_id supplier-id]))]
    (jdbc/execute!
      db
      (sql/format {:select [[:st.id :id]
                            [:st.display_name :label]
                            [:st.supplier_id :supplier_id]
                            [:s.display_name :supplier_display_name]
                            [rel :relevance]]
                   :from [[:stores :st]]
                   :join [[:suppliers :s] [:= :s.id :st.supplier_id]]
                   :where where
                   :order-by [[rel :desc] [:st.display_name :asc]]
                   :limit limit})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- quick-add-search-expense-categories
  [db term limit tenant-id]
  (let [p (pattern term)
        rel (relevance-score-expr term [:name])
        text-where (fuzzy-text-where [:name] term p)
        where (if tenant-id
                [:and text-where [:= :tenant_id tenant-id]]
                text-where)]
    (jdbc/execute!
      db
      (sql/format {:select [:id
                            [:name :label]
                            [rel :relevance]]
                   :from [:expense_categories]
                   :where where
                   :order-by [[rel :desc] [:name :asc]]
                   :limit limit})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- quick-add-article-last-prices
  [db article-ids tenant-id supplier-id]
  (if (and tenant-id (seq article-ids))
    (jdbc/execute!
      db
      (sql/format {:select [:article_id :unit_price :supplier_display_name]
                   :from [[{:select [[:aa.article_id :article_id]
                                     [:ei.unit_price :unit_price]
                                     [:sup.display_name :supplier_display_name]
                                     [[:over [[:row_number]
                                              {:partition-by :aa.article_id
                                               :order-by [[:e.purchased_at :desc]
                                                          [:ei.created_at :desc]
                                                          [:ei.id :desc]]}]]
                                      :rn]]
                            :from [[:expense_items :ei]]
                            :join [[:expenses :e] [:= :e.id :ei.expense_id]
                                   [:article_aliases :aa] [:= :aa.id :ei.alias_id]]
                            :left-join [[:suppliers :sup] [:= :sup.id :e.supplier_id]]
                            :where (cond-> [:and
                                            [:= :e.tenant_id tenant-id]
                                            [:in :aa.article_id article-ids]]
                                     supplier-id (conj [:= :e.supplier_id supplier-id]))}
                           :recent_article_prices]]
                   :where [:= :rn 1]})
      {:builder-fn rs/as-unqualified-lower-maps})
    []))

(defn- quick-add-search-articles
  [db term limit tenant-id supplier-id]
  (let [p (pattern term)
        rel (relevance-score-expr term [:canonical_name])
        articles (jdbc/execute!
                   db
                   (sql/format {:select [:id
                                         [:canonical_name :label]
                                         [rel :relevance]]
                                :from [:articles]
                                :where (fuzzy-text-where [:canonical_name] term p)
                                :order-by [[rel :desc] [:canonical_name :asc]]
                                :limit limit})
                   {:builder-fn rs/as-unqualified-lower-maps})
        article-ids (mapv :id articles)
        supplier-price-map (reduce (fn [acc {:keys [article_id] :as row}]
                                     (assoc acc article_id row))
                             {}
                             (if supplier-id
                               (quick-add-article-last-prices db article-ids tenant-id supplier-id)
                               []))
        global-price-map (reduce (fn [acc {:keys [article_id] :as row}]
                                   (assoc acc article_id row))
                           {}
                           (quick-add-article-last-prices db article-ids tenant-id nil))]
    (mapv (fn [article]
            (let [supplier-price (get supplier-price-map (:id article))
                  global-price (get global-price-map (:id article))
                  chosen-price (or supplier-price global-price)
                  price-source (cond
                                 supplier-price "supplier"
                                 global-price "global"
                                 :else nil)]
              (cond-> article
                chosen-price (assoc :last_price (:unit_price chosen-price)
                               :last_price_source price-source
                               :last_price_supplier_display_name (:supplier_display_name chosen-price)))))
      articles)))

(defn- cooccurring-articles
  "Find articles that historically appear on the same expense as `article-ids`.
   Returns up to `limit` articles ranked by co-occurrence frequency, with last-price data."
  [db article-ids limit tenant-id supplier-id]
  (if (and tenant-id (seq article-ids))
    (let [base-articles
          (jdbc/execute!
            db
            (sql/format
              {:select [[:a.id :id]
                        [:a.canonical_name :label]
                        [[:count [:distinct :ei.expense_id]] :co_count]]
               :from [[:expense_items :ei]]
               :join [[:article_aliases :aa] [:= :aa.id :ei.alias_id]
                      [:articles :a] [:= :a.id :aa.article_id]]
               :where [:and
                       [:= :ei.tenant_id tenant-id]
                       [:in :ei.expense_id
                        {:select [[:ei2.expense_id]]
                         :from [[:expense_items :ei2]]
                         :join [[:article_aliases :aa2] [:= :aa2.id :ei2.alias_id]]
                         :where [:and
                                 [:= :ei2.tenant_id tenant-id]
                                 [:in :aa2.article_id article-ids]]}]
                       [:not-in :aa.article_id article-ids]]
               :group-by [:a.id :a.canonical_name]
               :order-by [[:co_count :desc] [:a.canonical_name :asc]]
               :limit limit})
            {:builder-fn rs/as-unqualified-lower-maps})
          result-ids (mapv :id base-articles)
          supplier-price-map (reduce (fn [acc {:keys [article_id] :as row}]
                                       (assoc acc article_id row))
                               {}
                               (if supplier-id
                                 (quick-add-article-last-prices db result-ids tenant-id supplier-id)
                                 []))
          global-price-map (reduce (fn [acc {:keys [article_id] :as row}]
                                     (assoc acc article_id row))
                             {}
                             (quick-add-article-last-prices db result-ids tenant-id nil))]
      (mapv (fn [article]
              (let [supplier-price (get supplier-price-map (:id article))
                    global-price (get global-price-map (:id article))
                    chosen-price (or supplier-price global-price)
                    price-source (cond
                                   supplier-price "supplier"
                                   global-price "global"
                                   :else nil)]
                (-> (dissoc article :co_count)
                  (assoc :entity_type "article")
                  (cond->
                    chosen-price (assoc :last_price (:unit_price chosen-price)
                                   :last_price_source price-source
                                   :last_price_supplier_display_name (:supplier_display_name chosen-price))))))
        base-articles))
    []))

(defn quick-add-search-handler
  "Dedicated filtered search for Quick Add expense context and article inputs.

  Query params:
  - type: all | supplier | store | category | article
  - q: search string
  - limit: optional (default 8, max 25)
  - supplier_id: optional store filter; when provided, stores are limited to that supplier

  Quick Add never returns stores without a supplier_id."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/expenses-read-roles "Role required")]
        forbidden
        (let [tenant-id (h/get-tenant-id request)
              entity-type (or (parse-entity-type request) "all")
              q (parse-query request)
              limit (parse-search-limit request 8)
              supplier-id-raw (h/get-param (:query-params request) :supplier_id)
              supplier-id (some-> supplier-id-raw h/try-parse-uuid)]
          (cond
            (not (contains? #{"all" "supplier" "store" "category" "article"} entity-type))
            (h/json-response {:error "Invalid quick add search type"} 400)

            (and (some? supplier-id-raw) (not (str/blank? (str supplier-id-raw))) (nil? supplier-id))
            (h/json-response {:error "Invalid supplier_id"} 400)

            (or (nil? q) (< (count q) 2))
            (h/json-response {:results []})

            :else
            (try
              (let [results (if (= entity-type "all")
                              (->> [{:entity_type "supplier"
                                     :results (quick-add-search-suppliers db q limit)}
                                    {:entity_type "store"
                                     :results (quick-add-search-stores db q limit supplier-id)}
                                    {:entity_type "category"
                                     :results (quick-add-search-expense-categories db q limit tenant-id)}
                                    {:entity_type "article"
                                     :results (quick-add-search-articles db q limit tenant-id supplier-id)}]
                                (mapcat (fn [{:keys [entity_type results]}]
                                          (map #(assoc % :entity_type entity_type) results)))
                                (sort-by (fn [{:keys [relevance entity_type]}]
                                          ;; Primary: relevance desc. Secondary: article > category > store > supplier.
                                           [(- (or relevance 0))
                                            (case entity_type
                                              "article"  0
                                              "category" 1
                                              "store"    2
                                              "supplier" 3
                                              4)]))
                                vec)
                              (vec (case entity-type
                                     "supplier" (quick-add-search-suppliers db q limit)
                                     "store" (quick-add-search-stores db q limit supplier-id)
                                     "category" (quick-add-search-expense-categories db q limit tenant-id)
                                     "article" (quick-add-search-articles db q limit tenant-id supplier-id))))]
                (h/json-response {:results results}))
              (catch Exception e
                (log/error e "Error executing quick add search"
                  {:q q
                   :type entity-type
                   :supplier-id supplier-id})
                (h/json-response {:error "Quick add search failed"} 500))))))
      (h/unauthorized-response))))

(defn cooccurring-articles-handler
  "Returns articles that frequently co-occur on the same expense as the given article IDs.

  Query params:
  - article_ids: comma-separated UUIDs of currently selected articles
  - limit: optional (default 5, max 10)
  - supplier_id: optional, for supplier-specific last-price lookup"
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/expenses-read-roles "Role required")]
        forbidden
        (let [tenant-id (h/get-tenant-id request)
              raw-ids (h/get-param (:query-params request) :article_ids)
              article-ids (->> (str/split (str raw-ids) #",")
                            (keep #(some-> (str/trim %) not-empty h/try-parse-uuid))
                            vec)
              limit (min (or (some-> (h/get-param (:query-params request) :limit)
                               parse-long) 5) 10)
              supplier-id-raw (h/get-param (:query-params request) :supplier_id)
              supplier-id (some-> supplier-id-raw h/try-parse-uuid)]
          (if (empty? article-ids)
            (h/json-response {:results []})
            (try
              (let [results (cooccurring-articles db article-ids limit tenant-id supplier-id)]
                (h/json-response {:results results}))
              (catch Exception e
                (log/error e "Error fetching co-occurring articles" {:article-ids article-ids})
                (h/json-response {:error "Co-occurrence search failed"} 500))))))
      (h/unauthorized-response))))

(defn- context-suggestions-suppliers
  "Suppliers historically associated with the given articles, ranked by frequency."
  [db article-ids tenant-id limit]
  (jdbc/execute!
    db
    (sql/format
      {:select [[:s.id :id]
                [:s.display_name :label]
                [[:count [:distinct :e.id]] :frequency]]
       :from [[:expenses :e]]
       :join [[:expense_items :ei] [:= :ei.expense_id :e.id]
              [:article_aliases :aa] [:= :aa.id :ei.alias_id]
              [:suppliers :s] [:= :s.id :e.supplier_id]]
       :where [:and
               [:= :e.tenant_id tenant-id]
               [:in :aa.article_id article-ids]
               [:is-not :e.supplier_id nil]]
       :group-by [:s.id :s.display_name]
       :order-by [[:frequency :desc] [:s.display_name :asc]]
       :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- context-suggestions-stores
  "Stores historically associated with the given articles, ranked by frequency."
  [db article-ids tenant-id limit]
  (jdbc/execute!
    db
    (sql/format
      {:select [[:st.id :id]
                [:st.display_name :label]
                [:st.supplier_id :supplier_id]
                [:sup.display_name :supplier_display_name]
                [[:count [:distinct :e.id]] :frequency]]
       :from [[:expenses :e]]
       :join [[:expense_items :ei] [:= :ei.expense_id :e.id]
              [:article_aliases :aa] [:= :aa.id :ei.alias_id]
              [:stores :st] [:= :st.id :e.store_id]
              [:suppliers :sup] [:= :sup.id :st.supplier_id]]
       :where [:and
               [:= :e.tenant_id tenant-id]
               [:in :aa.article_id article-ids]
               [:is-not :e.store_id nil]]
       :group-by [:st.id :st.display_name :st.supplier_id :sup.display_name]
       :order-by [[:frequency :desc] [:st.display_name :asc]]
       :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- context-suggestions-categories
  "Expense categories historically associated with the given articles, ranked by frequency."
  [db article-ids tenant-id limit]
  (jdbc/execute!
    db
    (sql/format
      {:select [[:ec.id :id]
                [:ec.name :label]
                [[:count [:distinct :e.id]] :frequency]]
       :from [[:expenses :e]]
       :join [[:expense_items :ei] [:= :ei.expense_id :e.id]
              [:article_aliases :aa] [:= :aa.id :ei.alias_id]
              [:expense_categories :ec] [:= :ec.id :e.expense_category_id]]
       :where [:and
               [:= :e.tenant_id tenant-id]
               [:in :aa.article_id article-ids]
               [:is-not :e.expense_category_id nil]]
       :group-by [:ec.id :ec.name]
       :order-by [[:frequency :desc] [:ec.name :asc]]
       :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn context-suggestions-handler
  "Returns suppliers, stores, and categories historically associated with the given article IDs.

  Query params:
  - article_ids: comma-separated UUIDs of selected articles
  - limit: optional (default 5, max 10)"
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/expenses-read-roles "Role required")]
        forbidden
        (let [tenant-id (h/get-tenant-id request)
              raw-ids (h/get-param (:query-params request) :article_ids)
              article-ids (->> (str/split (str raw-ids) #",")
                            (keep #(some-> (str/trim %) not-empty h/try-parse-uuid))
                            vec)
              limit (min (or (some-> (h/get-param (:query-params request) :limit)
                               parse-long) 5) 10)]
          (if (or (empty? article-ids) (nil? tenant-id))
            (h/json-response {:suppliers [] :stores [] :categories []})
            (try
              (let [suppliers (context-suggestions-suppliers db article-ids tenant-id limit)
                    stores (context-suggestions-stores db article-ids tenant-id limit)
                    categories (context-suggestions-categories db article-ids tenant-id limit)]
                (h/json-response {:suppliers (vec suppliers)
                                  :stores (vec stores)
                                  :categories (vec categories)}))
              (catch Exception e
                (log/error e "Error fetching context suggestions" {:article-ids article-ids})
                (h/json-response {:error "Context suggestions failed"} 500))))))
      (h/unauthorized-response))))

;; ---------------------------------------------------------------------------
;; Related-records queries (called when a search result is selected)
;; ---------------------------------------------------------------------------

(defn- related-for-article
  "Rich article detail: metadata, price history, stores, aggregates."
  [db article-id _limit tenant-id]
  (let [;; 1. Article detail with manufacturer, category, subcategory
        detail (first
                 (jdbc/execute!
                   db
                   (sql/format {:select [[:a.canonical_name :canonical_name]
                                         [:a.link :link]
                                         [:m.display_name :manufacturer_display_name]
                                         [:sub.name :subcategory_name]
                                         [:c.name :category_name]]
                                :from [[:articles :a]]
                                :left-join [[:manufacturers :m] [:= :m.id :a.manufacturer_id]
                                            [:subcategories :sub] [:= :sub.id :a.subcategory_id]
                                            [:categories :c] [:= :c.id :sub.category_id]]
                                :where [:= :a.id article-id]})
                   {:builder-fn rs/as-unqualified-lower-maps}))

        ;; Common WHERE: expense_items links to articles via article_aliases
        ;; (expense_items.alias_id → article_aliases.article_id = article-id)
        aa-cond [:= :aa.article_id article-id]
        ei-where (if tenant-id
                   [:and aa-cond [:= :e.tenant_id tenant-id]]
                   aa-cond)

        ;; 2. Aggregate stats: total turnover, item count
        stats (first
                (jdbc/execute!
                  db
                  (sql/format {:select [[[:coalesce [:sum :ei.line_total] [:inline 0]] :total_turnover]
                                        [[:count :ei.id] :total_items]]
                               :from [[:expense_items :ei]]
                               :join [[:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                      [:expenses :e] [:= :e.id :ei.expense_id]]
                               :where ei-where})
                  {:builder-fn rs/as-unqualified-lower-maps}))

        ;; 3. Price history (most recent 50, one row per expense)
        price-history (jdbc/execute!
                        db
                        (sql/format {:select [[[:min :ei.unit_price] :unit_price]
                                              [[:sum :ei.qty] :qty]
                                              [[:sum :ei.line_total] :line_total]
                                              [:e.id :expense_id]
                                              [:e.purchased_at :purchased_at]
                                              [:st.id :store_id]
                                              [:st.display_name :store_display_name]
                                              [:st.address :store_address]
                                              [:sup.display_name :supplier_display_name]]
                                     :from [[:expense_items :ei]]
                                     :join [[:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                            [:expenses :e] [:= :e.id :ei.expense_id]]
                                     :left-join [[:stores :st] [:= :st.id :e.store_id]
                                                 [:suppliers :sup] [:= :sup.id :e.supplier_id]]
                                     :where ei-where
                                     :group-by [:e.id :e.purchased_at
                                                :st.id :st.display_name :st.address
                                                :sup.display_name]
                                     :order-by [[:e.purchased_at :desc]]
                                     :limit 50})
                        {:builder-fn rs/as-unqualified-lower-maps})

        ;; 4. Distinct stores (all, not limited by price history)
        stores (jdbc/execute!
                 db
                 (sql/format {:select-distinct [[:st.id :id]
                                                [:st.display_name :display_name]
                                                [:st.address :address]]
                              :from [[:expense_items :ei]]
                              :join [[:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                     [:expenses :e] [:= :e.id :ei.expense_id]
                                     [:stores :st] [:= :st.id :e.store_id]]
                              :where ei-where
                              :order-by [[:st.display_name :asc]]})
                 {:builder-fn rs/as-unqualified-lower-maps})

        ;; Last price = unit_price from the most recent entry
        last-price (:unit_price (first price-history))]
    {:detail (assoc detail :last_price last-price)
     :stats stats
     :price_history price-history
     :stores stores}))

(defn- related-for-supplier
  "Supplier detail: stores for this supplier + articles purchased from this supplier."
  [db supplier-id limit tenant-id]
  (let [sup-cond [:= :st.supplier_id supplier-id]
        expense-join-cond (if tenant-id
                            [:and [:= :e.store_id :st.id]
                             [:= :e.tenant_id tenant-id]]
                            [:= :e.store_id :st.id])
        where (if tenant-id
                [:and [:= :e.supplier_id supplier-id]
                 [:= :e.tenant_id tenant-id]]
                [:= :e.supplier_id supplier-id])

        ;; 1. All stores belonging to this supplier, with tenant-scoped total spendings when present.
        stores (jdbc/execute!
                 db
                 (sql/format {:select [[:st.id :id]
                                       [:st.display_name :display_name]
                                       [:st.address :address]
                                       [:st.supplier_id :supplier_id]
                                       [[:coalesce [:sum :e.total_amount] [:inline 0]] :total_spendings]]
                              :from [[:stores :st]]
                              :left-join [[:expenses :e] expense-join-cond]
                              :where sup-cond
                              :group-by [:st.id :st.display_name :st.address :st.supplier_id]
                              :order-by [[[:sum :e.total_amount] :desc]
                                         [:st.display_name :asc]]
                              :limit limit})
                 {:builder-fn rs/as-unqualified-lower-maps})

        ;; 2. Articles purchased from this supplier (aggregates)
        article-aggs (jdbc/execute!
                       db
                       (sql/format {:select [[:a.id :id]
                                             [:a.canonical_name :canonical_name]
                                             [[:sum :ei.qty] :total_qty]
                                             [[:sum :ei.line_total] :total_bam]]
                                    :from [[:expense_items :ei]]
                                    :join [[:expenses :e] [:= :e.id :ei.expense_id]
                                           [:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                           [:articles :a] [:= :a.id :aa.article_id]]
                                    :where where
                                    :group-by [:a.id :a.canonical_name]
                                    :order-by [[[:sum :ei.line_total] :desc]]
                                    :limit limit})
                       {:builder-fn rs/as-unqualified-lower-maps})

        ;; 3. Last price per article (most recent expense from this supplier)
        last-prices (when (seq article-aggs)
                      (jdbc/execute!
                        db
                        (sql/format {:select [:article_id :unit_price]
                                     :from [[{:select [[:aa.article_id :article_id]
                                                       [:ei.unit_price :unit_price]
                                                       [[:over [[:row_number]
                                                                {:partition-by :aa.article_id
                                                                 :order-by [[:e.purchased_at :desc]]}]]
                                                        :rn]]
                                              :from [[:expense_items :ei]]
                                              :join [[:expenses :e] [:= :e.id :ei.expense_id]
                                                     [:article_aliases :aa] [:= :aa.id :ei.alias_id]]
                                              :where where}
                                             :sub]]
                                     :where [:= :rn 1]})
                        {:builder-fn rs/as-unqualified-lower-maps}))

        price-map (reduce (fn [acc {:keys [article_id unit_price]}]
                            (assoc acc (str article_id) unit_price))
                    {} last-prices)
        articles (mapv (fn [art]
                         (assoc art :last_price (get price-map (str (:id art)))))
                   article-aggs)]
    {:stores stores
     :articles articles}))

(defn- related-for-store
  "Store detail (supplier, city, address) + articles purchased there."
  [db store-id _limit tenant-id]
  (let [;; 1. Store detail with supplier and city names
        detail (first
                 (jdbc/execute!
                   db
                   (sql/format {:select [[:s.display_name :display_name]
                                         [:s.address :address]
                                         [:sup.display_name :supplier_display_name]
                                         [:c.name :city_name]]
                                :from [[:stores :s]]
                                :left-join [[:suppliers :sup] [:= :sup.id :s.supplier_id]
                                            [:cities :c] [:= :c.id :s.city_id]]
                                :where [:= :s.id store-id]})
                   {:builder-fn rs/as-unqualified-lower-maps}))

        ;; 2. Articles purchased at this store (aggregates)
        store-cond [:= :e.store_id store-id]
        art-where  (if tenant-id
                     [:and store-cond [:= :e.tenant_id tenant-id]]
                     store-cond)

        article-aggs (jdbc/execute!
                       db
                       (sql/format {:select [[:a.id :id]
                                             [:a.canonical_name :canonical_name]
                                             [[:sum :ei.qty] :total_qty]
                                             [[:sum :ei.line_total] :total_bam]]
                                    :from [[:expense_items :ei]]
                                    :join [[:expenses :e] [:= :e.id :ei.expense_id]
                                           [:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                           [:articles :a] [:= :a.id :aa.article_id]]
                                    :where art-where
                                    :group-by [:a.id :a.canonical_name]
                                    :order-by [[[:sum :ei.line_total] :desc]]})
                       {:builder-fn rs/as-unqualified-lower-maps})

        ;; 3. Last price per article (most recent expense at this store)
        last-prices (when (seq article-aggs)
                      (jdbc/execute!
                        db
                        (sql/format {:select [:article_id :unit_price]
                                     :from [[{:select [[:aa.article_id :article_id]
                                                       [:ei.unit_price :unit_price]
                                                       [[:over [[:row_number]
                                                                {:partition-by :aa.article_id
                                                                 :order-by [[:e.purchased_at :desc]]}]]
                                                        :rn]]
                                              :from [[:expense_items :ei]]
                                              :join [[:expenses :e] [:= :e.id :ei.expense_id]
                                                     [:article_aliases :aa] [:= :aa.id :ei.alias_id]]
                                              :where art-where}
                                             :sub]]
                                     :where [:= :rn 1]})
                        {:builder-fn rs/as-unqualified-lower-maps}))

        ;; Merge last prices into article rows
        price-map (reduce (fn [acc {:keys [article_id unit_price]}]
                            (assoc acc (str article_id) unit_price))
                    {} last-prices)
        articles  (mapv (fn [art]
                          (assoc art :last_price (get price-map (str (:id art)))))
                    article-aggs)]
    {:detail detail
     :articles articles}))

(defn- related-for-payer
  "Recent expenses for this payer."
  [db payer-id limit tenant-id]
  (let [id-cond [:= :e.payer_id payer-id]
        where   (if tenant-id [:and id-cond [:= :e.tenant_id tenant-id]] id-cond)]
    {:expenses (jdbc/execute!
                 db
                 (sql/format {:select [[:e.id :id]
                                       [:e.total_amount :total_amount]
                                       [:e.currency :currency]
                                       [:e.purchased_at :purchased_at]
                                       [:s.display_name :supplier_display_name]]
                              :from [[:expenses :e]]
                              :left-join [[:suppliers :s] [:= :s.id :e.supplier_id]]
                              :where where
                              :order-by [[:e.purchased_at :desc]]
                              :limit limit})
                 {:builder-fn rs/as-unqualified-lower-maps})}))

(defn- related-for-expense-cat
  "Recent expenses in this category."
  [db cat-id limit tenant-id]
  (let [id-cond [:= :e.expense_category_id cat-id]
        where   (if tenant-id [:and id-cond [:= :e.tenant_id tenant-id]] id-cond)]
    {:expenses (jdbc/execute!
                 db
                 (sql/format {:select [[:e.id :id]
                                       [:e.total_amount :total_amount]
                                       [:e.currency :currency]
                                       [:e.purchased_at :purchased_at]
                                       [:s.display_name :supplier_display_name]]
                              :from [[:expenses :e]]
                              :left-join [[:suppliers :s] [:= :s.id :e.supplier_id]]
                              :where where
                              :order-by [[:e.purchased_at :desc]]
                              :limit limit})
                 {:builder-fn rs/as-unqualified-lower-maps})}))

(defn- related-for-manufacturer
  "Manufacturer detail: suppliers + articles purchased (aggregates + last prices).
   Articles include supplier_id so the frontend can filter by supplier."
  [db manufacturer-id _limit tenant-id]
  (let [;; Common WHERE: articles by this manufacturer, joined via expense_items
        mfr-cond [:= :a.manufacturer_id manufacturer-id]
        where    (if tenant-id
                   [:and mfr-cond [:= :e.tenant_id tenant-id]]
                   mfr-cond)

        ;; 1. Suppliers that carry this manufacturer's articles (via expenses)
        suppliers (jdbc/execute!
                    db
                    (sql/format {:select-distinct [[:sup.id :id]
                                                   [:sup.display_name :display_name]
                                                   [:sup.address :address]]
                                 :from [[:expense_items :ei]]
                                 :join [[:expenses :e] [:= :e.id :ei.expense_id]
                                        [:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                        [:articles :a] [:= :a.id :aa.article_id]
                                        [:suppliers :sup] [:= :sup.id :e.supplier_id]]
                                 :where where
                                 :order-by [[:sup.display_name :asc]]})
                    {:builder-fn rs/as-unqualified-lower-maps})

        ;; 2. Articles aggregated globally (all suppliers)
        article-aggs (jdbc/execute!
                       db
                       (sql/format {:select [[:a.id :id]
                                             [:a.canonical_name :canonical_name]
                                             [[:sum :ei.qty] :total_qty]
                                             [[:sum :ei.line_total] :total_bam]]
                                    :from [[:expense_items :ei]]
                                    :join [[:expenses :e] [:= :e.id :ei.expense_id]
                                           [:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                           [:articles :a] [:= :a.id :aa.article_id]]
                                    :where where
                                    :group-by [:a.id :a.canonical_name]
                                    :order-by [[[:sum :ei.line_total] :desc]]})
                       {:builder-fn rs/as-unqualified-lower-maps})

        ;; 3. Per-supplier article aggregates (for filtering)
        supplier-articles (jdbc/execute!
                            db
                            (sql/format {:select [[:a.id :article_id]
                                                  [:e.supplier_id :supplier_id]
                                                  [[:sum :ei.qty] :total_qty]
                                                  [[:sum :ei.line_total] :total_bam]]
                                         :from [[:expense_items :ei]]
                                         :join [[:expenses :e] [:= :e.id :ei.expense_id]
                                                [:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                                [:articles :a] [:= :a.id :aa.article_id]]
                                         :where where
                                         :group-by [:a.id :e.supplier_id]})
                            {:builder-fn rs/as-unqualified-lower-maps})

        ;; 4. Last price per article
        last-prices (when (seq article-aggs)
                      (jdbc/execute!
                        db
                        (sql/format {:select [:article_id :unit_price]
                                     :from [[{:select [[:aa.article_id :article_id]
                                                       [:ei.unit_price :unit_price]
                                                       [[:over [[:row_number]
                                                                {:partition-by :aa.article_id
                                                                 :order-by [[:e.purchased_at :desc]]}]]
                                                        :rn]]
                                              :from [[:expense_items :ei]]
                                              :join [[:expenses :e] [:= :e.id :ei.expense_id]
                                                     [:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                                     [:articles :a] [:= :a.id :aa.article_id]]
                                              :where where}
                                             :sub]]
                                     :where [:= :rn 1]})
                        {:builder-fn rs/as-unqualified-lower-maps}))

        price-map (reduce (fn [acc {:keys [article_id unit_price]}]
                            (assoc acc (str article_id) unit_price))
                    {} last-prices)
        articles  (mapv (fn [art]
                          (assoc art :last_price (get price-map (str (:id art)))))
                    article-aggs)]
    {:suppliers suppliers
     :articles articles
     :supplier-articles supplier-articles}))

(defn- related-for-category
  "Category detail: subcategories, stores, + articles (with subcategory/store filtering)."
  [db category-id _limit tenant-id]
  (let [;; 1. Subcategories
        subcategories (jdbc/execute!
                        db
                        (sql/format {:select [:id :name]
                                     :from [:subcategories]
                                     :where [:= :category_id category-id]
                                     :order-by [[:name :asc]]})
                        {:builder-fn rs/as-unqualified-lower-maps})

        ;; Common WHERE: articles in this category (via subcategories)
        cat-cond [:= :sub.category_id category-id]
        where    (if tenant-id
                   [:and cat-cond [:= :e.tenant_id tenant-id]]
                   cat-cond)

        ;; 2. Stores where articles from this category were purchased
        stores (jdbc/execute!
                 db
                 (sql/format {:select-distinct [[:st.id :id]
                                                [:st.display_name :display_name]
                                                [:st.address :address]]
                              :from [[:expense_items :ei]]
                              :join [[:expenses :e] [:= :e.id :ei.expense_id]
                                     [:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                     [:articles :a] [:= :a.id :aa.article_id]
                                     [:subcategories :sub] [:= :sub.id :a.subcategory_id]
                                     [:stores :st] [:= :st.id :e.store_id]]
                              :where where
                              :order-by [[:st.display_name :asc]]})
                 {:builder-fn rs/as-unqualified-lower-maps})

        ;; 3. Articles aggregated globally
        article-aggs (jdbc/execute!
                       db
                       (sql/format {:select [[:a.id :id]
                                             [:a.canonical_name :canonical_name]
                                             [[:sum :ei.qty] :total_qty]
                                             [[:sum :ei.line_total] :total_bam]]
                                    :from [[:expense_items :ei]]
                                    :join [[:expenses :e] [:= :e.id :ei.expense_id]
                                           [:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                           [:articles :a] [:= :a.id :aa.article_id]
                                           [:subcategories :sub] [:= :sub.id :a.subcategory_id]]
                                    :where where
                                    :group-by [:a.id :a.canonical_name]
                                    :order-by [[[:sum :ei.line_total] :desc]]})
                       {:builder-fn rs/as-unqualified-lower-maps})

        ;; 4. Per-subcategory article aggregates (for filtering)
        subcat-articles (jdbc/execute!
                          db
                          (sql/format {:select [[:a.id :article_id]
                                                [:a.subcategory_id :subcategory_id]
                                                [[:sum :ei.qty] :total_qty]
                                                [[:sum :ei.line_total] :total_bam]]
                                       :from [[:expense_items :ei]]
                                       :join [[:expenses :e] [:= :e.id :ei.expense_id]
                                              [:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                              [:articles :a] [:= :a.id :aa.article_id]
                                              [:subcategories :sub] [:= :sub.id :a.subcategory_id]]
                                       :where where
                                       :group-by [:a.id :a.subcategory_id]})
                          {:builder-fn rs/as-unqualified-lower-maps})

        ;; 5. Per-store article aggregates (for filtering)
        store-articles (jdbc/execute!
                         db
                         (sql/format {:select [[:a.id :article_id]
                                               [:e.store_id :store_id]
                                               [[:sum :ei.qty] :total_qty]
                                               [[:sum :ei.line_total] :total_bam]]
                                      :from [[:expense_items :ei]]
                                      :join [[:expenses :e] [:= :e.id :ei.expense_id]
                                             [:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                             [:articles :a] [:= :a.id :aa.article_id]
                                             [:subcategories :sub] [:= :sub.id :a.subcategory_id]]
                                      :where where
                                      :group-by [:a.id :e.store_id]})
                         {:builder-fn rs/as-unqualified-lower-maps})

        ;; 6. Last price per article
        last-prices (when (seq article-aggs)
                      (jdbc/execute!
                        db
                        (sql/format {:select [:article_id :unit_price]
                                     :from [[{:select [[:aa.article_id :article_id]
                                                       [:ei.unit_price :unit_price]
                                                       [[:over [[:row_number]
                                                                {:partition-by :aa.article_id
                                                                 :order-by [[:e.purchased_at :desc]]}]]
                                                        :rn]]
                                              :from [[:expense_items :ei]]
                                              :join [[:expenses :e] [:= :e.id :ei.expense_id]
                                                     [:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                                     [:articles :a] [:= :a.id :aa.article_id]
                                                     [:subcategories :sub] [:= :sub.id :a.subcategory_id]]
                                              :where where}
                                             :sub]]
                                     :where [:= :rn 1]})
                        {:builder-fn rs/as-unqualified-lower-maps}))

        price-map (reduce (fn [acc {:keys [article_id unit_price]}]
                            (assoc acc (str article_id) unit_price))
                    {} last-prices)
        articles  (mapv (fn [art]
                          (assoc art :last_price (get price-map (str (:id art)))))
                    article-aggs)]
    {:subcategories subcategories
     :stores stores
     :articles articles
     :subcat-articles subcat-articles
     :store-articles store-articles}))

(defn- related-for-subcategory
  "Subcategory detail: stores + articles in this subcategory."
  [db subcategory-id _limit tenant-id]
  (let [sub-cond [:= :a.subcategory_id subcategory-id]
        where    (if tenant-id
                   [:and sub-cond [:= :e.tenant_id tenant-id]]
                   sub-cond)

        ;; 1. Stores
        stores (jdbc/execute!
                 db
                 (sql/format {:select-distinct [[:st.id :id]
                                                [:st.display_name :display_name]
                                                [:st.address :address]]
                              :from [[:expense_items :ei]]
                              :join [[:expenses :e] [:= :e.id :ei.expense_id]
                                     [:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                     [:articles :a] [:= :a.id :aa.article_id]
                                     [:stores :st] [:= :st.id :e.store_id]]
                              :where where
                              :order-by [[:st.display_name :asc]]})
                 {:builder-fn rs/as-unqualified-lower-maps})

        ;; 2. Articles aggregated
        article-aggs (jdbc/execute!
                       db
                       (sql/format {:select [[:a.id :id]
                                             [:a.canonical_name :canonical_name]
                                             [[:sum :ei.qty] :total_qty]
                                             [[:sum :ei.line_total] :total_bam]]
                                    :from [[:expense_items :ei]]
                                    :join [[:expenses :e] [:= :e.id :ei.expense_id]
                                           [:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                           [:articles :a] [:= :a.id :aa.article_id]]
                                    :where where
                                    :group-by [:a.id :a.canonical_name]
                                    :order-by [[[:sum :ei.line_total] :desc]]})
                       {:builder-fn rs/as-unqualified-lower-maps})

        ;; 3. Per-store article aggregates
        store-articles (jdbc/execute!
                         db
                         (sql/format {:select [[:a.id :article_id]
                                               [:e.store_id :store_id]
                                               [[:sum :ei.qty] :total_qty]
                                               [[:sum :ei.line_total] :total_bam]]
                                      :from [[:expense_items :ei]]
                                      :join [[:expenses :e] [:= :e.id :ei.expense_id]
                                             [:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                             [:articles :a] [:= :a.id :aa.article_id]]
                                      :where where
                                      :group-by [:a.id :e.store_id]})
                         {:builder-fn rs/as-unqualified-lower-maps})

        ;; 4. Last price per article
        last-prices (when (seq article-aggs)
                      (jdbc/execute!
                        db
                        (sql/format {:select [:article_id :unit_price]
                                     :from [[{:select [[:aa.article_id :article_id]
                                                       [:ei.unit_price :unit_price]
                                                       [[:over [[:row_number]
                                                                {:partition-by :aa.article_id
                                                                 :order-by [[:e.purchased_at :desc]]}]]
                                                        :rn]]
                                              :from [[:expense_items :ei]]
                                              :join [[:expenses :e] [:= :e.id :ei.expense_id]
                                                     [:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                                     [:articles :a] [:= :a.id :aa.article_id]]
                                              :where where}
                                             :sub]]
                                     :where [:= :rn 1]})
                        {:builder-fn rs/as-unqualified-lower-maps}))

        price-map (reduce (fn [acc {:keys [article_id unit_price]}]
                            (assoc acc (str article_id) unit_price))
                    {} last-prices)
        articles  (mapv (fn [art]
                          (assoc art :last_price (get price-map (str (:id art)))))
                    article-aggs)]
    {:stores stores
     :articles articles
     :store-articles store-articles}))

(defn- related-results
  [db entity-type entity-id limit tenant-id]
  (case entity-type
    "articles" (related-for-article db entity-id limit tenant-id)
    "suppliers" (related-for-supplier db entity-id limit tenant-id)
    "stores" (related-for-store db entity-id limit tenant-id)
    "payers" (related-for-payer db entity-id limit tenant-id)
    "expense-cats" (related-for-expense-cat db entity-id limit tenant-id)
    "manufacturers" (related-for-manufacturer db entity-id limit tenant-id)
    "categories" (related-for-category db entity-id limit tenant-id)
    "subcategories" (related-for-subcategory db entity-id limit tenant-id)
    {}))

(defn- related-response
  [db request tenant-id]
  (let [params (:query-params request)
        entity-type (get params "type")
        entity-id (h/try-parse-uuid (get params "id"))
        limit (parse-search-limit request 8)]
    (if-not entity-id
      (h/json-response {:error "Missing or invalid id"} 400)
      (try
        (let [related (related-results db entity-type entity-id limit tenant-id)]
          (h/json-response {:related related
                            :type entity-type
                            :id (str entity-id)}))
        (catch Exception e
          (log/error e "Error fetching related records"
            {:type entity-type
             :id entity-id
             :tenant-id tenant-id})
          (h/json-response {:error "Failed to fetch related records"} 500))))))

(defn user-related-handler
  "Fetch related records for a selected search result.
   Query params: type (string), id (uuid string)."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/expenses-read-roles "Role required")]
        forbidden
        (related-response db request (h/get-tenant-id request)))
      (h/unauthorized-response))))

(defn admin-related-handler
  "Fetch related records for the admin search page using global scope."
  [db]
  (fn [request]
    (related-response db request nil)))

(comment
  ;; REPL test: load handler and call with mock request
  ;; (require '[app.domain.backend.expenses.handlers.search :as search] :reload)
  ;; ((search/user-search-handler db) {:query-params {"q" "coffee"} :session {:auth-session {:user {:id ...} :membership {:role "member"} :tenant {:id ...}}}})
  :rcf)
