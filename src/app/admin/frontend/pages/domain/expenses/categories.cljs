(ns app.admin.frontend.pages.domain.expenses.categories
  "Admin Categories page.

  Renders an admin-native list backed by the expenses admin API."
  (:require
    [app.admin.frontend.components.layout :as layout]
    [app.domain.frontend.expenses.events.categories :as categories-events]
    [app.template.frontend.components.list :refer [list-view]]
    [app.template.frontend.events.list.ui-state :as ui-state]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-callback use-effect]]
    [uix.re-frame :refer [use-subscribe]]

    ;; Side-effect requires
    app.domain.frontend.expenses.admin.adapters.admin-crud
    app.domain.frontend.expenses.admin.adapters.specs
    app.domain.frontend.expenses.admin.adapters.sync))

(defui admin-categories-page
  "Admin route: /admin/categories"
  []
  (let [entity-name :categories
        entity-spec (use-subscribe [(keyword "entity-specs" (name entity-name))])
        refresh-list (use-callback
                       (fn []
                         (rf/dispatch-sync [::ui-state/set-pagination-mode entity-name :server])
                         (rf/dispatch-sync [::ui-state/set-refresh-event entity-name [::categories-events/load-list]])
                         (rf/dispatch [::categories-events/load-list]))
                       [])]
    (use-effect
      (fn []
        (refresh-list)
        js/undefined)
      [refresh-list])

    ($ layout/admin-layout
      ($ :div {:class "p-6 min-h-screen"}
        ($ list-view
          {:entity-name entity-name
           :entity-spec entity-spec
           :title "Categories"})))))
