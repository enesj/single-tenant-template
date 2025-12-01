(ns app.template.frontend.pages.login-dom-test
  (:require
    ["react-dom/client" :as rdom]
    [app.template.frontend.pages.login :as login]
    [cljs.test :refer-macros [async deftest is testing]]
    [uix.core :refer [$]]
    [uix.re-frame :as uix-rf]))

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
      250)))

(deftest login-authenticated-view-test
  (async done
    (with-redefs [uix-rf/use-subscribe (fn [sub]
                                         (case (first sub)
                                           :auth-status {:authenticated true :user {:full-name "User"}}
                                           :current-tenant {:name "Acme" :subscription-tier :pro}
                                           nil))]
      (mount-component! ($ login/login-page)
        (fn [container]
          (is (re-find #"Welcome" (.-textContent container)))
          (done))))))

(deftest login-unauthenticated-view-test
  (async done
    (with-redefs [uix-rf/use-subscribe (fn [sub]
                                         (case (first sub)
                                           :auth-status {:authenticated false :loading? false}
                                           :current-tenant nil
                                           nil))]
      (mount-component! ($ login/login-page)
        (fn [container]
          (is (some? (.querySelector container "#login-google-btn")))
          (let [inputs (.querySelectorAll container "input")]
            (is (<= 1 (.-length inputs)))
            (is (= true (.-disabled (.item inputs 0)))))
          (done))))))
