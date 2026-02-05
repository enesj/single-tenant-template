(ns app.admin.frontend.pages.domain.expenses.articles
  "Admin Articles page.

   Renders an admin-native list backed by the expenses admin API (no iframe), so
   the admin shell stays visible and we don't depend on a user session/capability."
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
    app.domain.frontend.expenses.events.articles))

(defui admin-articles-page
  "Admin route: /admin/articles"
  []
  (let [entity-name :articles
        entity-spec (use-subscribe [(keyword "entity-specs" (name entity-name))])
        refresh-list (use-callback
                       (fn []
                         ;; Template list-view pagination is client-side; fetch a reasonably large
                         ;; set so the list can paginate without additional backend wiring.
                         (rf/dispatch [:app.domain.frontend.expenses.events.articles/load-list
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
            "Articles")
          ($ :p {:class "text-sm text-base-content/70 mt-1"}
            "Articles from the Expenses domain (admin API)."))

        ($ list-view
          {:entity-name entity-name
           :entity-spec entity-spec
           :title "Articles"
           :form-display :modal
           :allow-add? false
           :allow-edit? true
           :allow-delete? true
           :disallowed-action-mode :hide})))))
