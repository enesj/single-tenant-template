(ns app.domain.frontend.expenses.events.source-filter
  "Toggle state for showing/hiding manual vs receipt-based expenses.

   Shared between the user page (/expenses/list) and the admin page
   (/admin/expenses). Default: both visible.

   `source-filter->param` converts the boolean pair into the `source` query
   param expected by the backend:
     - both true   → nil (no filter)
     - manual only → \"manual\"
     - receipt only → \"receipt\"
     - neither     → \"none\"  (backend returns 0 rows)"
  (:require
    [app.domain.frontend.expenses.events.expenses :as expenses-events]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [re-frame.core :as rf]))

(def ^:private state-path [:expenses :source-filter])

(def default-state
  {:show-manual? true
   :show-receipts? true})

(defn source-filter->param
  [{:keys [show-manual? show-receipts?]}]
  (cond
    (and show-manual? show-receipts?) nil
    show-manual? "manual"
    show-receipts? "receipt"
    :else "none"))

(defn current-state
  [db]
  (merge default-state (get-in db state-path)))

(rf/reg-sub
  :expenses/source-filter
  (fn [db _]
    (current-state db)))

(rf/reg-event-fx
  :expenses/toggle-source-filter
  common-interceptors
  (fn [{:keys [db]} [which refresh-dispatch]]
    (let [flag-key (case which
                     :manual :show-manual?
                     :receipts :show-receipts?)
          path (conj state-path flag-key)
          current (get-in db path (get default-state flag-key))]
      {:db (assoc-in db path (not current))
       :fx [(when refresh-dispatch [:dispatch refresh-dispatch])]})))

;; Admin wrapper: read source-filter state, inject :source, delegate to the
;; factory-generated load-list. Used both as the admin page's refresh callback
;; and as the registered refresh event so paginate/sort clicks also carry :source.
(rf/reg-event-fx
  :admin-expenses/refresh-list
  common-interceptors
  (fn [{:keys [db]} [params]]
    (let [source-param (source-filter->param (current-state db))]
      {:dispatch [::expenses-events/load-list
                  (cond-> (or params {})
                    source-param (assoc :source source-param))]})))
