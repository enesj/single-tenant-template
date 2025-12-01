(ns app.admin.frontend.components.admin-page-wrapper
  "Admin page wrapper component using template system shared components.

   This wrapper provides admin-specific functionality while leveraging
   the universal template components for cross-cutting concerns."

  (:require
    [app.admin.frontend.components.layout :as layout]
    [app.admin.frontend.system.entity-registry :as entity-registry]
    [app.shared.keywords :as kw]
    [app.template.frontend.components.auth-guard :refer [auth-guard]]
    [app.template.frontend.components.message-display :refer [message-display]]
    [app.template.frontend.components.selection-counter :refer [selection-counter]]
    [app.template.frontend.utils.shared :as template-utils]
    [re-frame.core :refer [dispatch]]
    [taoensso.timbre :as log]
    [uix.core :refer [$ defui use-callback]]
    [uix.re-frame :refer [use-subscribe]]))

(defui admin-page-wrapper
  "Admin page wrapper component using template system shared components.

   This wrapper provides admin-specific functionality while leveraging
   universal template components for cross-cutting concerns.

   Props:
   - :entity-name - keyword for entity type (e.g., :users, :tenants, :audit-logs)
   - :page-title - string for page title
   - :page-description - string for page description
   - :adapter-init-fn - function to initialize adapter
   - :additional-effects - function for additional side effects (optional)
   - :custom-header-content - custom header content (optional)
   - :render-main-content - function that renders the main content
   - :batch-actions-config - configuration for batch actions (optional)
   - :show-selection-counter? - boolean to show selection counter (default: true)
   - :class - optional additional CSS classes"
  [{:keys [entity-name page-title page-description adapter-init-fn
           additional-effects custom-header-content render-main-content
           show-selection-counter? class]
    :or {show-selection-counter? true
         class ""}}]

  (let [{:keys [error success-message selected-ids]} (template-utils/use-entity-state entity-name :admin)
        {:keys [entity-spec display-settings]} (template-utils/use-entity-spec entity-name :admin)
        registry-init-fn (get-in entity-registry/entity-registry [entity-name :init-fn])]

    ;; Create stable function references using use-callback
    (let [init-fn (use-callback
                    (fn []
                      (log/info "Initializing admin page for" (or (kw/ensure-name entity-name) entity-name))
                      ;; Ensure admin UI configs are loaded
                      (dispatch [:admin/load-ui-configs])
                      ;; Initialize adapter only when callable to avoid runtime arity errors.
                      (let [effective-adapter-init-fn (cond
                                                        (fn? adapter-init-fn) adapter-init-fn
                                                        (fn? registry-init-fn) registry-init-fn
                                                        :else nil)]
                        (cond
                          (fn? effective-adapter-init-fn)
                          (effective-adapter-init-fn)

                          adapter-init-fn
                          (log/warn "adapter-init-fn is not callable for entity" entity-name
                            {:value adapter-init-fn})))
                      ;; Run additional effects handler when callable.
                      (cond
                        (fn? additional-effects)
                        (additional-effects)

                        additional-effects
                        (log/warn "additional-effects handler is not callable for entity" entity-name
                          {:value additional-effects})))
                    [registry-init-fn entity-name adapter-init-fn additional-effects])

          cleanup-fn (use-callback
                       (fn []
                         (log/info "Admin page unmounted for" (or (kw/ensure-name entity-name) entity-name))
                         (dispatch [:admin/clear-success-message]))
                       [entity-name])]

      ;; Initialize adapter and common effects using template utilities
      (template-utils/use-entity-initialization
        entity-name
        init-fn
        cleanup-fn))

    ($ :div
      ($ layout/admin-layout
        {:children
         ($ auth-guard
           {:authenticated? (use-subscribe [:admin/authenticated?])
            :auth-type :admin
            :children
            ($ :div {:class (str "p-6 min-h-screen " class)}
              ;; Custom admin header
              ($ :div {:class "mb-6"}
                ($ :div {:class "flex justify-between items-center mb-4"}
                  ($ :div
                    ($ :h1 {:class "text-2xl font-semibold text-white mb-2"} page-title)
                    ($ :p {:class "text-gray-400"} page-description))
                  ;; Custom header content
                  (when (fn? custom-header-content)
                    (custom-header-content))))

              ;; Message display using template component with admin theme
              ($ message-display
                {:success-message success-message
                 :error error
                 :variant :admin
                 :on-clear-success #(dispatch [:admin/clear-success-message])
                 :on-clear-error #(dispatch [:admin/clear-error-message])})

              ;; Selection counter using template component with admin theme
              (when show-selection-counter?
                ($ selection-counter
                  {:entity-type entity-name
                   :selected-ids selected-ids
                   :variant :admin}))

              ;; Main content area
              (when (fn? render-main-content)
                (render-main-content entity-spec display-settings)))})}))))
