(ns app.domain.frontend.expenses.admin.components.detail-views.article-alias
  "Article alias detail view component."
  (:require
    [app.template.frontend.components.shared-utils :as shared]
    [app.domain.frontend.expenses.admin.components.detail-views.utils :as utils]
    [app.domain.frontend.expenses.events.article-aliases :as aliases-events]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect]]
    [uix.re-frame :refer [use-subscribe]]))

(defui article-alias-detail-body
  [{:keys [alias-id]}]
  (let [alias (use-subscribe [:expenses/article-alias alias-id])
        loading? (use-subscribe [:expenses/article-alias-detail-loading?])
        error (use-subscribe [:expenses/article-aliases-error])]
    (use-effect
      (fn []
        (when alias-id
          (rf/dispatch [::aliases-events/load-detail alias-id]))
        js/undefined)
      [alias-id])

    ($ :div {:class "space-y-4"}
      (when error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span (str error))))

      (cond
        loading?
        ($ :div {:class "ds-loading ds-loading-spinner text-primary"})

        (nil? alias)
        ($ :div {:class "ds-alert"} ($ :span "Alias not found."))

        :else
        ($ :div {:class "grid gap-3 md:grid-cols-3"}
          (utils/label-value "Supplier" (:supplier-display-name alias))
          (utils/label-value "Article" (:article-canonical-name alias))
          (utils/label-value "Raw Label" (:raw-label alias))
          (utils/label-value "Raw Label Normalized" (:raw-label-normalized alias))
          (utils/label-value "Supplier ID" (:supplier-id alias))
          (utils/label-value "Article ID" (:article-id alias))
          (utils/label-value "Created At" (shared/format-date (:created-at alias)))
          (utils/label-value "ID" (:id alias)))))))
