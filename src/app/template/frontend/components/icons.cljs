(ns app.template.frontend.components.icons
  (:require
    [app.template.frontend.components.button :refer [button]]
    [uix.core :refer [$ defui]]))

(defui google-icon []
  ($ :svg {:class "w-4 h-4" :viewBox "0 0 24 24" :fill "currentColor"}
    ($ :path {:d "M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" :fill "#4285F4"})
    ($ :path {:d "M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" :fill "#34A853"})
    ($ :path {:d "M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" :fill "#FBBC05"})
    ($ :path {:d "M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" :fill "#EA4335"})
    ($ :path {:d "M1 1h22v22H1z" :fill "none"})))

(defui github-icon []
  ($ :svg {:class "w-4 h-4" :viewBox "0 0 24 24" :fill "currentColor"}
    ($ :path {:d "M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" :fill "#4285F4"})
    ($ :path {:d "M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" :fill "#34A853"})
    ($ :path {:d "M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" :fill "#FBBC05"})
    ($ :path {:d "M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" :fill "#EA4335"})
    ($ :path {:d "M1 1h22v22H1z" :fill "none"})))

(defui default-provider-icon []
  ($ :svg {:class "w-4 h-4" :viewBox "0 0 24 24" :fill "currentColor"}
    ($ :path {:d "M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"})))

(defui edit-icon []
  ($ :svg {:class "w-5 h-5"
           :fill "none"
           :stroke "currentColor"
           :viewBox "0 0 24 24"
           :xmlns "http://www.w3.org/2000/svg"}
    ($ :path {:stroke-linecap "round"
              :stroke-linejoin "round"
              :stroke-width "2"
              :d "M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"})))

(defui delete-icon []
  ($ :svg {:class "w-5 h-5"
           :fill "none"
           :stroke "currentColor"
           :viewBox "0 0 24 24"
           :xmlns "http://www.w3.org/2000/svg"}
    ($ :path {:stroke-linecap "round"
              :stroke-linejoin "round"
              :stroke-width "2"
              :d "M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"})))

(defui trash-icon []
  ($ :svg {:class "w-5 h-5"
           :fill "none"
           :stroke "currentColor"
           :viewBox "0 0 24 24"
           :xmlns "http://www.w3.org/2000/svg"}
    ($ :path {:stroke-linecap "round"
              :stroke-linejoin "round"
              :stroke-width "2"
              :d "M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"})))

(defui chevron-left-icon []
  ($ :svg {:class "w-5 h-5"
           :fill "none"
           :stroke "currentColor"
           :viewBox "0 0 24 24"
           :xmlns "http://www.w3.org/2000/svg"}
    ($ :path {:stroke-linecap "round"
              :stroke-linejoin "round"
              :stroke-width "2"
              :d "M15 19l-7-7 7-7"})))

(defui chevron-right-icon []
  ($ :svg {:class "w-5 h-5"
           :fill "none"
           :stroke "currentColor"
           :viewBox "0 0 24 24"
           :xmlns "http://www.w3.org/2000/svg"}
    ($ :path {:stroke-linecap "round"
              :stroke-linejoin "round"
              :stroke-width "2"
              :d "M9 5l7 7-7 7"})))

(defui plus-icon []
  ($ :svg {:class "w-5 h-5"
           :fill "none"
           :stroke "currentColor"
           :viewBox "0 0 24 24"
           :xmlns "http://www.w3.org/2000/svg"}
    ($ :path {:stroke-linecap "round"
              :stroke-linejoin "round"
              :stroke-width "2"
              :d "M12 6v6m0 0v6m0-6h6m-6 0H6"})))

(defui save-icon []
  ($ :svg {:class "w-5 h-5"
           :fill "none"
           :stroke "currentColor"
           :viewBox "0 0 24 24"
           :xmlns "http://www.w3.org/2000/svg"}
    ($ :path {:stroke-linecap "round"
              :stroke-linejoin "round"
              :stroke-width "2"
              :d "M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3 3m0 0l-3-3m3 3V4"})))

(defui cancel-icon []
  ($ :svg {:class "w-5 h-5"
           :fill "none"
           :stroke "currentColor"
           :viewBox "0 0 24 24"
           :xmlns "http://www.w3.org/2000/svg"}
    ($ :path {:stroke-linecap "round"
              :stroke-linejoin "round"
              :stroke-width "2"
              :d "M6 18L18 6M6 6l12 12"})))

