(ns app.template.frontend.events.auth.guard
  "Route-level auth guard for user pages.
   Mirrors :admin/check-auth-protected for cookie-based user sessions."
  (:require
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [re-frame.core :as rf]))

(defn- normalize-events
  "Normalize an on-success payload into a vector of valid event vectors.
   Accepts nil, a single event vector [:event/id ...], or many [[:a] [:b]]."
  [events]
  (cond
    (nil? events) []
    (and (sequential? events) (empty? events)) []
    (and (sequential? events) (sequential? (first events))) events
    (sequential? events) [events]
    :else []))

(rf/reg-event-fx
  :user/check-auth-protected
  common-interceptors
  (fn [{:keys [db]} [on-success-events]]
    (let [authenticated? (get-in db [:session :authenticated?])]
      (cond
        ;; Confirmed authenticated → dispatch page init events
        (true? authenticated?)
        (let [events (normalize-events on-success-events)]
          (if (seq events)
            {:dispatch-n events}
            {}))

        ;; Explicitly not authenticated (session expired) → redirect to login
        (false? authenticated?)
        {:dispatch [:navigate-to "/login"]}

        ;; Unknown (nil — bootstrap fetch still in flight) → verify with backend
        :else
        {:http-xhrio (http/api-request
                       {:method     :get
                        :uri        "/auth/status"
                        :on-success [:user/auth-guard-check-success on-success-events]
                        :on-failure [:user/auth-invalid]})}))))

(rf/reg-event-fx
  :user/auth-guard-check-success
  common-interceptors
  (fn [{:keys [db]} [on-success-events response]]
    (let [authenticated? (get response :authenticated false)]
      (if authenticated?
        {:db         (-> db
                       (assoc-in [:session :loading?] false)
                       (assoc-in [:session :authenticated?] true))
         :dispatch-n (normalize-events on-success-events)}
        {:db       (-> db
                     (assoc-in [:session :loading?] false)
                     (assoc-in [:session :authenticated?] false))
         :dispatch [:navigate-to "/login"]}))))

(rf/reg-event-fx
  :user/auth-invalid
  common-interceptors
  (fn [{:keys [db]} _]
    {:db       (-> db
                 (assoc-in [:session :loading?] false)
                 (assoc-in [:session :authenticated?] false))
     :dispatch [:navigate-to "/login"]}))
