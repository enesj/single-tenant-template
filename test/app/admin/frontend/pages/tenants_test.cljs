(ns app.admin.frontend.pages.tenants-test
  (:require
    [app.admin.frontend.pages.tenants :as tenants-page]
    [app.template.frontend.utils.test-utils :as test-utils]
    [cljs.test :refer [deftest is testing]]
    [clojure.string :as str]
    [uix.core :refer [$]]))

(test-utils/setup-test-environment!)

(deftest tenant-list-props-keep-canonical-action-settings-enabled
  (testing "tenant list props preserve canonical edit/delete/select controls and modal editing"
    (let [props (tenants-page/tenant-list-props (fn [_tenant-id] nil))
          display-settings (:display-settings props)]
      (is (= false (:allow-add? props)))
      (is (= true (:allow-edit? props)))
      (is (= true (:allow-delete? props)))
      (is (= :modal (:form-display props)))
      (is (= {:show-add-button? false
              :show-filtering? false}
            display-settings))
      (is (not (contains? display-settings :show-select?)))
      (is (not (contains? display-settings :show-batch-delete?)))
      (is (not (contains? display-settings :show-batch-edit?)))
      (is (fn? (:render-actions props))))))

(deftest tenant-detail-action-groups-provide-see-details-dropdown-action
  (testing "tenant row custom actions expose a See details dropdown action"
    (let [selected-tenant-id (atom nil)
          tenant-id "tenant-123"
          action-groups (tenants-page/tenant-detail-action-groups
                          (fn [id]
                            (reset! selected-tenant-id id))
                          tenant-id)
          action-item (-> action-groups first :items first)]
      (is (= "View" (:group-title (first action-groups))))
      (is (= "see-details" (:id action-item)))
      (is (= "See details" (:label action-item)))
      ((:on-click action-item) #js {})
      (is (= tenant-id @selected-tenant-id)))))

(deftest tenant-detail-modal-props-use-shared-modal-wrapper-settings
  (testing "tenant detail modal props keep the canonical modal configuration"
    (let [close-fn (fn [])
          props (tenants-page/tenant-detail-modal-props true close-fn)]
      (is (= "admin-tenant-details-modal" (:id props)))
      (is (= true (:visible? props)))
      (is (= "Tenant details" (:title props)))
      (is (= :extra-large (:size props)))
      (is (= true (:draggable? props)))
      (is (= "btn-close-admin-tenant-details-modal" (:close-button-id props)))
      (is (= "p-0" (:content-class props)))
      (is (identical? close-fn (:on-close props))))))

(deftest admin-tenants-page-renders-list-and-detail-modal
  (testing "tenants page keeps the list mounted and wires a detail modal alongside it"
    (let [captured-list-props (atom nil)
          captured-modal-props (atom nil)]
      (with-redefs [tenants-page/tenant-list-view
                    (fn [props]
                      (reset! captured-list-props (if (map? props) props (js->clj props :keywordize-keys true)))
                      ($ :div {:id "tenant-list-view-stub"} "tenant-list"))
                    tenants-page/tenant-detail-modal
                    (fn [props]
                      (reset! captured-modal-props (if (map? props) props (js->clj props :keywordize-keys true)))
                      ($ :div {:id "tenant-detail-modal-stub"} "tenant-modal"))]
        (let [markup (test-utils/render-to-static-markup ($ tenants-page/admin-tenants-page))
              list-props @captured-list-props
              modal-props @captured-modal-props]
          (is (str/includes? markup "tenant-list-view-stub"))
          (is (str/includes? markup "tenant-detail-modal-stub"))
          (is (fn? (or (:on-select list-props)
                     (:onSelect list-props))))
          (is (= false (:visible? modal-props)))
          (is (fn? (or (:on-close modal-props)
                     (:onClose modal-props)))))))))
