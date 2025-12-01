(ns app.template.frontend.components.pagination
  (:require
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.common :refer [input]]
    [app.template.frontend.components.icons :refer [chevron-left-icon
                                                    chevron-right-icon]]
    [app.template.frontend.events.list.settings :as settings-events]
    [taoensso.timbre :as log]
    [uix.core :refer [$ defui] :as uix]
    [uix.re-frame :refer [use-subscribe]]))

(defui pagination [{:keys [current-page total-pages on-page-change entity-name]}]
  (let [[go-to-page set-go-to-page!] (uix/use-state (str current-page))
        left-icon-el ($ chevron-left-icon)
        right-icon-el ($ chevron-right-icon)
        table-width (use-subscribe [::settings-events/table-width (when entity-name (keyword entity-name))])]
    ($ :div {:style (when table-width {:max-width (str table-width "px")})}
      ($ :hr {:class "ds-divider m-3"})
      ($ :div {:class "flex items-center justify-between"}
        ($ :div
          ($ :span (str "Page " current-page " of " total-pages)))

        ($ :div {:class "flex items-center space-x-3"}
          ;; Go to page input and button
          ($ :div {:class "flex items-center space-x-1"}
            ($ input {:type "number"
                      :id "input-goto-page"
                      :min 1
                      :max total-pages
                      :value go-to-page
                      :on-change #(set-go-to-page! (.. % -target -value))
                      :class "ds-input ds-input-bordered ds-input-sm w-16"})
            ($ button
              {:btn-type :primary
               :class "ds-btn-sm"
               :id "btn-goto-page"
               :on-click #(let [page (js/parseInt go-to-page)]
                            (when (and (>= page 1) (<= page total-pages))
                              (on-page-change page)
                              (set-go-to-page! (str page))))
               :children "Go to Page"})))

        ($ :div {:class "flex space-x-1"}
          ($ button
            {:shape "circle"
             :btn-type :outline
             :class "ds-btn-circle ds-btn-sm"
             :id "btn-prev-page"
             :disabled (= current-page 1)
             :on-click #(on-page-change (dec current-page))
             :children left-icon-el})
          ($ button
            {:shape "circle"
             :btn-type :outline
             :class "ds-btn-circle ds-btn-sm"
             :id "btn-next-page"
             :disabled (= current-page total-pages)
             :on-click #(on-page-change (inc current-page))
             :children right-icon-el})))
      ($ :hr {:class "ds-divider m-3"}))))
