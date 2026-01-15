(ns app.domain.frontend.expenses.events.user-expenses.supplier-detail
  "User suppliers: supplier detail + related lists used by the user Suppliers modal."
  (:require
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.xhrio :as x]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(def ^:private detail-path
  [:user-expenses :suppliers :detail])

(defn- assoc-loading
  [db k v]
  (assoc-in db (conj detail-path k) v))

(defn- assoc-value
  [db k v]
  (assoc-in db (conj detail-path k) v))

;; ---------------------------------------------------------------------------
;; Bootstrap / reset
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/init-supplier-detail
  common-interceptors
  (fn [{:keys [db]} [supplier-id]]
    (let [supplier-id-str (some-> supplier-id str)]
      (if (seq supplier-id-str)
        {:db (-> db
               (assoc-value :current-id supplier-id-str)
               (assoc-value :error nil)
               (assoc-value :expenses [])
               (assoc-value :expenses-error nil)
               (assoc-value :aliases [])
               (assoc-value :aliases-error nil)
               (assoc-value :observations [])
               (assoc-value :observations-error nil))
         :dispatch-n [[:user-expenses/fetch-supplier-detail supplier-id]
                      [:user-expenses/fetch-supplier-detail-expenses supplier-id]
                      [:user-expenses/fetch-supplier-detail-article-aliases supplier-id]
                      [:user-expenses/fetch-supplier-detail-price-observations supplier-id]]}
        {:db (-> db
               (assoc-value :current-id nil)
               (assoc-loading :loading? false)
               (assoc-value :error "Supplier id missing; cannot load supplier detail.")
               (assoc-value :expenses [])
               (assoc-value :aliases [])
               (assoc-value :observations []))}))))

(rf/reg-event-db
  :user-expenses/clear-supplier-detail
  common-interceptors
  (fn [db _]
    (assoc-in db detail-path {})))

;; ---------------------------------------------------------------------------
;; Supplier detail (GET /api/v1/expenses/suppliers/:id)
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-supplier-detail
  common-interceptors
  (fn [{:keys [db]} [supplier-id]]
    {:db (-> db
           (assoc-loading :loading? true)
           (assoc-value :error nil))
     :http-xhrio (x/xhrio db
                   {:method :get
                    :uri (str endpoints/suppliers-endpoint "/" supplier-id)
                    :admin-uri (str endpoints/admin-suppliers-endpoint "/" supplier-id)
                    :on-success [:user-expenses/fetch-supplier-detail-success supplier-id]
                    :on-failure [:user-expenses/fetch-supplier-detail-failure]})}))

(rf/reg-event-db
  :user-expenses/fetch-supplier-detail-success
  common-interceptors
  (fn [db [supplier-id response]]
    (let [supplier (or (:data response) (:supplier response) response)]
      (-> db
        (assoc-loading :loading? false)
        (assoc-value :error nil)
        (assoc-in (conj detail-path :by-id (str supplier-id)) supplier)))))

