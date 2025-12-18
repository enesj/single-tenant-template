(ns app.domain.frontend.expenses.routes.user
  "User-facing expense tracking routes.
   These routes are role-gated - users without proper role see waiting room."
  (:require
    [app.template.frontend.routes.controllers :as controllers]))

(defn routes
  "Generate user-facing expense tracking routes."
  []
  [;; Waiting room for unassigned users
   ["/waiting-room"
    {:name :waiting-room
     :view :waiting-room
     :controllers (controllers/make-simple-controller :page/init-waiting-room)}]

   ;; User expense dashboard (main entry point)
   ["/expenses"
    {:name :expenses-dashboard
     :view :expenses-dashboard
     :controllers (controllers/make-simple-controller :page/init-expenses-dashboard)}]

   ;; Alias for dashboard
   ["/dashboard"
    {:name :user-dashboard
     :view :expenses-dashboard
     :controllers (controllers/make-simple-controller :page/init-expenses-dashboard)}]

   ;; Explicit nested dashboard path to avoid catching by /expenses/:expense-id
   ["/expenses/dashboard"
    {:name :expenses-dashboard-alias
     :view :expenses-dashboard
     :controllers (controllers/make-simple-controller :page/init-expenses-dashboard)}]

   ;; Expense list with history
   ["/expenses/list"
    {:name :expenses-list
     :view :expenses-list
     :controllers (controllers/make-simple-controller :page/init-expenses-list)}]

   ;; Upload receipt
   ["/expenses/upload"
    {:name :expense-upload
     :view :expense-upload
     :controllers (controllers/make-simple-controller :page/init-expense-upload)}]

   ;; New expense (manual entry)
   ["/expenses/new"
    {:name :expense-new
     :view :expense-new
     :controllers (controllers/make-simple-controller :page/init-expense-new)}]

   ;; Reports
   ["/expenses/reports"
    {:name :expense-reports
     :view :expense-reports
     :controllers (controllers/make-simple-controller :page/init-expense-reports)}]

   ;; User settings
   ["/expenses/settings"
    {:name :expense-settings
     :view :expense-settings
     :controllers (controllers/make-simple-controller :page/init-expense-settings)}]

   ;; Expense detail (kept after literal routes to avoid catching static paths)
   ["/expenses/:expense-id"
    {:name :expense-detail
     :view :expense-detail
     :parameters {:path {:expense-id string?}}
     :controllers (controllers/make-simple-controller :page/init-expense-detail)}]])
