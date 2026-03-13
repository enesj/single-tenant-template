(ns app.admin.frontend.pages.domain.expenses.countries
  "Admin Countries page.

  Renders an admin-native list backed by the expenses admin API."
  (:require
    [app.admin.frontend.components.layout :as layout]
    [app.domain.frontend.expenses.events.countries :as countries-events]
    [app.template.frontend.components.list :refer [list-view]]
    [app.template.frontend.events.list.ui-state :as ui-state]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-callback use-effect]]
    [uix.re-frame :refer [use-subscribe]]

    ;; Side-effect requires
    app.domain.frontend.expenses.admin.adapters.admin-crud
    app.domain.frontend.expenses.admin.adapters.specs
    app.domain.frontend.expenses.admin.adapters.sync))

(defui admin-countries-page
  "Admin route: /admin/countries"
  []
  (let [entity-name :countries
        entity-spec (use-subscribe [(keyword "entity-specs" (name entity-name))])
        refresh-list (use-callback
                       (fn []
                         (rf/dispatch-sync [::ui-state/set-pagination-mode entity-name :client])
                         (rf/dispatch [::countries-events/load-list {:fetch-limit 500 :fetch-offset 0}]))
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
            "Countries")
          ($ :p {:class "text-sm text-base-content/70 mt-1"}
            "Countries from the Expenses domain (admin API)."))

        ($ list-view
          {:entity-name entity-name
           :entity-spec entity-spec
           :title "Countries"})))))
