(ns app.admin.frontend.components.settings-views
  "Shared view and edit components for settings pages.

   This namespace is an entry point that re-exports the public UI components from
   smaller modules under `components/settings_views/`."
  (:require
    [app.admin.frontend.components.settings-views.badges :as badges]
    [app.admin.frontend.components.settings-views.cards :as cards]
    [app.admin.frontend.components.settings-views.editor :as editor]
    [app.admin.frontend.components.settings-views.overview :as overview]
    [app.admin.frontend.components.settings-views.rows :as rows]))

;; Public re-exports (keeps existing call sites stable)
(def bulk-tristate-row rows/bulk-tristate-row)
(def display-setting-row rows/display-setting-row)
(def column-visibility-row rows/column-visibility-row)
(def admin-setting-badge badges/admin-setting-badge)
(def admin-entity-settings-card cards/admin-entity-settings-card)
(def user-entity-settings-card cards/user-entity-settings-card)
(def columns-policy-card cards/columns-policy-card)
(def domain-section overview/domain-section)
(def scope-overview-section overview/scope-overview-section)
(def settings-overview overview/settings-overview)
(def entity-settings-editor editor/entity-settings-editor)
