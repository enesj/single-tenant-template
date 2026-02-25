(ns app.domain.frontend.expenses.events.user-expenses.receipts
  "Compatibility facade — requires sub-namespaces to load all event registrations."
  (:require
    app.domain.frontend.expenses.events.user-expenses.receipts.list
    app.domain.frontend.expenses.events.user-expenses.receipts.detail
    app.domain.frontend.expenses.events.user-expenses.receipts.approve
    app.domain.frontend.expenses.events.user-expenses.receipts.ocr))
