(ns app.domain.frontend.expenses.pages.user.expense-reports.sections.categories
  "Category/subcategory spending breakdown section for user reports."
  (:require
    [app.domain.frontend.expenses.pages.user.expense-reports.components :refer [section-shell]]
    [app.domain.frontend.expenses.pages.user.expense-reports.utils :refer [->number
                                                                           format-int
                                                                           format-money]]
    [uix.core :refer [$ defui use-state]]))

(def ^:private category-colors
  ["bg-primary" "bg-secondary" "bg-accent"
   "bg-info" "bg-success" "bg-warning"
   "bg-error" "bg-primary/70" "bg-secondary/70"
   "bg-accent/70" "bg-info/70" "bg-success/70"])

(def ^:private top-n 10)

(defui subcategory-row [{:keys [sub primary-currency cat-total-amount]}]
  (let [total (or (->number (:total_amount sub)) 0)
        sub-pct (or (->number (:sub_pct sub)) 0)
        ratio (if (pos? cat-total-amount) (/ total cat-total-amount) 0)]
    ($ :div {:key (str "sub-" (:subcategory_id sub))
             :class "flex items-center gap-2 py-1.5 px-3 rounded-lg hover:bg-base-100/80 transition-colors"}
      ($ :div {:class "w-1 h-1 rounded-full bg-primary/40 flex-shrink-0"})
      ($ :div {:class "flex-1 min-w-0"}
        ($ :div {:class "flex items-baseline justify-between gap-2"}
          ($ :div {:class "flex items-baseline gap-2 min-w-0"}
            ($ :span {:class "text-xs font-medium text-base-content/80 truncate"}
              (:subcategory_name sub))
            ($ :span {:class "text-[10px] text-base-content/40"}
              (str (format-int (:item_count sub)) " st.")))
          ($ :div {:class "flex items-baseline gap-2 flex-shrink-0"}
            ($ :span {:class "text-[10px] text-base-content/50"}
              (str (.toFixed sub-pct 1) "%"))
            ($ :span {:class "font-mono text-xs font-medium text-base-content/70"}
              (format-money total primary-currency))))
        ($ :div {:class "h-1 rounded-full bg-base-200 overflow-hidden mt-0.5"}
          ($ :div {:class "h-full rounded-full bg-primary/30 transition-all duration-500"
                   :style {:width (str (* 100 ratio) "%")}}))))))

