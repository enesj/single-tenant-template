(ns app.domain.frontend.expenses.components.receipt-detail-modal-dom-test
  (:require
    [cljs.test :refer-macros [async deftest is]]
    [uix.core :refer [$]]
    [uix.re-frame :as uix-rf]
    ["react-dom/client" :as rdom]
    ["react-dom/test-utils" :as test-utils]
    [app.domain.frontend.expenses.components.receipt-detail-modal :as rdm]))

(defn- mount! [component f]
  (let [container (.createElement js/document "div")
        root (rdom/createRoot container)]
    (.appendChild (.-body js/document) container)
    (test-utils/act (fn [] (.render root component)))
    (f container)
    ;; Allow some time for async UI updates before unmounting
    (js/setTimeout
      (fn []
        (test-utils/act (fn [] (.unmount root)))
        (.removeChild (.-body js/document) container))
      300)))

(deftest receipt-detail-toggle-preview-panel
  (async done
    (with-redefs
      [uix-rf/use-subscribe (fn [query]
                              (cond
                                (= query [:test/open?]) true
                                (= query [:test/modal-id]) "rid1"
                                (= query [:test/loading]) false
                                (= query [:test/action-loading]) false
                                (= query [:test/error]) nil
                                (= query [:expenses/can? :expenses/receipts.approve]) false
                                 ;; Receipt map with minimal fields; id not critical since modal-id drives DOM ids
                                (= (first query) :test/receipt) {:id "rid1"
                                                                 :status "extracted"}
                                :else nil))]
      (let [ctx {:modal-open-sub :test/open?
                 :modal-id-sub :test/modal-id
                 :receipt-sub :test/receipt
                 :receipt-detail-loading-sub :test/loading
                 :receipt-action-loading-sub :test/action-loading
                 :receipts-error-sub :test/error
                 :fetch-receipt-event :test/fetch}]
        (mount!
          ($ rdm/receipt-detail-modal {:id "receipt-detail" :ctx ctx})
          (fn [container]
            (let [hide-btn (.querySelector container "#btn-hide-left-column-rid1")]
              (is (some? hide-btn) "initial hide button should be present")
              (test-utils/act (fn [] (.click hide-btn)))
              ;; Give React a moment to commit the update, then assert
              (js/setTimeout
                (fn []
                  (let [show-btn (.querySelector container "#btn-show-left-column-rid1")]
                    (is (some? show-btn) "show button should appear after hiding left column")
                    (done)))
                10))))))))