(defui view-icon [{:keys [title]}]
  ($ :svg {:class "w-5 h-5"
           :fill "none"
           :stroke "currentColor"
           :viewBox "0 0 24 24"
           :xmlns "http://www.w3.org/2000/svg"
           :title title}
    (when title ($ :title {} title))
    ($ :path {:stroke-linecap "round"
              :stroke-linejoin "round"
              :stroke-width "2"
              :d "M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z"})
    ($ :path {:stroke-linecap "round"
              :stroke-linejoin "round"
              :stroke-width "2"
              :d "M15 12a3 3 0 11-6 0 3 3 0 016 0z"})))

(defui hide-icon []
  ($ :svg {:class "w-5 h-5"
           :fill "none"
           :stroke "currentColor"
           :viewBox "0 0 24 24"
           :xmlns "http://www.w3.org/2000/svg"}
    ($ :path {:stroke-linecap "round"
              :stroke-linejoin "round"
              :stroke-width "2"
              :d "M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.574M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88"})))

(defui settings-icon []
  ($ :svg {:class "w-5 h-5"
           :fill "none"
           :stroke "currentColor"
           :viewBox "0 0 24 24"
           :xmlns "http://www.w3.org/2000/svg"}
    ($ :path {:stroke-linecap "round"
              :stroke-linejoin "round"
              :stroke-width "2"
              :d "M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"})
    ($ :path {:stroke-linecap "round"
              :stroke-linejoin "round"
              :stroke-width "2"
              :d "M15 12a3 3 0 11-6 0 3 3 0 016 0z"})))

(defui settings-icon-large []
  ($ :svg {:class "w-8 h-8 text-primary-content"
           :fill "none"
           :stroke "currentColor"
           :viewBox "0 0 24 24"
           :xmlns "http://www.w3.org/2000/svg"}
    ($ :path {:stroke-linecap "round"
              :stroke-linejoin "round"
              :stroke-width "2"
              :d "M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"})
    ($ :path {:stroke-linecap "round"
              :stroke-linejoin "round"
              :stroke-width "2"
              :d "M15 12a3 3 0 11-6 0 3 3 0 016 0z"})))

(defui filter-icon [{:keys [on-click active? field-id disabled? title]}]
  ($ button {:type "button"
             :btn-type :ghost
             :class (str "ds-btn-xs p-0 m-0 ml-1 "
                      (when disabled? "opacity-50 cursor-not-allowed"))
             :id (when field-id (str "filter-icon-" (name field-id)))
             :tab-index (if disabled? -1 0)
             :aria-label "Filter"
             :title (or title "Filter")
             :disabled disabled?
             :style {:line-height "0" :vertical-align "middle"}
             :on-click (fn [e]
                         (.stopPropagation e)
                         (when (and on-click (not disabled?)) (on-click e)))}
    ($ :svg {:class (str "w-4 h-4 hover:text-accent " (when active? "drop-shadow-sm"))
             :viewBox "0 0 24 24"
             :fill "none"
             :stroke (cond
                       disabled? "#d1d5db"  ; Light gray when disabled
                       active? "#3b82f6"    ; Blue when active
                       :else "#6b7280")     ; Gray when inactive
             :stroke-width (if active? "2.5" "1.7")
             :stroke-linecap "round"
             :stroke-linejoin "round"}
      ($ :polygon {:points "22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"}))))

(defui check-icon [{:keys [class] :or {class "h-5 w-5"}}]
  ($ :svg {:class class
           :fill "currentColor"
           :viewBox "0 0 20 20"
           :xmlns "http://www.w3.org/2000/svg"}
    ($ :path {:fill-rule "evenodd"
              :d "M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
              :clip-rule "evenodd"})))

(defui home-icon [{:keys [class] :or {class "h-5 w-5"}}]
  ($ :svg {:class class
           :fill "none"
           :viewBox "0 0 24 24"
           :stroke "currentColor"
           :xmlns "http://www.w3.org/2000/svg"}
    ($ :path {:stroke-linecap "round"
              :stroke-linejoin "round"
              :stroke-width "2"
              :d "M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"})))

