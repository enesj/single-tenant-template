(ns app.admin.frontend.components.user-actions-test
  (:require
    [app.admin.frontend.components.user-actions :as user-actions]
    [app.template.frontend.events.config :as config-events]
    [app.template.frontend.events.form :as form-events]
    [app.template.frontend.events.list.crud :as crud-events]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]))

(defn- stop-event []
  #js {:stopPropagation (fn [])})

(deftest edit-user-handler-prefers-row-edit-callback
  (testing "edit-user clears form state and uses the row-level edit callback when present"
    (let [dispatched (atom [])
          clicked (atom nil)
          user {:id 42
                :email "user@example.com"
                :on-edit-click (fn [item] (reset! clicked item))}
          handlers (user-actions/create-user-action-handlers user 42 "user@example.com")]
      (with-redefs [rf/dispatch (fn [event] (swap! dispatched conj event))]
        ((:edit-user handlers) (stop-event))
        (is (= user @clicked))
        (is (= [[::crud-events/clear-error :users]
                [::form-events/clear-form-errors :users]]
              @dispatched))))))

(deftest edit-user-handler-falls-back-to-inline-editing
  (testing "edit-user falls back to the canonical inline editing event when no row callback is present"
    (let [dispatched (atom [])
          user {:id 99 :email "fallback@example.com"}
          handlers (user-actions/create-user-action-handlers user 99 "fallback@example.com")]
      (with-redefs [rf/dispatch (fn [event] (swap! dispatched conj event))]
        ((:edit-user handlers) (stop-event))
        (is (= [[::crud-events/clear-error :users]
                [::form-events/clear-form-errors :users]
                [::config-events/set-editing 99]]
              @dispatched))))))
