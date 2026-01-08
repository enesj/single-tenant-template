(ns app.domain.frontend.expenses.pages.user.unmapped-items
  (:require
    [app.domain.frontend.expenses.components.unmapped-items :refer [unmapped-items-panel]]
    [app.template.frontend.components.auth-guard :as auth-guard]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

(defn- normalize-role
  [role]
  (cond
    (keyword? role) (name role)
    (string? role) role
    :else nil))

(defui unmapped-items-page
  []
  (let [current-user (use-subscribe [:current-user])
        role (normalize-role (use-subscribe [:user-role]))]
    (if (nil? current-user)
      ($ auth-guard/customer-auth-guard {:authenticated? false})
      ($ auth-guard/role-based-guard
        {:auth-type :customer
         :authenticated? true
         :user-roles [role]
         :required-roles ["admin" "owner"]
         :children ($ unmapped-items-panel
                     {:breadcrumbs [{:label "Dashboard" :href "/dashboard"}
                                    {:label "Unmapped Items"}]
                      :title "Unmapped Items"})}))))
