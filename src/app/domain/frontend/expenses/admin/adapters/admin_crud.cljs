(ns app.domain.frontend.expenses.admin.adapters.admin-crud
  "Side-effect namespace that registers expenses admin CRUD bridges."
  (:require
    [app.domain.frontend.expenses.admin.adapters.admin-crud.register :as register]))

(defonce ^:private _registered
  (do
    (register/register-all!)
    true))
