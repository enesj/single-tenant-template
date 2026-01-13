(ns app.domain.frontend.expenses.pages.user.price-observations
  "Power-user view of price observations (read-only list for now)."
  (:require
    [app.domain.frontend.expenses.components.page-guard :refer [expenses-page-guard]]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.list :refer [list-view]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-callback use-effect]]
    [uix.re-frame :refer [use-subscribe]]
    app.domain.frontend.expenses.subs.user-expenses))

(defui price-observations-page []
  (let [entity-name :price-observations
        entity-spec (use-subscribe [:entity-specs/by-name entity-name])
        refresh-list (use-callback
                       (fn []
                         (rf/dispatch [:user-expenses/fetch-price-observations]))
                       [])]
    (use-effect
      (fn []
        (refresh-list)
        js/undefined)
      [refresh-list])

    ($ expenses-page-guard
      {:capability :expenses/articles.manage
       :children
       ($ :div {:class "min-h-screen bg-base-100"}
         ($ :header {:class "bg-white border-b border-base-200"}
           ($ :div {:class "w-full px-4 py-4 sm:py-6"}
             ($ :div {:class "flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4"}
               ($ :div
                 ($ :h1 {:class "text-xl sm:text-2xl font-bold text-base-content"} "Price Observations")
                 ($ :p {:class "text-sm text-base-content/70"}
                   "Power-user price history/observations (read-only list)"))
               ($ :div {:class "flex gap-2 flex-wrap"}
                 ($ button {:id "btn-back-expenses-dashboard-price-observations"
                            :btn-type :ghost
                            :on-click #(rf/dispatch [:navigate-to "/expenses"])}
                   "Dashboard")))))

         ($ :main {:class "w-full px-4 py-6"}
           ($ list-view
             {:entity-name entity-name
              :entity-spec entity-spec
              :title "Price Observations"
              :form-display :modal
              :allow-add? false
              :render-actions (fn [_item] nil)})))})))

