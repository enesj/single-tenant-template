(ns app.domain.frontend.expenses.init
  "Expenses domain init - loads events/subs namespaces for side effects.

   Keep this domain-local so app.domain.frontend.registry stays stable as the
   domain grows." 
  (:require
    ;; Domain admin subs (expenses-specific loading/error subs)
    app.domain.frontend.expenses.admin.subs
    ;; Admin events/subs
    app.domain.frontend.expenses.events.article-alias-bulk
    app.domain.frontend.expenses.events.expenses
    app.domain.frontend.expenses.events.payers
    app.domain.frontend.expenses.events.receipts
    app.domain.frontend.expenses.events.suppliers
    app.domain.frontend.expenses.events.articles
    app.domain.frontend.expenses.events.expense-items
    app.domain.frontend.expenses.events.article-aliases
    app.domain.frontend.expenses.events.price-observations
    app.domain.frontend.expenses.events.unmapped-items
    app.domain.frontend.expenses.subs.article-alias-bulk
    app.domain.frontend.expenses.subs.expenses
    app.domain.frontend.expenses.subs.payers
    app.domain.frontend.expenses.subs.suppliers
    app.domain.frontend.expenses.subs.receipts
    app.domain.frontend.expenses.subs.articles
    app.domain.frontend.expenses.subs.expense-items
    app.domain.frontend.expenses.subs.article-aliases
    app.domain.frontend.expenses.subs.price-observations
    app.domain.frontend.expenses.subs.unmapped-items
    ;; User-expenses events and subs (domain-owned)
    app.domain.frontend.expenses.events.user-expenses
    app.domain.frontend.expenses.subs.user-expenses))
