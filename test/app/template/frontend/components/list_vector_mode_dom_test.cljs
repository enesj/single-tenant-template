(ns app.template.frontend.components.list-vector-mode-dom-test
  (:require
    ["react-dom/client" :as rdom]
    ["react-dom/test-utils" :as test-utils]
    [app.template.frontend.components.list :as list]
    [app.template.frontend.components.list.table :as list-table]
    [app.template.frontend.components.list.ui :as list-ui]
    [app.template.frontend.components.settings.list-view-settings :as list-view-settings]
    [app.template.frontend.i18n :as i18n]
    [app.template.frontend.events.list.ui-state :as ui-events]
    [app.template.frontend.events.list.settings :as settings-events]
    [app.template.frontend.hooks.display-settings :as display-settings]
    [app.template.frontend.utils.column-config :as column-config]
    [app.template.frontend.utils.shared :as template-utils]
    [clojure.string :as str]
    [cljs.test :refer-macros [async deftest is]]
    [re-frame.core :as rf]
    [uix.core :refer [$]]
    [uix.re-frame :as uix-rf]))

(defn- mount-component! [component assertions]
  (let [container (.createElement js/document "div")
        root (rdom/createRoot container)
        original-raf (when (exists? js/requestAnimationFrame) js/requestAnimationFrame)
        original-caf (when (exists? js/cancelAnimationFrame) js/cancelAnimationFrame)]
    (.appendChild (.-body js/document) container)
    (try
      (when-not (some? original-raf)
        (set! js/requestAnimationFrame (fn [callback]
                                         (js/setTimeout callback 0))))
      (when-not (some? original-caf)
        (set! js/cancelAnimationFrame (fn [handle]
                                        (js/clearTimeout handle))))
      (test-utils/act (fn [] (.render root component)))
      (assertions container)
      (finally
        (.unmount root)
        (if (some? original-raf)
          (set! js/requestAnimationFrame original-raf)
          (js-delete js/globalThis "requestAnimationFrame"))
        (if (some? original-caf)
          (set! js/cancelAnimationFrame original-caf)
          (js-delete js/globalThis "cancelAnimationFrame"))
        (.removeChild (.-body js/document) container)))))

(defn- button-by-text [container text]
  (let [buttons (.querySelectorAll container "button")]
    (some (fn [btn]
            (when (= text (.-textContent btn))
              btn))
      (array-seq buttons))))

(deftest table-headers-show-sort-direction-for-active-sorts
  (with-redefs [rf/dispatch (fn [_] nil)
                uix-rf/use-subscribe (fn [query]
                                       (cond
                                         (= query [:locale]) :bs
                                         (= (first query) :admin/sortable-columns) nil
                                         :else nil))]
    (let [headers (list-table/make-table-headers {:entity-name :receipts
                                                  :entity-spec [{:id :purchased-at-guess
                                                                 :label "Datum kupovine"
                                                                 :type :datetime}]
                                                  :sorts [{:field :purchased-at-guess :direction :asc}]
                                                  :all-items []
                                                  :selected-ids #{}
                                                  :active-filters {}
                                                  :filterable-fields []
                                                  :user-filterable-settings {}
                                                  :visible-columns {}
                                                  :column-order []})
          header-component (nth headers 1)]
      (mount-component!
        ($ :div ($ header-component))
        (fn [container]
          (let [header (.querySelector container "#header-purchased-at-guess")]
            (is (some? header) "Expected purchase-date header to render")
            (is (str/includes? (.-textContent header) "↑")
              "Active multi-sort state should render a sort arrow below the header label")))))))

(deftest active-sort-controls-toggle-direction-from-chip
  (async done
    (let [dispatched (atom nil)]
      (with-redefs [rf/dispatch (fn [evt] (reset! dispatched evt))
                    i18n/use-t (fn []
                                 (fn [k]
                                   (case k
                                     :common/sort "Sortiranje"
                                     :common/clear "Očisti"
                                     nil)))]
        (mount-component!
          ($ list-ui/active-sort-controls
            {:entity-name :receipts
             :sorts [{:field :purchased-at-guess :direction :asc}]
             :field-labels {:purchased-at-guess "Datum kupovine"}})
          (fn [container]
            (let [direction-btn (.querySelector container "#btn-sort-direction-receipts-purchased-at-guess")]
              (is (some? direction-btn) "Expected a clickable direction button on the sort chip")
              (is (= "↑" (.-textContent direction-btn)))
              (.click direction-btn)
              (is (= [::ui-events/set-sort-field :receipts :purchased-at-guess {:append? true}]
                    @dispatched)
                "Clicking the chip direction button should toggle sort direction in place")
              (done))))))))

