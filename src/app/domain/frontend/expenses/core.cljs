(ns app.domain.frontend.expenses.core
  "Expenses frontend bootstrap; wire page and feature init."
  (:require
    [app.domain.frontend.expenses.events.article-alias-bulk]
    [app.domain.frontend.expenses.events.article-aliases]
    [app.domain.frontend.expenses.events.articles]
    [app.domain.frontend.expenses.events.expenses]
    [app.domain.frontend.expenses.events.payers]
    [app.domain.frontend.expenses.events.price-observations]
    [app.domain.frontend.expenses.events.receipts]
    [app.domain.frontend.expenses.events.suppliers]
    [app.domain.frontend.expenses.events.unmapped-items]
    [app.domain.frontend.expenses.events.user-expenses]
    [app.domain.frontend.expenses.subs.article-alias-bulk]
    [app.domain.frontend.expenses.subs.article-aliases]
    [app.domain.frontend.expenses.subs.articles]
    [app.domain.frontend.expenses.subs.expenses]
    [app.domain.frontend.expenses.subs.payers]
    [app.domain.frontend.expenses.subs.price-observations]
    [app.domain.frontend.expenses.subs.receipts]
    [app.domain.frontend.expenses.subs.suppliers]
    [app.domain.frontend.expenses.subs.unmapped-items]
    [app.domain.frontend.expenses.subs.user-expenses]))
