(ns app.template.frontend.components.list-vector-mode-dom-test
  (:require
    ["react-dom/client" :as rdom]
    ["react-dom/test-utils" :as test-utils]
    [app.template.frontend.components.list :as list]
    [app.template.frontend.components.list.table :as list-table]
    [app.template.frontend.components.list.ui :as list-ui]
    [app.template.frontend.components.pagination :as pagination]
    [app.template.frontend.components.settings.list-view-settings :as list-view-settings]
    [app.template.frontend.components.table :as table]
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

(deftest rows-override-server-mode-skips-local-transforms
  (let [apply-transforms @#'list/apply-rows-override-transforms
        rows [{:id 1 :status "posted"}
              {:id 2 :status "review_required"}
              {:id 3 :status "failed"}]
        entity-spec {:fields [{:id :status :label "Status" :type :text}]}
        result (apply-transforms {:rows rows
                                  :active-filters {}
                                  :sorts [{:field :status :direction :asc}]
                                  :server-pagination? true
                                  :entity-name :receipts
                                  :entity-spec entity-spec})]
    (is (= rows result)
      "Server-paginated rows-override lists should preserve backend ordering")))

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
              (is (= [:admin/toggle-column-visibility :expenses :currency]
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
              (is (= [:admin/toggle-column-visibility :admins :email]
                    @dispatched)
                "Click must dispatch a normalized (non-namespaced) column key")
              (done))))))))

(deftest list-view-gates-vector-mode-by-admin-config-loaded
  (async done
    (let [seen-vector-mode (atom ::unset)
          ;; list-view dispatches some events in effects; ignore.
          noop-dispatch (fn [_] nil)]
      (with-redefs [rf/dispatch noop-dispatch
                    column-config/vector-config? (constantly true)
                    column-config/visible-columns-source (fn [vector-mode? _entity-kw]
                                                           (reset! seen-vector-mode vector-mode?)
                                                         ;; return a subscription vector that our stubbed use-subscribe can handle
                                                           [:app.template.frontend.subs.ui/visible-columns :expenses])
                    column-config/get-visible-columns (fn [_vector-mode? _entity-kw raw]
                                                        (or raw {}))
                    ;; Stub the heavy UI components away
                    list-table/make-table-headers (fn [_] [])
                    list-ui/header-section (fn [_] ($ :div))
                    table/table (fn [_] ($ :div))
                    ;; Keep list-view subscriptions stable
                    uix-rf/use-subscribe (fn [query]
                                           (cond
                                             ;; Critical: pretend we're NOT on an admin page.
                                             (= query [:admin/config-loaded?]) false

                                             ;; Entity/list subscriptions used by list-view
                                             (= (first query) :app.template.frontend.subs.entity/paginated-entities) []
                                             (= (first query) :app.template.frontend.subs.entity/loading?) false
                                             (= (first query) :app.template.frontend.subs.entity/error) nil
                                             (= (first query) :app.template.frontend.subs.list/total-pages) 1
                                             (= (first query) :app.template.frontend.subs.entity/current-page) 1
                                             (= (first query) :app.template.frontend.subs.list/selected-ids) #{}
                                             (= (first query) :app.template.frontend.subs.ui/editing) nil
                                             (= (first query) :app.template.frontend.subs.ui/show-add-form) false
                                             (= (first query) :app.template.frontend.subs.ui/recently-updated-entities) #{}
                                             (= (first query) :app.template.frontend.subs.ui/recently-created-entities) #{}
                                             (= (first query) :app.template.frontend.subs.ui/hardcoded-view-options) {}
                                             (= (first query) :app.template.frontend.subs.ui/entity-display-settings) {}
                                             (= (first query) :app.template.frontend.subs.ui/filterable-fields) []
                                             (= (first query) :app.template.frontend.events.list.settings/filterable-fields) {}
                                             (= (first query) :app.template.frontend.subs.list/sorts) []
                                             (= (first query) :app.template.frontend.subs.list/active-filters) {}
                                             (= (first query) :app.template.frontend.subs.list/batch-edit-inline) {:open? false}
                                             (= (first query) :app.template.frontend.subs.list/entity-ui-state) {}
                                             (= (first query) :app.template.frontend.events.list.settings/table-width) 1200
                                             ;; Form spec subscription
                                             (= (first query) :form-entity-specs/by-name) {:fields []}

                                             ;; Visible-columns subscription requested by our visible-columns-source stub
                                             (= (first query) :app.template.frontend.subs.ui/visible-columns) {}

                                             :else nil))]
        (mount-component!
          ($ list/list-view
            {:entity-name :expenses
             :entity-spec {:fields []}
             :title "Expenses"})
          (fn [_container]
            (is (= false @seen-vector-mode)
              "list-view must not enter vector-config mode when :admin/config-loaded? is false")
            (done)))))))

