(ns app.template.frontend.components.sidebar
  (:require [uix.core :refer [$ defui]]))

(defui sidebar-item [{:keys [id label href icon active? on-click className]}]
  ($ :li {:class "mb-1"}
    ($ :a {:id id
           :href href
           :on-click on-click
           :class (str (when active? "ds-active")
                    (when className (str " " className)))}
      (when icon
        icon)
      label)))

(defui sidebar-section [{:keys [title items subsections]}]
  ($ :<>
    (when title
      ($ :li {:class "ds-menu-title mt-4 mb-2"}
        ($ :span title)))

    (when (seq items)
      (for [{:keys [id label href] :as item} items]
        ($ sidebar-item (assoc item :key (or id href label)))))

    (when (seq subsections)
      (for [{sub-title :title sub-items :items sub-id :id} subsections]
        ($ :<> {:key (or sub-id sub-title)}
          (when sub-title
            ($ :li {:class "ds-menu-title mt-2 mb-1 pl-2"}
              ($ :span {:class "text-xs"} sub-title)))
          (for [{:keys [id label href className] :as item} sub-items]
            ($ sidebar-item
              (assoc item
                :key (or id href label)
                :className (str "pl-4" (when className (str " " className)))))))))))

(defui sidebar [{:keys [title sections footer className]}]
  ($ :div {:class (str "hidden md:flex md:flex-shrink-0 text-base-content " className)}
    ($ :div {:class "flex flex-col w-64"}
      ;; Sidebar container using base-200 background
      ($ :div {:class "flex flex-col h-0 flex-1 bg-base-200"}
        ;; Header area
        ($ :div {:class "flex-1 flex flex-col pt-5 pb-4 overflow-y-auto"}
          ($ :div {:class "flex items-center flex-shrink-0 px-4 mb-2"}
            ($ :h1 {:class "text-xl font-bold"} title))

          ;; Navigation Menu using DaisyUI menu component
          ($ :ul {:class "ds-menu ds-menu-md w-full px-2"}
            (for [{:keys [title] :as section} sections]
              ($ sidebar-section (assoc section :key (or title "main"))))))

        ;; Footer
        (when footer
          ($ :div {:class "flex-shrink-0 border-t border-base-300 p-4 bg-base-200"}
            footer))))))
