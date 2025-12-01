(ns app.admin.frontend.components.enhanced-action-buttons-test
  "Tests for enhanced action buttons component.

   Tests cover:
   - Button rendering based on permissions
   - Edit/delete functionality with role constraints
   - Deletion constraint checking
   - Custom actions rendering
   - Tooltip behavior
   - Loading states and disabled states"
  (:require
    [app.admin.frontend.components.enhanced-action-buttons :as buttons]
    [app.admin.frontend.test-setup :as setup]
    [app.frontend.utils.test-utils :as test-utils]
    [app.shared.frontend.components.action-buttons :as action-buttons]
    [cljs.test :refer [deftest is testing]]
    [clojure.string :as str]
    [re-frame.db :as rf-db]
    [uix.core :refer [$]]))

;; Initialize test environment for React component testing
(test-utils/setup-test-environment!)

;; Mock subscriptions for deletion constraints
(defn prime-constraint!
  "Utility helper to seed the re-frame app-db with deletion constraint state for tests."
  [entity-type {:keys [id can-delete? loading? error constraints warnings checked-at]}]
  (let [scope (case entity-type
                :tenants :tenants
                :users :users
                (throw (ex-info "Unsupported entity type" {:entity-type entity-type})))
        entity-id (or id 123)
        result (when (some? can-delete?)
                 {:can-delete? (boolean can-delete?)
                  :constraints (vec (or constraints []))
                  :warnings (vec (or warnings []))
                  :checked-at (or checked-at 0)})]
    (swap! rf-db/app-db
      (fn [db]
        (-> db
          (cond-> result
            (assoc-in [:deletion-constraints scope :results entity-id] result))
          (cond-> (nil? result)
            (update-in [:deletion-constraints scope :results] dissoc entity-id))
          (cond-> (= true loading?)
            (assoc-in [:deletion-constraints scope :loading entity-id] true))
          (cond-> (not= true loading?)
            (update-in [:deletion-constraints scope :loading] dissoc entity-id))
          (cond-> error
            (assoc-in [:deletion-constraints scope :errors entity-id] error))
          (cond-> (nil? error)
            (update-in [:deletion-constraints scope :errors] dissoc entity-id)))))))

(defn render-markup [props]
  (test-utils/enhanced-render-to-static-markup
    ($ buttons/enhanced-action-buttons props)))

(defn capture-action-config
  "Render component to static markup while capturing the props passed to shared action-buttons."
  [props]
  (let [captured (atom nil)]
    (with-redefs [action-buttons/action-buttons (fn [p]
                                                  (reset! captured p)
                                                  ($ :div {}))]
      (render-markup props))
    @captured))

(deftest enhanced-action-buttons-renders-edit-button-when-enabled
  (testing "renders edit button when show-edit? is true"
    (setup/reset-db!)
    (prime-constraint! :users {:id 123 :can-delete? true})
    (let [markup (render-markup {:entity-name :users
                                 :item {:id 123 :email "test@example.com" :name "Test User"}
                                 :show-edit? true
                                 :show-delete? false})]
      (is (str/includes? markup "btn-edit-users-123"))))

  (testing "does not render edit button when show-edit? is false"
    (setup/reset-db!)
    (prime-constraint! :users {:id 123 :can-delete? true})
    (let [markup (render-markup {:entity-name :users
                                 :item {:id 123 :email "test@example.com" :name "Test User"}
                                 :show-edit? false
                                 :show-delete? false})]
      (is (not (str/includes? markup "btn-edit-users-123"))))))

(deftest enhanced-action-buttons-renders-delete-button-when-enabled
  (testing "renders delete button when show-delete? is true"
    (setup/reset-db!)
    (prime-constraint! :users {:id 123 :can-delete? true})

    (let [markup (render-markup {:entity-name :users
                                 :item {:id 123 :email "test@example.com" :name "Test User"}
                                 :show-edit? false
                                 :show-delete? true})]
      (is (str/includes? markup "btn-delete-users-123"))
      (is (str/includes? markup "ds-btn-circle"))))

  (testing "does not render delete button when show-delete? is false"
    (setup/reset-db!)
    (prime-constraint! :users {:id 123 :can-delete? true})

    (let [markup (render-markup {:entity-name :users
                                 :item {:id 123 :email "test@example.com" :name "Test User"}
                                 :show-edit? false
                                 :show-delete? false})]
      (is (not (str/includes? markup "btn-delete-users-123"))))))

(deftest enhanced-action-buttons-handles-admin-protection
  (testing "disables delete button for active admin user"
    (setup/reset-db!)
    (prime-constraint! :users {:id 123 :can-delete? true})

    (let [markup (render-markup {:entity-name :users
                                 :item {:id 123 :email "admin@example.com" :name "Admin User" :role "admin" :status "active"}
                                 :show-edit? false
                                 :show-delete? true})]
      (is (str/includes? markup "btn-delete-users-123"))
      (is (str/includes? markup "opacity-50"))))

  (testing "disables delete button for active owner user"
    (setup/reset-db!)
    (prime-constraint! :users {:id 123 :can-delete? true})

    (let [markup (render-markup {:entity-name :users
                                 :item {:id 123 :email "owner@example.com" :name "Owner User" :role "owner" :status "active"}
                                 :show-edit? false
                                 :show-delete? true})]
      (is (str/includes? markup "opacity-50"))))

  (testing "allows delete for inactive admin user"
    (setup/reset-db!)
    (prime-constraint! :users {:id 123 :can-delete? true})

    (let [markup (render-markup {:entity-name :users
                                 :item {:id 123 :email "admin@example.com" :name "Admin User" :role "admin" :status "inactive"}
                                 :show-edit? false
                                 :show-delete? true})]
      (is (str/includes? markup "btn-delete-users-123")))))

