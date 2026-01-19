(ns app.domain.frontend.expenses.admin.components.detail-views.article
  "Article detail view component and add-aliases modal."
  (:require
    [app.template.frontend.components.shared-utils :as shared]
    [app.domain.frontend.expenses.admin.components.detail-views.utils :as utils]
    [app.domain.frontend.expenses.events.article-alias-bulk :as alias-bulk-events]
    [app.domain.frontend.expenses.events.article-aliases :as aliases-events]
    [app.domain.frontend.expenses.events.articles :as articles-events]
    [app.domain.frontend.expenses.events.price-observations :as price-obs-events]
    [app.domain.frontend.expenses.subs.article-alias-bulk :as alias-bulk-subs]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.modal-wrapper :refer [modal-wrapper]]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect use-state]]
    [uix.re-frame :refer [use-subscribe]]))

(defui article-add-aliases-modal
  [{:keys [article-id]}]
  (let [open? (use-subscribe [::alias-bulk-subs/open?])
        working? (use-subscribe [::alias-bulk-subs/working?])
        error (use-subscribe [::alias-bulk-subs/error])
        result (use-subscribe [::alias-bulk-subs/result])
        suppliers (use-subscribe [:expenses/suppliers])
        [supplier-id set-supplier-id!] (use-state "")
        [raw-labels set-raw-labels!] (use-state "")
        [allow-reassign? set-allow-reassign?!] (use-state false)]
    (use-effect
      (fn []
        (when open?
          (set-supplier-id! "")
          (set-raw-labels! "")
          (set-allow-reassign?! false))
        js/undefined)
      [open?])

    ($ modal-wrapper
      {:visible? open?
       :title "Add aliases"
       :size :large
       :close-button-id (when article-id (str "btn-close-add-aliases-article-" article-id))
       :on-close [::alias-bulk-events/close]}
      ($ :div {:id (when article-id (str "modal-add-aliases-article-" article-id))
               :class "space-y-4"}
        (when error
          ($ :div {:class "ds-alert ds-alert-error"}
            ($ :span (str error))))

        ($ :div {:class "grid gap-3 md:grid-cols-2"}
          ($ :div {:class "space-y-2"}
            ($ :label {:class "text-sm font-medium"} "Supplier")
            ($ :select
              {:id (when article-id (str "select-supplier-add-aliases-article-" article-id))
               :class "ds-select ds-select-bordered w-full"
               :value supplier-id
               :on-change #(set-supplier-id! (-> % .-target .-value))}
              ($ :option {:value ""} "Select supplier…")
              (for [s (or suppliers [])
                    :let [sid (:id s)
                          label (or (:display-name s) (:normalized-key s) (str sid))]]
                ($ :option {:key (str sid) :value sid} label))))

          ($ :div {:class "space-y-2"}
            ($ :label {:class "text-sm font-medium"} "Options")
            ($ :label {:class "flex items-center gap-2"}
              ($ :input
                {:id (when article-id (str "toggle-reassign-conflicts-add-aliases-article-" article-id))
                 :type "checkbox"
                 :class "ds-toggle ds-toggle-sm"
                 :checked (boolean allow-reassign?)
                 :on-change #(set-allow-reassign?! (-> % .-target .-checked))})
              ($ :span {:class "text-sm"} "Reassign conflicts (dangerous)"))))

        ($ :div {:class "space-y-2"}
          ($ :label {:class "text-sm font-medium"} "Raw labels (one per line)")
          ($ :textarea
            {:id (when article-id (str "textarea-raw-labels-add-aliases-article-" article-id))
             :class "ds-textarea ds-textarea-bordered w-full"
             :rows 6
             :placeholder "e.g.\nCoca Cola Zero 330ml\nCoke Zero 0.33l"
             :value raw-labels
             :on-change #(set-raw-labels! (-> % .-target .-value))}))

        (when result
          ($ :div {:class "ds-alert ds-alert-info"}
            ($ :div {:class "space-y-1"}
              ($ :div {:class "font-medium"} "Result")
              ($ :div {:class "text-sm"}
                (str "Created: " (count (:created result))
                  ", skipped: " (count (:skipped result))
                  ", conflicts: " (count (:conflicts result))
                  ", reassigned: " (count (:reassigned result)))))))

        ($ :div {:class "flex justify-end gap-2"}
          ($ button
            {:id (when article-id (str "btn-submit-add-aliases-article-" article-id))
             :btn-type :primary
             :loading working?
             :disabled (or working?
                         (str/blank? supplier-id)
                         (str/blank? raw-labels))
             :on-click (fn []
                         (rf/dispatch [::alias-bulk-events/submit
                                       {:article-id article-id
                                        :supplier-id supplier-id
                                        :raw-labels raw-labels
                                        :allow-reassign? allow-reassign?}]))}
            "Add aliases"))))))

(defui article-detail-body
  [{:keys [article-id]}]
  (let [article (use-subscribe [:expenses/article article-id])
        loading? (use-subscribe [:expenses/article-detail-loading?])
        error (use-subscribe [:expenses/articles-error])
        aliases (use-subscribe [:expenses/article-aliases])
        observations (use-subscribe [:expenses/price-observations])]
    (use-effect
      (fn []
        (when article-id
          (rf/dispatch [::articles-events/load-detail article-id])
          (rf/dispatch [::aliases-events/load-list {:article_id article-id :limit 10 :offset 0}])
          (rf/dispatch [::price-obs-events/load-list {:article_id article-id :limit 10 :offset 0}]))
        js/undefined)
      [article-id])

    ($ :div {:class "space-y-6"}
      ($ article-add-aliases-modal {:article-id article-id})

      (when error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span (str error))))

      (cond
        loading?
        ($ :div {:class "ds-loading ds-loading-spinner text-primary"})

        (nil? article)
        ($ :div {:class "ds-alert"} ($ :span "Article not found."))

        :else
        ($ :div {:class "space-y-6"}
          ($ :div {:class "grid gap-3 md:grid-cols-3"}
            (utils/label-value "Name" (:canonical-name article))
            (utils/label-value "Category" (:category article))
            (utils/label-value "Normalized Key" (:normalized-key article))
            (utils/label-value "Created At" (shared/format-date (:created-at article)))
            (utils/label-value "ID" (:id article)))

          ($ :div {:class "grid gap-4 lg:grid-cols-2"}
            ($ utils/related-table
              {:title "Article Aliases"
               :rows aliases
               :columns [{:label "Alias" :value-fn #(:raw-label-normalized %)}
                         {:label "Supplier" :value-fn #(:supplier-display-name %)}
                         {:label "Confidence" :value-fn #(:confidence %)}]
               :empty-label "No aliases mapped to this article."
               :header-actions ($ button
                                {:id (when article-id (str "btn-add-aliases-article-" article-id))
                                 :btn-type :ghost
                                 :class "ds-btn-xs"
                                 :on-click (fn []
                                             (rf/dispatch [::alias-bulk-events/open article-id]))}
                                "Add aliases")
               :view-all-href (when article-id
                                (str "/admin/article-aliases?article_id=" article-id))
               :view-all-id (when article-id
                              (str "btn-view-article-aliases-article-" article-id))})
            ($ utils/related-table
              {:title "Price Observations"
               :rows observations
               :columns [{:label "Observed" :value-fn #(shared/format-date (:observed-at %))}
                         {:label "Supplier" :value-fn #(:supplier-display-name %)}
                         {:label "Unit Price" :value-fn #(:unit-price %)}
                         {:label "Currency" :value-fn #(:currency %)}]
               :empty-label "No price observations for this article."
               :view-all-href (when article-id
                                (str "/admin/price-observations?article_id=" article-id))
               :view-all-id (when article-id
                              (str "btn-view-price-observations-article-" article-id))})))))))
