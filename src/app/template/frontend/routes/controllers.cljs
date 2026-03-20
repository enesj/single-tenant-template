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

(defn user-guarded-start
  "Creates a route controller that runs init-event only after confirming user auth.
   Redirects to /login if the session is expired or the user is not authenticated.
   Mirrors the admin's guarded-start but for cookie-based user sessions."
  [init-event]
  [{:start (fn [_]
             (rf/dispatch [:user/check-auth-protected (when init-event [init-event])]))
    :stop  (fn [_]
             (rf/dispatch [:page/cleanup]))}])

(defn power-user-guarded-start
  "Creates a route controller that requires admin or owner role.
   Chains: auth check → role check → page init.
   Redirects to /dashboard if the user lacks power-user privileges."
  [init-event]
  [{:start (fn [_]
             (rf/dispatch [:user/check-auth-protected
                           [:user/check-power-user-then-init (when init-event [init-event])]]))
    :stop  (fn [_]
             (rf/dispatch [:page/cleanup]))}])
