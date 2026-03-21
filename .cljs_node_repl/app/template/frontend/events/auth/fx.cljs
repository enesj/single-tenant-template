(ns app.template.frontend.events.auth.fx
  (:require
    [re-frame.core :as rf]))

;; ========================================================================
;; Effects
;; ========================================================================

;; Helper effect handler for redirects
(rf/reg-fx
  :redirect
  (fn [path]
    (set! (.-href js/window.location) path)))

