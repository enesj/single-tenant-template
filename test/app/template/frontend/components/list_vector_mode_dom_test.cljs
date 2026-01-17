(ns app.template.frontend.components.list-vector-mode-dom-test
  (:require
    ["react-dom/client" :as rdom]
    ["react-dom/test-utils" :as test-utils]
    [app.template.frontend.components.list :as list]
    [app.template.frontend.components.list.table :as list-table]
    [app.template.frontend.components.list.ui :as list-ui]
    [app.template.frontend.components.settings.list-view-settings :as list-view-settings]
    [app.template.frontend.components.table :as table]
    [app.template.frontend.events.list.ui-state :as ui-events]
    [app.template.frontend.events.list.settings :as settings-events]
    [app.template.frontend.utils.column-config :as column-config]
    [app.template.frontend.utils.shared :as template-utils]
    [clojure.string :as str]
    [cljs.test :refer-macros [async deftest is]]
    [re-frame.core :as rf]
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

(defn- button-by-text [container text]
  (let [buttons (.querySelectorAll container "button")]
    (some (fn [btn]
            (when (= text (.-textContent btn))
              btn))
      (array-seq buttons))))

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
                                             (= (first query) :app.template.frontend.subs.list/sort-config) {:field nil :direction nil}
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
                                             (= (first query) :app.template.frontend.subs.list/sort-config) {:field nil :direction nil}
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
            (is (some #(= [::ui-events/set-per-page :users 25] %)
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
                                             (= (first query) :app.template.frontend.subs.list/sort-config) {:field nil :direction nil}
                                             (= (first query) :app.template.frontend.subs.list/active-filters) {}
                                             (= (first query) :app.template.frontend.subs.list/batch-edit-inline) {:open? false}
                                             ;; Critical: simulate an existing per-page in UI state
                                             (= (first query) :app.template.frontend.subs.list/entity-ui-state) {:per-page 10}
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
            (is (not (some #(= [::ui-events/set-per-page :users 25] %)
                       @dispatched))
              "list-view must not overwrite an existing per-page")
            (done)))))))