(defui user-icon [{:keys [class] :or {class "h-5 w-5"}}]
  ($ :svg {:class class
           :fill "currentColor"
           :viewBox "0 0 20 20"
           :xmlns "http://www.w3.org/2000/svg"}
    ($ :path {:fill-rule "evenodd"
              :d "M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z"
              :clip-rule "evenodd"})))

(defui crown [{:keys [class] :or {class "w-5 h-5"}}]
  ($ :svg {:class class
           :fill "currentColor"
           :viewBox "0 0 24 24"
           :xmlns "http://www.w3.org/2000/svg"}
    ($ :path {:d "M5 16L3 4l5.5 3.5L12 4l3.5 3.5L21 4l-2 12H5z"})))

(defui chart-bar [{:keys [class] :or {class "w-5 h-5"}}]
  ($ :svg {:class class
           :fill "none"
           :stroke "currentColor"
           :viewBox "0 0 24 24"
           :xmlns "http://www.w3.org/2000/svg"}
    ($ :path {:stroke-linecap "round"
              :stroke-linejoin "round"
              :stroke-width "2"
              :d "M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"})))

(defui arrow-up [{:keys [class] :or {class "w-5 h-5"}}]
  ($ :svg {:class class
           :fill "none"
           :stroke "currentColor"
           :viewBox "0 0 24 24"
           :xmlns "http://www.w3.org/2000/svg"}
    ($ :path {:stroke-linecap "round"
              :stroke-linejoin "round"
              :stroke-width "2"
              :d "M7 14l5-5 5 5"})))

(defui credit-card [{:keys [class] :or {class "w-5 h-5"}}]
  ($ :svg {:class class
           :fill "none"
           :stroke "currentColor"
           :viewBox "0 0 24 24"
           :xmlns "http://www.w3.org/2000/svg"}
    ($ :path {:stroke-linecap "round"
              :stroke-linejoin "round"
              :stroke-width "2"
              :d "M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z"})))

(defui x-mark [{:keys [class] :or {class "w-5 h-5"}}]
  ($ :svg {:class class
           :fill "none"
           :stroke "currentColor"
           :viewBox "0 0 24 24"
           :xmlns "http://www.w3.org/2000/svg"}
    ($ :path {:stroke-linecap "round"
              :stroke-linejoin "round"
              :stroke-width "2"
              :d "M6 18L18 6M6 6l12 12"})))

(defui exclamation-triangle [{:keys [class] :or {class "w-5 h-5"}}]
  ($ :svg {:class class
           :fill "none"
           :stroke "currentColor"
           :viewBox "0 0 24 24"
           :xmlns "http://www.w3.org/2000/svg"}
    ($ :path {:stroke-linecap "round"
              :stroke-linejoin "round"
              :stroke-width "2"
              :d "M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.704-.833-2.464 0L4.34 16.5c-.77.833.192 2.5 1.732 2.5z"})))

(defui exclamation-circle [{:keys [class] :or {class "w-5 h-5"}}]
  ($ :svg {:class class
           :fill "none"
           :stroke "currentColor"
           :viewBox "0 0 24 24"
           :xmlns "http://www.w3.org/2000/svg"}
    ($ :path {:stroke-linecap "round"
              :stroke-linejoin "round"
              :stroke-width "2"
              :d "M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"})))

(defui arrow-path [{:keys [class] :or {class "w-5 h-5"}}]
  ($ :svg {:class class
           :fill "none"
           :stroke "currentColor"
           :viewBox "0 0 24 24"
           :xmlns "http://www.w3.org/2000/svg"}
    ($ :path {:stroke-linecap "round"
              :stroke-linejoin "round"
              :stroke-width "2"
              :d "M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"})))

(defui check-circle [{:keys [class] :or {class "w-5 h-5"}}]
  ($ :svg {:class class
           :fill "currentColor"
           :viewBox "0 0 20 20"
           :xmlns "http://www.w3.org/2000/svg"}
    ($ :path {:fill-rule "evenodd"
              :d "M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
              :clip-rule "evenodd"})))

(defui mail [{:keys [class] :or {class "w-5 h-5"}}]
  ($ :svg {:class class
           :fill "none"
           :stroke "currentColor"
           :viewBox "0 0 24 24"
           :xmlns "http://www.w3.org/2000/svg"}
    ($ :path {:stroke-linecap "round"
              :stroke-linejoin "round"
              :stroke-width "2"
              :d "M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"})))