(defui category-row [{:keys [cat idx primary-currency max-amount]}]
  (let [[expanded?, set-expanded!] (use-state false)
        total (or (->number (:total_amount cat)) 0)
        pct (or (->number (:allocation_pct cat)) 0)
        ratio (if (pos? max-amount) (/ total max-amount) 0)
        color-class (nth category-colors (mod idx (count category-colors)))
        subcategories (:subcategories cat)
        has-subs? (and (seq subcategories) (> (count subcategories) 1))]
    ($ :div {:key (str "cat-" (:category_id cat))
             :class "rounded-lg border border-base-200/60 bg-white overflow-hidden transition-all duration-200 hover:border-primary/20"}
      ($ :button {:type "button"
                  :class "w-full text-left px-3 py-2 transition-colors hover:bg-base-50/50"
                  :on-click #(when has-subs? (set-expanded! (not expanded?)))}
        ($ :div {:class "flex items-center justify-between gap-2 mb-1"}
          ($ :div {:class "flex items-center gap-2 min-w-0 flex-1"}
            ($ :div {:class (str "w-2.5 h-2.5 rounded-sm flex-shrink-0 " color-class)})
            ($ :span {:class "font-semibold text-sm text-base-content/90 truncate"}
              (:category_name cat))
            ($ :span {:class "text-[10px] text-base-content/40 flex-shrink-0"}
              (str (format-int (:item_count cat)) " st."))
            (when has-subs?
              ($ :span {:class (str "text-[10px] text-base-content/40 transition-transform duration-200 "
                                 (when expanded? "rotate-180"))}
                "▾")))
          ($ :div {:class "flex items-baseline gap-1.5 flex-shrink-0"}
            ($ :span {:class "text-[10px] font-bold text-primary/80"}
              (str (.toFixed pct 1) "%"))
            ($ :span {:class "font-mono text-sm font-bold text-base-content"}
              (format-money total primary-currency))))

        ;; Compact bar
        ($ :div {:class "h-2 rounded bg-base-100 overflow-hidden"}
          ($ :div {:class (str "h-full rounded transition-all duration-700 " color-class " opacity-80")
                   :style {:width (str (* 100 ratio) "%")}})))

      ;; Expanded subcategories
      (when (and expanded? has-subs?)
        ($ :div {:class "px-3 pb-2 pt-1 border-t border-base-100 bg-base-50/30 space-y-0"}
          ($ :div {:class "text-[9px] font-bold text-base-content/40 uppercase tracking-widest mb-1 px-3"}
            "Podkategorije")
          (mapv (fn [sub]
                  ($ subcategory-row {:key (str "sub-" (:subcategory_id sub))
                                      :sub sub
                                      :primary-currency primary-currency
                                      :cat-total-amount total}))
            subcategories))))))

(defn- build-others-category
  "Aggregate categories beyond top-n into a single 'Ostalo' entry."
  [rest-categories]
  (let [total (reduce + 0 (map #(or (->number (:total_amount %)) 0) rest-categories))
        items (reduce + 0 (map #(or (->number (:item_count %)) 0) rest-categories))
        pct (reduce + 0 (map #(or (->number (:allocation_pct %)) 0) rest-categories))]
    {:category_id "others"
     :category_name (str "Ostalo (" (count rest-categories) ")")
     :total_amount total
     :item_count items
     :allocation_pct pct
     :subcategories []}))

(defui categories-tab [{:keys [by-category-loading?
                               by-category-error
                               category-data
                               primary-currency]}]
  (let [top-categories (take top-n category-data)
        rest-categories (drop top-n category-data)
        display-data (if (seq rest-categories)
                       (conj (vec top-categories) (build-others-category rest-categories))
                       category-data)
        max-amount (apply max (cons 0 (map #(or (->number (:total_amount %)) 0) display-data)))]
    ($ section-shell {:title "Potrošnja po kategorijama artikala"
                      :subtitle "Top 10 kategorija artikala po potrošnji"
                      :loading? by-category-loading?
                      :error by-category-error}
      (if (seq display-data)
        ($ :div {:class "space-y-1.5"}
          ;; Summary stacked bar
          (when (> (count display-data) 1)
            ($ :div {:class "mb-3"}
              ($ :div {:class "h-5 rounded-lg overflow-hidden flex bg-base-100"}
                (mapv (fn [idx cat]
                        (let [pct (or (->number (:allocation_pct cat)) 0)
                              color-class (nth category-colors (mod idx (count category-colors)))]
                          (when (pos? pct)
                            ($ :div {:key (str "bar-" (:category_id cat))
                                     :class (str "h-full " color-class " opacity-80 transition-all duration-700"
                                              " hover:opacity-100 relative group")
                                     :style {:width (str pct "%")}
                                     :title (str (:category_name cat) " — " (.toFixed pct 1) "%")}
                              (when (> pct 8)
                                ($ :span {:class "absolute inset-0 flex items-center justify-center text-[9px] font-bold text-white/90 truncate px-1"}
                                  (str (.toFixed pct 0) "%")))))))
                  (range) display-data))))

          ;; Compact category rows
          (mapv (fn [idx cat]
                  ($ category-row {:key (str "cat-" (:category_id cat))
                                   :cat cat
                                   :idx idx
                                   :primary-currency primary-currency
                                   :max-amount max-amount}))
            (range) display-data))

        ($ :div {:class "flex flex-col items-center justify-center h-48 text-center p-8 bg-base-50 rounded-xl border border-dashed border-base-300"}
          ($ :p {:class "text-base-content/60"} "Nema podataka o kategorijama artikala.")
          ($ :p {:class "text-xs text-base-content/40 mt-2"} "Kategorije se automatski popunjavaju iz artikala na računima."))))))
