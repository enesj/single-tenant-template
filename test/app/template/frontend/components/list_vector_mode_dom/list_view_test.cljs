(ns app.template.frontend.components.list-vector-mode-dom.list-view-test
  (:require
    ["react-dom/client" :as rdom]
    ["react-dom/test-utils" :as test-utils]
    [app.template.frontend.components.list :as list]
    [app.template.frontend.components.list.table :as list-table]
    [app.template.frontend.components.list.ui :as list-ui]
    [app.template.frontend.components.table :as table]
    [app.template.frontend.i18n :as i18n]
    [app.template.frontend.events.list.ui-state :as ui-events]
    [app.template.frontend.utils.column-config :as column-config]
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

