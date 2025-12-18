(ns app.template.frontend.utils.test-utils
  "Shared test utilities for frontend components.
   Thin wrapper around specialized testing modules."
  (:require
    [app.template.frontend.utils.test.env :as env]
    [app.template.frontend.utils.test.render :as render]))

;; Re-export environment utilities
(def setup-test-environment! env/setup-test-environment!)
(def reset-test-environment! env/reset-test-environment!)

;; Re-export rendering utilities
(def render-to-static-markup render/render-to-static-markup)
(def enhanced-render-to-static-markup render/enhanced-render-to-static-markup)
(def component-contains? render/component-contains?)
(def component-classes render/component-classes)
