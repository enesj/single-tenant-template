(ns app.admin.frontend.events.settings
  "Events for managing view-options, form-fields, and table-columns configs via backend API"
  (:require
    ;; Side-effect requires: registers events/subs.
    [app.admin.frontend.events.settings.form-fields]
    [app.admin.frontend.events.settings.subs]
    [app.admin.frontend.events.settings.table-columns]
    [app.admin.frontend.events.settings.ui]
    [app.admin.frontend.events.settings.view-options]))