(deftest list-view-seeds-per-page-from-prop-when-ui-state-missing
  (async done
    (let [dispatched (atom [])
          record-dispatch (fn [evt] (swap! dispatched conj evt))]
      (with-redefs [rf/dispatch record-dispatch
                    column-config/vector-config? (constantly false)
                    ;; Stub the heavy UI components away
                    list-table/make-table-headers (fn [_] [])
                    list-ui/header-section (fn [_] ($ :div))
                    table/table (fn [_] ($ :div))
                    ;; Keep list-view subscriptions stable
                    uix-rf/use-subscribe (fn [query]
                                           (cond
                                             (= query [:admin/config-loaded?]) false

                                             ;; Entity/list subscriptions used by list-view
                                             (= (first query) :app.template.frontend.subs.entity/paginated-entities) []
                                             (= (first query) :app.template.frontend.subs.entity/loading?) false
                                             (= (first query) :app.template.frontend.subs.entity/error) nil
                                             (= (first query) :app.template.frontend.subs.list/total-pages) 1
                                             (= (first query) :app.template.frontend.subs.entity/current-page) 1
                                             (= (first query) :app.template.frontend.subs.list/selected-ids) #{}
                                             (= (first query) :app.template.frontend.subs.ui/editing) nil
                                             (= (first query) :app.template.frontend.subs.ui/show-add-form) false
                                             (= (first query) :app.template.frontend.subs.ui/recently-updated-entities) #{}
                                             (= (first query) :app.template.frontend.subs.ui/recently-created-entities) #{}
                                             (= (first query) :app.template.frontend.subs.ui/hardcoded-view-options) {}
                                             (= (first query) :app.template.frontend.subs.ui/entity-display-settings) {}
                                             (= (first query) :app.template.frontend.subs.ui/filterable-fields) []
                                             (= (first query) :app.template.frontend.events.list.settings/filterable-fields) {}
                                             (= (first query) :app.template.frontend.subs.list/sorts) []
                                             (= (first query) :app.template.frontend.subs.list/active-filters) {}
                                             (= (first query) :app.template.frontend.subs.list/batch-edit-inline) {:open? false}
                                             ;; Critical: simulate no existing per-page in UI state
                                             (= (first query) :app.template.frontend.subs.list/entity-ui-state) {}
                                             (= (first query) :app.template.frontend.events.list.settings/table-width) 1200
                                             ;; Form spec subscription
                                             (= (first query) :form-entity-specs/by-name) {:fields []}

                                             ;; Visible-columns subscription
                                             (= (first query) :app.template.frontend.subs.ui/visible-columns) {}

                                             :else nil))]
        (mount-component!
          ($ list/list-view
            {:entity-name :users
             :entity-spec {:fields []}
             :title "Users"
             ;; Provide per-page as a top-level prop (admin pages do this)
             :per-page 25
             :display-settings {}})
          (fn [_container]
            (is (some #(= [::ui-events/seed-per-page-from-config :users 25] %)
                  @dispatched)
              "list-view should seed per-page into UI state when missing")
            (done)))))))

