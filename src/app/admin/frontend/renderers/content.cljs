(ns app.admin.frontend.renderers.content
  "Renderers for configuration-driven admin entity pages"
  (:require
    [app.admin.frontend.renderers.actions :as actions-renderer]
    [app.template.frontend.components.list :refer [list-view]]
    [app.template.frontend.utils.shared :as shared-utils]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch]]
    [uix.core :refer [$]]
    [uix.re-frame :refer [use-subscribe]]))

(defn- entity-active-filters-sub
  [entity-key]
  (case entity-key
    :audit-logs [:admin/audit-active-filters]
    nil))

(defn- remove-filter-event
  [entity-key]
  (case entity-key
    :audit-logs :admin/remove-audit-filter
    nil))

(defn- render-active-filters
  [entity-key]
  (when-let [sub (entity-active-filters-sub entity-key)]
    (let [active-filters (use-subscribe sub)
          remove-event (remove-filter-event entity-key)]
      (when (seq active-filters)
        ($ :div {:class "flex flex-wrap gap-2 mb-4"}
          (for [[filter-key filter-value] active-filters]
            ($ :div {:key (str (name filter-key) "-" filter-value)
                     :class "ds-badge ds-badge-primary ds-badge-outline flex items-center gap-2"}
              (str (name filter-key) ": " filter-value)
              (when remove-event
                ($ :button {:type "button"
                            :class "text-xs text-red-200 hover:text-red-400"
                            :on-click #(dispatch [remove-event filter-key])}
                  "✕")))))))))

(defn- effective-display-settings
  [entity-config propagated-settings]
  (let [defaults (shared-utils/default-list-display-settings)
        config-overrides (:display-settings entity-config)
        clean-propagated (into {}
                           (remove (fn [[_ v]] (nil? v))
                             (or propagated-settings {})))
        base (-> defaults
               (merge config-overrides)
               ;; Let propagated settings (hardcoded or user preferences) override config defaults
               (merge clean-propagated))
        ;; Map feature flags to display toggles where it affects visibility.
        ;; This ensures high-level feature switches also hide related UI.
        {:keys [features]} entity-config
        overlay (cond-> {}
                  (false? (:batch-operations? features))
                  (merge {:show-select? false
                          :show-batch-edit? false
                          :show-batch-delete? false})
                  (:read-only? features)
                  (merge {:show-add-button? false
                          :show-delete? false
                          :show-edit? false}))]
    (merge base overlay)))

(defn create-main-content-renderer
  "Create entity-specific main content renderer"
  [entity-config]
  (let [{:keys [entity-key page-title custom-content]} entity-config]
    (fn [entity-spec propagated-settings]
      (let [display-settings (effective-display-settings entity-config propagated-settings)
            per-page (or (:per-page display-settings) 10)
            actions (actions-renderer/create-actions-renderer entity-config display-settings)
            list-title (or page-title (str/capitalize (name entity-key)))]
        ($ :div {:class "ds-card ds-bg-base-100 ds-shadow-xl"}
          ($ :div {:class "ds-card-body p-0"}
            (when (true? (:active-filters-display custom-content))
              (render-active-filters entity-key))
            ($ :div {:class "w-full pb-0 [&>div>table]:w-full"}
              ($ list-view
                {:entity-name entity-key
                 :entity-spec entity-spec
                 :title list-title
                 :show-add-form? (true? (:show-add-button? display-settings))
                 :per-page per-page
                 :display-settings display-settings
                 :page-display-settings (:display-settings entity-config)
                 :render-actions actions}))))))))
