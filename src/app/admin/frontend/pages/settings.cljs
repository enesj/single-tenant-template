(ns app.admin.frontend.pages.settings
  "Admin page displaying all hardcoded list view settings from view-options.edn,
   form-fields.edn, and table-columns.edn with editing capabilities.

   Entry-point namespace that re-exports the current implementation from
   `pages/settings/page.cljs`."
  (:require
    [app.admin.frontend.pages.settings.page :as page]))

(def admin-settings-content page/admin-settings-content)

