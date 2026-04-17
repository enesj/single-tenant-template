(ns app.domain.frontend.expenses.pages.user.payers-test
  (:require
    [app.domain.frontend.expenses.components.user-reference-forms :as forms]
    [app.domain.frontend.expenses.pages.user.payers :as payers]
    [cljs.test :refer-macros [deftest is testing]]))

(deftest payer-row-class-highlights-system-rows-only
  (let [payer-row-class @#'payers/payer-row-class]
    (testing "system payer rows get the custom border class"
      (is (re-find #"border-l-4" (payer-row-class {:payer-type "system"})))
      (is (re-find #"border-y-primary/20" (payer-row-class {:type "system"}))))

    (testing "non-system payer rows do not receive a custom class"
      (is (nil? (payer-row-class {:payer-type "custom"})))
      (is (nil? (payer-row-class {:label "Regular payer"}))))))

(deftest payer-edit-mode-preserves-system-payer-edits-for-managers
  (let [payer-edit-mode @#'payers/payer-edit-mode]
    (testing "managers keep edit access to system payers, but in the reduced mode"
      (is (= :label-and-default
            (payer-edit-mode {:can-manage? true
                              :user-payer-id "payer-123"}
              {:id "payer-123"
               :type "system"}))))

    (testing "regular payer edits stay full for managers"
      (is (= :full
            (payer-edit-mode {:can-manage? true
                              :user-payer-id "payer-123"}
              {:id "payer-456"}))))

    (testing "members can still only edit their own payer label"
      (is (= :label-only
            (payer-edit-mode {:can-manage? false
                              :user-payer-id "payer-123"}
              {:id "payer-123"})))
      (is (nil? (payer-edit-mode {:can-manage? false
                                  :user-payer-id "payer-123"}
                  {:id "payer-456"}))))))

(deftest payer-action-visibility-keeps-system-payers-protected-and-linked-custom-payers-visible
  (let [show-edit? @#'payers/show-edit-payer-action?
        show-delete? @#'payers/show-delete-payer-action?
        delete-disabled-reason @#'payers/payer-delete-disabled-reason
        manager-view {:can-manage? true
                      :user-payer-id "payer-123"}
        system-payer {:id "payer-123"
                      :type "system"}
        linked-custom-payer {:id "payer-456"
                             :type "custom"
                             :related-expense-count 2}
        regular-payer {:id "payer-789"}
        t (fn [k]
            (case k
              :payers/delete-disabled-linked "linked"
              :payers/delete-disabled-generic "generic"
              (name k)))]
    (testing "system payers still render the edit action for managers"
      (is (true? (show-edit? manager-view system-payer)))
      (is (false? (show-delete? manager-view system-payer))))

    (testing "linked custom payers keep the delete action visible but disabled"
      (is (true? (show-delete? manager-view linked-custom-payer)))
      (is (= "linked" (delete-disabled-reason t linked-custom-payer))))

    (testing "item-level visibility flags are still respected"
      (is (false? (show-edit? manager-view (assoc system-payer :show-edit? false))))
      (is (false? (show-delete? manager-view (assoc regular-payer :show-delete? false)))))))

(deftest payer-edit-form-helpers-limit-type-editing-and-allow-custom-deactivation
  (let [payer-edit-form-spec @#'forms/payer-edit-form-spec
        payer-edit-initial-values @#'forms/payer-edit-initial-values
        payer-edit-submit-data @#'forms/payer-edit-submit-data
        t (fn [k]
            (case k
              :common/label "Naziv"
              :common/is-default "Zadano"
              :common/status "Status"
              :common/active "Aktivan"
              :common/payer-type "Vrsta kupca"
              :payers/form-placeholder "Unesite naziv kupca"
              :payers/type-system "Sistemski"
              :payers/type-custom "Prilagođeni"
              (name k)))]
    (testing "system payer mode only exposes label and default"
      (is (= [:label :is-default]
            (mapv :id (payer-edit-form-spec t :label-and-default))))
      (is (= {:label "Company card"
              :is-default true}
            (payer-edit-initial-values :label-and-default
              {:label "Company card"
               :is_default true})))
      (is (= {:label "Company card"
              :is_default false}
            (payer-edit-submit-data :label-and-default
              {:label "Company card"
               :is-default false
               :type "custom"}))))

    (testing "custom payer full mode allows active toggle but never payer type edits"
      (is (= [:label :is-default :is-active]
            (mapv :id (payer-edit-form-spec t :full))))
      (is (= {:label "Shared wallet"
              :is-default false
              :is-active false}
            (payer-edit-initial-values :full
              {:label "Shared wallet"
               :is_default false
               :is_active false
               :type "custom"})))
      (is (= {:label "Shared wallet"
              :is_default true
              :is_active false}
            (payer-edit-submit-data :full
              {:label "Shared wallet"
               :is-default true
               :is-active false
               :type "system"}))))

    (testing "label-only mode still submits just the label"
      (is (= {:label "My payer"}
            (payer-edit-submit-data :label-only
              {:label "My payer"
               :is-default true
               :is-active false}))))))

(deftest payers-entity-spec-localization-translates-payer-table-fields
  (let [localize-payer-entity-spec @#'payers/localize-payer-entity-spec
        t (fn [k]
            (case k
              :common/email "E-mail"
              :common/user "Korisnik"
              :common/yes "Da"
              :common/no "Ne"
              :payers/type-system "Sistemski"
              :payers/type-custom "Prilagođeni"
              (name k)))
        localized (localize-payer-entity-spec t [{:id :label :label "Naziv"}
                                                 {:id :payer-type-label :label "Vrsta kupca"}
                                                 {:id :is-default :label "Zadano"}
                                                 {:id :user_email :label "Email"}])]
    (is (= [{:value "system" :label "Sistemski"}
            {:value "custom" :label "Prilagođeni"}]
          (:options (second localized))))
    (is (= "select" (:type (second localized))))
    (is (= [{:value true :label "Da"}
            {:value false :label "Ne"}]
          (:options (nth localized 2))))
    (is (= "E-mail" (:label (nth localized 3))))))
