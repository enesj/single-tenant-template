(ns app.domain.frontend.expenses.pages.admin.unmapped-items
  (:require
    [app.admin.frontend.components.layout :as layout]
    [app.domain.frontend.expenses.components.unmapped-items :refer [unmapped-items-panel]]
    [uix.core :refer [$ defui]]))


(defui admin-unmapped-items-page
  []
  ($ layout/admin-layout
    ($ unmapped-items-panel
      {:breadcrumbs [{:label "Admin" :href "/admin"}
                     {:label "Unmapped Items"}]
       :title "Unmapped Items"})))
