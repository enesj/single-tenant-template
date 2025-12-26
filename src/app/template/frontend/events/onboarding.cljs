(ns app.template.frontend.events.onboarding
  "Onboarding workflow events and subscriptions for the template frontend.

   Handles onboarding flow including tenant setup and step management."
  (:require
    #_[app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [re-frame.core :as rf]
    #_[taoensso.timbre :as log]))

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
                       :financial {:currency "USD"}}
                 existing)))
           (assoc-in [:onboarding :validation-errors] {})
           (assoc-in [:onboarding :loading] false)
           (update-in [:onboarding :completed-steps] (fn [steps] (or steps #{})))
           (assoc-in [:onboarding :completed?] false))}))
*** End Patch

                     {}
        {:db (assoc-in db [:onboarding :validation-errors] errors)}

;; ========================================================================
;; Reset Events
;; ========================================================================

#_(rf/reg-event-db
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

#_(rf/reg-sub
    :onboarding/current-step
    (fn [db]
      (get-in db [:onboarding :current-step] 1)))

#_(rf/reg-sub
    :onboarding/step-data
    (fn [db [_ step-key]]
      (get-in db [:onboarding :step-data step-key] {})))

#_(rf/reg-sub
    :onboarding/validation-errors
    (fn [db]
      (get-in db [:onboarding :validation-errors] {})))

#_(rf/reg-sub
    :onboarding/can-proceed
    (fn [db]
      (empty? (get-in db [:onboarding :validation-errors] {}))))

#_(rf/reg-sub
    :onboarding/is-loading
    (fn [db]
      (get-in db [:onboarding :loading] false)))

#_(rf/reg-sub
    :onboarding/completed-steps
    (fn [db]
      (get-in db [:onboarding :completed-steps] #{})))

#_(rf/reg-sub
    :onboarding/progress-percentage
    (fn [db]
      (let [current-step (get-in db [:onboarding :current-step] 1)
            total-steps 5]
        (* (/ current-step total-steps) 100))))
