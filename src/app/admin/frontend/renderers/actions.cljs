(ns app.admin.frontend.renderers.actions
  "Render helpers for admin entity actions"
  (:require
    [app.shared.keywords :as kw]
    [app.admin.frontend.components.enhanced-action-buttons :as enhanced-actions]
    [uix.core :refer [$]]))

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

(defn create-actions-renderer
  "Return a function that renders row actions based on entity configuration"
  [entity-config display-settings]
  (let [{:keys [entity-key components features]} entity-config
        {:keys [actions custom-actions]} components
        custom-render (when (fn? custom-actions)
                        (wrap-custom-action custom-actions entity-key))]
    (cond
      (and (fn? actions)
        (= actions enhanced-actions/enhanced-action-buttons))
      (let [show-edit (get display-settings :show-edit? true)
            show-delete (get display-settings :show-delete? true)
            constraints-enabled? (true? (:deletion-constraints? features))]
        (fn [entity-item]
          ($ actions {:entity-name entity-key
                      :item entity-item
                      :show-edit? show-edit
                      :show-delete? show-delete
                      :constraints-enabled? constraints-enabled?
                      :custom-actions custom-render})))

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
