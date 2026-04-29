(ns app.template.frontend.components.list.gates
  (:require
    [app.domain.frontend.expenses.authz :as expenses-authz]))

(defn normalize-gate-id
  [x]
  (cond
    (keyword? x) x
    (string? x) (keyword x)
    :else nil))

(defn gate-allows-action?
  [gate-id {:keys [admin-route? expenses-role can-write? power-user?]}]
  (let [gate-id (normalize-gate-id gate-id)]
    (cond
      admin-route? true
      (nil? gate-id) true
      (= gate-id :expenses/can-write) (boolean can-write?)
      (= gate-id :expenses/power-user) (boolean power-user?)
      (expenses-authz/can? expenses-role gate-id) true
      :else false)))