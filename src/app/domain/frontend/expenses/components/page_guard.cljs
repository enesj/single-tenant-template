(ns app.domain.frontend.expenses.components.page-guard
  "Expenses domain page guards.
   
   Provides consistent route-level access control for all Expenses pages.
   Uses the authz module for capability checks."
  (:require
    [app.domain.frontend.expenses.authz :as authz]
    [app.template.frontend.components.auth-guard :as auth-guard]
    [app.template.frontend.components.button :refer [button]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

(defui waiting-room-redirect
  "Redirects unassigned users to waiting room."
  []
  ($ :div {:class "min-h-screen flex items-center justify-center bg-base-100"}
    ($ :div {:class "text-center p-8"}
      ($ :h1 {:class "text-2xl font-bold mb-4"} "Account Pending")
      ($ :p {:class "text-base-content/70 mb-6"}
        "Your account is being reviewed. You'll be notified when you can access expenses.")
      ($ button {:btn-type :primary
                 :id "btn-go-waiting-room-expenses-guard"
                 :on-click #(rf/dispatch [:navigate-to "/waiting-room"])}
        "Go to Waiting Room"))))

(defui access-denied
  "Shows access denied message with navigation option."
  [{:keys [message]}]
  ($ :div {:class "min-h-screen flex items-center justify-center bg-base-100"}
    ($ :div {:class "text-center p-8"}
      ($ :h1 {:class "text-2xl font-bold mb-4"} "Access Denied")
      ($ :p {:class "text-base-content/70 mb-6"}
        (or message "You don't have permission to access this page."))
      ($ button {:btn-type :primary
                 :id "btn-go-dashboard-expenses-guard"
                 :on-click #(rf/dispatch [:navigate-to "/expenses"])}
        "Go to Dashboard"))))

(defui expenses-page-guard
  "Guards an Expenses page by required capability.
   
   Props:
   - :capability - keyword like :expenses/access or :expenses/unmapped.access
   - :children - content to render if authorized
   - :redirect-unassigned? - if true, redirects unassigned users to waiting room (default true)"
  [{:keys [capability children redirect-unassigned?]
    :or {redirect-unassigned? true}}]
  (let [current-user (use-subscribe [:current-user])
        role (use-subscribe [:expenses/user-role])
        is-assigned? (authz/assigned? role)
        has-capability? (authz/can? role capability)]
    (cond
      (nil? current-user)
      ($ auth-guard/customer-auth-guard {:authenticated? false})

      (and redirect-unassigned? (not is-assigned?))
      ($ waiting-room-redirect)

      (not has-capability?)
      ($ access-denied {:message "You don't have the required permissions for this page."})

      :else
      children)))

(defui power-user-guard
  "Guards a page for admin/owner users only.
   
   Props:
   - :children - content to render if authorized"
  [{:keys [children]}]
  ($ expenses-page-guard
    {:capability :expenses/unmapped.access
     :children children}))

(defui write-access-guard
  "Guards a page for users who can write (member+).
   
   Props:
   - :children - content to render if authorized"
  [{:keys [children]}]
  ($ expenses-page-guard
    {:capability :expenses/expense.write
     :children children}))
