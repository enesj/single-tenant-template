(ns app.domain.frontend.expenses.events.user-expenses.profile
  "Profile page events and subscriptions for the settings hierarchy rollout."
  (:require
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.xhrio :as x]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(def ^:private profile-path [:user-expenses :profile])
(def ^:private defaults-action-path (conj profile-path :actions :defaults))
(def ^:private tenant-settings-action-path (conj profile-path :actions :tenant-settings))
(def ^:private tenant-name-action-path (conj profile-path :actions :tenant-name))

(defn- set-loading [db path value]
  (assoc-in db (conj path :loading?) value))

(defn- set-error [db path value]
  (assoc-in db (conj path :error) value))

(rf/reg-event-fx
  :profile/fetch
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (-> db
           (assoc-in (conj profile-path :loading?) true)
           (assoc-in (conj profile-path :error) nil))
     :http-xhrio (x/xhrio db
                   {:method :get
                    :uri endpoints/profile-endpoint
                    :on-success [:profile/fetch-success]
                    :on-failure [:profile/fetch-failure]})}))

(rf/reg-event-db
  :profile/fetch-success
  common-interceptors
  (fn [db [response]]
    (-> db
      (assoc-in (conj profile-path :data) (:data response))
      (assoc-in (conj profile-path :loading?) false)
      (assoc-in (conj profile-path :error) nil))))

(rf/reg-event-db
  :profile/fetch-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch profile" {:error error})
    (-> db
      (assoc-in (conj profile-path :loading?) false)
      (assoc-in (conj profile-path :error) (http/extract-error-message error)))))

(rf/reg-event-fx
  :profile/update-defaults
  common-interceptors
  (fn [{:keys [db]} [params]]
    {:db (-> db
           (set-loading defaults-action-path true)
           (set-error defaults-action-path nil))
     :http-xhrio (x/xhrio db
                   {:method :put
                    :uri endpoints/profile-defaults-endpoint
                    :params params
                    :on-success [:profile/update-defaults-success]
                    :on-failure [:profile/update-defaults-failure]})}))

(rf/reg-event-fx
  :profile/update-defaults-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [result (:data response)]
      {:db (-> db
             (assoc-in (conj profile-path :data :settings :default-expense-category-id)
               (:default-expense-category-id result))
             (set-loading defaults-action-path false)
             (set-error defaults-action-path nil))
       :dispatch [:toast {:type :success :message "Profile defaults updated"}]})))

(rf/reg-event-db
  :profile/update-defaults-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to update profile defaults" {:error error})
    (-> db
      (set-loading defaults-action-path false)
      (set-error defaults-action-path (http/extract-error-message error)))))

(rf/reg-event-fx
  :profile/update-tenant-settings
  common-interceptors
  (fn [{:keys [db]} [params]]
    {:db (-> db
           (set-loading tenant-settings-action-path true)
           (set-error tenant-settings-action-path nil))
     :http-xhrio (x/xhrio db
                   {:method :put
                    :uri endpoints/tenant-settings-endpoint
                    :params params
                    :on-success [:profile/update-tenant-settings-success]
                    :on-failure [:profile/update-tenant-settings-failure]})}))

(rf/reg-event-fx
  :profile/update-tenant-settings-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    {:db (-> db
           (assoc-in (conj profile-path :data :tenant-settings) (:settings response))
           (set-loading tenant-settings-action-path false)
           (set-error tenant-settings-action-path nil))
     :dispatch [:toast {:type :success :message "Workspace settings updated"}]}))

(rf/reg-event-db
  :profile/update-tenant-settings-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to update tenant settings" {:error error})
    (-> db
      (set-loading tenant-settings-action-path false)
      (set-error tenant-settings-action-path (http/extract-error-message error)))))

(rf/reg-event-fx
  :profile/update-tenant-name
  common-interceptors
  (fn [{:keys [db]} [params]]
    {:db (-> db
           (set-loading tenant-name-action-path true)
           (set-error tenant-name-action-path nil))
     :http-xhrio (x/xhrio db
                   {:method :put
                    :uri endpoints/tenant-name-endpoint
                    :params params
                    :on-success [:profile/update-tenant-name-success]
                    :on-failure [:profile/update-tenant-name-failure]})}))

(rf/reg-event-fx
  :profile/update-tenant-name-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [tenant (:tenant response)]
      {:db (-> db
             (assoc-in [:session :tenant] tenant)
             (assoc-in (conj profile-path :data :tenant-name) (:name tenant))
             (set-loading tenant-name-action-path false)
             (set-error tenant-name-action-path nil))
       :dispatch [:toast {:type :success :message "Workspace name updated"}]})))

(rf/reg-event-db
  :profile/update-tenant-name-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to update tenant name" {:error error})
    (-> db
      (set-loading tenant-name-action-path false)
      (set-error tenant-name-action-path (http/extract-error-message error)))))

(rf/reg-sub
  :profile/data
  (fn [db _]
    (get-in db (conj profile-path :data) {})))

(rf/reg-sub
  :profile/loading?
  (fn [db _]
    (boolean (get-in db (conj profile-path :loading?) false))))

(rf/reg-sub
  :profile/error
  (fn [db _]
    (or (get-in db (conj profile-path :error))
      (get-in db (conj defaults-action-path :error))
      (get-in db (conj tenant-settings-action-path :error))
      (get-in db (conj tenant-name-action-path :error)))))

(rf/reg-sub
  :profile/saving?
  (fn [db _]
    (boolean
      (or (get-in db (conj defaults-action-path :loading?) false)
        (get-in db (conj tenant-settings-action-path :loading?) false)
        (get-in db (conj tenant-name-action-path :loading?) false)))))

(rf/reg-sub
  :profile/export-loading?
  (fn [db _]
    (boolean (get-in db [:user-expenses :export :loading?] false))))

(rf/reg-sub
  :profile/delete-loading?
  (fn [db _]
    (boolean (get-in db [:user-expenses :bulk :loading?] false))))
