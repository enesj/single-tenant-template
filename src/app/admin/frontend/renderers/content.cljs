(ns app.admin.frontend.renderers.content
  "Renderers for configuration-driven admin entity pages"
  (:require
    [app.admin.frontend.renderers.actions :as actions-renderer]
    [app.shared.pagination :as pagination]
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
  (let [{:keys [entity-key page-title custom-content]} entity-config
        list-overrides-raw (get-in entity-config [:components :list])
        list-overrides (when (map? list-overrides-raw) list-overrides-raw)
        ;; Guard against accidentally passing EDN symbols (from config files) where
        ;; runtime functions are expected.
        sanitize-overrides
        (fn [overrides]
          (let [fn-keys #{:render-add-form :render-edit-form :on-add-success :on-edit-success :render-actions}]
            (reduce-kv
              (fn [m k v]
                (if (contains? fn-keys k)
                  (cond
                    (nil? v) (assoc m k nil)
                    (fn? v) (assoc m k v)
                    :else (do
                            (when ^boolean js/goog.DEBUG
                              (js/console.warn
                                "[admin/content] Ignoring non-function list override"
                                (clj->js {:entity entity-key :key k :value v})))
                            m))
                  (assoc m k v)))
              {}
              (or overrides {}))))]
    (fn [entity-spec propagated-settings]
      (let [display-settings (effective-display-settings entity-config propagated-settings)
            per-page (or (:per-page display-settings) pagination/default-page-size)
            actions (actions-renderer/create-actions-renderer entity-config display-settings)
            list-title (or page-title (str/capitalize (name entity-key)))
            overrides (sanitize-overrides list-overrides)
            effective-entity-spec (or (:entity-spec overrides) entity-spec)
            base-props {:entity-name entity-key
                        :entity-spec effective-entity-spec
                        :title list-title
                        :show-add-form? (true? (:show-add-button? display-settings))
                        :per-page per-page
                        :display-settings display-settings
                        :page-display-settings (:display-settings entity-config)
                        :render-actions actions}
            list-props (merge base-props (dissoc overrides :entity-spec))]
        ($ :div {:class "ds-card ds-bg-base-100 ds-shadow-xl"}
          ($ :div {:class "ds-card-body p-0"}
            (when (true? (:active-filters-display custom-content))
              (render-active-filters entity-key))
            ($ :div {:class "w-full pb-0 [&>div>table]:w-full"}
              ($ list-view list-props))))))))
