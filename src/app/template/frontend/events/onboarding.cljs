(ns app.template.frontend.events.onboarding
  "Onboarding workflow events and subscriptions for the template frontend.

   Handles complete onboarding flow including tenant setup, property creation,
   and step management with validation."
  (:require
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ========================================================================
;; Page Initialization
;; ========================================================================

(rf/reg-event-fx
  :page/init-onboarding
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (-> db
           (assoc-in [:page :current] :onboarding)
           (assoc-in [:onboarding :current-step] 1)
           (update-in [:onboarding :step-data]
             (fn [existing]
               (merge {:organization {}
                       :property {}
                       :financial {:currency "USD"}}
                 existing)))
           (assoc-in [:onboarding :validation-errors] {})
           (assoc-in [:onboarding :loading] false)
           (update-in [:onboarding :completed-steps] (fn [steps] (or steps #{})))
           (assoc-in [:onboarding :completed?] false))}))

;; ========================================================================
;; Main Onboarding Workflow Events
;; ========================================================================

;; Main onboarding completion event
(rf/reg-event-fx
  :onboarding/complete
  common-interceptors
  (fn [{:keys [db]} [event-data]]
    ;; Use step-data from our new structure or fall back to provided event-data
    (let [step-data (get-in db [:onboarding :step-data])
          tenant-data (or (:tenant event-data) (:organization step-data))
          property-data (or (:property event-data) (:property step-data))
          financial-data (:financial step-data)]
      {:db (-> db
             (assoc-in [:onboarding :completed?] true)
             (assoc-in [:onboarding :loading] true)
             (assoc-in [:onboarding :tenant-data] tenant-data)
             (assoc-in [:onboarding :property-data] property-data)
             (assoc-in [:onboarding :financial-data] financial-data))
       :dispatch-n [;; Update tenant settings if needed
                    (when (not-empty (:organization-name tenant-data))
                      [:onboarding/update-tenant-settings tenant-data])
                    ;; Create first property if provided and not skipped
                    (when (and (not-empty (:name property-data))
                            (not (:skip-property property-data)))
                      [:onboarding/create-first-property property-data])
                    ;; Navigate to dashboard after a brief delay
                    [:dispatch-later [{:ms 1500 :dispatch [:navigate/to-dashboard]}]]]})))

;; ========================================================================
;; API Integration Events
;; ========================================================================

(rf/reg-event-fx
  :onboarding/update-tenant-settings
  common-interceptors
  (fn [{:keys [db]} [tenant-data]]
    (let [auth-status (get db :auth-status)
          is-authenticated (:authenticated auth-status)]
      (if is-authenticated
        {:http-xhrio (http/api-request
                       {:method :put
                        :uri "/api/v1/tenant/settings"
                        :params tenant-data
                        :on-success [:onboarding/tenant-settings-updated]
                        :on-failure [:onboarding/settings-update-failed]})}
        {:db (assoc-in db [:onboarding :settings-update-failed]
                       "Authentication required to update tenant settings")
         :dispatch [:navigate-to "/login?redirect=/onboarding&type=tenant"]}))))

(rf/reg-event-fx
  :onboarding/create-first-property
  common-interceptors
  (fn [{:keys [db]} [property-data]]
    (let [auth-status (get db :auth-status)
          is-authenticated (:authenticated auth-status)]
      (if is-authenticated
        (do
          (log/info "🏠 Task 2.2: Calling onboarding property creation API endpoint" {:property-data property-data})
          {:http-xhrio (http/api-request
                         {:method :post
                          :uri "/api/v1/onboarding/properties"
                          :params property-data
                          :on-success [:onboarding/property-created]
                          :on-failure [:onboarding/property-creation-failed]})})
        {:db (assoc-in db [:onboarding :property-creation-failed]
                       "Authentication required to create property")
         :dispatch [:navigate-to "/login?redirect=/onboarding&type=tenant"]}))))

;; ========================================================================
;; API Response Handlers
;; ========================================================================

(rf/reg-event-db
  :onboarding/tenant-settings-updated
  common-interceptors
  (fn [db [response]]
    (-> db
      (assoc-in [:onboarding :tenant-settings-updated?] true)
      (assoc-in [:session :tenant] response))))

(rf/reg-event-db
  :onboarding/property-created
  common-interceptors
  (fn [db [response]]
    (log/info "🏠 Property created successfully, storing in app-db" {:property (:property response)})
    (-> db
      (assoc-in [:onboarding :first-property] (:property response))
      (assoc-in [:onboarding :property-created?] true))))

(rf/reg-event-db
  :onboarding/settings-update-failed
  common-interceptors
  (fn [db [error]]
    (assoc-in db [:onboarding :error]
              {:type :tenant-settings
               :message "Failed to update tenant settings"
               :details error})))

(rf/reg-event-db
  :onboarding/property-creation-failed
  common-interceptors
  (fn [db [error]]
    (assoc-in db [:onboarding :error]
              {:type :property-creation
               :message "Failed to create property"
               :details error})))

;; ========================================================================
;; Navigation Events
;; ========================================================================

(rf/reg-event-fx
  :navigate/to-dashboard
  common-interceptors
  (fn [{:keys [db]} _]
    {:redirect "/entities"}))

;; ========================================================================
;; Step Management Events
;; ========================================================================

(rf/reg-event-db
  :onboarding/set-step
  common-interceptors
  (fn [db [step-number]]
    (assoc-in db [:onboarding :current-step] step-number)))

(rf/reg-event-fx
  :onboarding/next-step
  common-interceptors
  (fn [{:keys [db]} _]
    (let [current-step (get-in db [:onboarding :current-step] 1)
          next-step (inc current-step)
          max-step 5]
      (if (<= next-step max-step)
        {:db (-> db
               (assoc-in [:onboarding :current-step] next-step)
               (update-in [:onboarding :completed-steps] (fn [steps] (conj (or steps #{}) current-step))))}
        {:db db}))))

(rf/reg-event-fx
  :onboarding/previous-step
  common-interceptors
  (fn [{:keys [db]} _]
    (let [current-step (get-in db [:onboarding :current-step] 1)
          previous-step (dec current-step)]
      (if (>= previous-step 1)
        {:db (assoc-in db [:onboarding :current-step] previous-step)}
        {:db db}))))

(rf/reg-event-db
  :onboarding/update-step-data
  common-interceptors
  (fn [db [step-key field-key value]]
    (assoc-in db [:onboarding :step-data step-key field-key] value)))

;; ========================================================================
;; Validation Events
;; ========================================================================

(rf/reg-event-fx
  :onboarding/validate-step
  common-interceptors
  (fn [{:keys [db]} [step-number]]
    (let [step-data (get-in db [:onboarding :step-data])
          errors (case step-number
                   2 (let [org-data (:organization step-data)]
                       (cond-> {}
                         (or (nil? (:organization-name org-data))
                           (< (count (:organization-name org-data)) 2))
                         (assoc :organization-name "Organization name is required (minimum 2 characters)")))

                   3 (let [prop-data (:property step-data)]
                       (if (:skip-property prop-data)
                         {}
                         (cond-> {}
                           (or (nil? (:name prop-data))
                             (< (count (:name prop-data)) 2))
                           (assoc :name "Property name is required (minimum 2 characters)"))))

                   4 (let [fin-data (:financial step-data)]
                       (cond-> {}
                         (nil? (:currency fin-data))
                         (assoc :currency "Currency selection is required")))

                   {})]
      {:db (assoc-in db [:onboarding :validation-errors] errors)})))

;; ========================================================================
;; Reset Events
;; ========================================================================

(rf/reg-event-db
  :onboarding/reset
  common-interceptors
  (fn [db _]
    (assoc db :onboarding
      {:current-step 1
       :step-data {:organization {}
                   :property {}
                   :financial {}}
       :validation-errors {}
       :loading false
       :completed-steps #{}
       :completed? false})))

;; ========================================================================
;; Subscriptions
;; ========================================================================

(rf/reg-sub
  :onboarding/current-step
  (fn [db]
    (get-in db [:onboarding :current-step] 1)))

(rf/reg-sub
  :onboarding/step-data
  (fn [db [_ step-key]]
    (get-in db [:onboarding :step-data step-key] {})))

(rf/reg-sub
  :onboarding/validation-errors
  (fn [db]
    (get-in db [:onboarding :validation-errors] {})))

(rf/reg-sub
  :onboarding/can-proceed
  (fn [db]
    (empty? (get-in db [:onboarding :validation-errors] {}))))

(rf/reg-sub
  :onboarding/is-loading
  (fn [db]
    (get-in db [:onboarding :loading] false)))

(rf/reg-sub
  :onboarding/completed-steps
  (fn [db]
    (get-in db [:onboarding :completed-steps] #{})))

(rf/reg-sub
  :onboarding/progress-percentage
  (fn [db]
    (let [current-step (get-in db [:onboarding :current-step] 1)
          total-steps 5]
      (* (/ current-step total-steps) 100))))
