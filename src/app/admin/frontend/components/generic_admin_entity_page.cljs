(ns app.admin.frontend.components.generic-admin-entity-page
  "Configuration-driven admin entity page"
  (:require
    [app.admin.frontend.components.admin-page-wrapper :refer [admin-page-wrapper]]
    [app.admin.frontend.handlers.generic :as generic-handlers]
    [app.admin.frontend.renderers.content :as content-renderer]
    [app.admin.frontend.renderers.modals :as modal-renderer]
    [app.frontend.utils.id :as id-utils]
    [app.template.frontend.subs.entity :as entity-subs]
    [uix.core :refer [$ defui use-memo]]
    [uix.re-frame :refer [use-subscribe]]))

(defn- build-header-renderer
  [header-component]
  (when (fn? header-component)
    (fn []
      ($ header-component))))

(defn- extract-entity-key
  "Extract the actual entity key from various possible formats"
  [entity-key]
  (cond
    (keyword? entity-key) entity-key
    (map? entity-key) (:children entity-key)  ; Extract from {:children :users} format
    (simple-symbol? entity-key) (keyword entity-key)
    :else :unknown))

(defui generic-admin-entity-page
  "Generic configuration-driven admin entity page"
  [entity-key]
  (let [actual-entity-key (extract-entity-key entity-key)
        entity-config (use-subscribe [:admin/entity-config actual-entity-key])
        ;; Call all hooks at the top level to satisfy Rules of Hooks
        entity-data (use-subscribe [::entity-subs/paginated-entities actual-entity-key])
        {:keys [page-title page-description adapter-init-fn features components] :as config} (or entity-config {})
        {:keys [modals custom-header]} components
        entity-ids (->> entity-data (map id-utils/extract-entity-id) (filter some?) vec)
        selection-change-handler (use-memo #(generic-handlers/create-generic-selection-handler actual-entity-key)
                                  [actual-entity-key])
        additional-effects (use-memo #(when entity-config
                                        (generic-handlers/create-generic-additional-effects config))
                               [entity-config config])
        render-main-content (when entity-config (content-renderer/create-main-content-renderer config))
        header-render (use-memo #(build-header-renderer custom-header) [custom-header])
        ;; Always call the hook, but pass conditional parameters to handle logic inside
        _ (generic-handlers/use-deletion-constraints
            actual-entity-key
            entity-ids
            (and entity-config (true? (:deletion-constraints? features))))]

    (when ^boolean js/goog.DEBUG
      (js/console.log "generic-admin-entity-page state"
        (clj->js {:entity actual-entity-key
                  :has-config? (boolean entity-config)
                  :entity-data-count (count entity-data)})))

    (if-not entity-config
      ($ :div {:class "text-sm text-gray-400"}
        (str "Loading configuration for " (name actual-entity-key) "..."))

      ($ :<>
        ($ admin-page-wrapper
          {:entity-name actual-entity-key
           :page-title (or page-title "Entity Management")
           :page-description page-description
           :adapter-init-fn adapter-init-fn
           :additional-effects additional-effects
           :selection-change-handler selection-change-handler
           :custom-header-content header-render
           :render-main-content render-main-content
           :show-batch-operations? (true? (:batch-operations? features))
           :show-selection-counter? (true? (:batch-operations? features))})
        (when modals
          (modal-renderer/render-modals modals))))))
