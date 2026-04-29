(ns app.admin.frontend.events.user-settings
  "Admin UI for editing domain-owned, user-facing UI defaults.

  It is intentionally NOT per-user localStorage prefs."
  (:require
    [app.admin.frontend.events.user-settings.entities]
    [app.admin.frontend.events.user-settings.form-fields]
    [app.admin.frontend.events.user-settings.load-save]
    [app.admin.frontend.events.user-settings.navigation]
    [app.admin.frontend.events.user-settings.subs]
    [app.admin.frontend.events.user-settings.table-columns]
    [app.admin.frontend.events.user-settings.tabs]
    [app.admin.frontend.events.user-settings.view-options]))
