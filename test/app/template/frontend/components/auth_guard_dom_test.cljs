(ns app.template.frontend.components.auth-guard-dom-test
  (:require
    ["react-dom/client" :as rdom]
    [app.template.frontend.components.auth-guard :as auth-guard]
    [app.template.frontend.components.button :as button]
    [cljs.test :refer-macros [async deftest is testing]]
    [re-frame.core :as rf]
    [uix.core :refer [$]]))

(defn- mount-component! [component assertions]
  (let [container (.createElement js/document "div")
        root (rdom/createRoot container)]
    (.appendChild (.-body js/document) container)
    (.render root component)
    (js/setTimeout
      (fn []
        (try
          (assertions container)
          (finally
            (.unmount root)
            (.removeChild (.-body js/document) container))))
      100)))

(deftest loading-state-test
  (async done
    (with-redefs [button/button (fn [{:keys [on-click]} & children]
                                  ($ :button {:on-click on-click} children))]
      (mount-component! ($ auth-guard/auth-guard {:loading? true})
        (fn [container]
          (is (re-find #"Checking authentication" (.-textContent container)))
          (done))))))

(deftest unauthenticated-test
  (async done
    (let [clicked (atom false)]
      (with-redefs [button/button (fn [{:keys [on-click]} & children]
                                    ($ :button {:id "redirect-btn" :on-click (fn [] (on-click))} children))]
        (mount-component!
          ($ auth-guard/auth-guard {:authenticated? false
                                    :auth-type :admin
                                    :on-redirect #(reset! clicked true)})
          (fn [container]
            (let [btn (.querySelector container "#redirect-btn")]
              (when btn (.click btn)))
            (js/setTimeout (fn [] (done)) 50)))))))

(deftest authenticated-shows-children-test
  (async done
    (mount-component!
      ($ auth-guard/auth-guard {:authenticated? true}
        ($ :span {:id "child"} "CONTENT"))
      (fn [container]
        (is (some? (.querySelector container "#child")))
        (done)))))

(deftest role-based-guard-test
  (async done
    (with-redefs [rf/dispatch (fn [_])
                  button/button (fn [{:keys [on-click]} & children]
                                  ($ :button {:on-click on-click} children))]
      (mount-component!
        ($ auth-guard/role-based-guard
          {:user-roles [:viewer]
           :required-roles [:admin]
           :fallback-component ($ :div {:id "denied"} "Denied")
           :authenticated? false})
        (fn [container]
          (is (re-find #"Denied" (.-textContent container)))
          (done))))))