(deftest list-view-does-not-seed-per-page-when-ui-state-already-set
  (async done
    (let [dispatched (atom [])
          record-dispatch (fn [evt] (swap! dispatched conj evt))]
      (with-redefs [rf/dispatch record-dispatch
                    column-config/vector-config? (constantly false)
                    list-table/make-table-headers (fn [_] [])
                    list-ui/header-section (fn [_] ($ :div))
                    table/table (fn [_] ($ :div))
                    uix-rf/use-subscribe (fn [query]
                                           (cond
                                             (= query [:admin/config-loaded?]) false

                                             ;; Entity/list subscriptions used by list-view
                                             (= (first query) :app.template.frontend.subs.entity/paginated-entities) []
                                             (= (first query) :app.template.frontend.subs.entity/loading?) false
                                             (= (first query) :app.template.frontend.subs.entity/error) nil
                                             (= (first query) :app.template.frontend.subs.list/total-pages) 1
                                             (= (first query) :app.template.frontend.subs.entity/current-page) 1
                                             (= (first query) :app.template.frontend.subs.list/selected-ids) #{}
                                             (= (first query) :app.template.frontend.subs.ui/editing) nil
                                             (= (first query) :app.template.frontend.subs.ui/show-add-form) false
                                             (= (first query) :app.template.frontend.subs.ui/recently-updated-entities) #{}
                                             (= (first query) :app.template.frontend.subs.ui/recently-created-entities) #{}
                                             (= (first query) :app.template.frontend.subs.ui/hardcoded-view-options) {}
                                             (= (first query) :app.template.frontend.subs.ui/entity-display-settings) {}
                                             (= (first query) :app.template.frontend.subs.ui/filterable-fields) []
                                             (= (first query) :app.template.frontend.events.list.settings/filterable-fields) {}
                                             (= (first query) :app.template.frontend.subs.list/sorts) []
                                             (= (first query) :app.template.frontend.subs.list/active-filters) {}
                                             (= (first query) :app.template.frontend.subs.list/batch-edit-inline) {:open? false}
                                             ;; Critical: simulate an existing per-page in UI state
                                             (= (first query) :app.template.frontend.subs.list/entity-ui-state) {:per-page 10}
                                             ;; Simulate a user-set per-page preference (from localStorage)
                                             (= (first query) :app.template.frontend.subs.ui/entity-display-prefs) {:per-page 10}
                                             (= (first query) :app.template.frontend.events.list.settings/table-width) 1200
                                             (= (first query) :form-entity-specs/by-name) {:fields []}
                                             (= (first query) :app.template.frontend.subs.ui/visible-columns) {}

                                             :else nil))]
        (mount-component!
          ($ list/list-view
            {:entity-name :users
             :entity-spec {:fields []}
             :title "Users"
             :per-page 25
             :display-settings {}})
          (fn [_container]
            (is (not (some #(= [::ui-events/seed-per-page-from-config :users 25] %)
                       @dispatched))
              "list-view must not overwrite an existing per-page")
            (done)))))))

(deftest list-view-applies-filters-and-sort-to-rows-override
  (async done
    (let [captured-rows (atom nil)]
      (with-redefs [rf/dispatch (fn [_] nil)
                    column-config/vector-config? (constantly false)
                    list-table/make-table-headers (fn [_] [])
                    list-ui/header-section (fn [_] ($ :div))
                    table/table (fn [props]
                                  (let [rows (or (:rows props)
                                               (when (some? props)
                                                 (aget props "rows")))]
                                    (reset! captured-rows rows))
                                  ($ :div))
                    uix-rf/use-subscribe (fn [query]
                                           (cond
                                             (= query [:admin/config-loaded?]) false

                                             (= (first query) :app.template.frontend.subs.entity/paginated-entities) []
                                             (= (first query) :app.template.frontend.subs.entity/loading?) false
                                             (= (first query) :app.template.frontend.subs.entity/error) nil
                                             (= (first query) :app.template.frontend.subs.list/total-pages) 1
                                             (= (first query) :app.template.frontend.subs.entity/current-page) 1
                                             (= (first query) :app.template.frontend.subs.list/selected-ids) #{}
                                             (= (first query) :app.template.frontend.subs.ui/editing) nil
                                             (= (first query) :app.template.frontend.subs.ui/show-add-form) false
                                             (= (first query) :app.template.frontend.subs.ui/recently-updated-entities) #{}
                                             (= (first query) :app.template.frontend.subs.ui/recently-created-entities) #{}
                                             (= (first query) :app.template.frontend.subs.ui/hardcoded-view-options) {}
                                             (= (first query) :app.template.frontend.subs.ui/entity-display-settings) {}
                                             (= (first query) :app.template.frontend.subs.ui/filterable-fields) []
                                             (= (first query) :app.template.frontend.events.list.settings/filterable-fields) {}
                                             (= (first query) :app.template.frontend.subs.list/sorts) [{:field :amount :direction :asc}]
                                             (= (first query) :app.template.frontend.subs.list/active-filters) {:description "alp"}
                                             (= (first query) :app.template.frontend.subs.list/batch-edit-inline) {:open? false}
                                             (= (first query) :app.template.frontend.subs.list/entity-ui-state) {}
                                             (= (first query) :app.template.frontend.events.list.settings/table-width) 1200
                                             (= (first query) :form-entity-specs/by-name) {:fields []}
                                             (= (first query) :app.template.frontend.subs.ui/visible-columns) {}

                                             :else nil))]
        (mount-component!
          ($ list/list-view
            {:entity-name :expenses
             :entity-spec {:fields [{:id :description :input-type "text"}
                                    {:id :amount :input-type "number"}]}
             :title "Expenses"
             :rows-override [{:id 1 :description "beta" :amount 30}
                             {:id 2 :description "alpha" :amount 20}
                             {:id 3 :description "alphabet" :amount 10}]})
          (fn [_container]
            (let [rows (or (some-> @captured-rows (js->clj :keywordize-keys true))
                         @captured-rows
                         [])]
              (is (= [3 2] (mapv :id rows))
                "rows-override should be filtered then sorted using current list UI state"))
            (done)))))))

