(ns app.admin.frontend.pages.domain.expenses.unmapped-aliases
  "Admin Unmapped Aliases page.

  Renders an admin-native list backed by the expenses admin API."
  (:require
    [app.admin.frontend.components.layout :as layout]
    [app.template.frontend.components.list :refer [list-view]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-callback use-effect]]
    [uix.re-frame :refer [use-subscribe]]

    ;; Side-effect requires
    app.domain.frontend.expenses.admin.adapters.specs
    app.domain.frontend.expenses.admin.adapters.sync
    app.domain.frontend.expenses.events.unmapped-aliases))

(defui admin-unmapped-aliases-page
  "Admin route: /admin/unmapped-aliases"
  []
  (let [entity-name :unmapped-aliases
        entity-spec (use-subscribe [(keyword "entity-specs" (name entity-name))])
        refresh-list (use-callback
                       (fn []
                         (rf/dispatch [:app.domain.frontend.expenses.events.unmapped-aliases/load-list
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
            "Unmapped Aliases")
          ($ :p {:class "text-sm text-base-content/70 mt-1"}
            "Unmapped article aliases (admin API helper endpoint)."))

        ($ list-view
          {:entity-name entity-name
           :entity-spec entity-spec
           :title "Unmapped Aliases"
           :form-display :modal
           :allow-add? false
           :allow-edit? false
           :allow-delete? false
           :disallowed-action-mode :hide})))))
