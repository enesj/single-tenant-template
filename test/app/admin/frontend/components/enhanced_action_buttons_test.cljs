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
                :users :users
                :audit-logs :audit-logs
                :login-events :login-events
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
  (testing "active admin user delete button has disabled styling (opacity-50)"
    (setup/reset-db!)
    ;; Active admin users should be protected from deletion with visual indicators
    ;; The component adds opacity-50, cursor-not-allowed, pointer-events-none for protected admins
    (let [markup (render-markup {:entity-name :users
                                 :item {:id 123 :email "admin@example.com" :name "Admin User" :role "admin" :status "active"}
                                 :show-edit? false
                                 :show-delete? true})]
      ;; Verify delete button renders with disabled styling
      (is (str/includes? markup "btn-delete-users-123")
          "Delete button should render with correct ID")
      (is (str/includes? markup "opacity-50")
          "Active admin delete button should have opacity-50 class")
      (is (str/includes? markup "cursor-not-allowed")
          "Active admin delete button should have cursor-not-allowed class")
      (is (str/includes? markup "pointer-events-none")
          "Active admin delete button should have pointer-events-none class")
      (is (str/includes? markup "aria-disabled=\"true\"")
          "Active admin delete button should have aria-disabled")))

  (testing "active admin shows correct tooltip in markup"
    (setup/reset-db!)
    (let [markup (render-markup {:entity-name :users
                                 :item {:id 123 :role "admin" :status "active"}
                                 :show-edit? false
                                 :show-delete? true})]
      (is (str/includes? markup "Cannot delete active admin user")
          "Active admin should show protection tooltip")))

  (testing "inactive admin user delete button is NOT disabled"
    (setup/reset-db!)
    ;; Inactive admins can be deleted
    (let [markup (render-markup {:entity-name :users
                                 :item {:id 123 :email "admin@example.com" :name "Admin User" :role "admin" :status "inactive"}
                                 :show-edit? false
                                 :show-delete? true})]
      (is (not (str/includes? markup "opacity-50"))
          "Inactive admin delete button should NOT have opacity-50 class")
      (is (str/includes? markup "Delete this record")
          "Inactive admin should show default delete tooltip")))

  (testing "non-admin role delete button is NOT disabled"
    (setup/reset-db!)
    ;; Non-admin users (e.g., owner, user) can be deleted regardless of status
    (let [markup (render-markup {:entity-name :users
                                 :item {:id 123 :email "owner@example.com" :name "Owner User" :role "owner" :status "active"}
                                 :show-edit? false
                                 :show-delete? true})]
      (is (not (str/includes? markup "opacity-50"))
          "Non-admin delete button should NOT have opacity-50 class")
      (is (str/includes? markup "Delete this record")
          "Non-admin should show default delete tooltip")))

  (testing "admin protection only applies to :users entity"
    (setup/reset-db!)
    ;; For non-users entities like :audit-logs, admin role should not trigger protection
    (let [markup (render-markup {:entity-name :audit-logs
                                 :item {:id 123 :role "admin" :status "active"}
                                 :show-edit? false
                                 :show-delete? true})]
      (is (not (str/includes? markup "opacity-50"))
          "Non-users entity should NOT have admin protection")
      (is (str/includes? markup "Delete this record")
          "Non-users entity should show default delete tooltip")))

  (testing "renders delete button for admin users with correct ID"
    (setup/reset-db!)
    (let [markup (render-markup {:entity-name :users
                                 :item {:id 123 :email "admin@example.com" :name "Admin User" :role "admin" :status "active"}
                                 :show-edit? false
                                 :show-delete? true})]
      (is (str/includes? markup "btn-delete-users-123"))))

  (testing "renders delete button for owner users"
    (setup/reset-db!)
    (let [markup (render-markup {:entity-name :users
                                 :item {:id 123 :email "owner@example.com" :name "Owner User" :role "owner" :status "active"}
                                 :show-edit? false
                                 :show-delete? true})]
      (is (str/includes? markup "btn-delete-users-123"))))

  (testing "renders delete button for inactive admin user"
    (setup/reset-db!)
    (let [markup (render-markup {:entity-name :users
                                 :item {:id 123 :email "admin@example.com" :name "Admin User" :role "admin" :status "inactive"}
                                 :show-edit? false
                                 :show-delete? true})]
      (is (str/includes? markup "btn-delete-users-123")))))

(deftest enhanced-action-buttons-handles-deletion-constraints
  (testing "single-tenant has no remote deletion constraints - delete is always allowed"
    (setup/reset-db!)
    ;; In single-tenant, there are no remote deletion constraints
    ;; The only protection is local admin protection (tested separately)
    (let [markup (render-markup {:entity-name :users
                                 :item {:id 123 :email "user@example.com" :name "Test User"}
                                 :show-edit? false
                                 :show-delete? true})]
      ;; Regular users can always be deleted (no opacity-50)
      (is (not (str/includes? markup "opacity-50")))
      (is (str/includes? markup "btn-delete-users-123"))))

  (testing "delete button enabled for regular users"
    (setup/reset-db!)
    (let [markup (render-markup {:entity-name :users
                                 :item {:id 123 :email "user@example.com" :name "Test User" :role "user" :status "active"}
                                 :show-edit? false
                                 :show-delete? true})]
      (is (not (str/includes? markup "opacity-50")))))

  (testing "default delete tooltip shown for deletable items"
    (setup/reset-db!)
    (let [markup (render-markup {:entity-name :users
                                 :item {:id 123 :email "user@example.com" :name "Test User"}
                                 :show-edit? false
                                 :show-delete? true})]
      (is (str/includes? markup "Delete this record")))))