(deftest list-view-renders-active-filters-below-header-section
  (async done
    (with-redefs [rf/dispatch (fn [_] nil)
                  column-config/vector-config? (constantly false)
                  list-table/make-table-headers (fn [_] [])
                  list-ui/header-section (fn [_] ($ :div {:id "list-header-stub"} "Receipts"))
                  table/table (fn [_] ($ :div))
                  i18n/use-t (fn []
                               (fn [k & _]
                                 (case k
                                   :common/active-filters "Aktivni filteri"
                                   :common/remove-filter "Ukloni ovaj filter"
                                   :common/record-singular "zapis"
                                   :common/record-plural "zapisa"
                                   :common/selected "odabrano"
                                   :common/hidden "skriveno"
                                   :common/loading "Učitavanje..."
                                   nil)))
                  uix-rf/use-subscribe (fn [query]
                                         (cond
                                           (= query [:admin/config-loaded?]) false
                                           (= (first query) :app.template.frontend.subs.entity/paginated-entities) []
                                           (= (first query) :app.template.frontend.subs.entity/loading?) false
                                           (= (first query) :app.template.frontend.subs.entity/error) nil
                                           (= (first query) :app.template.frontend.subs.list/total-pages) 1
                                           (= (first query) :app.template.frontend.subs.entity/current-page) 1
                                           (= (first query) :app.template.frontend.subs.list/selected-ids) #{}
                                           (= (first query) :app.template.frontend.subs.ui/editing) nil
                                           (= (first query) :app.template.frontend.subs.ui/show-add-form) false
                                           (= (first query) :app.template.frontend.subs.ui/recently-updated-entities) #{}
                                           (= (first query) :app.template.frontend.subs.ui/recently-created-entities) #{}
                                           (= (first query) :app.template.frontend.subs.ui/hardcoded-view-options) {}
                                           (= (first query) :app.template.frontend.subs.ui/entity-display-settings) {}
                                           (= (first query) :app.template.frontend.subs.ui/filterable-fields) []
                                           (= (first query) :app.template.frontend.events.list.settings/filterable-fields) {}
                                           (= (first query) :app.template.frontend.subs.list/sorts) []
                                           (= (first query) :app.template.frontend.subs.list/active-filters) {:status [{:value "posted" :label "Objavljeno"}]}
                                           (= (first query) :app.template.frontend.subs.list/batch-edit-inline) {:open? false}
                                           (= (first query) :app.template.frontend.subs.list/entity-ui-state) {}
                                           (= (first query) :app.template.frontend.events.list.settings/table-width) 1200
                                           (= (first query) :app.template.frontend.subs.ui/entity-display-prefs) {}
                                           (= (first query) :form-entity-specs/by-name) {:fields []}
                                           (= (first query) :app.template.frontend.subs.ui/visible-columns) {}
                                           (= (first query) :app.template.frontend.subs.entity/entity-config) {:fields [{:id :status :label "Status"}]}
                                           (= (first query) :app.template.frontend.subs.entity/entities) []
                                           :else nil))]
      (mount-component!
        ($ list/list-view
          {:entity-name :expenses
           :entity-spec {:fields [{:id :status :label "Status" :input-type "select"}]}
           :title "Receipts"})
        (fn [container]
          (let [header (.querySelector container "#list-header-stub")
                active-filters (.querySelector container "#active-filters-expenses")]
            (is (some? header) "Expected the list header to render")
            (is (some? active-filters) "Expected the compact active filters section to render")
            (is (pos? (bit-and (.compareDocumentPosition header active-filters)
                        (.-DOCUMENT_POSITION_FOLLOWING js/Node)))
              "Active filter chips should render below the list header/title, not above it")
            (done)))))))

