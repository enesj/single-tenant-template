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

(defui subcategory-row [{:keys [sub primary-currency cat-total-amount]}]
  (let [total (or (->number (:total_amount sub)) 0)
        sub-pct (or (->number (:sub_pct sub)) 0)
        ratio (if (pos? cat-total-amount) (/ total cat-total-amount) 0)]
    ($ :div {:key (str "sub-" (:subcategory_id sub))
             :class "flex items-center gap-3 py-2 px-3 rounded-lg hover:bg-base-100/80 transition-colors group"}
      ($ :div {:class "w-1.5 h-1.5 rounded-full bg-primary/40 flex-shrink-0"})
      ($ :div {:class "flex-1 min-w-0"}
        ($ :div {:class "flex items-baseline justify-between gap-2 mb-1"}
          ($ :div {:class "flex items-baseline gap-2 min-w-0"}
            ($ :span {:class "text-sm font-medium text-base-content/80 truncate"}
              (:subcategory_name sub))
            ($ :span {:class "text-xs text-base-content/40"}
              (str (format-int (:item_count sub)) " items")))
          ($ :div {:class "flex items-baseline gap-2 flex-shrink-0"}
            ($ :span {:class "text-xs text-base-content/50"}
              (str (.toFixed sub-pct 1) "%"))
            ($ :span {:class "font-mono text-sm font-medium text-base-content/70"}
              (format-money total primary-currency))))
        ($ :div {:class "h-1.5 rounded-full bg-base-200 overflow-hidden"}
          ($ :div {:class "h-full rounded-full bg-primary/30 transition-all duration-500"
                   :style {:width (str (* 100 ratio) "%")}}))))))

(defui category-card [{:keys [cat idx primary-currency max-amount]}]
  (let [[expanded?, set-expanded!] (use-state false)
        total (or (->number (:total_amount cat)) 0)
        pct (or (->number (:allocation_pct cat)) 0)
        ratio (if (pos? max-amount) (/ total max-amount) 0)
        color-class (nth category-colors (mod idx (count category-colors)))
        subcategories (:subcategories cat)
        has-subs? (and (seq subcategories) (> (count subcategories) 1))]
    ($ :div {:key (str "cat-" (:category_id cat))
             :class "rounded-xl border border-base-200/80 bg-white overflow-hidden transition-all duration-200 hover:border-primary/20 hover:shadow-sm"}
      ;; Category header — clickable to expand
      ($ :button {:type "button"
                  :class "w-full text-left p-4 transition-colors hover:bg-base-50/50"
                  :on-click #(when has-subs? (set-expanded! (not expanded?)))}
        ($ :div {:class "flex items-center justify-between gap-3 mb-2"}
          ($ :div {:class "flex items-center gap-3 min-w-0 flex-1"}
            ($ :div {:class (str "w-3 h-3 rounded-sm flex-shrink-0 " color-class)})
            ($ :span {:class "font-bold text-base text-base-content/90 truncate"}
              (:category_name cat))
            ($ :span {:class "text-xs text-base-content/40 flex-shrink-0"}
              (str (format-int (:item_count cat)) " items"))
            (when has-subs?
              ($ :span {:class (str "text-xs text-base-content/40 transition-transform duration-200 "
                                 (when expanded? "rotate-180"))}
                "▾")))
          ($ :div {:class "flex items-baseline gap-2 flex-shrink-0"}
            ($ :span {:class "ds-badge ds-badge-sm bg-primary/10 text-primary border-primary/20 font-bold"}
              (str (.toFixed pct 1) "%"))
            ($ :span {:class "font-mono text-lg font-bold text-base-content"}
              (format-money total primary-currency))))

        ;; Main bar
        ($ :div {:class "h-4 rounded-lg bg-base-100 overflow-hidden"}
          ($ :div {:class (str "h-full rounded-lg transition-all duration-700 " color-class " opacity-80")
                   :style {:width (str (* 100 ratio) "%")}})))

      ;; Expanded subcategories
      (when (and expanded? has-subs?)
        ($ :div {:class "px-4 pb-4 pt-1 border-t border-base-100 bg-base-50/30 space-y-0.5 animate-in fade-in slide-in-from-top-1 duration-200"}
          ($ :div {:class "text-[10px] font-bold text-base-content/40 uppercase tracking-widest mb-2 px-3"}
            "Podkategorije")
          (mapv (fn [sub]
                  ($ subcategory-row {:key (str "sub-" (:subcategory_id sub))
                                      :sub sub
                                      :primary-currency primary-currency
                                      :cat-total-amount total}))
            subcategories))))))

(defui categories-tab [{:keys [by-category-loading?
                               by-category-error
                               category-data
                               primary-currency]}]
  (let [max-amount (apply max (cons 0 (map #(or (->number (:total_amount %)) 0) category-data)))]
    ($ section-shell {:title "Troškovi po kategorijama"
                      :subtitle "Raspodjela troškova po kategorijama artikala"
                      :loading? by-category-loading?
                      :error by-category-error}
      (if (seq category-data)
        ($ :div {:class "space-y-3"}
          ;; Summary bar showing all categories stacked
          (when (> (count category-data) 1)
            ($ :div {:class "mb-4"}
              ($ :div {:class "h-6 rounded-lg overflow-hidden flex bg-base-100"}
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
                                ($ :span {:class "absolute inset-0 flex items-center justify-center text-[10px] font-bold text-white/90 truncate px-1"}
                                  (str (.toFixed pct 0) "%")))))))
                  (range) category-data))))

          ;; Individual category cards
          (mapv (fn [idx cat]
                  ($ category-card {:key (str "cat-" (:category_id cat))
                                    :cat cat
                                    :idx idx
                                    :primary-currency primary-currency
                                    :max-amount max-amount}))
            (range) category-data))

        ($ :div {:class "flex flex-col items-center justify-center h-48 text-center p-8 bg-base-50 rounded-xl border border-dashed border-base-300"}
          ($ :p {:class "text-base-content/60"} "Nema podataka o kategorijama artikala.")
          ($ :p {:class "text-xs text-base-content/40 mt-2"} "Kategorije se automatski popunjavaju iz artikala na računima."))))))
