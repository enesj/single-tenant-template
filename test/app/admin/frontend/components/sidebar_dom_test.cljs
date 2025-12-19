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
            (is (= "Users" (.-textContent active-link))
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
