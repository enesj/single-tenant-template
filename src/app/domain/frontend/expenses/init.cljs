(ns app.domain.frontend.expenses.init
  "Expenses domain init - loads events/subs namespaces for side effects.

   Keep this domain-local so app.domain.frontend.registry stays stable as the
   domain grows."
  (:require
    ;; User-expenses events and subs (domain-owned)
    app.domain.frontend.expenses.events.user-expenses
    app.domain.frontend.expenses.subs.user-expenses

    ;; Power-user unmapped items page (user routes)
    app.domain.frontend.expenses.events.unmapped-items
    app.domain.frontend.expenses.subs.unmapped-items))
