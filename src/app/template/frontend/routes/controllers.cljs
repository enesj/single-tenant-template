(ns app.template.frontend.routes.controllers
  "Route controllers for the template application.
   Handles page initialization and cleanup during navigation."
  (:require
    [re-frame.core :as rf]))

(defn make-simple-controller
  "Creates a simple controller that dispatches an init event on start and cleanup on stop"
  [init-event]
  [{:start (fn [_] (rf/dispatch [init-event]))
    :stop (fn [_] (rf/dispatch [:page/cleanup]))}])

(defn extract-entity-name
  "Extract entity name from match-or-identity"
  [match-or-identity]
  (if (map? match-or-identity)
    (get-in match-or-identity [:parameters :path :entity-name])
    (if (vector? match-or-identity)
      (first match-or-identity)                             ;; Handle vector case [entity-name item-id]
      match-or-identity)))                                  ;; Handle string case

(defn extract-item-id
  "Extract item-id from match-or-identity and ensure it's a number if possible"
  [match-or-identity]
  (when (or (map? match-or-identity) (vector? match-or-identity))
    (let [raw-id (if (map? match-or-identity)
                   (get-in match-or-identity [:parameters :path :item-id])
                   (second match-or-identity))]             ;; Handle vector case [entity-name item-id]
      ;; Convert to number if possible
      (if (and (string? raw-id) (re-matches #"^\d+$" raw-id))
        (js/parseInt raw-id)
        raw-id))))

(defn make-entity-controller
  "Creates a controller for entity routes"
  [route-type]
  (let [identity-fn (case route-type
                      :update (fn [match]
                                [(get-in match [:parameters :path :entity-name])
                                 (get-in match [:parameters :path :item-id])])
                      (fn [match]
                        (get-in match [:parameters :path :entity-name])))
        event-name (case route-type
                     :add :page/init-entity-add
                     :update :page/init-entity-update
                     :detail :page/init-entity-detail)]
    [{:identity identity-fn
      :start (fn [match-or-identity]
               (let [entity-name (extract-entity-name match-or-identity)
                     ;; Extract item-id (already normalized to number in extract-item-id function)
                     item-id (when (= route-type :update)
                               (extract-item-id match-or-identity))]
                 (cond
                   ;; Update needs both entity-name and item-id
                   (and (= route-type :update) entity-name item-id)
                   (rf/dispatch [event-name entity-name item-id])

                   ;; Add and detail only need entity-name
                   (and (not= route-type :update) entity-name (not= entity-name "nil"))
                   (rf/dispatch [event-name entity-name])

                   :else
                   nil)))
      :stop (fn [_] (rf/dispatch [:page/cleanup]))}]))

(defn user-guarded-start
  "Creates a route controller that runs init-event only after confirming user auth.
   Redirects to /login if the session is expired or the user is not authenticated.
   Mirrors the admin's guarded-start but for cookie-based user sessions."
  [init-event]
  [{:start (fn [_]
             (rf/dispatch [:user/check-auth-protected (when init-event [init-event])]))
    :stop  (fn [_]
             (rf/dispatch [:page/cleanup]))}])
