(ns app.admin.frontend.components.sidebar-dom-test
  (:require
    ["react-dom/client" :as rdom]
    ["react-dom/test-utils" :as test-utils]
    [app.admin.frontend.components.layout :as layout]
    [cljs.test :refer-macros [deftest is testing]]
    [uix.core :refer [$]]
    [uix.re-frame :as uix-rf]))

(defn- mount-component! [component assertions]
  (let [container (.createElement js/document "div")
        root (rdom/createRoot container)]
    (.appendChild (.-body js/document) container)
    (try
      (test-utils/act (fn [] (.render root component)))
      (assertions container)
      (finally
        (.unmount root)
        (.removeChild (.-body js/document) container)))))

(deftest sidebar-active-route-test
  (testing "Sidebar highlights the active route correctly"
    (with-redefs [uix-rf/use-subscribe (fn [query]
                                         (cond
                                           (= query [:current-route]) {:data {:name :admin-users}}
                                           (= query [:admin/current-user-role]) :admin
                                           :else nil))]
      (mount-component!
        ($ layout/admin-sidebar)
        (fn [container]
          (let [active-link (.querySelector container "a.ds-active")]
            (is (some? active-link) "There should be an active link")
            (is (= "User Accounts" (.-textContent active-link))
              "The 'Users' link should be active when route is :admin-users"))))))

  (testing "Sidebar highlights dashboard when route matches"
    (with-redefs [uix-rf/use-subscribe (fn [query]
                                         (cond
                                           (= query [:current-route]) {:name :admin-dashboard}
                                           (= query [:admin/current-user-role]) :admin
                                           :else nil))]
      (mount-component!
        ($ layout/admin-sidebar)
        (fn [container]
          (let [active-link (.querySelector container "a.ds-active")]
            (is (some? active-link) "There should be an active link")
            (is (= "Dashboard" (.-textContent active-link)))))))))

(deftest sidebar-hides-links-disabled-in-navigation-config
  (testing "navigation config can hide an admin sidebar link"
    (with-redefs [uix-rf/use-subscribe (fn [query]
                                         (cond
                                           (= query [:current-route]) {:data {:name :admin-dashboard}}
                                           (= query [:admin/current-user-role]) :owner
                                           (= query [:admin/unread-api-failure-count]) 0
                                             (= query [:admin/navigation]) {:sections [{:id :system-administration
                                                          :items [{:id :dashboard}
                                                            {:id :backlog :visible? false}
                                                            {:id :tenants}
                                                            {:id :users}
                                                            {:id :admins}
                                                            {:id :audit-logs}
                                                            {:id :login-events}]}
                                                         {:id :expenses
                                                          :items [{:id :reports}
                                                            {:id :expenses-settings}
                                                            {:id :search}
                                                            {:id :expenses}
                                                            {:id :receipts}
                                                            {:id :articles}
                                                            {:id :manufacturers}
                                                            {:id :categories}
                                                            {:id :subcategories}
                                                            {:id :suppliers}
                                                            {:id :stores}
                                                            {:id :countries}
                                                            {:id :cities}
                                                            {:id :unmapped-aliases}
                                                            {:id :article-aliases}
                                                            {:id :supplier-aliases}
                                                            {:id :store-aliases}
                                                            {:id :duplicates}]}]}
                                           :else nil))]
      (mount-component!
        ($ layout/admin-sidebar)
        (fn [container]
          (is (nil? (.querySelector container "#admin-sidebar-backlog"))
            "Backlog link should not be rendered when hidden in navigation config")
          (is (some? (.querySelector container "#admin-sidebar-dashboard"))
            "Other links should still render"))))))
