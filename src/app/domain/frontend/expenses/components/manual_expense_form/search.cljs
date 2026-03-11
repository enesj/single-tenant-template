(ns app.domain.frontend.expenses.components.manual-expense-form.search
  "Client-side fuzzy search across expense entity types.
   Pure functions, no side effects."
  (:require
    [clojure.string :as str]))

;; ─────────────────────────────────────────────
;; Entity type metadata
;; ─────────────────────────────────────────────

(def entity-type-config
  {:supplier {:icon "🏪" :label "Supplier"  :color "blue"
              :name-fn #(or (:display-name %) (:display_name %) "")}
   :store    {:icon "🏬" :label "Store"     :color "emerald"
              :name-fn #(or (:display-name %) (:display_name %) "")}
   :category {:icon "📂" :label "Category"  :color "purple"
              :name-fn #(or (:name %) "")}
   :article  {:icon "📦" :label "Article"   :color "amber"
              :name-fn #(or (:canonical-name %) (:canonical_name %) "")}})

(defn entity-type-info
  [entity-type]
  (get entity-type-config entity-type))

;; ─────────────────────────────────────────────
;; Number detection and parsing
;; ─────────────────────────────────────────────

(defn number-input?
  "Returns true if the trimmed input looks like a monetary amount."
  [s]
  (some? (re-matches #"^\d+([.,]\d{0,2})?$" (str/trim (str s)))))

(defn parse-amount
  "Parse a monetary amount string, handling comma as decimal separator."
  [s]
  (let [normalized (-> (str s) str/trim (str/replace "," "."))]
    (let [n (js/parseFloat normalized)]
      (when-not (js/isNaN n) n))))

;; ─────────────────────────────────────────────
;; Fuzzy matching
;; ─────────────────────────────────────────────

(defn- score-match
  "Score how well `query` matches `text`. Returns 0-100 or nil for no match."
  [query text]
  (let [q (str/lower-case (str/trim (str query)))
        t (str/lower-case (str text))]
    (cond
      (str/blank? q) nil
      (= q t) 100
      (str/starts-with? t q) 85
      ;; Any word in text starts with query
      (some #(str/starts-with? % q) (str/split t #"\s+")) 70
      ;; Substring match
      (str/includes? t q) 55
      :else nil)))

;; ─────────────────────────────────────────────
;; Unified search
;; ─────────────────────────────────────────────

(defn search-all-entities
  "Search across all entity types. Returns sorted results.
   `entities` is {:suppliers [] :stores [] :categories [] :articles []}
   `opts` is {:selected-supplier-id id-or-nil} for store filtering."
  [query entities opts]
  (let [q (str/trim (str query))]
    (when (>= (count q) 2)
      (let [{:keys [suppliers stores categories articles]} entities
            {:keys [selected-supplier-id]} opts
            search-type (fn [items entity-type]
                          (let [{:keys [name-fn]} (get entity-type-config entity-type)]
                            (->> items
                              (keep (fn [item]
                                      (let [item-name (name-fn item)
                                            score (score-match q item-name)]
                                        (when score
                                          {:id (:id item)
                                           :label item-name
                                           :entity-type entity-type
                                           :score score
                                           :entity item}))))
                              (sort-by :score >)
                              (take 4))))
            ;; Filter stores by selected supplier if one is chosen
            filtered-stores (if selected-supplier-id
                              (filter #(= (str selected-supplier-id)
                                         (str (or (:supplier-id %) (:supplier_id %))))
                                stores)
                              stores)]
        (->> (concat
               (search-type suppliers :supplier)
               (search-type filtered-stores :store)
               (search-type categories :category)
               (search-type articles :article))
          (sort-by :score >)
          (take 10)
          vec)))))
