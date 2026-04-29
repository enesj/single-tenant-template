(ns app.template.frontend.components.list.toggles
  (:require
    [uix.core :refer [$]]))

(defn toggle-group-pill
  "Render a vector of toggle specs as a single rounded segmented pill.

   Spec: {:id :label :toggles [{:id :label :active? :on-click} ...]}
   Returns nil when toggles is empty."
  [{group-id :id group-label :label toggles :toggles}]
  (when (and group-id (seq toggles))
    ($ :div {:key group-id
             :id group-id
             :class (str "flex items-center gap-3 py-0.5 rounded-full border border-base-300 bg-base-100 "
                      (if group-label "pl-3 pr-1" "p-0"))}
      (when group-label
        ($ :span {:class "text-xs text-base-content/60"} group-label))
      ($ :div {:class "flex"}
        (for [[idx {:keys [id label active? on-click]}] (map-indexed vector toggles)
              :when (and id label on-click)]
          ($ :button
            {:key id
             :id id
             :type "button"
             :class (str "px-3 py-0.5 text-sm border border-base-300 "
                      (cond
                        (zero? idx) "rounded-l-full "
                        (= idx (dec (count toggles))) "rounded-r-full border-l-0 "
                        :else "border-l-0 ")
                      (if active?
                        "font-semibold"
                        "font-light hover:bg-base-200"))
             :style (if active?
                      {:background-color "rgba(100, 116, 139, 0.65)"
                       :color "white"}
                      {})
             :on-click on-click}
            label))))))