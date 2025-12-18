(ns app.admin.frontend.pages.settings.tabs
  (:require
    [app.admin.frontend.components.tabs :as tabs]
    [app.admin.frontend.pages.settings.components :as comps]
    [app.admin.frontend.pages.settings.constants :as c]
    [app.admin.frontend.pages.settings.editors :as editors]
    [uix.core :refer [$ defui]]))

;; =============================================================================
;; Main Content Tabs
;; =============================================================================

(defui view-options-tab-content
  "Content for the view options tab"
  [{:keys [all-view-options editing? on-change active-domain-tab set-domain-tab!]}]
  (let [sorted-entities (sort-by first all-view-options)
        grouped-entities (c/group-entities-by-domain sorted-entities)]
    ($ :div
      ;; Domain tabs
      ($ :div {:class "ds-tabs ds-tabs-bordered mb-6"}
        (tabs/tab-link {:label "🏠 System"
                        :active? (= active-domain-tab "system")
                        :on-select #(set-domain-tab! "system")})
        (tabs/tab-link {:label "💼 Domain"
                        :active? (= active-domain-tab "domain")
                        :on-select #(set-domain-tab! "domain")})
        (when (get grouped-entities :other)
          (tabs/tab-link {:label "📦 Other"
                          :active? (= active-domain-tab "other")
                          :on-select #(set-domain-tab! "other")})))

      ;; Tab content
      (cond
        (= active-domain-tab "system")
        ($ :div {:class "space-y-8"}
          (when-let [entities (get grouped-entities :user-management)]
            ($ comps/domain-section {:domain-key :user-management
                                     :domain-config (get c/domain-groups :user-management)
                                     :entities entities
                                     :editing? editing?
                                     :on-change on-change
                                     :show-actions? true}))
          (when-let [entities (get grouped-entities :security-audit)]
            ($ comps/domain-section {:domain-key :security-audit
                                     :domain-config (get c/domain-groups :security-audit)
                                     :entities entities
                                     :editing? editing?
                                     :on-change on-change
                                     :show-actions? true})))

        (= active-domain-tab "domain")
        ($ :div {:class "space-y-8"}
          (when-let [entities (get grouped-entities :expenses)]
            ($ comps/domain-section {:domain-key :expenses
                                     :domain-config (get c/domain-groups :expenses)
                                     :entities entities
                                     :editing? editing?
                                     :on-change on-change
                                     :show-actions? true})))

        (= active-domain-tab "other")
        (when-let [entities (get grouped-entities :other)]
          ($ :div {:class "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4"}
            (for [[entity-name settings] entities]
              ($ comps/entity-settings-card {:key entity-name
                                             :entity-name entity-name
                                             :settings settings
                                             :editing? editing?
                                             :on-change on-change
                                             :setting-keys c/all-setting-keys}))))))))

(defui form-fields-tab-content
  "Content for the form fields tab"
  [{:keys [form-fields editing? on-save loading?]}]
  (let [sorted-entities (sort-by first form-fields)]
    ($ :div
      (when loading?
        ($ :div {:class "flex items-center justify-center py-8"}
          ($ :span {:class "ds-loading ds-loading-spinner ds-loading-lg"})))

      (when-not loading?
        ($ :div
          ($ :div {:class "ds-alert ds-alert-info mb-6"}
            ($ :p {:class "text-sm"}
              "Configure which fields appear in create and edit forms for each entity. "
              "Required fields must be filled before submission."))

          ($ :div {:class "grid grid-cols-1 lg:grid-cols-2 gap-4"}
            (for [[entity-name config] sorted-entities]
              ($ editors/form-fields-entity-editor {:key entity-name
                                                    :entity-name entity-name
                                                    :config config
                                                    :editing? editing?
                                                    :on-save on-save}))))))))

(defui table-columns-tab-content
  "Content for the table columns tab"
  [{:keys [table-columns editing? on-save loading?]}]
  (let [sorted-entities (sort-by first table-columns)]
    ($ :div
      (when loading?
        ($ :div {:class "flex items-center justify-center py-8"}
          ($ :span {:class "ds-loading ds-loading-spinner ds-loading-lg"})))

      (when-not loading?
        ($ :div
          ($ :div {:class "ds-alert ds-alert-info mb-6"}
            ($ :p {:class "text-sm"}
              "Configure table column visibility and behavior. Hidden columns can still be shown by users. "
              "Always visible columns cannot be hidden. Unfilterable/unsortable columns have those features disabled."))

          ($ :div {:class "grid grid-cols-1 lg:grid-cols-2 gap-4"}
            (for [[entity-name config] sorted-entities]
              ($ editors/table-columns-entity-editor {:key entity-name
                                                      :entity-name entity-name
                                                      :config config
                                                      :editing? editing?
                                                      :on-save on-save}))))))))

