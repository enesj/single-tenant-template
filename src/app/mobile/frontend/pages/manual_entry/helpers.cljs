(ns app.mobile.frontend.pages.manual-entry.helpers
  (:require
    [app.domain.frontend.expenses.shared.manual-entry.core :as manual-entry]
    [clojure.string :as str]))

(def chip-colors
  {:supplier "bg-blue-100 text-blue-800"
   :store    "bg-green-100 text-green-800"
   :category "bg-purple-100 text-purple-800"
   :article  "bg-amber-100 text-amber-800"
   :payer    "bg-teal-100 text-teal-800"
   :date     "bg-sky-100 text-sky-800"
   :currency "bg-orange-100 text-orange-800"})

(def currency-options
  ["BAM" "EUR" "USD" "GBP" "CHF" "HRK" "RSD"])

(defn mobile-entity-type-label
  [t entity-type]
  (case entity-type
    :supplier (t :smart-expense/entity-supplier)
    :store    (t :smart-expense/entity-store)
    :category (t :smart-expense/entity-category)
    :article  (t :smart-expense/entity-article)
    :payer    (t :smart-expense/entity-payer)
    :date     (t :smart-expense/entity-date)
    :currency (t :smart-expense/entity-currency)
    ""))

(defn result-display-name [result]
  (or (:label result)
    (:name result)
    (:display_name result)
    (:display-name result)
    (:canonical_name result)
    (:canonical-name result)
    ""))

(defn result-entity-type [result]
  (keyword (or (:entity-type result) (:type result) "article")))

(defn result-article-price [result]
  (or (get-in result [:price_info :avg_price])
    (:last-price result)
    (:last_price result)
    (:unit_price result)
    (:unit-price result)))

(defn context-entry-label [entry]
  (or (:label entry) (:name entry) ""))

(defn normalize-article-pick [article]
  {:id (or (:id article) (:article_id article) (:article-id article))
   :label (result-display-name article)
   :entity-type :article
   :last-price (result-article-price article)
   :entity article})

(defn submit-error-key
  [items payer-id]
  (let [prepared (manual-entry/prepare-submit-items items)
        total (reduce + 0 (map :line_total prepared))]
    (cond
      (empty? prepared)
      :smart-expense/err-no-items

      (str/blank? (str payer-id))
      :smart-expense/err-no-payer

      (<= total 0)
      :smart-expense/err-no-total

      :else nil)))

(defn phase-one-category-picks [expense-categories context search-query]
  (if (and (str/blank? search-query)
        (not (:category context)))
    (->> expense-categories
      (take 5)
      vec)
    []))

(defn phase-one-history-picks [history items context search-query]
  (let [blank-search? (str/blank? search-query)
        items-empty? (empty? items)
        show-articles? (and blank-search? items-empty?)
        show-stores? (and show-articles?
                       (not (:store context))
                       (or (:category context)
                         (:supplier context)))]
    {:stores (if show-stores?
               (->> (:stores history)
                 (take 5)
                 vec)
               [])
     :articles (if show-articles?
                 (->> (:articles history)
                   (map normalize-article-pick)
                   (take 5)
                   vec)
                 [])}))

(defn add-context-entry
  [context entity-type result]
  (let [label (result-display-name result)
        base-entry {:id (or (:id result) (:entity_id result) (:entity-id result))
                    :label label}
        supplier-id (or (:supplier_id result)
                      (:supplier-id result)
                      (get-in result [:entity :supplier_id])
                      (get-in result [:entity :supplier-id]))
        supplier-label (or (:supplier_display_name result)
                         (:supplier-display-name result)
                         (get-in result [:entity :supplier_display_name])
                         (get-in result [:entity :supplier-display-name]))]
    (case entity-type
      :supplier
      (let [next-context (assoc context :supplier base-entry)
            store-supplier-id (some-> (or (get-in next-context [:store :supplier-id])
                                        (get-in next-context [:store :supplier_id]))
                                str)]
        (if (and (:store next-context)
              store-supplier-id
              (not= store-supplier-id (some-> (:id base-entry) str)))
          (dissoc next-context :store)
          next-context))

      :store
      (cond-> (assoc context :store (assoc base-entry
                                      :supplier-id supplier-id
                                      :supplier-display-name supplier-label))
        supplier-id
        (assoc :supplier {:id supplier-id
                          :label (or supplier-label (get-in context [:supplier :label]))}))

      :category
      (assoc context :category base-entry)

      :expense-category
      (assoc context :category base-entry)

      (assoc context entity-type base-entry))))