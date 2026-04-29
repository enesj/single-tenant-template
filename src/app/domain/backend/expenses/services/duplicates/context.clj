(ns app.domain.backend.expenses.services.duplicates.context
  (:require
    [app.domain.backend.expenses.services.duplicates.config :as dup-config]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))

(defn derive-price-label
  [{:keys [unit_price qty line_total currency]}]
  (let [amount (cond
                 (some? unit_price) (bigdec unit_price)
                 (and (some? line_total)
                   (some? qty)
                   (not (zero? (bigdec qty))))
                 (.divide (bigdec line_total) (bigdec qty) 2 java.math.RoundingMode/HALF_UP)
                 (some? line_total) (bigdec line_total)
                 :else nil)]
    (when amount
      (str (.setScale amount 2 java.math.RoundingMode/HALF_UP)
        (when (seq (str currency))
          (str " " currency))))))

(defn article-price-labels-by-id
  [db all-ids]
  (let [direct-rows (jdbc/execute!
                      db
                      (sql/format {:select [[:ei.article_id :entity_id]
                                            :ei.unit_price
                                            :ei.qty
                                            :ei.line_total
                                            [:e.currency :currency]]
                                   :from [[:expense_items :ei]]
                                   :left-join [[:expenses :e] [:= :e.id :ei.expense_id]]
                                   :where [:in :ei.article_id all-ids]})
                      {:builder-fn rs/as-unqualified-lower-maps})
        alias-rows (jdbc/execute!
                     db
                     (sql/format {:select [[:aa.article_id :entity_id]
                                           :ei.unit_price
                                           :ei.qty
                                           :ei.line_total
                                           [:e.currency :currency]]
                                  :from [[:article_aliases :aa]]
                                  :join [[:expense_items :ei] [:= :ei.alias_id :aa.id]]
                                  :left-join [[:expenses :e] [:= :e.id :ei.expense_id]]
                                  :where [:in :aa.article_id all-ids]})
                     {:builder-fn rs/as-unqualified-lower-maps})]
    (reduce
      (fn [acc {:keys [entity_id] :as row}]
        (if-let [label (derive-price-label row)]
          (update acc entity_id (fnil conj []) label)
          acc))
      {}
      (concat direct-rows alias-rows))))

(defn article-manufacturer-names-by-id
  [db all-ids]
  (->> (jdbc/execute!
         db
         (sql/format {:select [[:a.id :entity_id]
                               [:m.display_name :manufacturer_name]]
                      :from [[:articles :a]]
                      :left-join [[:manufacturers :m] [:= :m.id :a.manufacturer_id]]
                      :where [:in :a.id all-ids]})
         {:builder-fn rs/as-unqualified-lower-maps})
    (reduce (fn [acc {:keys [entity_id manufacturer_name]}]
              (if (some? manufacturer_name)
                (assoc acc entity_id {:manufacturer-name manufacturer_name})
                acc))
      {})))

(defn store-supplier-names-by-id
  [db all-ids]
  (->> (jdbc/execute!
         db
         (sql/format {:select [[:st.id :entity_id]
                               [:s.display_name :supplier_display_name]]
                      :from [[:stores :st]]
                      :join [[:suppliers :s] [:= :s.id :st.supplier_id]]
                      :where [:in :st.id all-ids]})
         {:builder-fn rs/as-unqualified-lower-maps})
    (reduce (fn [acc {:keys [entity_id supplier_display_name]}]
              (assoc acc entity_id {:supplier-display-name supplier_display_name}))
      {})))

(defn subcategory-category-names-by-id
  [db all-ids]
  (->> (jdbc/execute!
         db
         (sql/format {:select [[:sc.id :entity_id]
                               [:c.name :category_name]]
                      :from [[:subcategories :sc]]
                      :join [[:categories :c] [:= :c.id :sc.category_id]]
                      :where [:in :sc.id all-ids]})
         {:builder-fn rs/as-unqualified-lower-maps})
    (reduce (fn [acc {:keys [entity_id category_name]}]
              (assoc acc entity_id {:category-name category_name}))
      {})))

(defn contextual-info-by-id
  [db entity-type all-ids]
  (case entity-type
    :articles
    (merge-with merge
      (->> (article-price-labels-by-id db all-ids)
        (reduce-kv (fn [acc entity-id labels]
                     (assoc acc entity-id {:price-labels (->> labels distinct sort vec)}))
          {}))
      (article-manufacturer-names-by-id db all-ids))

    :stores
    (store-supplier-names-by-id db all-ids)

    :subcategories
    (subcategory-category-names-by-id db all-ids)

    {}))

(defn enrich-members-with-context
  [db entity-type members]
  (let [all-ids (->> members
                  (map :id)
                  distinct
                  vec)]
    (if (empty? all-ids)
      (vec members)
      (let [context-by-id (contextual-info-by-id db entity-type all-ids)]
        (mapv (fn [member]
                (merge member (get context-by-id (:id member) {})))
          members)))))

(defn enrich-with-usage-counts
  [db entity-type clusters]
  (let [config (dup-config/get-entity-config! entity-type)
        fk-tables (:fk-tables config)
        all-ids (->> clusters
                  (mapcat :members)
                  (map :id)
                  distinct
                  vec)]
    (if (empty? all-ids)
      clusters
      (let [counts-by-id
            (if (empty? fk-tables)
              {}
              (reduce
                (fn [acc [fk-table {:keys [col]}]]
                  (let [rows (jdbc/execute!
                               db
                               (sql/format {:select [[col :entity_id]
                                                     [[:count :*] :cnt]]
                                            :from [(keyword (name fk-table))]
                                            :where [:in col all-ids]
                                            :group-by [col]})
                               {:builder-fn rs/as-unqualified-lower-maps})]
                    (reduce
                      (fn [a {:keys [entity_id cnt]}]
                        (update a entity_id (fnil + 0) cnt))
                      acc
                      rows)))
                {}
                fk-tables))
            context-by-id (contextual-info-by-id db entity-type all-ids)]
        (mapv
          (fn [cluster]
            (update cluster :members
              (fn [members]
                (mapv (fn [member]
                        (merge member
                          {:usage-count (get counts-by-id (:id member) 0)}
                          (get context-by-id (:id member) {})))
                  members))))
          clusters)))))

(defn filter-article-clusters-with-distinct-manufacturers
  [entity-type clusters]
  (if (= entity-type :articles)
    (->> clusters
      (remove (fn [{:keys [members]}]
                (let [manufacturer-names (->> members
                                           (keep (fn [member]
                                                   (some-> (or (:manufacturer-name member)
                                                             (:manufacturer_name member))
                                                     str
                                                     str/trim
                                                     not-empty))))]
                  (and (> (count members) 1)
                    (= (count manufacturer-names) (count members))
                    (= (count (distinct manufacturer-names))
                      (count members))))))
      vec)
    (vec clusters)))