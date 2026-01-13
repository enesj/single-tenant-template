(ns app.domain.frontend.expenses.pages.user.unmapped-items
  (:require
    [app.domain.frontend.expenses.components.unmapped-items :refer [unmapped-items-panel]]
    [app.domain.frontend.expenses.components.page-guard :as page-guard]
    [uix.core :refer [$ defui]]))

(defui unmapped-items-page
  []
  ($ page-guard/power-user-guard
    {:children ($ unmapped-items-panel
                 {:breadcrumbs [{:label "Dashboard" :href "/dashboard"}
                                {:label "Unmapped Items"}]
                  :title "Unmapped Items"})}))