(deftest list-view-keeps-existing-rows-mounted-while-loading
  (async done
    (with-redefs [rf/dispatch (fn [_] nil)
                  column-config/vector-config? (constantly false)
                  list-table/make-table-headers (fn [_] [])
                  list-ui/header-section (fn [_] ($ :div {:id "list-header-stub"} "Users"))
                  table/table (fn [_] ($ :div {:id "table-content-stub"}))
                  i18n/use-t (fn []
                               (fn [k & _]
                                 (case k
                                   :common/loading "Učitavanje..."
                                   :common/record-singular "zapis"
                                   :common/record-plural "zapisa"
                                   :common/selected "odabrano"
                                   :common/hidden "skriveno"
                                   nil)))
                  uix-rf/use-subscribe (fn [query]
                                         (cond
                                           (= query [:admin/config-loaded?]) false
                                           (= (first query) :app.template.frontend.subs.entity/paginated-entities) [{:id 1 :name "Row 1"}]
                                           (= (first query) :app.template.frontend.subs.entity/loading?) true
                                           (= (first query) :app.template.frontend.subs.entity/error) nil
                                           (= (first query) :app.template.frontend.subs.list/total-pages) 1
                                           (= (first query) :app.template.frontend.subs.entity/current-page) 1
                                           (= (first query) :app.template.frontend.subs.list/selected-ids) #{}
                                           (= (first query) :app.template.frontend.subs.ui/editing) nil
                                           (= (first query) :app.template.frontend.subs.ui/show-add-form) false
                                           (= (first query) :app.template.frontend.subs.ui/recently-updated-entities) #{}
                                           (= (first query) :app.template.frontend.subs.ui/recently-created-entities) #{}
                                           (= (first query) :app.template.frontend.subs.ui/hardcoded-view-options) {}
                                           (= (first query) :app.template.frontend.subs.ui/entity-display-settings) {}
                                           (= (first query) :app.template.frontend.subs.ui/filterable-fields) []
                                           (= (first query) :app.template.frontend.events.list.settings/filterable-fields) {}
                                           (= (first query) :app.template.frontend.subs.list/sorts) []
                                           (= (first query) :app.template.frontend.subs.list/active-filters) {}
                                           (= (first query) :app.template.frontend.subs.list/batch-edit-inline) {:open? false}
                                           (= (first query) :app.template.frontend.subs.list/entity-ui-state) {}
                                           (= (first query) :app.template.frontend.events.list.settings/table-width) 1200
                                           (= (first query) :app.template.frontend.subs.ui/entity-display-prefs) {}
                                           (= (first query) :form-entity-specs/by-name) {:fields []}
                                           (= (first query) :app.template.frontend.subs.ui/visible-columns) {}
                                           :else nil))]
      (mount-component!
        ($ list/list-view
          {:entity-name :users
           :entity-spec {:fields []}
           :title "Users"})
        (fn [container]
          (is (some? (.querySelector container "#table-shell-users"))
            "Existing rows should keep the table shell mounted while a refresh is in flight")
          (is (some? (.querySelector container "#table-content-stub"))
            "Existing rows should keep the table content mounted while loading")
          (is (not (str/includes? (.-textContent container) "Učitavanje..."))
            "Loading refreshes with existing rows should not replace the list with a loading placeholder")
          (done))))))

(deftest list-view-shows-loading-placeholder-when-no-rows-exist
  (async done
    (with-redefs [rf/dispatch (fn [_] nil)
                  column-config/vector-config? (constantly false)
                  list-table/make-table-headers (fn [_] [])
                  list-ui/header-section (fn [_] ($ :div {:id "list-header-stub"} "Users"))
                  table/table (fn [_] ($ :div {:id "table-content-stub"}))
                  i18n/use-t (fn []
                               (fn [k & _]
                                 (case k
                                   :common/loading "Učitavanje..."
                                   nil)))
                  uix-rf/use-subscribe (fn [query]
                                         (cond
                                           (= query [:admin/config-loaded?]) false
                                           (= (first query) :app.template.frontend.subs.entity/paginated-entities) []
                                           (= (first query) :app.template.frontend.subs.entity/loading?) true
                                           (= (first query) :app.template.frontend.subs.entity/error) nil
                                           (= (first query) :app.template.frontend.subs.list/total-pages) 1
                                           (= (first query) :app.template.frontend.subs.entity/current-page) 1
                                           (= (first query) :app.template.frontend.subs.list/selected-ids) #{}
                                           (= (first query) :app.template.frontend.subs.ui/editing) nil
                                           (= (first query) :app.template.frontend.subs.ui/show-add-form) false
                                           (= (first query) :app.template.frontend.subs.ui/recently-updated-entities) #{}
                                           (= (first query) :app.template.frontend.subs.ui/recently-created-entities) #{}
                                           (= (first query) :app.template.frontend.subs.ui/hardcoded-view-options) {}
                                           (= (first query) :app.template.frontend.subs.ui/entity-display-settings) {}
                                           (= (first query) :app.template.frontend.subs.ui/filterable-fields) []
                                           (= (first query) :app.template.frontend.events.list.settings/filterable-fields) {}
                                           (= (first query) :app.template.frontend.subs.list/sorts) []
                                           (= (first query) :app.template.frontend.subs.list/active-filters) {}
                                           (= (first query) :app.template.frontend.subs.list/batch-edit-inline) {:open? false}
                                           (= (first query) :app.template.frontend.subs.list/entity-ui-state) {}
                                           (= (first query) :app.template.frontend.events.list.settings/table-width) 1200
                                           (= (first query) :app.template.frontend.subs.ui/entity-display-prefs) {}
                                           (= (first query) :form-entity-specs/by-name) {:fields []}
                                           (= (first query) :app.template.frontend.subs.ui/visible-columns) {}
                                           :else nil))]
      (mount-component!
        ($ list/list-view
          {:entity-name :users
           :entity-spec {:fields []}
           :title "Users"})
        (fn [container]
          (is (nil? (.querySelector container "#table-shell-users"))
            "Empty initial loads should still use the loading placeholder instead of an empty table shell")
          (is (str/includes? (.-textContent container) "Učitavanje...")
            "Loading placeholder text should still render when there are no rows to keep mounted")
          (done))))))