(rf/reg-event-db
  :user-expenses/fetch-supplier-detail-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch supplier detail" {:error error})
    (-> db
      (assoc-loading :loading? false)
      (assoc-value :error (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Related lists
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-supplier-detail-expenses
  common-interceptors
  (fn [{:keys [db]} [supplier-id]]
    {:db (-> db
           (assoc-loading :expenses-loading? true)
           (assoc-value :expenses-error nil)
           (assoc-value :expenses []))
     :http-xhrio (x/xhrio db
                   {:method :get
                    :uri endpoints/list-endpoint
                    :admin-uri endpoints/admin-expenses-endpoint
                    :params {:supplier_id (str supplier-id)
                            :limit 10
                            :offset 0
                            :order_dir "desc"}
                    :on-success [:user-expenses/fetch-supplier-detail-expenses-success]
                    :on-failure [:user-expenses/fetch-supplier-detail-expenses-failure]})}))

(rf/reg-event-db
  :user-expenses/fetch-supplier-detail-expenses-success
  common-interceptors
  (fn [db [response]]
    (let [rows (vec (or (:data response)
                      (:expenses response)
                      response
                      []))]
      (-> db
        (assoc-loading :expenses-loading? false)
        (assoc-value :expenses-error nil)
        (assoc-value :expenses rows)))))

(rf/reg-event-db
  :user-expenses/fetch-supplier-detail-expenses-failure
  common-interceptors
  (fn [db [error]]
    (let [msg (http/extract-error-message error)]
      (log/warn "Failed to fetch supplier detail expenses" {:error error})
      (-> db
        (assoc-loading :expenses-loading? false)
        (assoc-value :expenses-error msg)
        (assoc-value :expenses [])))))

(rf/reg-event-fx
  :user-expenses/fetch-supplier-detail-article-aliases
  common-interceptors
  (fn [{:keys [db]} [supplier-id]]
    {:db (-> db
           (assoc-loading :aliases-loading? true)
           (assoc-value :aliases-error nil)
           (assoc-value :aliases []))
     :http-xhrio (x/xhrio db
                   {:method :get
                    :uri endpoints/article-aliases-endpoint
                    :admin-uri endpoints/admin-article-aliases-endpoint
                    :params {:supplier_id (str supplier-id)
                            :limit 10
                            :offset 0}
                    :on-success [:user-expenses/fetch-supplier-detail-article-aliases-success]
                    :on-failure [:user-expenses/fetch-supplier-detail-article-aliases-failure]})}))

(rf/reg-event-db
  :user-expenses/fetch-supplier-detail-article-aliases-success
  common-interceptors
  (fn [db [response]]
    (let [rows (vec (or (:data response)
                      (:article-aliases response)
                      response
                      []))]
      (-> db
        (assoc-loading :aliases-loading? false)
        (assoc-value :aliases-error nil)
        (assoc-value :aliases rows)))))

(rf/reg-event-db
  :user-expenses/fetch-supplier-detail-article-aliases-failure
  common-interceptors
  (fn [db [error]]
    (let [msg (http/extract-error-message error)]
      (log/warn "Failed to fetch supplier detail article aliases" {:error error})
      (-> db
        (assoc-loading :aliases-loading? false)
        (assoc-value :aliases-error msg)
        (assoc-value :aliases [])))))

(rf/reg-event-fx
  :user-expenses/fetch-supplier-detail-price-observations
  common-interceptors
  (fn [{:keys [db]} [supplier-id]]
    {:db (-> db
           (assoc-loading :observations-loading? true)
           (assoc-value :observations-error nil)
           (assoc-value :observations []))
     :http-xhrio (x/xhrio db
                   {:method :get
                    :uri endpoints/price-observations-endpoint
                    :admin-uri endpoints/admin-price-observations-endpoint
                    :params {:supplier_id (str supplier-id)
                            :limit 10
                            :offset 0
                            :order_dir "desc"}
                    :on-success [:user-expenses/fetch-supplier-detail-price-observations-success]
                    :on-failure [:user-expenses/fetch-supplier-detail-price-observations-failure]})}))

(rf/reg-event-db
  :user-expenses/fetch-supplier-detail-price-observations-success
  common-interceptors
  (fn [db [response]]
    (let [rows (vec (or (:data response)
                      (:price-observations response)
                      response
                      []))]
      (-> db
        (assoc-loading :observations-loading? false)
        (assoc-value :observations-error nil)
        (assoc-value :observations rows)))))

(rf/reg-event-db
  :user-expenses/fetch-supplier-detail-price-observations-failure
  common-interceptors
  (fn [db [error]]
    (let [msg (http/extract-error-message error)]
      (log/warn "Failed to fetch supplier detail price observations" {:error error})
      (-> db
        (assoc-loading :observations-loading? false)
        (assoc-value :observations-error msg)
        (assoc-value :observations [])))))
