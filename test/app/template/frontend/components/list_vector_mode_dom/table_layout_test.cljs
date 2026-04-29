(ns app.template.frontend.components.list-vector-mode-dom.table-layout-test
  (:require
    ["react-dom/client" :as rdom]
    ["react-dom/test-utils" :as test-utils]
    [app.template.frontend.components.list :as list]
    [app.template.frontend.components.list.table :as list-table]
    [app.template.frontend.components.list.ui :as list-ui]
    [app.template.frontend.components.pagination :as pagination]
    [app.template.frontend.components.table :as table]
    [app.template.frontend.i18n :as i18n]
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
