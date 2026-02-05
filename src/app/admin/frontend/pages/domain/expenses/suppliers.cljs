(ns app.admin.frontend.pages.domain.expenses.suppliers
  "Admin Suppliers page.

  Renders an admin-native list backed by the expenses admin API."
  (:require
    [app.admin.frontend.components.layout :as layout]
    [app.template.frontend.components.list :refer [list-view]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-callback use-effect]]
    [uix.re-frame :refer [use-subscribe]]

    ;; Side-effect requires: register sync handlers, entity spec fallbacks, CRUD bridges,
    ;; and list-loading events.
    app.domain.frontend.expenses.admin.adapters.admin-crud
    app.domain.frontend.expenses.admin.adapters.specs
    app.domain.frontend.expenses.admin.adapters.sync
    app.domain.frontend.expenses.events.suppliers))

(defui admin-suppliers-page
  "Admin route: /admin/suppliers"
  []
  (let [entity-name :suppliers
        entity-spec (use-subscribe [(keyword "entity-specs" (name entity-name))])
        refresh-list (use-callback
                       (fn []
                         (rf/dispatch [:app.domain.frontend.expenses.events.suppliers/load-list
                                       {:fetch-limit 1000 :fetch-offset 0}]))
                       [])]
    (use-effect
      (fn []
        (refresh-list)
        js/undefined)
      [refresh-list])

    ($ layout/admin-layout
      ($ :div {:class "p-6 min-h-screen"}
        ($ :div {:class "mb-6"}
          ($ :h1 {:class "text-2xl font-semibold text-base-content"}
            "Suppliers")
          ($ :p {:class "text-sm text-base-content/70 mt-1"}
            "Suppliers from the Expenses domain (admin API)."))

        ($ list-view
          {:entity-name entity-name
           :entity-spec entity-spec
           :title "Suppliers"
           :form-display :modal
           :allow-add? false
           :allow-edit? true
           :allow-delete? true
           :disallowed-action-mode :hide})))))
