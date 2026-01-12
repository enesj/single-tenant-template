(ns app.domain.frontend.expenses.events.suppliers
  "Suppliers domain events - generated using the expenses event factory.

   Also includes inline-create events used by the admin expense form."
  (:require
    [ajax.core :as ajax]
    [app.admin.frontend.utils.http :as admin-http]
    [app.domain.frontend.expenses.events.entity-configs :as configs]
    [app.domain.frontend.expenses.events.events-factory :as factory]
    [app.template.frontend.components.confirm-dialog :as confirm-dialog]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$]]))

;; Register standard CRUD events for suppliers using the factory
(factory/register-entity-events! configs/suppliers-config)

(def ^:private base-path [:admin :expenses :suppliers])
(def ^:private inline-create-path (conj base-path :inline-create))
(def ^:private include-archived-path (conj base-path :include-archived?))
(def ^:private archive-path (conj base-path :archive))
(def ^:private purge-path (conj base-path :purge))

(rf/reg-event-db
  ::set-include-archived
  (fn [db [_ include-archived?]]
    (assoc-in db include-archived-path (boolean include-archived?))))

(defn- supplier-sort-key
  [supplier]
  (-> (or (:display-name supplier) (:display_name supplier) "")
    str
    str/trim
    str/lower-case))

(defn- upsert-supplier-into-list
  [db supplier]
  (let [items (vec (or (get-in db (conj base-path :items)) []))
        supplier-id (:id supplier)
        items* (->> (conj (vec (remove #(= (:id %) supplier-id) items)) supplier)
                 (sort-by supplier-sort-key)
                 vec)]
    (assoc-in db (conj base-path :items) items*)))

(rf/reg-event-db
  ::clear-inline-create
  (fn [db _]
    (-> db
      (assoc-in (conj inline-create-path :loading?) false)
      (assoc-in (conj inline-create-path :error) nil)
      (assoc-in (conj inline-create-path :last-created) nil))))

(rf/reg-event-fx
  ::create-inline
  (fn [{:keys [db]} [_ {:keys [display_name] :as form-data} on-success]]
    (let [name* (some-> display_name str str/trim)]
      (if (str/blank? name*)
        {:db (-> db
               (assoc-in (conj inline-create-path :loading?) false)
               (assoc-in (conj inline-create-path :error) "Display name is required."))}
        {:db (-> db
               (assoc-in (conj inline-create-path :loading?) true)
               (assoc-in (conj inline-create-path :error) nil))
         :http-xhrio (admin-http/admin-post
                       {:uri "/admin/api/expenses/suppliers"
                        :params (assoc form-data :display_name name*)
                        :response-format (ajax/json-response-format {:keywords? true})
                        :on-success [::create-inline-success on-success]
                        :on-failure [::create-inline-failed]})}))))

(rf/reg-event-fx
  ::create-inline-success
  (fn [{:keys [db]} [_ on-success response]]
    (let [supplier (:supplier response)
          supplier-id (:id supplier)]
      {:db (cond-> (-> db
                     (assoc-in (conj inline-create-path :loading?) false)
                     (assoc-in (conj inline-create-path :error) nil)
                     (assoc-in (conj inline-create-path :last-created) supplier-id))
             (map? supplier) (upsert-supplier-into-list supplier))
       :dispatch [::load-list {:limit 200 :offset 0}]
       :fx [(when (fn? on-success)
              [:dispatch-later {:ms 50
                                :dispatch [::call-inline-callback on-success supplier]}])]})))

;; ---------------------------------------------------------------------------
;; Supplier lifecycle operations
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  ::archive-supplier
  (fn [{:keys [db]} [_ supplier-id]]
    {:db (-> db
           (assoc-in (conj archive-path :loading?) true)
           (assoc-in (conj archive-path :error) nil))
     :http-xhrio (admin-http/admin-delete
                   {:uri (str "/admin/api/expenses/suppliers/" supplier-id)
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success [::archive-supplier-success supplier-id]
                    :on-failure [::archive-supplier-failed supplier-id]})}))

(rf/reg-event-fx
  ::archive-supplier-success
  (fn [{:keys [db]} [_ supplier-id _response]]
    (let [include-archived? (true? (get-in db include-archived-path))]
      {:db (-> db
             (assoc-in (conj archive-path :loading?) false)
             (assoc-in (conj archive-path :error) nil)
             (assoc-in (conj base-path :error) nil))
       :dispatch-n [[:admin/show-success-message "Supplier archived."]
                    [::load-detail supplier-id]
                    [::load-list {:include_archived include-archived?}]]})))

(rf/reg-event-fx
  ::archive-supplier-failed
  (fn [{:keys [db]} [_ _supplier-id error]]
    (let [msg (admin-http/extract-error-message error)]
      {:db (-> db
             (assoc-in (conj archive-path :loading?) false)
             (assoc-in (conj archive-path :error) msg)
             (assoc-in (conj base-path :error) msg))
       :dispatch [:admin/show-error-message msg]})))

(rf/reg-event-fx
  ::open-purge-confirm
  (fn [{:keys [db]} [_ supplier-id]]
    {:db (-> db
           (assoc-in (conj purge-path :loading?) true)
           (assoc-in (conj purge-path :error) nil))
     :http-xhrio (admin-http/admin-get
                   {:uri (str "/admin/api/expenses/suppliers/" supplier-id "/purge-preview")
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success [::open-purge-confirm-success supplier-id]
                    :on-failure [::open-purge-confirm-failed supplier-id]})}))

(rf/reg-event-fx
  ::open-purge-confirm-success
  (fn [{:keys [db]} [_ supplier-id response]]
    (let [preview (:preview response)
          can-purge? (true? (:can-purge? preview))
          active-expenses (long (or (:active-expenses preview) 0))
          soft-expenses-total (long (or (:soft-deleted-expenses-total preview) 0))
          soft-items-total (long (or (:soft-deleted-expense-items-total preview) 0))
          soft-expenses (vec (or (:soft-deleted-expenses preview) []))
          truncated? (true? (:soft-deleted-expenses-truncated? preview))]
      (cond
        (not can-purge?)
        {:db (-> db
               (assoc-in (conj purge-path :loading?) false)
               (assoc-in (conj purge-path :error) nil))
         :dispatch [:admin/show-error-message
                    (cond
                      (pos? active-expenses) (str "Cannot purge supplier: it has " active-expenses " active expense(s).")
                      :else "Cannot purge supplier. It must be archived and must not have active expenses.")]}

        :else
        (let [soft-expenses-lines
              (when (seq soft-expenses)
                (str/join
                  "\n"
                  (for [{:keys [id purchased-at total-amount currency expense-items-count]} soft-expenses]
                    (str "- "
                      (or purchased-at "(unknown date)")
                      " • "
                      total-amount
                      " "
                      currency
                      " • "
                      expense-items-count
                      " item(s)"
                      " • "
                      id))))
              warning
              (when (pos? soft-expenses-total)
                ($ :div {:class "ds-alert ds-alert-warning"}
                  ($ :div {}
                    ($ :div {:class "font-semibold"}
                      "Soft-deleted expenses will also be purged")
                    ($ :div {:class "text-xs opacity-80"}
                      (str soft-expenses-total " soft-deleted expense(s) and "
                        soft-items-total " line item(s) will be permanently deleted together with the supplier."))
                    (when soft-expenses-lines
                      ($ :pre {:class "mt-2 text-xs max-h-40 overflow-auto whitespace-pre-wrap"}
                        soft-expenses-lines))
                    (when truncated?
                      ($ :div {:class "mt-2 text-xs opacity-70"}
                        "List truncated. Use Expenses search if you need the full list.")))))
              message
              ($ :div {:class "space-y-3 text-sm text-base-content text-left"}
                ($ :p nil "This will permanently delete the supplier. This cannot be undone.")
                warning)]
          {:db (-> db
                 (assoc-in (conj purge-path :loading?) false)
                 (assoc-in (conj purge-path :error) nil))
           :dispatch
           [::confirm-dialog/open-confirm-dialog
            {:title "Purge supplier permanently"
             :confirm-text "Purge permanently"
             :cancel-text "Cancel"
             :message message
             :on-confirm (fn []
                           (rf/dispatch [::purge-supplier supplier-id]))}]})))))

(rf/reg-event-fx
  ::open-purge-confirm-failed
  (fn [{:keys [db]} [_ _supplier-id error]]
    (let [msg (admin-http/extract-error-message error)]
      {:db (-> db
             (assoc-in (conj purge-path :loading?) false)
             (assoc-in (conj purge-path :error) msg)
             (assoc-in (conj base-path :error) msg))
       :dispatch [:admin/show-error-message msg]})))

(rf/reg-event-fx
  ::purge-supplier
  (fn [{:keys [db]} [_ supplier-id]]
    {:db (-> db
           (assoc-in (conj purge-path :loading?) true)
           (assoc-in (conj purge-path :error) nil))
     :http-xhrio (admin-http/admin-post
                   {:uri (str "/admin/api/expenses/suppliers/" supplier-id "/purge")
                    :params {}
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success [::purge-supplier-success supplier-id]
                    :on-failure [::purge-supplier-failed supplier-id]})}))

(rf/reg-event-fx
  ::purge-supplier-success
  (fn [{:keys [db]} [_ _supplier-id _response]]
    (let [include-archived? (true? (get-in db include-archived-path))]
      {:db (-> db
             (assoc-in (conj purge-path :loading?) false)
             (assoc-in (conj purge-path :error) nil)
             (assoc-in (conj base-path :error) nil))
       :dispatch-n [[:admin/show-success-message "Supplier purged permanently."]
                    ;; Close modal (if open) and navigate back to the list.
                    [::close-detail-modal]
                    [:admin/navigate-client "/admin/suppliers"]
                    [::load-list {:include_archived include-archived?}]]})))

(rf/reg-event-fx
  ::purge-supplier-failed
  (fn [{:keys [db]} [_ _supplier-id error]]
    (let [msg (admin-http/extract-error-message error)]
      {:db (-> db
             (assoc-in (conj purge-path :loading?) false)
             (assoc-in (conj purge-path :error) msg)
             (assoc-in (conj base-path :error) msg))
       :dispatch [:admin/show-error-message msg]})))

(rf/reg-event-fx
  ::create-inline-failed
  (fn [{:keys [db]} [_ error]]
    {:db (-> db
           (assoc-in (conj inline-create-path :loading?) false)
           (assoc-in (conj inline-create-path :error) (admin-http/extract-error-message error)))}))

(rf/reg-event-fx
  ::call-inline-callback
  (fn [_ [_ callback supplier]]
    (when callback
      (callback supplier))
    {}))