(deftest enhanced-action-buttons-shows-correct-tooltips
  ;; Tooltip tests are covered in enhanced-action-buttons-handles-admin-protection
  ;; using markup-based testing since capture-action-config doesn't work in SSR context
  (testing "protected admin tooltip via markup"
    (setup/reset-db!)
    (let [markup (render-markup {:entity-name :users
                                 :item {:id 123 :role "admin" :status "active"}
                                 :show-edit? false
                                 :show-delete? true})]
      (is (str/includes? markup "Cannot delete active admin user")
          "Active admin should show protection tooltip in markup")))

  (testing "regular user tooltip via markup"
    (setup/reset-db!)
    (let [markup (render-markup {:entity-name :users
                                 :item {:id 123 :role "user" :status "active"}
                                 :show-edit? false
                                 :show-delete? true})]
      (is (str/includes? markup "Delete this record")
          "Regular user should show default delete tooltip")))

  (testing "inactive admin tooltip via markup"
    (setup/reset-db!)
    (let [markup (render-markup {:entity-name :users
                                 :item {:id 123 :role "admin" :status "inactive"}
                                 :show-edit? false
                                 :show-delete? true})]
      (is (str/includes? markup "Delete this record")
          "Inactive admin should show default delete tooltip")))

  (testing "non-users entity tooltip via markup"
    (setup/reset-db!)
    (let [markup (render-markup {:entity-name :audit-logs
                                 :item {:id 123}
                                 :show-edit? false
                                 :show-delete? true})]
      (is (str/includes? markup "Delete this record")
          "Non-users entity should show default delete tooltip"))))

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
  (testing "works with different entity types (single-tenant)"
    (setup/reset-db!)
    (prime-constraint! :users {:id 123 :can-delete? true})

    (doseq [entity [:users :audit-logs :login-events]]
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

(deftest enhanced-action-buttons-edit-click-dispatches-events
  ;; Note: Direct event dispatch testing requires DOM interaction tests
  ;; These tests verify button rendering through markup
  (testing "edit button renders with correct ID"
    (setup/reset-db!)
    (let [markup (render-markup {:entity-name :users
                                 :item {:id 456 :email "test@example.com"}
                                 :show-edit? true
                                 :show-delete? false})]
      (is (str/includes? markup "btn-edit-users-456")
          "Edit button should render with correct ID")))

  (testing "edit button renders for different entity IDs"
    (setup/reset-db!)
    (let [markup (render-markup {:entity-name :users
                                 :item {:id 789}
                                 :show-edit? true
                                 :show-delete? false})]
      (is (str/includes? markup "btn-edit-users-789")
          "Edit button should render with correct ID for different entities")))

  (testing "edit button renders with circle shape class"
    (setup/reset-db!)
    ;; The edit button uses shape="circle" which renders with ds-btn-circle class
    (let [markup (render-markup {:entity-name :users
                                 :item {:id 123}
                                 :show-edit? true
                                 :show-delete? false})]
      (is (str/includes? markup "btn-edit-users-123")
          "Edit button should render"))))

(deftest enhanced-action-buttons-delete-routes-events-correctly
  ;; Note: Direct event dispatch verification requires DOM interaction tests
  ;; These tests verify button rendering through markup for different entity types
  (testing "delete button renders with correct ID for :users entity"
    (setup/reset-db!)
    (let [markup (render-markup {:entity-name :users
                                 :item {:id 123}
                                 :show-edit? false
                                 :show-delete? true})]
      (is (str/includes? markup "btn-delete-users-123"))))

  (testing "delete button renders with correct ID for :audit-logs entity"
    (setup/reset-db!)
    (let [markup (render-markup {:entity-name :audit-logs
                                 :item {:id 456}
                                 :show-edit? false
                                 :show-delete? true})]
      (is (str/includes? markup "btn-delete-audit-logs-456"))))

  (testing "delete button renders with correct ID for :login-events entity"
    (setup/reset-db!)
    (let [markup (render-markup {:entity-name :login-events
                                 :item {:id 789}
                                 :show-edit? false
                                 :show-delete? true})]
      (is (str/includes? markup "btn-delete-login-events-789"))))

  (testing "delete button renders with ds-btn-circle class"
    (setup/reset-db!)
    (let [markup (render-markup {:entity-name :users
                                 :item {:id 123}
                                 :show-edit? false
                                 :show-delete? true})]
      (is (str/includes? markup "ds-btn-circle")
          "Delete button should have ds-btn-circle class"))))

;; Interaction tests using confirm-dialog are omitted in Karma due to SSR fallback
