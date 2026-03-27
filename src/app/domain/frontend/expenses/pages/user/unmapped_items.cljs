(ns app.domain.frontend.expenses.pages.user.unmapped-items
  (:require
    [app.domain.frontend.expenses.components.page-guard :as page-guard]
    [app.template.frontend.components.list :refer [list-view]]
    [app.template.frontend.events.list.ui-state :as ui-state]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-callback use-effect]]
    [uix.re-frame :refer [use-subscribe]]

    ;; Side-effect requires
    app.domain.frontend.expenses.admin.adapters.specs
    app.domain.frontend.expenses.admin.adapters.sync
    app.domain.frontend.expenses.events.user-expenses.unmapped-aliases))

(defn dispatch-tenant-unmapped-aliases-refresh!
  [dispatch! dispatch-sync!]
  (dispatch-sync! [::ui-state/set-pagination-mode :unmapped-aliases :server])
  (dispatch-sync! [::ui-state/set-refresh-event :unmapped-aliases [:user-expenses/refresh-unmapped-aliases-list]])
  (dispatch-sync! [::ui-state/set-per-page :unmapped-aliases 50])
  (dispatch-sync! [::ui-state/set-current-page :unmapped-aliases 1])
  (dispatch! [:user-expenses/refresh-unmapped-aliases-list {:page 1 :per-page 50}]))

(defui unmapped-items-page
  []
  (let [entity-name :unmapped-aliases
        entity-spec (use-subscribe [(keyword "entity-specs" (name entity-name))])
        refresh-list (use-callback
                       (fn []
                         (dispatch-tenant-unmapped-aliases-refresh! rf/dispatch rf/dispatch-sync))
                       [])]
    (use-effect
      (fn []
        (refresh-list)
        js/undefined)
      [refresh-list])

    ($ page-guard/power-user-guard
      {:children ($ :div {:class "p-6 min-h-screen"}
                   ($ list-view
                     {:entity-name entity-name
                      :entity-spec entity-spec
                      :title "Unmapped Aliases"
                      :allow-add? false
                      :allow-edit? false
                      :allow-delete? false
                      :display-settings {:show-add-button? false
                                         :show-edit? false
                                         :show-delete? false
                                         :show-batch-edit? false
                                         :show-batch-delete? false}}))})))