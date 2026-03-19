(ns app.domain.backend.expenses.routes.global-settings
  "Admin routes for global settings, currencies, exchange rates, and alerts.
   Mounted under /admin/api/expenses."
  (:require
    [app.domain.backend.expenses.handlers.admin-global-settings :as gs-handlers]))

(defn routes
  "Admin global settings routes."
  [db & [app-config]]
  [["/global-settings"
    {:get (gs-handlers/get-global-settings-handler db)
     :put (gs-handlers/update-global-settings-handler db)}]

   ["/enabled-currencies"
    {:get (gs-handlers/list-enabled-currencies-handler db)
     :post (gs-handlers/add-enabled-currency-handler db)}]

   ["/enabled-currencies/:code"
    {:delete (gs-handlers/remove-enabled-currency-handler db)}]

   ["/daily-exchange-rates"
    {:get (gs-handlers/get-daily-exchange-rates-handler db)}]

   ["/daily-exchange-rates/fetch"
    {:post (gs-handlers/fetch-daily-rates-handler db app-config)}]

   ["/exchange-rate-alerts"
    {:get (gs-handlers/list-exchange-rate-alerts-handler db)}]

   ["/exchange-rate-alerts/:id/acknowledge"
    {:put (gs-handlers/acknowledge-alert-handler db)}]])