(deftest list-view-keeps-selected-toggle-rightmost
  (async done
    (with-redefs [rf/dispatch (fn [_] nil)
                  column-config/vector-config? (constantly false)
                  list-table/make-table-headers (fn [_] [])
                  list-ui/header-section (fn [_] ($ :div {:id "list-header-stub"} "Expenses"))
                  table/table (fn [_] ($ :div {:id "table-content-stub"}))
                  i18n/use-t (fn []
                               (fn [k & _]
                                 (case k
                                   :common/record-singular "zapis"
                                   :common/record-plural "zapisa"
                                   :common/selected "odabrano"
                                   :common/hidden "skriveno"
                                   :list/toggle-selected-rows "Odabrano"
                                   :list/toggle-unselected-rows "Neodabrano"
                                   nil)))
                  uix-rf/use-subscribe (fn [query]
                                         (cond
                                           (= query [:admin/config-loaded?]) false
                                           (= (first query) :app.template.frontend.subs.entity/paginated-entities) []
                                           (= (first query) :app.template.frontend.subs.entity/loading?) false
                                           (= (first query) :app.template.frontend.subs.entity/error) nil
                                           (= (first query) :app.template.frontend.subs.list/total-pages) 1
                                           (= (first query) :app.template.frontend.subs.entity/current-page) 1
                                           (= (first query) :app.template.frontend.subs.list/selected-ids) #{}
                                           (= (first query) :app.template.frontend.subs.ui/editing) nil
                                           (= (first query) :app.template.frontend.subs.ui/show-add-form) false
                                           (= (first query) :app.template.frontend.subs.ui/recently-updated-entities) #{}
                                           (= (first query) :app.template.frontend.subs.ui/recently-created-entities) #{}
                                           (= (first query) :app.template.frontend.subs.ui/hardcoded-view-options) {}
                                           (= (first query) :app.template.frontend.subs.ui/entity-display-settings) {}
                                           (= (first query) :app.template.frontend.subs.ui/filterable-fields) []
                                           (= (first query) :app.template.frontend.events.list.settings/filterable-fields) {}
                                           (= (first query) :app.template.frontend.subs.list/sorts) []
                                           (= (first query) :app.template.frontend.subs.list/active-filters) {}
                                           (= (first query) :app.template.frontend.subs.list/batch-edit-inline) {:open? false}
                                           (= (first query) :app.template.frontend.subs.list/entity-ui-state) {}
                                           (= (first query) :app.template.frontend.events.list.settings/table-width) 1200
                                           (= (first query) :app.template.frontend.subs.ui/entity-display-prefs) {}
                                           (= (first query) :form-entity-specs/by-name) {:fields []}
                                           (= (first query) :app.template.frontend.subs.ui/visible-columns) {}
                                           :else nil))]
      (mount-component!
        ($ list/list-view
          {:entity-name :expenses
           :entity-spec {:fields []}
           :title "Expenses"
           :extra-settings-toggle-groups [{:id "toggle-group-expense-source"
                                           :toggles [{:id "toggle-show-manual-expenses"
                                                      :label "Ručni unos"
                                                      :active? true
                                                      :on-click (fn [_] nil)}
                                                     {:id "toggle-show-receipt-expenses"
                                                      :label "Računi"
                                                      :active? true
                                                      :on-click (fn [_] nil)}]}]})
        (fn [container]
          (let [source-group (.querySelector container "#toggle-group-expense-source")
                row-group (.querySelector container "#toggle-group-row-visibility-expenses")
                unselected-toggle (.querySelector container "#toggle-unselected-rows-expenses")
                selected-toggle (.querySelector container "#toggle-selected-rows-expenses")]
            (is (some? source-group) "Expected custom expense-source toggle group to render")
            (is (some? row-group) "Expected row visibility toggle group to render")
            (is (pos? (bit-and (.compareDocumentPosition source-group row-group)
                        (.-DOCUMENT_POSITION_FOLLOWING js/Node)))
              "Row visibility controls should render after custom toggle groups")
            (is (pos? (bit-and (.compareDocumentPosition unselected-toggle selected-toggle)
                        (.-DOCUMENT_POSITION_FOLLOWING js/Node)))
              "The selected toggle should be the rightmost segment in the row visibility pill"))
          (done))))))

