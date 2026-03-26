(ns app.admin.frontend.pages.domain.expenses.unmapped-aliases
  "Admin Unmapped Aliases page.

  Renders an admin-native list backed by the expenses admin API."
  (:require
    [app.admin.frontend.components.layout :as layout]
    [app.domain.frontend.expenses.events.unmapped-aliases :as unmapped-aliases-events]
    [app.template.frontend.components.list :refer [list-view]]
    [app.template.frontend.events.list.ui-state :as ui-state]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-callback use-effect]]
    [uix.re-frame :refer [use-subscribe]]

    ;; Side-effect requires
    app.domain.frontend.expenses.admin.adapters.specs
    app.domain.frontend.expenses.admin.adapters.sync))

(defn dispatch-admin-unmapped-aliases-refresh!
  [dispatch! dispatch-sync!]
  (dispatch-sync! [::ui-state/set-pagination-mode :unmapped-aliases :server])
  (dispatch-sync! [::ui-state/set-refresh-event :unmapped-aliases [::unmapped-aliases-events/load-list]])
  (dispatch! [::unmapped-aliases-events/load-list {:page 1 :per-page 50}]))

(defui admin-unmapped-aliases-page
  "Admin route: /admin/unmapped-aliases"
  []
  (let [entity-name :unmapped-aliases
        entity-spec (use-subscribe [(keyword "entity-specs" (name entity-name))])
        refresh-list (use-callback
                       (fn []
                         (dispatch-admin-unmapped-aliases-refresh! rf/dispatch rf/dispatch-sync))
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
           :title "Unmapped Aliases"})))))
