(ns app.admin.frontend.test-setup
  "Node test bootstrap: installs jsdom, resets re-frame db, and stubs side effects."
  (:require
    [cljs.test :refer [use-fixtures]]
    [goog.object :as gobj]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]
    [reitit.frontend.easy :as rtfe]))

;; Install jsdom globals (window, document, localStorage) for Node tests
(defonce ^:private _jsdom
  (try
    (let [setup (js/require "jsdom-global")]
      (setup nil (clj->js {:url "https://admin.test.local"
                           :pretendToBeVisual true})))
    (catch :default _e
      nil)))

(defn- ensure-window-alias! []
  (let [global js/globalThis]
    (when (nil? (gobj/get global "window"))
      (gobj/set global "window" global))
    (when-let [window (gobj/get global "window")]
      (when (nil? (gobj/get window "globalThis"))
        (gobj/set window "globalThis" global)))))

(defn- build-storage []
  (let [state (atom {})
        storage (js-obj)]
    (gobj/set storage "getItem" (fn [k] (get @state (str k) nil)))
    (gobj/set storage "setItem" (fn [k v]
                                  (swap! state assoc (str k) (str (or v "")))
                                  nil))
    (gobj/set storage "removeItem" (fn [k]
                                     (swap! state dissoc (str k))
                                     nil))
    (gobj/set storage "clear" (fn []
                                (reset! state {})
                                nil))
    (gobj/set storage "key" (fn [idx]
                              (nth (keys @state) idx nil)))
    (js/Object.defineProperty storage "length"
      (js-obj "get" (fn [] (count @state))))
    storage))

(defn- ensure-storage! [k]
  (let [global js/globalThis
        window (gobj/get global "window")
        storage (build-storage)]
    (aset global k storage)
    (when window
      (aset window k storage))))

(defn- patch-react-use-sync-external-store! []
  (try
    (let [react (js/require "react")]
      (when (and react (not (gobj/get react "__patchedUseSyncExternalStore__")))
        (when-let [orig (gobj/get react "useSyncExternalStore")]
          (gobj/set react "__patchedUseSyncExternalStore__" true)
          (gobj/set react "useSyncExternalStore"
            (fn [subscribe getSnapshot & [getServerSnapshot]]
              (orig subscribe getSnapshot (or getServerSnapshot getSnapshot)))))))
    (catch :default _ nil)))

(defn- install-routing-stub! []
  (set! rtfe/push-state
    (fn
      ([_route] nil)
      ([_route _params] nil)
      ([_route _params _query & _] nil))))

(defonce ^:private _browser-shim
  (do
    (ensure-window-alias!)
    (ensure-storage! "localStorage")
    (ensure-storage! "sessionStorage")
    (patch-react-use-sync-external-store!)
    (install-routing-stub!)
    true))

(defonce captured-http-requests (atom []))

(defn install-http-stub!
  "Intercept :http-xhrio effects and capture request maps into atom.
   Returns a function to remove the stub (re-register a no-op)."
  []
  (reset! captured-http-requests [])
  ;; Check if http-xhrio is already registered (in browser environment)
  (let [original-handler (get-in @re-frame.db/app-db [:re-frame.db/registrar :fx :http-xhrio])]
    (rf/reg-fx :http-xhrio
      (fn [req]
        (swap! captured-http-requests conj req)
        (when ^boolean goog.DEBUG
          (.debug js/console "captured :http-xhrio" (clj->js req)))))
    ;; Return function to restore original handler or register no-op
    #(if original-handler
       (rf/reg-fx :http-xhrio original-handler)
       (rf/reg-fx :http-xhrio (fn [_] nil)))))

(defn last-http-request [] (last @captured-http-requests))

(defn respond-success!
  "Dispatch the on-success event from the last captured request with `response`."
  [response]
  (when-let [req (last-http-request)]
    (when-let [on-success (:on-success req)]
      (rf/dispatch-sync (conj on-success response)))))

(defn respond-failure!
  "Dispatch the on-failure event from the last captured request with `error`."
  [error]
  (when-let [req (last-http-request)]
    (when-let [on-failure (:on-failure req)]
      (rf/dispatch-sync (conj on-failure error)))))

(defn reset-db!
  "Reset re-frame db and initialize with basic admin state for testing (single-tenant)"
  []
  (reset! rf-db/app-db
    {:admin/token "test-token"
     :admin/authenticated? true
     :admin/current-user {:id 1 :role :admin}
     :admin/login-loading? false
     :admin/auth-checking? false
     :admin/users-success-message nil
     :admin/users-error nil
     :admin/audit-logs-success-message nil
     :admin/audit-logs-error nil
     :admin/login-events-success-message nil
     :admin/login-events-error nil
     :deletion-constraints {:users {:results {} :loading {} :errors {}}}
     :admin/config {:table-columns {:users {:available-columns [:id :email :name :role :status]
                                            :column-config {:id {:width 80 :always-visible true}
                                                            :email {:width 200}
                                                            :name {:width 150}
                                                            :role {:width 100}
                                                            :status {:width 100}}
                                            :always-visible [:id :email]
                                            :computed-fields {}}
                                    :audit-logs {:available-columns [:id :action :timestamp :admin_id]
                                                 :column-config {:id {:width 80 :always-visible true}
                                                                 :action {:width 150}
                                                                 :timestamp {:width 150}
                                                                 :admin_id {:width 100}}
                                                 :always-visible [:id :action]
                                                 :computed-fields {}}
                                    :login-events {:available-columns [:id :event_type :ip_address :created_at]
                                                   :column-config {:id {:width 80 :always-visible true}
                                                                   :event_type {:width 120}
                                                                   :ip_address {:width 150}
                                                                   :created_at {:width 150}}
                                                   :always-visible [:id :event_type]
                                                   :computed-fields {}}}}})

  ;; Set admin token in localStorage for admin-context detection
  (when (and (exists? js/globalThis) (.-localStorage js/globalThis))
    (.setItem (.-localStorage js/globalThis) "admin-token" "test-token")))

(defn put-token! [token]
  (.setItem js/localStorage "admin-token" (or token "")))

(defn setup-entity-subscriptions!
  "Set up subscription handlers for admin entities in test environment"
  [entity-key loading-value error-value]
  ;; Register admin loading subscription
  (rf/reg-sub
    (keyword "admin" (str (name entity-key) "-loading?"))
    (fn [_db _]
      loading-value))

  ;; Register admin error subscription
  (rf/reg-sub
    (keyword "admin" (str (name entity-key) "-error"))
    (fn [_db _]
      error-value))

  ;; Register admin success message subscription
  (rf/reg-sub
    :admin/success-message
    (fn [_db _]
      nil)))

;; Global fixtures for tests in this ns and downstream requires
(use-fixtures :each
  (fn [t]
    (reset-db!)
    (install-http-stub!)
    (t)))
