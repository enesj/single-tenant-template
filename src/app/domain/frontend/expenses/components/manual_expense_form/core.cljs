(ns app.domain.frontend.expenses.components.manual-expense-form.core
  "Facade — delegates to smart-input for the progressive expense entry form."
  (:require
    [app.domain.frontend.expenses.components.manual-expense-form.smart-input :as smart-input]))

(def manual-expense-form smart-input/smart-expense-form)
