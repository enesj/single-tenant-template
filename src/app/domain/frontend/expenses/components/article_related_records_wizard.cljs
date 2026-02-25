(ns app.domain.frontend.expenses.components.article-related-records-wizard
  (:require
    [app.domain.frontend.expenses.events.articles :as articles-events]
    app.domain.frontend.expenses.subs.articles
    [app.template.frontend.components.modal-wrapper :refer [modal-wrapper]]
    [app.template.frontend.components.shared-utils :as shared]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

(def ^:private related-type-options
  [{:id "expenses"
    :label "Expenses"
    :description "Expenses that include this article."}
   {:id "receipts"
    :label "Receipts"
    :description "Receipts connected through related expenses."}
   {:id "providers"
    :label "Providers"
    :description "Providers associated with this article."}
   {:id "stores"
    :label "Stores"
    :description "Stores where this article appears."}
   {:id "manufacturers"
    :label "Manufacturers"
    :description "Manufacturers linked on this article."}
   {:id "subcategories"
    :label "Subcategories"
    :description "Subcategories assigned to this article."}])

(defn- article-id
  [article]
  (or (:id article) (:article-id article)))

(defn- article-name
  [article]
  (or (:canonical-name article)
    (:canonical_name article)
    "Selected article"))

(defn- related-type-label
  [related-type]
  (case related-type
    "expenses" "Expenses"
    "receipts" "Receipts"
    "providers" "Providers"
    "stores" "Stores"
    "manufacturers" "Manufacturers"
    "subcategories" "Subcategories"
    "Records"))

(defn- details-label
  [key-value]
  (-> key-value
    name
    (str/replace #"-" " ")
    (str/split #"\s+")
    (->> (map str/capitalize)
      (str/join " "))))

(defn- money-label
  [amount currency]
  (if (some? amount)
    (str amount (when (seq currency) (str " " currency)))
    "—"))

(defn- record-summary
  [related-type record]
  (case related-type
    "expenses"
    (str (or (shared/format-date (:purchased-at record)) "Unknown date")
      " • "
      (or (:supplier-display-name record) "Unknown provider")
      " • "
      (money-label (:total-amount record) (:currency record)))

    "receipts"
    (str (or (:original-filename record) "Unnamed receipt")
      " • "
      (or (:status record) "unknown"))

    "providers"
    (str (or (:display-name record) "Unnamed provider")
      (when-let [normalized-key (:normalized-key record)]
        (str " • " normalized-key)))

    "stores"
    (str (or (:display-name record) "Unnamed store")
      (when-let [supplier (:supplier-display-name record)]
        (str " • " supplier)))

    "manufacturers"
    (str (or (:display-name record) "Unnamed manufacturer")
      (when-let [normalized-key (:normalized-key record)]
        (str " • " normalized-key)))

    "subcategories"
    (str (or (:name record) "Unnamed subcategory")
      (when-let [category-name (:category-name record)]
        (str " • " category-name)))

    (or (str (:id record)) "Record")))

(defui article-related-records-wizard
  []
  (let [open? (use-subscribe [:expenses/article-related-records-modal-open?])
        article (use-subscribe [:expenses/article-related-records-modal-entity])
        step (use-subscribe [:expenses/article-related-records-step])
        related-type (use-subscribe [:expenses/article-related-records-type])
        records (use-subscribe [:expenses/article-related-records])
        selected-record (use-subscribe [:expenses/article-related-record])
        loading? (use-subscribe [:expenses/article-related-records-loading?])
        error (use-subscribe [:expenses/article-related-records-error])
        article-id* (or (article-id article) "unknown")]
    ($ modal-wrapper
      {:id (str "modal-related-records-article-" article-id*)
       :visible? open?
       :title "Show related records"
       :size :large
       :close-button-id (str "btn-close-related-records-article-" article-id*)
       :on-close [::articles-events/close-related-records-modal]}
      ($ :div {:class "space-y-5"
               :id (str "wizard-related-records-article-" article-id*)}
        ($ :div {:class "rounded-lg border border-base-300 bg-base-200/40 p-4 space-y-2"
                 :id (str "context-related-records-article-" article-id*)}
          ($ :div {:class "flex items-center justify-between gap-3"}
            ($ :div {:class "ds-badge ds-badge-outline ds-badge-sm"
                     :id (str "text-related-records-step-article-" article-id*)}
              (str "Step " step " of 3"))
            ($ :div {:class "text-xs uppercase tracking-wide text-base-content/60"}
              "Selected article"))
          ($ :div {:class "text-sm font-semibold text-base-content break-words"
                   :id (str "text-related-records-article-name-article-" article-id*)}
            (article-name article)))

        (case step
          1
          ($ :div {:class "space-y-4"
                   :id (str "step-1-related-records-article-" article-id*)}
            ($ :div {:class "text-sm text-base-content/80"}
              "Choose related record type")
            ($ :div {:class "grid grid-cols-1 sm:grid-cols-2 gap-2"}
              (for [{:keys [id label description]} related-type-options
                    :let [selected? (= related-type id)]]
                ($ :button {:key id
                            :id (str "btn-related-type-" id "-article-" article-id*)
                            :class (str "w-full rounded-lg border px-4 py-3 text-left transition "
                                     "focus:outline-none focus-visible:ring-2 focus-visible:ring-primary "
                                     (if selected?
                                       "border-primary bg-primary/10 shadow-sm"
                                       "border-base-300 bg-base-100 hover:border-base-content/30 hover:bg-base-200/60"))
                            :on-click (fn []
                                        (rf/dispatch [::articles-events/select-related-type id]))}
                  ($ :div {:class "flex items-start justify-between gap-2"}
                    ($ :span {:class "text-sm font-medium text-base-content"}
                      label)
                    (when selected?
                      ($ :span {:class "ds-badge ds-badge-primary ds-badge-sm"}
                        "Selected")))
                  ($ :div {:class "text-xs text-base-content/70 mt-1"}
                    description))))
            ($ :div {:class "flex items-center justify-between gap-3"}
              ($ :div {:id (str "text-related-step-1-selection-article-" article-id*)
                       :class "text-xs text-base-content/60"}
                (if related-type
                  (str "Selected: " (related-type-label related-type))
                  "Select one type to continue."))
              ($ :button {:id (str "btn-next-related-step-1-article-" article-id*)
                          :class "ds-btn ds-btn-primary"
                          :disabled (nil? related-type)
                          :on-click (fn []
                                      (rf/dispatch [::articles-events/next-related-records-step]))}
                "Next")))

          2
          ($ :div {:class "space-y-3"
                   :id (str "step-2-related-records-article-" article-id*)}
            ($ :div {:class "flex items-center justify-between gap-3"}
              ($ :div {:class "text-sm text-base-content/80"}
                (str "Choose one " (str/lower-case (related-type-label related-type)) " record"))
              ($ :div {:class "flex items-center gap-2"}
                ($ :button {:id (str "btn-back-related-step-1-article-" article-id*)
                            :class "ds-btn ds-btn-ghost ds-btn-sm"
                            :on-click (fn []
                                        (rf/dispatch [::articles-events/back-related-records-step]))}
                  "Back")
                ($ :button {:id (str "btn-next-related-step-2-article-" article-id*)
                            :class "ds-btn ds-btn-primary ds-btn-sm"
                            :disabled (or loading? (nil? selected-record))
                            :on-click (fn []
                                        (rf/dispatch [::articles-events/next-related-records-step]))}
                  "Next")))

            (cond
              loading?
              ($ :div {:id (str "loading-related-records-article-" article-id*)
                       :class "rounded-lg border border-base-300 bg-base-200/40 p-3 flex items-center gap-2"}
                ($ :span {:class "ds-loading ds-loading-spinner ds-loading-sm"})
                ($ :span {:class "text-sm"} "Loading related records..."))

              error
              ($ :div {:id (str "error-related-records-article-" article-id*)
                       :class "ds-alert ds-alert-error"}
                ($ :span (str error)))

              (seq records)
              ($ :div {:class "space-y-2 max-h-[420px] overflow-y-auto pr-1"
                       :id (str "list-related-records-article-" article-id*)}
                (for [record records
                      :let [record-id (or (:id record) (hash record))
                            selected? (= selected-record record)]]
                  ($ :button {:key (str record-id)
                              :id (str "btn-select-related-record-" related-type "-" record-id "-article-" article-id*)
                              :class (str "w-full rounded-lg border p-3 text-left transition "
                                       "focus:outline-none focus-visible:ring-2 focus-visible:ring-primary "
                                       (if selected?
                                         "border-primary bg-primary/10 shadow-sm"
                                         "border-base-300 bg-base-100 hover:border-base-content/30 hover:bg-base-200/60"))
                              :on-click (fn []
                                          (rf/dispatch [::articles-events/select-related-record record]))}
                    ($ :div {:class "flex items-start justify-between gap-3"}
                      ($ :span {:class "text-sm text-base-content"}
                        (record-summary related-type record))
                      (when selected?
                        ($ :span {:class "ds-badge ds-badge-primary ds-badge-sm"}
                          "Selected"))))))

              :else
              ($ :div {:id (str "empty-related-records-article-" article-id*)
                       :class "rounded-lg border border-dashed border-base-300 bg-base-100 p-3 text-sm text-base-content/70"}
                "No related records found.")))

          3
          ($ :div {:class "space-y-3"
                   :id (str "step-3-related-records-article-" article-id*)}
            ($ :div {:class "flex items-center justify-between"}
              ($ :div {:class "text-sm text-base-content/80"}
                (str "Details: " (related-type-label related-type)))
              ($ :button {:id (str "btn-back-related-step-2-article-" article-id*)
                          :class "ds-btn ds-btn-ghost ds-btn-sm"
                          :on-click (fn []
                                      (rf/dispatch [::articles-events/back-related-records-step]))}
                "Back"))

            (if (map? selected-record)
              ($ :div {:class "grid grid-cols-1 md:grid-cols-2 gap-2"
                       :id (str "details-related-record-article-" article-id*)}
                (for [[key-value value] (sort-by (comp name key) (seq selected-record))]
                  ($ :div {:key (str (name key-value))
                           :id (str "detail-related-record-" (name key-value) "-article-" article-id*)
                           :class "p-3 rounded-lg border border-base-300 bg-base-100 space-y-1"}
                    ($ :div {:class "text-xs uppercase tracking-wide text-base-content/60"}
                      (details-label key-value))
                    ($ :div {:class "text-sm"}
                      (shared/format-value value "—" false)))))
              ($ :div {:class "rounded-lg border border-dashed border-base-300 bg-base-100 p-3 text-sm text-base-content/70"}
                "No record selected.")))

          nil)))))
