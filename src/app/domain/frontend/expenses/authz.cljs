(ns app.domain.frontend.expenses.authz
  "Expenses domain authorization module.
   
   Centralizes role-based access control for the Expenses domain.
   Roles (ascending privilege): unassigned < viewer < member < admin < owner
   
   Usage:
     (authz/power-user? role)           ; admin or owner?
     (authz/can? role :expenses/access) ; capability check"
  (:require
    [re-frame.core :as rf]))

(def ^:private role-hierarchy
  "Role privilege levels (higher = more privileged)"
  {"unassigned" 0
   "viewer"     1
   "member"     2
   "admin"      3
   "owner"      4})

(def ^:private capability-requirements
  "Maps capabilities to minimum required role level or explicit role set.
   - Integer: minimum role level required
   - Set: explicit role membership required"
  {:expenses/access          1  ; viewer+
   :expenses/expense.write   2  ; member+
   :expenses/expense.delete  3  ; admin+
  :expenses/expense-items.manage 3 ; admin+ (power-user line items page)
  :expenses/reference.write 2  ; member+ (suppliers, payers)
   :expenses/unmapped.access 3  ; admin+
   :expenses/articles.manage 3  ; admin+
  :expenses/supplier-aliases.manage 3 ; admin+
   :expenses/danger.execute  3  ; admin+ (danger zone actions)
   :expenses/settings.write  3  ; admin+
   :expenses/upload          2  ; member+
   :expenses/reports.export  1  ; viewer+
   :expenses/receipts.approve 2}) ; member+

(defn normalize-role
  "Normalize role to a string. Returns nil for invalid input."
  [role]
  (cond
    (keyword? role) (name role)
    (string? role)  role
    :else           nil))

(defn role-level
  "Get the privilege level for a role. Returns 0 for unknown roles."
  [role]
  (get role-hierarchy (normalize-role role) 0))

(defn power-user?
  "Returns true if role is admin or owner."
  [role]
  (>= (role-level role) 3))

(defn assigned?
  "Returns true if user has any assigned role (not unassigned)."
  [role]
  (>= (role-level role) 1))

(defn can-write?
  "Returns true if user can create/update (member+)."
  [role]
  (>= (role-level role) 2))

(defn can?
  "Check if a role has a specific capability.
   
   Arguments:
   - role: user's role (string or keyword)
   - capability: keyword like :expenses/access
   
   Returns true if the role satisfies the capability requirement."
  [role capability]
  (let [normalized-role (normalize-role role)
        level (role-level normalized-role)
        requirement (get capability-requirements capability)]
    (cond
      (nil? requirement) false ; unknown capability = denied
      (integer? requirement) (>= level requirement)
      (set? requirement) (contains? requirement normalized-role)
      :else false)))

(rf/reg-sub
  :expenses/user-role
  :<- [:user-role]
  (fn [role _]
    (normalize-role role)))

(rf/reg-sub
  :expenses/power-user?
  :<- [:expenses/user-role]
  (fn [role _]
    (power-user? role)))

(rf/reg-sub
  :expenses/assigned?
  :<- [:expenses/user-role]
  (fn [role _]
    (assigned? role)))

(rf/reg-sub
  :expenses/can-write?
  :<- [:expenses/user-role]
  (fn [role _]
    (can-write? role)))

(rf/reg-sub
  :expenses/can?
  :<- [:expenses/user-role]
  (fn [role [_ capability]]
    (can? role capability)))
