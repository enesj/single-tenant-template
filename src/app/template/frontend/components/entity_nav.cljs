(ns app.template.frontend.components.entity-nav
  "Reusable navigation component for switching between entity views."
  (:require
    [app.template.frontend.components.button :refer [nav-button]]
    [clojure.string :as str]
    [uix.core :refer [$ defui]]))

(defn- normalize-item
  [{:keys [id label button-props] :as _item}
   title-fn]
  (let [entity-id (some-> id keyword)
        derived-label (when entity-id (title-fn entity-id))]
    {:id entity-id
     :label (or label derived-label (str entity-id))
     :button-props button-props}))

(defui entity-nav
  "Render a horizontal row of entity navigation buttons with optional trailing content."
  [{:keys [items active-entity trailing container-class buttons-class trailing-class title-fn]}]
  (let [title-fn (or title-fn (fn [entity]
                                (-> entity name (str/replace "_" " ") str/capitalize)))
        normalized-items (->> (or items [])
                           (map #(normalize-item % title-fn))
                           (filter :id))
        trailing-node (cond
                        (fn? trailing) (trailing active-entity)
                        (vector? trailing) trailing
                        (some? trailing) trailing
                        :else nil)]
    ($ :div {:class (str "flex justify-between mb-8 " (or container-class ""))}
      ($ :div {:class (str "flex space-x-4 " (or buttons-class ""))}
        (for [{:keys [id label button-props]} normalized-items]
          ($ nav-button
            (merge
              {:key (name id)
               :entity-type active-entity
               :target-page id
               :label label}
              button-props))))
      (when trailing-node
        ($ :div {:class (or trailing-class "")}
          trailing-node)))))
