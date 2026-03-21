(ns app.template.frontend.components.button
  (:require
    [app.template.frontend.events.bootstrap :as bootstrap-events]
    [app.template.frontend.subs.list :as list-subs]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

(def button-props
  {:btn-type {:type :keyword
              :required false}
   :disabled {:type :boolean
              :required false}
   :loading {:type :boolean
             :required false}
   :type {:type :string
          :required false}
   :on-click {:type :function
              :required false}
   :class {:type :string
           :required false}
   :children {:type :any
              :required false}
   :shape {:type :string
           :required false}})

(defui button
  {:prop-types button-props}
  [{:keys [btn-type disabled loading type on-click class children shape]
    :as props}]
  (let [base-classes "ds-btn opacity-85"
        type-class (case btn-type
                     :primary "ds-btn-primary"
                     :secondary "ds-btn-secondary"
                     :success "ds-btn-success"
                     :warning "ds-btn-warning"
                     :accent "ds-btn-accent"
                     :info "ds-btn-info"
                     :error "ds-btn-error"
                     :danger "ds-btn-error"
                     :ghost "ds-btn-ghost"
                     :link "ds-btn-link"
                     :save "ds-btn-primary"
                     :update "ds-btn-secondary"
                     :cancel "ds-btn-outline"
                     :delete "ds-btn-error"
                     :outline "ds-btn-outline"
                     "ds-btn-primary")
        shape-class (when (= shape "circle") "ds-btn-circle")
        loading-class (when loading "ds-loading")
        custom-class (or class "")
        button-type (or type "button")
        click-handler (when (not= button-type "submit") on-click)
        ;; Build the button props map properly
        button-props (merge
                       {:class (str/join " " [base-classes type-class shape-class loading-class custom-class])
                        :disabled (or disabled loading)
                        :type button-type}
                       (when click-handler
                         {:on-click click-handler})
                       (dissoc props :btn-type :loading :type :on-click :class :children :shape))]
    ($ :button button-props
      ($ :div {:class "flex items-center gap-2"}
        (cond
          (vector? children)
          (map-indexed
            (fn [idx child]
              ($ :div {:key idx}
                (if (string? child)
                  ($ :span child)
                  child)))
            children)

          (string? children)
          children

          :else
          children)))))

(defui change-theme [_]
  (let [current-theme (use-subscribe [::list-subs/theme])]
    ($ :div
      ($ :select
        {:id "theme-selector"
         :class "ds-select ds-select-sm"
         :value (or current-theme "light")
         :on-change #(rf/dispatch [::bootstrap-events/set-theme (-> % .-target .-value)])}
        ($ :option {:value "light"} "☀️ Light")
        ($ :option {:value "dark"} "🌙 Dark")
        ($ :option {:value "cupcake"} "🧁 Cupcake")
        ($ :option {:value "bumblebee"} "🐝 Bumblebee")
        ($ :option {:value "emerald"} "💎 Emerald")
        ($ :option {:value "corporate"} "🏢 Corporate")
        ($ :option {:value "synthwave"} "🌆 Synthwave")
        ($ :option {:value "retro"} "📺 Retro")
        ($ :option {:value "cyberpunk"} "🤖 Cyberpunk")
        ($ :option {:value "valentine"} "💝 Valentine")
        ($ :option {:value "halloween"} "🎃 Halloween")
        ($ :option {:value "garden"} "🌸 Garden")
        ($ :option {:value "forest"} "🌲 Forest")
        ($ :option {:value "aqua"} "💧 Aqua")
        ($ :option {:value "lofi"} "🎵 Lofi")
        ($ :option {:value "pastel"} "🎨 Pastel")
        ($ :option {:value "fantasy"} "🔮 Fantasy")
        ($ :option {:value "wireframe"} "📱 Wireframe")
        ($ :option {:value "black"} "⚫ Black")
        ($ :option {:value "luxury"} "✨ Luxury")
        ($ :option {:value "dracula"} "🧛 Dracula")
        ($ :option {:value "cmyk"} "🖨️ CMYK")
        ($ :option {:value "autumn"} "🍂 Autumn")
        ($ :option {:value "business"} "💼 Business")
        ($ :option {:value "acid"} "🌈 Acid")
        ($ :option {:value "lemonade"} "🍋 Lemonade")
        ($ :option {:value "night"} "🌃 Night")
        ($ :option {:value "coffee"} "☕ Coffee")
        ($ :option {:value "winter"} "❄️ Winter")
        ($ :option {:value "dim"} "🔅 Dim")
        ($ :option {:value "nord"} "🗺️ Nord")
        ($ :option {:value "sunset"} "🌅 Sunset")))))


