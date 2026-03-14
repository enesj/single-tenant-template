(ns app.domain.backend.expenses.handlers.search.quick-add
  "Dedicated filtered search for Quick Add expense creation flow.
   Includes article pricing and co-occurrence suggestions."
  (:require
    [app.domain.backend.expenses.handlers.search.helpers :as sh]
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [taoensso.timbre :as log]))

(defn- quick-add-search-suppliers
  [db term limit]
  (let [p (sh/pattern term)
        rel (sh/relevance-score-expr term [:display_name :address])]
    (jdbc/execute!
      db
      (sql/format {:select [:id
                            [:display_name :label]
                            [rel :relevance]]
                   :from [:suppliers]
                   :where (sh/fuzzy-text-where [:display_name :address] term p)
                   :order-by [[rel :desc] [:display_name :asc]]
                   :limit limit})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- quick-add-search-stores
  [db term limit supplier-id]
  (let [p (sh/pattern term)
        rel (sh/relevance-score-expr term [:st.display_name :st.address :s.display_name])
        base-where [:and
                    [:is-not :st.supplier_id nil]
                    (sh/fuzzy-text-where [:st.display_name :st.address :s.display_name] term p)]
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
  (let [p (sh/pattern term)
        rel (sh/relevance-score-expr term [:name])
        text-where (sh/fuzzy-text-where [:name] term p)
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
  (let [p (sh/pattern term)
        rel (sh/relevance-score-expr term [:canonical_name])
        articles (jdbc/execute!
                   db
                   (sql/format {:select [:id
                                         [:canonical_name :label]
                                         [rel :relevance]]
                                :from [:articles]
                                :where (sh/fuzzy-text-where [:canonical_name] term p)
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
              entity-type (or (sh/parse-entity-type request) "all")
              q (sh/parse-query request)
              limit (sh/parse-search-limit request 8)
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
