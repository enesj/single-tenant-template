(ns app.admin.frontend.pages.unified-settings
  "Unified admin settings page with scope switching.

   Entry-point namespace that re-exports the actual page implementation from
   `pages/unified_settings/page.cljs`."
  (:require
    [app.admin.frontend.pages.unified-settings.page :as page]))

(def unified-settings-content page/unified-settings-content)
(def unified-settings-page page/unified-settings-page)
(def admin-settings-page page/admin-settings-page)
(def user-settings-page page/user-settings-page)

