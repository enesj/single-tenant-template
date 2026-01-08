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

(deftest create-article-name-prefill-does-not-stick-between-modal-openings
  (testing "Create-new article name is re-prefilled from the newly selected raw label"
    (setup/reset-db!)

    (async done
      (let [container (.createElement js/document "div")
            root (rdom/createRoot container)]
        (.appendChild (.-body js/document) container)

        (seed-unmapped-items! {:items [{:id 1 :supplier-id "supplier-1" :raw-label "Raw Label A"}]
                               :selected-ids #{1}})

        (.render root ($ unmapped-ui/map-to-article-modal))

        (rf/dispatch-sync [::unmapped-events/open-map-modal])
        (js/setTimeout
          (fn []
            (when-let [btn (.getElementById js/document "btn-create-article-from-labels")]
              (.click btn))

            (js/setTimeout
              (fn []
                (let [input (.getElementById js/document "input-new-article-name")]
                  (is (= "Raw Label A" (.-value input))
                    "First opening should prefill canonical name from selected label"))

                (rf/dispatch-sync [::unmapped-events/close-map-modal])

                ;; Switch selection to a different raw label and reopen.
                ;; Use a tick between close/open so React sees open? false -> true (no batching).
                (js/setTimeout
                  (fn []
                    (seed-unmapped-items! {:items [{:id 2 :supplier-id "supplier-1" :raw-label "Raw Label B"}]
                                           :selected-ids #{2}})

                    (rf/dispatch-sync [::unmapped-events/open-map-modal])
                    (js/setTimeout
                      (fn []
                        ;; Give the component a tick to apply its \"reset on open\" effect,
                        ;; then switch to :new mode (which should prefill from the new selection).
                        (js/setTimeout
                          (fn []
                            (when-let [btn (.getElementById js/document "btn-create-article-from-labels")]
                              (.click btn))

                            (js/setTimeout
                              (fn []
                                (let [input (.getElementById js/document "input-new-article-name")]
                                  (is (= "Raw Label B" (.-value input))
                                    "Second opening should prefill from the newly selected label (not the previous one)"))

                                (.unmount root)
                                (.removeChild (.-body js/document) container)
                                (done))
                              0))
                          0))
                      0))
                  0))
              0))
          0)))))
