(ns app.admin.frontend.pages.entities
  (:require
    [app.admin.frontend.components.admin-layout :refer [admin-layout]]
    [app.admin.frontend.components.entity-page :refer [entity-page]]
    [app.template.frontend.components.headline :refer [headline]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]))

(defui generic-admin-entity-page [entity-key]
  (let [{:keys [page-title page-description adapter-init-fn]} @(rf/subscribe [:admin/entity-config entity-key])
        auth-status @(rf/subscribe [:admin/auth-status])]
    ;; For visitors, show a friendly message
    (if (not (:authenticated auth-status))
      ($ admin-layout
        ($ :div {:class "max-w-3xl mx-auto py-12 px-4"}
          ($ headline {:title "Welcome" :subtitle "Please sign in to access the admin panel."})))

      ;; Authenticated experience
      ($ admin-layout
        (when adapter-init-fn
          (adapter-init-fn))
        ($ :div {:class "max-w-6xl mx-auto py-6 px-4"}
          ($ headline {:title (or page-title "Admin")
                       :subtitle (or page-description "Manage your data")})
          ($ entity-page {:entity-key entity-key}))))))

(defui admin-entities-page []
  (let [entities (-> @(rf/subscribe [:admin/all-entity-configs]) keys vec)
        {:keys [selected-entity]} @(rf/subscribe [:admin/entity-ui-state])
        selected-entity (or selected-entity (first entities))]
    ($ admin-layout
      ($ :div {:class "max-w-6xl mx-auto py-6 px-4"}
        ($ headline {:title "Entities"
                     :subtitle "Manage all entities in one place"})
        ;; Entity selector
        ($ :div {:class "mb-4"}
          ($ :label {:for "entity-select" :class "block text-sm font-medium text-base-content"} "Select entity")
          ($ :select {:id "entity-select"
                      :class "mt-1 block w-full pl-3 pr-10 py-2 text-base border-base-300 focus:outline-none focus:ring-primary focus:border-primary sm:text-sm rounded-md"
                      :value (str selected-entity)
                      :on-change #(rf/dispatch [:admin.ui/select-entity (keyword (.. % -target -value))])}
            (for [e entities]
              ($ :option {:key (str e) :value (str e)} (name e)))))

        ;; Render selected entity page
        ($ entity-page {:entity-key selected-entity})))))