(deftest active-sort-controls-match-filter-section-styling
  (with-redefs [rf/dispatch (fn [_] nil)
                i18n/use-t (fn []
                             (fn [k]
                               (case k
                                 :common/sort "Sortiranje"
                                 :common/clear-all "Očisti sve"
                                 nil)))]
    (mount-component!
      ($ list-ui/active-sort-controls
        {:entity-name :receipts
         :sorts [{:field :total-amount-guess :direction :asc}]
         :field-labels {:total-amount-guess "Pretpostavljeni iznos"}})
      (fn [container]
        (let [section (.querySelector container "#active-sorts-receipts")
              chip (.querySelector container "#sort-chip-receipts-total-amount-guess")
              clear-btn (.querySelector container "#btn-clear-sorts-receipts")]
          (is (some? section) "Expected sort chip section to render")
          (is (str/includes? (.-className section) "bg-blue-50")
            "Sorting section should share the same blue container styling as active filters")
          (is (str/includes? (.-className section) "border-blue-200")
            "Sorting section should use the same border tone as active filters")
          (is (str/includes? (.-textContent section) "Sortiranje (1):")
            "Sorting section should show the same count-driven heading pattern as active filters")
          (is (some? chip) "Expected the sort chip to render")
          (is (str/includes? (.-className chip) "border-blue-300")
            "Sort chips should reuse the filter chip border styling")
          (is (nil? clear-btn)
            "Single-sort state should rely on the chip remove button instead of a separate clear-all button"))))))