(deftest enhanced-action-buttons-handles-deletion-constraints
  (testing "disables delete button when cannot delete due to constraints"
    (setup/reset-db!)
    (prime-constraint! :users {:id 123
                               :can-delete? false
                               :constraints [{:message "User has active sessions"}]})

    (let [markup (render-markup {:entity-name :users
                                 :item {:id 123 :email "user@example.com" :name "Test User"}
                                 :show-edit? false
                                 :show-delete? true})]
      (is (str/includes? markup "opacity-50"))
      (is (str/includes? markup "User has active sessions"))))

  (testing "disables delete button when loading constraints"
    (setup/reset-db!)
    (prime-constraint! :users {:id 123 :can-delete? true :loading? true})

    (let [markup (render-markup {:entity-name :users
                                 :item {:id 123 :email "user@example.com" :name "Test User"}
                                 :show-edit? false
                                 :show-delete? true})]
      (is (str/includes? markup "opacity-50"))))

  (testing "shows custom tooltip when provided"
    (setup/reset-db!)
    (prime-constraint! :users {:id 123
                               :can-delete? false
                               :constraints [{:message "Custom deletion constraint"}]})

    (let [markup (render-markup {:entity-name :users
                                 :item {:id 123 :email "user@example.com" :name "Test User"}
                                 :show-edit? false
                                 :show-delete? true})]
      (is (str/includes? markup "Custom deletion constraint")))))

(deftest enhanced-action-buttons-handles-custom-actions
  (testing "renders custom actions when provided"
    (setup/reset-db!)
    (prime-constraint! :users {:id 123 :can-delete? true})

    (let [custom-actions (fn [item]
                           ($ :button {:class "custom-action-btn"}
                             (str "Custom-" (:id item))))
          markup (render-markup {:entity-name :users
                                 :item {:id 123 :email "user@example.com" :name "Test User"}
                                 :show-edit? false
                                 :show-delete? false
                                 :custom-actions custom-actions})]
      (is (str/includes? markup "custom-action-btn"))
      (is (str/includes? markup "Custom-123"))))

  (testing "renders both default and custom actions"
    (setup/reset-db!)
    (prime-constraint! :users {:id 123 :can-delete? true})

    (let [custom-actions (fn [item]
                           ($ :button {:class "custom-action-btn"}
                             (str "View-" (:id item))))
          markup (render-markup {:entity-name :users
                                 :item {:id 123 :email "user@example.com" :name "Test User"}
                                 :show-edit? true
                                 :show-delete? true
                                 :custom-actions custom-actions})]
      (is (str/includes? markup "btn-edit-users-123"))
      (is (str/includes? markup "btn-delete-users-123"))
      (is (str/includes? markup "custom-action-btn")))))

(deftest enhanced-action-buttons-handles-namespaced-ids
  (testing "handles namespaced ID keys"
    (setup/reset-db!)
    (prime-constraint! :users {:id 123 :can-delete? true})

    (let [markup (render-markup {:entity-name :users
                                 :item {:users/id 123 :users/email "user@example.com" :users/name "Test User"}
                                 :show-edit? true
                                 :show-delete? true})]
      (is (str/includes? markup "btn-edit-users-123"))
      (is (str/includes? markup "btn-delete-users-123"))))

  (testing "handles unnamespaced ID keys"
    (setup/reset-db!)
    (prime-constraint! :users {:id 123 :can-delete? true})

    (let [markup (render-markup {:entity-name :users
                                 :item {:id 123 :email "user@example.com" :name "Test User"}
                                 :show-edit? true
                                 :show-delete? true})]
      (is (str/includes? markup "btn-edit-users-123"))
      (is (str/includes? markup "btn-delete-users-123")))))

(deftest enhanced-action-buttons-handles-different-entity-types
  (testing "works with different entity types"
    (setup/reset-db!)
    (prime-constraint! :users {:id 123 :can-delete? true})
    (prime-constraint! :tenants {:id 123 :can-delete? true})

    (doseq [entity [:users :tenants :audit-logs]]
      (let [markup (render-markup {:entity-name entity
                                   :item {:id 123 :name "Test Item"}
                                   :show-edit? true
                                   :show-delete? true})]
        (is (str/includes? markup "btn-edit-"))
        (is (str/includes? markup "btn-delete-"))))))

(deftest enhanced-action-buttons-handles-event-dispatch
  (testing "renders edit/delete buttons with correct IDs"
    (setup/reset-db!)
    (prime-constraint! :users {:id 123 :can-delete? true})
    (let [markup (render-markup {:entity-name :users
                                 :item {:id 123 :email "user@example.com" :name "Test User"}
                                 :show-edit? true
                                 :show-delete? true})]
      (is (str/includes? markup "btn-edit-users-123"))
      (is (str/includes? markup "btn-delete-users-123")))))

;; Interaction tests using confirm-dialog are omitted in Karma due to SSR fallback
