(ns app.domain.expenses.frontend.events.expenses
  (:require
    [ajax.core :as ajax]
    [app.admin.frontend.adapters.expenses :as expenses-adapter]
    [app.admin.frontend.utils.http :as admin-http]
    [app.template.frontend.db.paths :as paths]
    [day8.re-frame.http-fx]
    [re-frame.core :as rf]))

(def ^:private entity-key :expenses)
(def ^:private base-path [:admin :expenses :entries])
(def ^:private form-path [:admin :expenses :form])

(def ^:private default-per-page 25)

(defn- resolve-pagination
  [db {:keys [limit offset page per-page]}]
  (let [existing-per-page (or (get-in db (paths/list-per-page entity-key))
                            (get-in db (conj (paths/list-ui-state entity-key) :per-page))
                            (get-in db (conj (paths/list-ui-state entity-key) :pagination :per-page))
                            default-per-page)
        existing-page (or (get-in db (paths/list-current-page entity-key))
                        (get-in db (conj (paths/list-ui-state entity-key) :current-page))
                        (get-in db (conj (paths/list-ui-state entity-key) :pagination :current-page))
                        1)
        per-page (or limit per-page existing-per-page default-per-page)
        page (or page (when offset (inc (quot offset (max per-page 1)))) existing-page 1)
        offset (or offset (* (max 0 (dec page)) per-page))]
    {:limit per-page
     :offset offset
     :page page
     :per-page per-page}))

(defn- begin-load
  [db {:keys [limit offset page per-page] :as params}]
  (let [{:keys [limit offset page per-page]} (resolve-pagination db params)]
    (-> db
      (assoc-in (paths/entity-loading? entity-key) true)
      (assoc-in (paths/entity-error entity-key) nil)
      (assoc-in [:admin :expenses :loading?] true)
      (assoc-in [:admin :expenses :error] nil)
      (assoc-in (paths/list-per-page entity-key) per-page)
      (assoc-in (paths/list-current-page entity-key) page)
      (assoc-in (conj (paths/list-ui-state entity-key) :pagination) {:current-page page :per-page per-page})
      (assoc-in (conj (paths/entity-metadata entity-key) :pagination) {:page page :per-page per-page})
      (assoc-in (conj base-path :loading?) true)
      (assoc-in (conj base-path :error) nil))))

(defn- finish-load
  [db error?]
  (let [error-val (when error? (admin-http/extract-error-message error?))]
    (-> db
      (assoc-in (paths/entity-loading? entity-key) false)
      (assoc-in (paths/entity-error entity-key) error-val)
      (assoc-in [:admin :expenses :loading?] false)
      (assoc-in [:admin :expenses :error] error-val)
      (assoc-in (conj base-path :loading?) false)
      (assoc-in (conj base-path :error) error-val))))

(rf/reg-event-fx
  ::load-list
  (fn [{:keys [db]} [_ params]]
    (let [{:keys [limit offset] :as pagination} (resolve-pagination db params)]
      {:db (begin-load db pagination)
       :http-xhrio (admin-http/admin-get
                     {:uri "/admin/api/expenses/entries"
                      :params {:limit limit :offset offset}
                      :response-format (ajax/json-response-format {:keywords? true})
                      :on-success [::list-loaded pagination]
                      :on-failure [::load-failed]})})))

(rf/reg-event-fx
  ::list-loaded
  (fn [{:keys [db]} [_ {:keys [limit offset]} {:keys [expenses]}]]
    (let [db* (-> db
                (finish-load nil)
                (assoc-in (conj base-path :items) (vec (or expenses []))))]
        per-page (or limit default-per-page)
        page (inc (quot (or offset 0) (max per-page 1)))
      {:db (-> db*
             (assoc-in (conj (paths/entity-metadata entity-key) :pagination) {:page page :per-page per-page})
             (assoc-in (conj (paths/list-ui-state entity-key) :pagination) {:current-page page :per-page per-page}))
       :dispatch-n [[::expenses-adapter/sync-expenses expenses]]})))

(rf/reg-event-fx
  ::load-failed
  (fn [{:keys [db]} [_ error]]
    {:db (finish-load db error)}))

(rf/reg-event-fx
  ::load-detail
  (fn [{:keys [db]} [_ entry-id]]
    {:db (-> db
           (assoc-in (conj base-path :detail-loading?) true)
           (assoc-in (conj base-path :error) nil))
     :http-xhrio (admin-http/admin-get
                   {:uri (str "/admin/api/expenses/entries/" entry-id)
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success [::detail-loaded entry-id]
                    :on-failure [::load-failed]})}))

(rf/reg-event-db
  ::detail-loaded
  (fn [db [_ entry-id {:keys [expense]}]]
    (-> db
      (assoc-in (conj base-path :detail-loading?) false)
      (assoc-in (conj base-path :error) nil)
      (assoc-in (conj base-path :by-id entry-id) expense))))

(rf/reg-event-fx
  ::create-entry
  (fn [{:keys [db]} [_ form-data]]
    {:db (-> db
           (assoc-in (conj form-path :loading?) true)
           (assoc-in (conj form-path :error) nil))
     :http-xhrio (admin-http/admin-post
                   {:uri "/admin/api/expenses/entries"
                    :params form-data
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success [::create-entry-success]
                    :on-failure [::create-entry-failed]})}))

(rf/reg-event-fx
  ::create-entry-success
  (fn [{:keys [db]} [_ {:keys [expense]}]]
    (let [expense-id (:id expense)]
      {:db (-> db
             (assoc-in (conj form-path :loading?) false)
             (assoc-in (conj form-path :error) nil)
             (assoc-in (conj form-path :last-created) expense-id))
       :dispatch-n [[::expenses-adapter/sync-expenses [expense]]
                    [:admin/navigate-client (str "/admin/expenses/" expense-id)]]})))

(rf/reg-event-fx
  ::create-entry-failed
  (fn [{:keys [db]} [_ error]]
    {:db (-> db
           (assoc-in (conj form-path :loading?) false)
           (assoc-in (conj form-path :error) (admin-http/extract-error-message error)))}))
