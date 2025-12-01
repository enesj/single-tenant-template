(ns app.admin.frontend.components.tenant-actions-interaction-dom-test
  (:require
    ["react-dom/client" :as rdom]
    [app.admin.frontend.components.tenant-actions :as tenant-actions]
    [app.admin.frontend.test-setup :as setup]
    [app.template.frontend.components.confirm-dialog :as confirm-dialog]
    [app.template.frontend.components.dropdown :as dropdown]
    [cljs.test :refer-macros [deftest is testing async]]
    [re-frame.db :as rf-db]
    [uix.core :refer [$]]))

 ;; Render a simplified dropdown so we can click real DOM buttons deterministically
(defn- simple-dropdown [{:keys [actions]}]
  (let [buttons (->> actions
                  (mapcat :items))]
    ($ :div {}
      (for [{:keys [id label on-click disabled?]} buttons]
        ($ :button {:key id
                    :id id
                    :disabled (boolean disabled?)
                    :on-click (fn [e] (when on-click (on-click e)))}
          (or label id))))))

(deftest tenant-delete-disabled-from-constraints-dom
  (testing "delete action disabled when constraints forbid deletion (DOM)"
    (setup/reset-db!)
    (reset! rf-db/app-db {:admin/authenticated? true
                          :admin/current-user {:id 1 :role :admin}
                          :deletion-constraints {:tenants {:results {999 {:can-delete? false
                                                                          :constraints [{:message "Deps"}]}}
                                                           :loading {}
                                                           :errors {}}}})
    (async done
      (let [container (.createElement js/document "div")
            root (rdom/createRoot container)]
        (with-redefs [dropdown/action-dropdown simple-dropdown]
          (.appendChild (.-body js/document) container)
          (.render root ($ tenant-actions/admin-tenant-actions {:tenant {:id 999 :name "X"}}))
          (js/setTimeout
            (fn []
              (let [btn (.getElementById js/document "delete-tenant")]
                (when btn
                  (is (= true (.-disabled btn))))
                (.unmount root)
                (.removeChild (.-body js/document) container)
                (done)))
            0))))))

(deftest tenant-delete-confirms-dom
  (testing "clicking delete opens confirm dialog (DOM)"
    (setup/reset-db!)
    (reset! rf-db/app-db {:admin/authenticated? true
                          :admin/current-user {:id 1 :role :admin}
                          :deletion-constraints {:tenants {:results {5 {:can-delete? true}}}}})
    (async done
      (let [container (.createElement js/document "div")
            root (rdom/createRoot container)
            captured (atom nil)
            btn-found (atom false)]
        (with-redefs [dropdown/action-dropdown simple-dropdown
                      confirm-dialog/show-confirm (fn [cfg] (reset! captured cfg))]
          (.appendChild (.-body js/document) container)
          (.render root ($ tenant-actions/admin-tenant-actions {:tenant {:id 5 :name "T"}}))
          (js/setTimeout
            (fn []
              (when-let [btn (.getElementById js/document "delete-tenant")]
                (reset! btn-found true)
                (.click btn))
              (js/setTimeout
                (fn []
                  (when @btn-found
                    (is (some? @captured))
                    (is (= "Confirm Tenant Deletion" (:title @captured)))
                    (is (fn? (:on-confirm @captured))))
                  (.unmount root)
                  (.removeChild (.-body js/document) container)
                  (done))
                0))
            0))))))
