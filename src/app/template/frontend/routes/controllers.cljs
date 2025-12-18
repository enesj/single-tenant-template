(ns app.template.frontend.routes.controllers
  "Route controllers for the template application.
   Handles page initialization and cleanup during navigation."
  (:require
    [re-frame.core :as rf]
    [reitit.frontend.easy :as rtfe]
    [taoensso.timbre :as log]))

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

(defn make-app-entity-controller
  "Creates a controller for app routes that map to entity functionality.
   Maps app route paths to specific entity names."
  [route-type app-path]
  (let [;; Map app paths to entity names
        entity-mapping {"/app/properties" "properties"
                        "/app/transactions" "transactions_v2"
                        "/app/financials" "transactions_v2"
                        "/app/reports" "cohost_balances"
                        "/app/cohosts" "property_cohosts"
                        "/app/users" "users"
                        "/app/invitations" "invitations"}

        entity-name (get entity-mapping app-path)

        identity-fn (case route-type
                      :update (fn [match]
                                [entity-name
                                 (or (get-in match [:parameters :path :property-id])
                                   (get-in match [:parameters :path :transaction-id])
                                   (get-in match [:parameters :path :cohost-id])
                                   (get-in match [:parameters :path :user-id])
                                   (get-in match [:parameters :path :item-id]))])
                      (fn [_match] entity-name))

        event-name (case route-type
                     :add :page/init-entity-add
                     :update :page/init-entity-update
                     :detail :page/init-entity-detail)]

    ;; Log the mapping for debugging
    (when ^boolean js/goog.DEBUG
      (log/info "🔧 App route controller created:"
        {:route-type route-type :app-path app-path :entity-name entity-name :event-name event-name}))

    [{:identity identity-fn
      :start (fn [match-or-identity]
               (when ^boolean js/goog.DEBUG
                 (log/info "🚀 App route controller starting:"
                   {:route-type route-type
                    :app-path app-path
                    :entity-name entity-name
                    :match-or-identity (if (map? match-or-identity)
                                         (select-keys match-or-identity [:parameters :data :path-params])
                                         match-or-identity)}))

               (let [item-id (when (= route-type :update)
                               (or (get-in match-or-identity [:parameters :path :property-id])
                                 (get-in match-or-identity [:parameters :path :transaction-id])
                                 (get-in match-or-identity [:parameters :path :cohost-id])
                                 (get-in match-or-identity [:parameters :path :user-id])
                                 (get-in match-or-identity [:parameters :path :item-id])))]
                 (cond
                   ;; Update needs both entity-name and item-id
                   (and (= route-type :update) entity-name item-id)
                   (do
                     (log/info "🎯 Dispatching update event:" {:entity-name entity-name :item-id item-id})
                     (rf/dispatch [event-name entity-name item-id]))

                   ;; Add and detail only need entity-name
                   (and (not= route-type :update) entity-name (not= entity-name "nil"))
                   (do
                     (log/info "🎯 Dispatching detail/add event:" {:entity-name entity-name :event-name event-name})
                     (rf/dispatch [event-name entity-name]))

                   :else
                   (do
                     (log/warn "❌ App route controller: missing entity mapping or item-id"
                       {:route-type route-type :app-path app-path :entity-name entity-name :item-id item-id})
                     ;; Try to dispatch anyway if we have entity-name
                     (when entity-name
                       (log/info "🔄 Attempting dispatch anyway with entity-name:" entity-name)
                       (rf/dispatch [event-name entity-name]))))))
      :stop (fn [_]
              (when ^boolean js/goog.DEBUG
                (log/info "🛑 App route controller stopping"))
              (rf/dispatch [:page/cleanup]))}]))

(defn make-redirect-controller
  "Creates a controller that redirects to an entity URL using SPA navigation"
  [entity-name route-type]
  (let [target-route (case route-type
                       :detail [:entity-detail {:entity-name entity-name}]
                       :add [:entity-add {:entity-name entity-name}]
                       :update (fn [item-id]
                                 [:entity-update {:entity-name entity-name
                                                  :item-id (str item-id)}]))]
    [{:start (fn [match-or-identity]
               (when ^boolean js/goog.DEBUG
                 (log/info "🔄 Redirecting app route to entity:"
                   {:entity-name entity-name :route-type route-type}))

               (if (= route-type :update)
                 ;; For update routes, extract the item-id from parameters
                 (let [item-id (or (get-in match-or-identity [:parameters :path :property-id])
                                 (get-in match-or-identity [:parameters :path :transaction-id])
                                 (get-in match-or-identity [:parameters :path :cohost-id])
                                 (get-in match-or-identity [:parameters :path :user-id])
                                 (get-in match-or-identity [:parameters :path :item-id]))]
                   (when item-id
                     ;; Use reitit navigation
                     (js/setTimeout #(apply rtfe/push-state (target-route item-id)) 10)))
                 ;; For detail and add routes, redirect directly using reitit navigation
                 (js/setTimeout #(apply rtfe/push-state target-route) 10)))
      :stop (fn [_] nil)}]))
