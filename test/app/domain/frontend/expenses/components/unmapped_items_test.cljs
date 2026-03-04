(ns app.domain.frontend.expenses.components.unmapped-items-test
  (:require
    ["react-dom/client" :as rdom]
    [app.admin.frontend.test-setup :as setup]
    [app.domain.frontend.expenses.components.unmapped-items :as unmapped-ui]
    ;; Ensure subs/events are registered for use-subscribe / dispatch-sync.
    [app.domain.frontend.expenses.events.unmapped-items :as unmapped-events]
    [app.domain.frontend.expenses.subs.unmapped-items]
    [app.template.frontend.utils.test-utils :as test-utils]
    [cljs.test :refer-macros [async deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]
    [uix.core :refer [$]]))

(test-utils/setup-test-environment!)

(defn- seed-unmapped-items!
  [{:keys [items selected-ids]}]
  (swap! rf-db/app-db
    (fn [db]
      (-> db
        (assoc-in [:admin :expenses :unmapped-items :items] (vec (or items [])))
        (assoc-in [:admin :expenses :unmapped-items :selection :item-ids] (or selected-ids #{}))
        (assoc-in [:admin :expenses :unmapped-items :lookups :loading?] false)
        (assoc-in [:admin :expenses :unmapped-items :lookups :error] nil)
        (assoc-in [:admin :expenses :unmapped-items :lookups :suppliers] [])
        (assoc-in [:admin :expenses :unmapped-items :lookups :articles] [])))))

(defn- wait-for!
  "Poll (pred) on successive ticks until it returns truthy or times out.

  Calls (cb result) with the truthy result, or nil on timeout."
  ([pred cb]
   (wait-for! pred cb 25))
  ([pred cb tries]
   (letfn [(tick [n]
             (let [res (try (pred) (catch :default _ nil))]
               (if res
                 (cb res)
                 (if (pos? n)
                   (js/setTimeout #(tick (dec n)) 0)
                   (cb nil)))))]
     (tick tries))))

(deftest create-article-name-prefill-does-not-stick-between-modal-openings
  (testing "Create-new article name is re-prefilled from the newly selected raw label"
    (setup/reset-db!)

    (async done
      (let [container (.createElement js/document "div")
            root (rdom/createRoot container)
            cleanup! (fn []
                       (.unmount root)
                       (.removeChild (.-body js/document) container)
                       (done))]
        (.appendChild (.-body js/document) container)

        (seed-unmapped-items! {:items [{:id 1 :supplier-id "supplier-1" :raw-label "Raw Label A"}]
                               :selected-ids #{1}})

        (.render root ($ unmapped-ui/map-to-article-modal))

        (rf/dispatch-sync [::unmapped-events/open-map-modal])

        (wait-for!
          #(.getElementById js/document "btn-create-article-from-labels")
          (fn [btn]
            (when-not btn
              (is (some? btn) "Create-article button should render")
              (cleanup!))

            (.click btn)

            (wait-for!
              #(let [input (.getElementById js/document "input-new-article-name")]
                 (when (and input (= "Raw Label A" (.-value input))) input))
              (fn [input]
                (when-not input
                  (is (some? input) "New-article input should render (first opening)")
                  (cleanup!))

                (is (= "Raw Label A" (.-value input))
                  "First opening should prefill canonical name from selected label")

                (rf/dispatch-sync [::unmapped-events/close-map-modal])

                ;; Switch selection to a different raw label and reopen.
                ;; Use a tick between close/open so React sees open? false -> true (no batching).
                (js/setTimeout
                  (fn []
                    (seed-unmapped-items! {:items [{:id 2 :supplier-id "supplier-1" :raw-label "Raw Label B"}]
                                           :selected-ids #{2}})

                    (rf/dispatch-sync [::unmapped-events/open-map-modal])

                    (wait-for!
                      #(.getElementById js/document "btn-create-article-from-labels")
                      (fn [btn2]
                        (when-not btn2
                          (is (some? btn2) "Create-article button should render (second opening)")
                          (cleanup!))

                        (.click btn2)

                        (wait-for!
                          #(let [input2 (.getElementById js/document "input-new-article-name")]
                             (when (and input2 (= "Raw Label B" (.-value input2))) input2))
                          (fn [input2]
                            (when-not input2
                              (is (some? input2) "New-article input should render (second opening)")
                              (cleanup!))

                            (is (= "Raw Label B" (.-value input2))
                              "Second opening should prefill from the newly selected label (not the previous one)")

                            (cleanup!)))))))
                0))))))))

(deftest unmapped-items-panel-renders-translated-table-headers
  (testing "Unmapped items panel uses translated header labels"
    (setup/reset-db!)

    (async done
      (let [container (.createElement js/document "div")
            root (rdom/createRoot container)
            cleanup! (fn []
                       (.unmount root)
                       (.removeChild (.-body js/document) container)
                       (done))]
        (.appendChild (.-body js/document) container)
        (swap! rf-db/app-db assoc :locale :bs)
        (seed-unmapped-items! {:items [{:id 1 :supplier-id "supplier-1" :raw-label "Mlijeko" :occurrence-count 2}]})

        (.render root ($ unmapped-ui/unmapped-items-panel {:title "Unmapped"}))

        (wait-for!
          #(let [text (some-> container .-textContent)]
             (when (and text
                     (.includes text "Originalna oznaka")
                     (.includes text "Dobavljač")
                     (.includes text "Ponavljanja"))
               text))
          (fn [text]
            (is (some? text) "Translated Bosnian headers should render in the table")
            (cleanup!)))))))
