(ns app.admin.frontend.renderers.actions
  "Render helpers for admin entity actions"
  (:require
    [app.shared.keywords :as kw]
    [app.admin.frontend.components.enhanced-action-buttons :as enhanced-actions]
    [app.template.frontend.subs.ui :as ui-subs]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

(defn- entity-prop-key
  [entity-key]
  (case entity-key
    :audit-logs :audit-log
    :users :user
    :tenants :tenant
    (kw/ensure-keyword entity-key)))

(defn- wrap-custom-action
  [custom-component entity-key]
  (let [prop-key (entity-prop-key entity-key)]
    (fn [entity-item]
      ($ custom-component {prop-key entity-item}))))

(defui reactive-enhanced-actions
  "Enhanced action buttons that reactively subscribe to display settings.
   This wrapper ensures Edit/Delete visibility responds to user toggling in the settings panel."
  [{:keys [entity-key entity-item actions-component constraints-enabled? custom-render]}]
  (let [;; Subscribe to entity display settings - this makes the component reactive
        entity-settings (use-subscribe [::ui-subs/entity-display-settings entity-key])
        show-edit? (:show-edit? entity-settings)
        show-delete? (:show-delete? entity-settings)]
    ($ actions-component {:entity-name entity-key
                          :item entity-item
                          :show-edit? show-edit?
                          :show-delete? show-delete?
                          :constraints-enabled? constraints-enabled?
                          :custom-actions custom-render})))

(defn create-actions-renderer
  "Return a function that renders row actions based on entity configuration.
   For enhanced-action-buttons, uses a reactive wrapper that subscribes to display settings."
  [entity-config _display-settings]
  (let [{:keys [entity-key components features]} entity-config
        {:keys [actions custom-actions]} components
        custom-render (when (fn? custom-actions)
                        (wrap-custom-action custom-actions entity-key))]
    (cond
      (and (fn? actions)
        (= actions enhanced-actions/enhanced-action-buttons))
      (let [constraints-enabled? (true? (:deletion-constraints? features))]
        ;; Use a reactive wrapper component that subscribes to display settings
        (fn [entity-item]
          ($ reactive-enhanced-actions {:entity-key entity-key
                                        :entity-item entity-item
                                        :actions-component actions
                                        :constraints-enabled? constraints-enabled?
                                        :custom-render custom-render})))

      (fn? actions)
      (let [prop-key (entity-prop-key entity-key)]
        (fn [entity-item]
          ($ actions {prop-key entity-item})))

      custom-render
      (fn [entity-item]
        (custom-render entity-item))

      :else
      (let [_ (js/console.warn "🔧 [ACTIONS] No action component found for" entity-key)]
        (fn [_] nil)))))
