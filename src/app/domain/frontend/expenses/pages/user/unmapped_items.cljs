(ns app.domain.frontend.expenses.pages.user.unmapped-items
  (:require
    [app.domain.frontend.expenses.components.unmapped-items :refer [unmapped-items-panel]]
    [app.domain.frontend.expenses.components.page-guard :as page-guard]
    [app.template.frontend.i18n :refer [use-t]]
    [uix.core :refer [$ defui]]))

(defui unmapped-items-page
  []
  (let [t (use-t)]
    ($ page-guard/power-user-guard
      {:children ($ unmapped-items-panel
                   {:breadcrumbs [{:label (t :unmapped-items/breadcrumb-dashboard) :href "/dashboard"}
                                  {:label (t :unmapped-items/title)}]
                    :title (t :unmapped-items/title)})})))