(ns app.domain.frontend.expenses.events.expenses
  "Expenses domain events - generated using the expenses event factory.
   
   Also includes modal-specific events that call callbacks instead of navigating."
  (:require

    [app.domain.frontend.expenses.events.entity-configs :as configs]
    [app.domain.frontend.expenses.events.events-factory :as factory]
    [app.template.frontend.shared.crud.success :as crud-success]
    [re-frame.core :as rf]))

;; Register standard CRUD events for expenses using the factory
;; This includes list, detail, and form events (create, success, failure)
(factory/register-entity-events! configs/expenses-config)

;; =============================================================================
;; Modal-specific Events
;; These events support callbacks for modal workflows (create/edit in modal,
;; call success callback instead of navigating)
;; =============================================================================

(def ^:private form-path [:admin :expenses :form])

(rf/reg-event-fx
  ::create-entry-modal-success
  (fn [{:keys [db]} [_ on-success response]]
    (let [entity (get response :expense)   ;; singular response key
          highlight-id (some-> (crud-success/extract-entity-id entity) str)]
      {:db (-> db
             (assoc-in (conj form-path :loading?) false)
             (assoc-in (conj form-path :error) nil)
             (assoc-in (conj form-path :last-created) (:id entity))
             (cond-> highlight-id
               (crud-success/track-recently-created :expenses highlight-id)))
       :dispatch-n [[:admin/refresh-entity :expenses entity]
                    ;; Reload the list to show the new expense
                    [::load-list {}]]
       ;; Call the success callback (closes modal)
       :fx [(when on-success [:dispatch-later {:ms 100 :dispatch [::call-modal-callback on-success]}])]})))

(rf/reg-event-fx
  ::update-entry-modal-success
  (fn [{:keys [db]} [_ expense-id on-success response]]
    (let [entity (get response :expense)   ;; singular response key
          highlight-id (some-> (or (crud-success/extract-entity-id entity) expense-id) str)]
      {:db (-> db
             (assoc-in (conj form-path :loading?) false)
             (assoc-in (conj form-path :error) nil)
             (cond-> highlight-id
               (crud-success/track-recently-updated :expenses highlight-id)))
       :dispatch-n [[:admin/refresh-entity :expenses entity]
                    ;; Reload the list to show updated expense
                    [::load-list {}]]
       ;; Call the success callback (closes modal)
       :fx [(when on-success [:dispatch-later {:ms 100 :dispatch [::call-modal-callback on-success]}])]})))

;; Helper event to call modal callback
(rf/reg-event-fx
  ::call-modal-callback
  (fn [_ [_ callback]]
    (when callback
      (callback))
    {}))
