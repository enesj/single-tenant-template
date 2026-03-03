(ns app.template.frontend.pages.tenant-members-test
  (:require
    [app.template.frontend.pages.tenant-members :as tenant-members-page]
    [app.template.frontend.utils.test-utils :as test-utils]
    [cljs.test :refer [deftest is testing]]))

(test-utils/setup-test-environment!)

(deftest member-management-capabilities-respect-owner-guardrails
  (testing "owners and admins can manage non-owner members, while owner records stay protected"
    (let [owner-target {:id "m-1" :role "owner"}
          admin-target {:id "m-2" :role "admin"}
          owner-capabilities (tenant-members-page/member-management-capabilities "owner" true owner-target)
          admin-capabilities (tenant-members-page/member-management-capabilities "owner" true admin-target)
          manager-capabilities (tenant-members-page/member-management-capabilities :admin false {:id "m-3" :role "member"})]
      (is (= {:role "owner"
              :can-change-role? false
              :can-remove? false
              :can-transfer? false}
            owner-capabilities))
      (is (= {:role "admin"
              :can-change-role? true
              :can-remove? true
              :can-transfer? true}
            admin-capabilities))
      (is (= {:role "member"
              :can-change-role? true
              :can-remove? true
              :can-transfer? false}
            manager-capabilities)))))

(deftest tenant-member-row-builds-display-friendly-fields
  (testing "tenant member rows derive canonical display keys and fall back to email when name is missing"
    (let [member {:id "m-42"
                  :user_email "member@example.com"
                  :role "admin"
                  :status "suspended"
                  :user_status "inactive"
                  :created_at "2026-03-03T10:20:30Z"}
          row (tenant-members-page/tenant-member-row member)]
      (is (= "member@example.com" (:member_name row)))
      (is (= "member@example.com" (:member_email row)))
      (is (= "admin" (:member_role row)))
      (is (= "suspended" (:membership_status row)))
      (is (= "inactive" (:account_status row)))
      (is (= "2026-03-03" (:joined_on row))))))

(deftest member-row-action-state-respects-guardrails-and-table-visibility
  (testing "edit/delete/enable visibility follows both management permissions, account state, and table settings"
    (let [manageable-member {:id "m-2"
                             :role "admin"
                             :show-edit? true
                             :show-delete? true}
          hidden-by-settings {:id "m-3"
                              :role "member"
                              :show-edit? false
                              :show-delete? false}
          owner-target {:id "m-4"
                        :role "owner"
                        :show-edit? true
                        :show-delete? true}
          suspended-member {:id "m-5"
                            :role "member"
                            :status "suspended"
                            :show-edit? true
                            :show-delete? true}
          inactive-account-member {:id "m-6"
                                   :role "admin"
                                   :status "active"
                                   :user_status "suspended"
                                   :show-edit? true
                                   :show-delete? true}
          manageable-state (tenant-members-page/member-row-action-state "owner" true manageable-member)
          hidden-state (tenant-members-page/member-row-action-state "owner" true hidden-by-settings)
          protected-owner-state (tenant-members-page/member-row-action-state "admin" false owner-target)
          suspended-state (tenant-members-page/member-row-action-state "owner" true suspended-member)
          inactive-account-state (tenant-members-page/member-row-action-state "owner" true inactive-account-member)]
      (is (= true (:show-edit? manageable-state)))
      (is (= true (:show-delete? manageable-state)))
      (is (= false (:edit-disabled? manageable-state)))
      (is (= false (:delete-disabled? manageable-state)))
      (is (= true (:can-transfer? manageable-state)))

      (is (= false (:show-edit? hidden-state)))
      (is (= false (:show-delete? hidden-state)))

      (is (= true (:show-edit? protected-owner-state)))
      (is (= true (:show-delete? protected-owner-state)))
      (is (= true (:edit-disabled? protected-owner-state)))
      (is (= true (:delete-disabled? protected-owner-state)))
      (is (= false (:can-transfer? protected-owner-state)))

      (is (= false (:show-edit? suspended-state)))
      (is (= false (:show-delete? suspended-state)))
      (is (= true (:show-enable? suspended-state)))

      (is (= false (:can-transfer? inactive-account-state))))))

(deftest tenant-member-list-props-use-canonical-list-view-contract
  (testing "tenant members page passes canonical list-view props with modal edit/delete controls and member rows as overrides"
    (let [props (tenant-members-page/tenant-member-list-props
                  [{:id "m-1"
                    :user_full_name "Ada Lovelace"
                    :user_email "ada@example.com"
                    :role "admin"
                    :created_at "2026-03-03T10:20:30Z"}]
                  "owner"
                  true)
          display-settings (:display-settings props)
          row (first (:rows-override props))]
      (is (= :tenant-members (:entity-name props)))
      (is (= "Current Members" (:title props)))
      (is (= 25 (:per-page props)))
      (is (= :modal (:form-display props)))
      (is (= false (:allow-add? props)))
      (is (= true (:allow-edit? props)))
      (is (= true (:allow-delete? props)))
      (is (= {:show-add-button? false
              :show-filtering? false
              :show-select? false
              :show-batch-edit? false
              :show-batch-delete? false}
            display-settings))
      (is (= "Ada Lovelace" (:member_name row)))
      (is (= "ada@example.com" (:member_email row)))
      (is (= "admin" (:member_role row)))
      (is (= "2026-03-03" (:joined_on row)))
      (is (fn? (:render-edit-form props)))
      (is (fn? (:render-actions props))))))