(deftest admin-routes-bypass-action-gates
  (let [gate-allows? @#'list/gate-allows-action?]
    (is (true? (gate-allows? :expenses/articles.manage
                 {:admin-route? true
                  :expenses-role nil
                  :can-write? false
                  :power-user? false}))
      "Admin routes should bypass expenses action gates")
    (is (false? (gate-allows? :expenses/articles.manage
                  {:admin-route? false
                   :expenses-role nil
                   :can-write? false
                   :power-user? false}))
      "Non-admin routes should still respect expenses action gates")))

(deftest selection-hook-admin-routes-bypass-select-gate
  (let [gate-allows? @#'display-settings/gate-allows-action?]
    (is (true? (gate-allows? "expenses/can-write"
                 {:admin-route? true
                  :expenses-role nil
                  :can-write? false
                  :power-user? false}))
      "Admin routes should keep reactive selection cells visible even when the select gate is configured")
    (is (false? (gate-allows? "expenses/can-write"
                  {:admin-route? false
                   :expenses-role nil
                   :can-write? false
                   :power-user? false}))
      "Non-admin routes should still hide reactive selection cells when the user lacks the configured gate")
    (is (true? (gate-allows? "expenses/can-write"
                 {:admin-route? false
                  :expenses-role nil
                  :can-write? true
                  :power-user? false}))
      "Non-admin routes should continue to allow selection when the user satisfies the configured gate")))

(deftest column-visibility-uses-legacy-when-admin-config-not-loaded
  (async done
    (let [dispatched (atom nil)]
      (with-redefs [rf/dispatch (fn [evt] (reset! dispatched evt))
                    ;; Simulate an entity that *would* have vector-config available.
                    column-config/vector-config? (constantly true)
                    column-config/get-visible-columns (fn [_vector-mode? _entity-kw raw]
                                                        (or raw {}))
                    ;; Ensure the component doesn't depend on the real entity spec hook.
                    template-utils/use-entity-spec (fn [_ _] {:entity-spec nil})
                    ;; Stub use-subscribe to keep this test focused.
                    uix-rf/use-subscribe (fn [query]
                                           (cond
                                             (= query [:admin/config-loaded?]) false
                                             ;; legacy visible-columns subscription
                                             (= (first query) :app.template.frontend.subs.ui/visible-columns) {}
                                             ;; other subscriptions used by the component
                                             (= (first query) :app.admin.frontend.subs.config/entity-config) {}
                                             (= (first query) :app.template.frontend.subs.ui/filterable-fields) []
                                             (= (first query) :app.template.frontend.events.list.settings/filterable-fields) {}
                                             :else nil))]
        (mount-component!
          ($ list-view-settings/column-visibility-settings
            {:entity-name :expenses
             :entity-spec {:fields [{:id :currency :label "Currency"}]}})
          (fn [container]
            (let [btn (button-by-text container "Currency")]
              (is (some? btn) "Currency toggle button should render")
              (.click btn)
              (is (= [::settings-events/toggle-column-visibility :expenses :currency]
                    @dispatched)
                "Non-admin pages must dispatch legacy (user prefs) toggle event")
              (done))))))))

(deftest column-visibility-uses-admin-when-admin-config-loaded
  (async done
    (let [dispatched (atom nil)]
      (with-redefs [rf/dispatch (fn [evt] (reset! dispatched evt))
                    column-config/vector-config? (constantly true)
                    column-config/get-visible-columns (fn [_vector-mode? _entity-kw _raw]
                                                        {:currency true})
                    template-utils/use-entity-spec (fn [_ _] {:entity-spec nil})
                    uix-rf/use-subscribe (fn [query]
                                           (cond
                                             (= query [:admin/config-loaded?]) true
                                             ;; admin visible-columns subscription (vector)
                                             (= (first query) :app.admin.frontend.subs.config/visible-columns) []
                                             (= (first query) :app.admin.frontend.subs.config/entity-config) {:always-visible []}
                                             (= (first query) :app.template.frontend.subs.ui/filterable-fields) []
                                             (= (first query) :app.template.frontend.events.list.settings/filterable-fields) {}
                                             :else nil))]
        (mount-component!
          ($ list-view-settings/column-visibility-settings
            {:entity-name :expenses
             :entity-spec {:fields [{:id :currency :label "Currency"}]}})
          (fn [container]
            (let [btn (button-by-text container "Currency")]
              (is (some? btn) "Currency toggle button should render")
              (.click btn)
              (is (= [:app.admin.frontend.events.config/toggle-column-visibility :expenses :currency]
                    @dispatched)
                "Admin pages must dispatch admin vector-config toggle event")
              (done))))))))

(deftest column-visibility-normalizes-namespaced-field-ids
  (async done
    (let [dispatched (atom nil)]
      (with-redefs [rf/dispatch (fn [evt] (reset! dispatched evt))
                    column-config/vector-config? (constantly true)
                    ;; Not used directly by this component, but keep stable.
                    column-config/get-visible-columns (fn [_vector-mode? _entity-kw raw]
                                                        (or raw {}))
                    template-utils/use-entity-spec (fn [_ _] {:entity-spec nil})
                    uix-rf/use-subscribe (fn [query]
                                           (cond
                                             (= query [:admin/config-loaded?]) true
                                             ;; Unified visible-columns subscription used for UI state.
                                             (= (first query) :app.template.frontend.subs.ui/visible-columns) {:email false}
                                             ;; Locks: none.
                                             (= (first query) :app.template.frontend.subs.ui/locked-visible-columns) {}
                                             ;; Entity config: no always-visible.
                                             (= (first query) :app.admin.frontend.subs.config/entity-config) {:always-visible []}
                                             ;; Filtering deps.
                                             (= (first query) :app.template.frontend.subs.ui/filterable-fields) []
                                             (= (first query) :app.template.frontend.events.list.settings/filterable-fields) {}
                                             :else nil))]
        (mount-component!
          ($ list-view-settings/column-visibility-settings
            {:entity-name :admins
             ;; Namespaced id is common in DB-backed specs; we must normalize to :email.
             :entity-spec {:fields [{:id :admins/email :label "Email"}]}})
          (fn [container]
            (let [btn (button-by-text container "Email")]
              (is (some? btn) "Email toggle button should render")
              ;; When visible-columns says {:email false}, the button should not be bold.
              (is (not (str/includes? (.-className btn) "font-semibold"))
                "Hidden column should not render as visible when id is namespaced")
              (.click btn)
              (is (= [:app.admin.frontend.events.config/toggle-column-visibility :admins :email]
                    @dispatched)
                "Click must dispatch a normalized (non-namespaced) column key")
              (done))))))))