(deftest table-header-cells-stick-to-top-of-scroll-viewport
  (async done
    (mount-component!
      ($ table/resizable-cell
        {:is-header? true
         :index 0}
        "Name")
      (fn [container]
        (let [header (.querySelector container "th")
              header-inner (.querySelector container "th > span")]
          (is (some? header) "Expected a rendered header cell")
          (is (some? header-inner) "Expected the header wrapper span to render")
          (is (= "sticky" (.. header -style -position))
            "Header cells should use sticky positioning inside the scroll viewport")
          (is (= "0px" (.. header -style -top))
            "Header cells should pin to the top edge of the scroll viewport")
          (is (str/includes? (.-className header) "bg-base-100")
            "Sticky header cells should use an opaque header background class")
          (is (str/includes? (.-className header) "align-top")
            "Header cells should top-align their content instead of centering it vertically")
          (is (str/includes? (.-className header-inner) "h-full")
            "Header wrapper should stretch so inner header layouts can use full cell height"))
        (done)))))

(deftest sticky-table-cells-avoid-scroll-repaint-classes
  (async done
    (mount-component!
      ($ :table
        ($ :thead
          ($ :tr
            ($ table/resizable-cell
              {:is-header? true
               :index 0
               :sticky? true
               :sticky-position :left
               :fixed-width "50px"}
              "Pinned")))
        ($ :tbody
          ($ :tr
            ($ table/resizable-cell
              {:is-header? false
               :index 0
               :sticky? true
               :sticky-position :right
               :fixed-width "150px"}
              ($ :span "Actions")))))
      (fn [container]
        (let [header (.querySelector container "th")
              body (.querySelector container "td")
              header-class (.-className header)
              body-class (.-className body)]
          (is (some? header) "Expected sticky header cell to render")
          (is (some? body) "Expected sticky body cell to render")
          (is (not (str/includes? header-class "transition-all"))
            "Sticky header cells should not animate all properties during scroll")
          (is (not (str/includes? body-class "transition-all"))
            "Sticky body cells should not animate all properties during scroll")
          (is (not (str/includes? header-class "backdrop-blur"))
            "Sticky header cells should avoid backdrop filters while scrolling")
          (is (not (str/includes? body-class "backdrop-blur"))
            "Sticky body cells should avoid backdrop filters while scrolling")
          (is (str/includes? body-class "bg-base-100")
            "Sticky body cells should keep an opaque background behind icons"))
        (done)))))

(deftest table-header-keeps-filter-controls-below-long-labels
  (async done
    (with-redefs [i18n/use-t (fn [] (fn [_ & _] "Filter by"))]
      (mount-component!
        ($ list-table/table-header
          {:label "Originalni naziv dat"
           :sortable? true
           :on-click (fn [_] nil)
           :sort-direction nil
           :filter-on-click (fn [_] nil)
           :filter-active? false
           :show-filtering? true
           :is-field-filterable? true
           :header-id "header-original-filename"
           :active-inline-filter? false
           :field-id :original-filename})
        (fn [container]
          (let [header (.querySelector container "#header-original-filename")
                filter-btn (.querySelector container "#filter-icon-original-filename")
                children (when header (array-seq (.-children header)))
                label-row (first children)
                controls-row (second children)]
            (is (some? header) "Expected a rendered table header")
            (is (some? filter-btn) "Expected a filter button for filterable columns")
            (is (some? label-row) "Expected a dedicated label row")
            (is (some? controls-row) "Expected a dedicated controls row")
            (is (str/includes? (.-className header) "min-h-[4.5rem]")
              "Header should reserve vertical space so labels stay top-aligned and controls stay bottom-aligned")
            (is (str/includes? (.-textContent label-row) "Originalni naziv dat")
              "Long header text should remain in the label row")
            (is (str/includes? (.-className controls-row) "mt-auto")
              "Controls row should push itself to the bottom of the header cell")
            (is (true? (boolean (and controls-row filter-btn (.contains controls-row filter-btn))))
              "Filter button should render in the controls row below the label")
            (done)))))))

