(ns app.domain.frontend.expenses.pages.user.expense-reports.components
  (:require
    [app.template.frontend.components.button :refer [button]]
    [clojure.string :as str]
    [uix.core :refer [$ defui use-effect use-ref use-state]]))

(defui stat-card [{:keys [title value subtitle icon loading?]}]
  ($ :div {:class "bg-gradient-to-br from-white via-primary/5 to-secondary/10 rounded-xl shadow-sm border border-primary/20 p-6 flex flex-col justify-between h-full transition-all duration-300 hover:shadow-lg hover:shadow-primary/10 hover:border-primary/30 hover:-translate-y-0.5"}
    ($ :div {:class "flex items-start justify-between mb-2"}
      ($ :div
        ($ :p {:class "text-sm font-bold text-base-content/50 tracking-wider uppercase"} title)
        (if loading?
          ($ :div {:class "h-8 w-32 bg-base-200 rounded animate-pulse mt-2"})
          ($ :p {:class "text-3xl font-extrabold mt-1 tracking-tight text-base-content bg-gradient-to-br from-base-content to-base-content/70 bg-clip-text"} value)))
      (when icon
        ($ :div {:class "p-3 bg-gradient-to-br from-primary/20 to-secondary/20 rounded-2xl text-primary ring-1 ring-primary/20 shadow-sm"}
          ($ :span {:class "text-2xl"} icon))))
    (when subtitle
      ($ :div {:class "mt-4 pt-4 border-t border-base-100/60"}
        ($ :p {:class "text-xs font-medium text-base-content/50 flex items-center gap-1.5"}
          ($ :span {:class "w-1.5 h-1.5 rounded-full bg-primary/60 shadow-[0_0_8px_rgba(var(--p)/0.4)]"})
          subtitle)))))

(defui section-shell [{:keys [title subtitle loading? error children header-actions]}]
  ($ :section {:class "bg-gradient-to-br from-white via-base-100 to-primary/5 rounded-2xl shadow-sm border border-primary/15 overflow-hidden flex flex-col h-full transition-all hover:border-primary/30"}
    ($ :div {:class "px-6 py-4 border-b border-primary/10 flex items-center justify-between bg-gradient-to-r from-primary/10 via-secondary/10 to-base-100"}
      ($ :div
        ($ :h3 {:class "font-bold text-lg text-base-content/90 flex items-center gap-2"}
          ($ :span {:class "w-1 h-5 bg-primary/40 rounded-full"})
          title)
        (when subtitle
          ($ :p {:class "text-xs text-base-content/50 mt-0.5 font-medium pl-3"} subtitle)))
      ($ :div {:class "flex items-center gap-3"}
        header-actions
        (when loading?
          ($ :span {:class "ds-loading ds-loading-spinner ds-loading-sm text-primary"}))))
    ($ :div {:class "p-6 flex-1"}
      (if (seq error)
        ($ :div {:class "ds-alert ds-alert-error text-sm rounded-xl shadow-sm"}
          ($ :span (str error)))
        ($ :div {:class "space-y-4 h-full"}
          children)))))

(defui report-multi-select
  [{:keys [id field-label all-label options selected-ids on-change]}]
  (let [[open?, set-open!] (use-state false)
        field-id (or id "reports-filter")
        container-ref (use-ref nil)
        selected-ids* (vec (or selected-ids []))
        selected-set (set selected-ids*)
        selected-count (count selected-ids*)
        option-name-by-id (into {} (map (juxt :id :name) (or options [])))
        first-selection-name (some-> (first selected-ids*) option-name-by-id)
        button-label (cond
                       (zero? selected-count) (or all-label (str "All " (str/lower-case (or field-label "items"))))
                       (= 1 selected-count) (or first-selection-name (or all-label "Select"))
                       :else (str selected-count " selected"))
        toggle-option! (fn [option-id]
                         (let [next-selected (if (contains? selected-set option-id)
                                               (vec (remove #(= % option-id) selected-ids*))
                                               (conj selected-ids* option-id))]
                           (when on-change
                             (on-change (when (seq next-selected) next-selected)))))
        clear-selection! (fn []
                           (when on-change
                             (on-change nil)))]
    (use-effect
      (fn []
        (let [handle-outside-click (fn [event]
                                     (when (and open?
                                             @container-ref
                                             (not (.contains @container-ref (.-target event))))
                                       (set-open! false)))]
          (.addEventListener js/document "mousedown" handle-outside-click)
          (fn []
            (.removeEventListener js/document "mousedown" handle-outside-click))))
      [open?])

    ($ :div {:id (str field-id "-multi-select")
             :ref container-ref
             :class "relative"}
      ($ :button {:id (str field-id "-toggle")
                  :type "button"
                  :class "w-full min-h-8 px-3 py-1.5 text-left bg-white border border-base-300 rounded-lg text-xs flex items-center justify-between gap-2 hover:border-primary/40 focus:outline-none focus:ring-2 focus:ring-primary/20"
                  :on-click #(set-open! (not open?))}
        ($ :span {:class "truncate"} button-label)
        ($ :span {:class "text-base-content/50"} (if open? "▴" "▾")))

      (when open?
        ($ :div {:id (str field-id "-options-panel")
                 :class "absolute z-40 mt-1 w-full rounded-lg border border-base-300 bg-white shadow-lg"}
          ($ :div {:class "max-h-56 overflow-y-auto"}
            ($ :button {:id (str field-id "-option-all")
                        :type "button"
                        :class (str "sticky top-0 z-10 w-full px-3 py-2 text-left text-xs border-b border-base-200 bg-white hover:bg-base-100 "
                                 (if (zero? selected-count)
                                   "font-semibold text-primary"
                                   "text-base-content/70"))
                        :on-click clear-selection!}
              (or all-label "All"))

            (if (seq options)
              (mapv (fn [{:keys [id name]}]
                      (let [option-id (str id)
                            checked? (contains? selected-set option-id)]
                        ($ :button {:id (str field-id "-option-" option-id)
                                    :key option-id
                                    :type "button"
                                    :class "w-full px-3 py-2 text-left text-xs hover:bg-base-100 border-b border-base-100/60 last:border-b-0"
                                    :on-click #(toggle-option! option-id)}
                          ($ :div {:class "flex items-center gap-2"}
                            ($ :input {:type "checkbox"
                                       :class "checkbox checkbox-xs"
                                       :checked checked?
                                       :read-only true})
                            ($ :span {:class "truncate text-base-content/80"} name)))))
                options)
              ($ :div {:class "px-3 py-2 text-xs text-base-content/50"}
                "No options available."))))))))

(def ^:private report-tab-options
  [{:id :expenses :label "Expenses"}
   {:id :articles :label "Articles"}
   {:id :providers-stores :label "Providers & Stores"}
   {:id :categories :label "Categories"}])

(defui report-tabs [{:keys [active-report-tab set-active-report-tab!]}]
  ($ :div {:id "expense-reports-tab-list"
           :class "w-full rounded-2xl border border-primary/20 bg-white/70 backdrop-blur-sm p-2 shadow-sm flex flex-wrap gap-2"}
    (mapv
      (fn [{:keys [id label]}]
        (let [active? (= active-report-tab id)]
          ($ button {:id (str "tab-expense-reports-" (name id))
                     :key (name id)
                     :btn-type (if active? :primary :ghost)
                     :size :sm
                     :class (str "min-w-[120px] font-semibold "
                              (if active?
                                "shadow-sm"
                                "text-base-content/70 hover:text-base-content"))
                     :aria-pressed active?
                     :on-click #(set-active-report-tab! id)}
            label)))
      report-tab-options)))