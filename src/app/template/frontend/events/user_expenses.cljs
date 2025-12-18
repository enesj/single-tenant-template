(ns app.template.frontend.events.user-expenses
  "User-facing expense dashboard events.
   Fetches expense summary, recent expenses, and aggregates for the user scope."
  (:require
    [app.template.frontend.events.user-expenses.by-month]
    [app.template.frontend.events.user-expenses.by-supplier]
    [app.template.frontend.events.user-expenses.crud]
    [app.template.frontend.events.user-expenses.dashboard]
    [app.template.frontend.events.user-expenses.detail]
    [app.template.frontend.events.user-expenses.export]
    [app.template.frontend.events.user-expenses.lookups]
    [app.template.frontend.events.user-expenses.recent]
    [app.template.frontend.events.user-expenses.settings]
    [app.template.frontend.events.user-expenses.summary]))