(deftest table-thead-keeps-settings-row-sticky-with-header
  (let [thead-class (:class table/sticky-thead-props)
        settings-class table/settings-row-cell-class]
    (is (str/includes? thead-class "sticky")
      "Thead should stay sticky so the settings row scrolls with the header")
    (is (str/includes? thead-class "top-0")
      "Sticky thead should pin to the top edge of the scroll viewport")
    (is (str/includes? settings-class "bg-base-200")
      "Settings row should keep an opaque background while sticky")))

(deftest list-view-keeps-pagination-outside-scroll-viewport
  (async done
    (with-redefs [rf/dispatch (fn [_] nil)
                  column-config/vector-config? (constantly false)
                  list-table/make-table-headers (fn [_] [])
                  table/table (fn [_] ($ :div {:id "table-content-stub"}))
                  pagination/pagination (fn [_] ($ :div {:id "pagination-stub"}))
                  list-ui/header-section (fn [_] ($ :div {:id "list-header-stub"}))
                  uix-rf/use-subscribe (fn [query]
                                         (cond
                                           (= query [:admin/config-loaded?]) false
                                           (= (first query) :app.template.frontend.subs.entity/paginated-entities) [{:id 1}]
                                           (= (first query) :app.template.frontend.subs.entity/loading?) false
                                           (= (first query) :app.template.frontend.subs.entity/error) nil
                                           (= (first query) :app.template.frontend.subs.list/total-pages) 3
                                           (= (first query) :app.template.frontend.subs.entity/current-page) 1
                                           (= (first query) :app.template.frontend.subs.list/selected-ids) #{}
                                           (= (first query) :app.template.frontend.subs.ui/editing) nil
                                           (= (first query) :app.template.frontend.subs.ui/show-add-form) false
                                           (= (first query) :app.template.frontend.subs.ui/recently-updated-entities) #{}
                                           (= (first query) :app.template.frontend.subs.ui/recently-created-entities) #{}
                                           (= (first query) :app.template.frontend.subs.ui/hardcoded-view-options) {}
                                           (= (first query) :app.template.frontend.subs.ui/entity-display-settings) {}
                                           (= (first query) :app.template.frontend.subs.ui/filterable-fields) []
                                           (= (first query) :app.template.frontend.events.list.settings/filterable-fields) {}
                                           (= (first query) :app.template.frontend.subs.list/sorts) []
                                           (= (first query) :app.template.frontend.subs.list/active-filters) {}
                                           (= (first query) :app.template.frontend.subs.list/batch-edit-inline) {:open? false}
                                           (= (first query) :app.template.frontend.subs.list/entity-ui-state) {}
                                           (= (first query) :app.template.frontend.events.list.settings/table-width) 1200
                                           (= (first query) :app.template.frontend.subs.ui/entity-display-prefs) {}
                                           (= (first query) :form-entity-specs/by-name) {:fields []}
                                           (= (first query) :app.template.frontend.subs.ui/visible-columns) {}
                                           :else nil))]
      (mount-component!
        ($ list/list-view
          {:entity-name :users
           :entity-spec {:fields []}
           :title "Users"})
        (fn [container]
          (let [shell (.querySelector container "#table-shell-users")
                viewport (.querySelector container "#table-scroll-viewport-users")
                pagination-container (.querySelector container "#table-pagination-users")
                pagination-stub (.querySelector container "#pagination-stub")]
            (is (some? shell) "Expected a dedicated list shell wrapper")
            (is (some? viewport) "Expected a dedicated scroll viewport for the table")
            (is (str/includes? (.-className viewport) "overflow-auto")
              "Scroll viewport should manage vertical scrolling")
            (is (str/includes? (.-className viewport) "scroll-smooth")
              "Scroll viewport should opt into smooth programmatic scrolling")
            (is (str/includes? (.-className viewport) "overscroll-contain")
              "Scroll viewport should keep vertical scroll momentum inside the list")
            (is (some? pagination-container) "Expected a dedicated pagination footer container")
            (is (some? pagination-stub) "Expected the pagination stub to render")
            (is (.contains pagination-container pagination-stub)
              "Pagination UI should live inside the footer container")
            (is (not (.contains viewport pagination-stub))
              "Pagination UI should stay outside the scrolling table viewport"))
          (done))))))
