(ns app.template.frontend.components.summary-cards
  "Generic summary card grid for small dashboard metrics."
  (:require
    [uix.core :refer [$ defui]]))

(def default-container-classes "grid grid-cols-1 md:grid-cols-3 gap-6 mb-8")
(def default-card-classes "bg-white p-6 rounded-lg shadow")
(def default-title-classes "text-lg font-semibold text-gray-800")
(def default-value-classes "text-3xl font-bold text-gray-900")

(defn- ensure-node
  "Convert values, vectors, or functions to a renderable UI node."
  [value]
  (cond
    (fn? value) (value)
    (vector? value) value
    (nil? value) ($ :span "—")
    :else ($ :span value)))

(defui summary-card-grid
  "Render a responsive grid of summary cards.

   Props:
   - :items           Vector of maps with :id, :label, and :value or :render
   - :container-class Optional overrides for the outer grid classes
   - :card-class      Default card classes (per-item override with :card-class)
   - :title-class     Default title classes (per-item override with :label-class)
   - :value-class     Default value classes (per-item override with :value-class)
   - :subtitle-class  Classes for optional :subtitle"
  [{:keys [items container-class card-class title-class value-class subtitle-class]}]
  (let [container-class (or container-class default-container-classes)
        default-card-class (or card-class default-card-classes)
        default-title-class (or title-class default-title-classes)
        default-value-class (or value-class default-value-classes)
        subtitle-class (or subtitle-class "text-sm text-gray-500")]
    ($ :div {:class container-class}
      (for [{:keys [id label subtitle value render] :as item} items
            :let [card-id (or id (keyword (str (gensym "summary-card"))))
                  render-node (ensure-node (or render value))
                  item-card-class (:card-class item)
                  item-title-class (:label-class item)
                  item-value-class (:value-class item)
                  item-wrapper-class (:value-wrapper-class item)]]
        ($ :div {:key (name card-id)
                 :class (or item-card-class default-card-class)}
          (when label
            ($ :h3 {:class (or item-title-class default-title-class)} label))
          (when subtitle
            ($ :div {:class subtitle-class} subtitle))
          ($ :div {:class (or item-wrapper-class "")}
            (let [content-class (or item-value-class default-value-class)]
              (if (vector? render-node)
                ($ :div {:class content-class} render-node)
                ($ :div {:class content-class} render-node)))))))))
