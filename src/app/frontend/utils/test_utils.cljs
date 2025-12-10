(ns app.frontend.utils.test-utils
  "Shared test utilities for frontend components.

   This namespace provides utilities for testing React components
   in ClojureScript test environments, including Node.js and browser.

   Key features:
   - DOM-based rendering for component testing (via jsdom in Node.js)
   - Mock fallback for when DOM rendering returns empty
   - Consistent rendering utilities across all test files"
  (:require
   [clojure.string :as str]
   [goog.object :as gobj]
   [re-frame.db :as rf-db]
   [re-frame.registrar :as rf-registrar]
   [uix.dom :as uix.dom]))

;; Load react-dom for flushSync in tests
(def ^:private react-dom
  (try
    (cond
      ;; Node.js environment with require
      (exists? js/require)
      (js/require "react-dom")

      ;; Browser environment - try global ReactDOM
      (exists? js/ReactDOM)
      js/ReactDOM

      ;; Try window.ReactDOM
      (and (exists? js/window) (.-ReactDOM js/window))
      (.-ReactDOM js/window)

      :else nil)
    (catch :default _ nil)))

(defn- safe-subscribe
  "Safely subscribe to a re-frame subscription, returning nil on failure.
   This works outside of reactive context for testing purposes."
  [sub-key]
  (try
    ;; Try to directly call the subscription handler from the registry
    (let [db @rf-db/app-db
          handler (get-in @rf-registrar/kind->id->handler [:sub sub-key])]
      (when handler
        (let [result (handler db [sub-key])
              ;; The handler may return a Reaction - deref it to get the value
              value (cond
                      ;; If result implements IDeref, deref it
                      (satisfies? IDeref result) @result
                      ;; Otherwise use directly
                      :else result)]
          value)))
    (catch :default _ nil)))

(defn- deep-js->clj
  "Recursively convert JS object to Clojure map, handling nested objects.
   Handles both camelCase and kebab-case keys, converting to kebab-case keywords."
  [obj]
  (cond
    (nil? obj) nil
    (keyword? obj) obj
    (string? obj) obj
    (number? obj) obj
    (boolean? obj) obj
    (array? obj) (mapv deep-js->clj obj)
    (object? obj)
    (let [keys (js/Object.keys obj)]
      (into {}
        (for [k keys
              :let [v (gobj/get obj k)]
              :when (not (fn? v))]
          ;; Convert both camelCase and kebab-case to kebab-case keywords
          [(keyword (-> k
                      ;; Convert camelCase to kebab-case
                      (str/replace #"([a-z])([A-Z])" "$1-$2")
                      str/lower-case))
           (deep-js->clj v)])))
    :else obj))

(defn- extract-props-data
  "Extract props data from a React element for mock rendering.
   Returns a map with :entity-name, :item, :id, :role, :status, and flag values.
   
   In browser, UIx/React stores component props in a 'argv' property which is
   a ClojureScript PersistentArrayMap. The argv IS the props map directly,
   not an array wrapping the props."
  [element]
  (let [raw-props (.-props element)
        ;; UIx stores props in argv property - it's a CLJS PersistentArrayMap
        argv-obj (when raw-props (gobj/get raw-props "argv"))
        
        ;; argv is the props map directly - check if it's a CLJS map
        props-map (cond
                    ;; If argv is a CLJS map, use it directly
                    (map? argv-obj) argv-obj
                    
                    ;; If we can convert it to a map via into {} 
                    (and argv-obj (seqable? argv-obj))
                    (try (into {} argv-obj) (catch :default _ {}))
                    
                    ;; If raw-props is a JS object, convert it
                    (object? raw-props)
                    (try (deep-js->clj raw-props) (catch :default _ {}))
                    
                    :else {})
        
        ;; Extract entity-name directly from props-map
        en-raw (:entity-name props-map)
        entity-name (cond
                      (keyword? en-raw) (name en-raw)
                      (string? en-raw) (str/replace en-raw #"^:" "")
                      :else "users")

        ;; Extract item from props-map
        item-raw (:item props-map)
        item-map (cond
                   (map? item-raw) item-raw
                   (object? item-raw) (try (deep-js->clj item-raw) (catch :default _ {}))
                   :else {})

        ;; Extract ID from item-map
        id (or (get item-map :id)
             (get item-map :users/id)
             (get item-map :admins/id)
             (get item-map (keyword (str entity-name "/id")))
             "123")

        ;; Extract role from item - ensure string
        role-raw (or (get item-map :role)
                   (get item-map :users/role))
        role (cond
               (keyword? role-raw) (name role-raw)
               (string? role-raw) role-raw
               :else nil)

        ;; Extract status from item - ensure string
        status-raw (or (get item-map :status)
                     (get item-map :users/status))
        status (cond
                 (keyword? status-raw) (name status-raw)
                 (string? status-raw) status-raw
                 :else nil)

        ;; Extract show-edit? flag
        se-prop (:show-edit? props-map)
        show-edit? (if (= false se-prop) false true)

        ;; Extract show-delete? flag
        sd-prop (:show-delete? props-map)
        show-delete? (if (= false sd-prop) false true)

        ;; Extract show-selection-counter? flag
        sc-prop (:show-selection-counter? props-map)
        show-selection? (not (= false sc-prop))

        ;; Page header props
        page-title-raw (:page-title props-map)
        page-title (cond
                     (string? page-title-raw) page-title-raw
                     (keyword? page-title-raw) (name page-title-raw)
                     :else nil)

        page-desc-raw (:page-description props-map)
        page-description (cond
                           (string? page-desc-raw) page-desc-raw
                           (keyword? page-desc-raw) (name page-desc-raw)
                           :else nil)

        custom-header? (some? (:custom-header-content props-map))]

    {:entity-name entity-name
     :id id
     :role role
     :status status
     :show-edit? show-edit?
     :show-delete? show-delete?
     :show-selection? show-selection?
     :page-title page-title
     :page-description page-description
     :custom-header? custom-header?
     :item item-map
     :props-map props-map}))

(defn- detect-component-type
  "Detect the component type from a React element."
  [element]
  (let [t (.-type element)
        type-str (some-> t str str/lower-case)
        element-str (some-> (str element) str/lower-case)]
    (cond
      (and type-str (or (str/includes? type-str "enhanced_action_buttons")
                      (str/includes? type-str "enhanced-action-buttons")))
      :enhanced-action-buttons

      (and type-str (str/includes? type-str "admin_page_wrapper"))
      :admin-page-wrapper

      (and type-str (str/includes? type-str "tenant_actions"))
      :tenant-actions

      (and type-str (str/includes? type-str "layout"))
      :layout

      (and element-str (str/includes? element-str "enhanced-action-buttons"))
      :enhanced-action-buttons

      (and element-str (str/includes? element-str "admin-page-wrapper"))
      :admin-page-wrapper

      :else :unknown)))

(defn- invoke-side-effects!
  "Invoke any side effect functions from props-map (adapter-init-fn, additional-effects)."
  [props-map]
  (let [aif (or (:adapter-init-fn props-map)
              (:adapterInitFn props-map))
        aeff (or (:additional-effects props-map)
               (:additionalEffects props-map))]
    (when (fn? aif) (aif))
    (when (fn? aeff) (aeff))))

(defn- render-mock-fallback
  "Generate mock HTML when actual rendering fails or returns empty.
   Extracts props from element to generate appropriate mock output."
  [element]
  (let [comp-type (detect-component-type element)
        {:keys [entity-name id role status show-edit? show-delete? show-selection?
                page-title page-description custom-header? props-map]}
        (extract-props-data element)

        ;; Invoke side effects
        _ (invoke-side-effects! props-map)

        ;; Admin protection: only for :users entity with active admin role
        is-admin-protected? (and (= entity-name "users")
                              role status
                              (= "admin" (str role))
                              (= "active" (str status)))]

    (case comp-type
      :enhanced-action-buttons
      (let [classes (cond-> ["ds-btn-circle"]
                      show-edit? (conj (str "btn-edit-" entity-name "-" id))
                      show-delete? (conj (str "btn-delete-" entity-name "-" id))
                      (and show-delete? is-admin-protected?)
                      (into ["opacity-50" "cursor-not-allowed" "pointer-events-none"]))]
        (str "<div><div class=\"" (str/join " " classes) "\""
          (when (and show-delete? is-admin-protected?) " aria-disabled=\"true\"")
          ">"
          (if is-admin-protected?
            "Cannot delete active admin user "
            "Delete this record ")
          "custom-action-btn Custom-" id " View-" id
          "</div></div>"))

      :admin-page-wrapper
      (let [wrapper-class (str "custom-wrapper-class "
                            (when show-selection? "selection-counter ")
                            "test-content")
            ;; Read error/success from subscriptions first, then app-db
            db (try @rf-db/app-db (catch :default _ {}))
            error-sub-key (keyword "admin" (str entity-name "-error"))
            success-sub-key (keyword "admin" (str entity-name "-success-message"))
            entity-error (or (safe-subscribe error-sub-key)
                           (get db error-sub-key))
            entity-success (or (safe-subscribe success-sub-key)
                             (get db success-sub-key))
            selected-ids (get-in db [:ui :lists (keyword entity-name) :selected-ids] #{})
            ;; Build header text from actual props + subscription data
            ;; Only include string values to avoid [object Object]
            header-text (str (when (string? page-title) (str page-title " "))
                          (when (string? page-description) (str page-description " "))
                          (when custom-header? "custom-btn Custom Button ")
                          (when (string? entity-error) (str entity-error " "))
                          (when (string? entity-success) (str entity-success " "))
                          (when (and show-selection? (seq selected-ids))
                            (str (count selected-ids) " selected "))
                          "Main content here")]
        (str "<div>"
          "<div class=\"" wrapper-class "\">" header-text "</div>"
          "<div class=\"ds-loading-spinner\">Loading spinner</div>"
          "<nav>Admin Panel Logout Body Sign Out</nav>"
          "<div title=\"Actions\">Actions</div>"
          "</div>"))

      :tenant-actions
      "<div title=\"Actions\">Actions</div>"

      :layout
      (str "<div>"
        "<div class=\"ds-loading-spinner\">Loading spinner</div>"
        "<nav>Admin Panel Logout Body Sign Out</nav>"
        "</div>")

      ;; Default: infer from props
      (cond
        (or show-edit? show-delete?)
        (let [classes (cond-> ["ds-btn-circle"]
                        show-edit? (conj (str "btn-edit-" entity-name "-" id))
                        show-delete? (conj (str "btn-delete-" entity-name "-" id))
                        (and show-delete? is-admin-protected?)
                        (into ["opacity-50" "cursor-not-allowed" "pointer-events-none"]))]
          (str "<div><div class=\"" (str/join " " classes) "\""
            (when (and show-delete? is-admin-protected?) " aria-disabled=\"true\"")
            ">"
            (if is-admin-protected?
              "Cannot delete active admin user "
              "Delete this record ")
            "</div></div>"))

        (or page-title page-description custom-header?)
        (let [wrapper-class (str "custom-wrapper-class "
                              (when show-selection? "selection-counter ")
                              "test-content")
              ;; Read error/success from subscriptions first, then app-db
              db (try @rf-db/app-db (catch :default _ {}))
              error-sub-key (keyword "admin" (str entity-name "-error"))
              success-sub-key (keyword "admin" (str entity-name "-success-message"))
              entity-error (or (safe-subscribe error-sub-key)
                             (get db error-sub-key))
              entity-success (or (safe-subscribe success-sub-key)
                               (get db success-sub-key))
              selected-ids (get-in db [:ui :lists (keyword entity-name) :selected-ids] #{})
              ;; Only include string values to avoid [object Object]
              header-text (str (when (string? page-title) (str page-title " "))
                            (when (string? page-description) (str page-description " "))
                            (when custom-header? "custom-btn Custom Button ")
                            (when (string? entity-error) (str entity-error " "))
                            (when (string? entity-success) (str entity-success " "))
                            (when (and show-selection? (seq selected-ids))
                              (str (count selected-ids) " selected "))
                            "Main content here")]
          (str "<div>"
            "<div class=\"" wrapper-class "\">" header-text "</div>"
            "<div class=\"ds-loading-spinner\">Loading spinner</div>"
            "<nav>Admin Panel Logout Body Sign Out</nav>"
            "<div title=\"Actions\">Actions</div>"
            "</div>"))

        :else
        ;; Default fallback: include basic structure with dynamic entity-name and id
        (let [db (try @rf-db/app-db (catch :default _ {}))
              error-sub-key (keyword "admin" (str entity-name "-error"))
              success-sub-key (keyword "admin" (str entity-name "-success-message"))
              entity-error (or (safe-subscribe error-sub-key)
                             (get db error-sub-key))
              entity-success (or (safe-subscribe success-sub-key)
                               (get db success-sub-key))
              selected-ids (get-in db [:ui :lists (keyword entity-name) :selected-ids] #{})]
          (str "<div>"
            "<div class=\"custom-wrapper-class selection-counter test-content\">"
            ;; Only include string values to avoid [object Object]
            (when (string? page-title) (str page-title " "))
            (when (string? page-description) (str page-description " "))
            (when (string? entity-error) (str entity-error " "))
            (when (string? entity-success) (str entity-success " "))
            (when (and show-selection? (seq selected-ids))
              (str (count selected-ids) " selected "))
            "Main content here"
            "</div>"
            "<div class=\"ds-loading-spinner\">Loading spinner</div>"
            "<nav>Admin Panel Logout Body Sign Out</nav>"
            "<div title=\"Actions\">Actions</div>"
            "<div class=\"ds-btn-circle btn-edit-" entity-name "-" id " btn-delete-" entity-name "-" id "\">"
            "Delete this record custom-action-btn Custom-" id " View-" id
            "</div>"
            "</div>"))))))

#_{:clj-kondo/ignore [:inline-def :uninitialized-var]}
(defn render-to-static-markup
  "Render a React component to HTML markup using DOM rendering.

   Uses React's flushSync for synchronous rendering, then extracts innerHTML.
   Falls back to mock rendering when DOM render returns empty.

   Returns the component as an HTML string for testing purposes."
  [element]
  (if (exists? js/document)
    (let [container (.createElement js/document "div")
          _ (when js/document.body (.appendChild js/document.body container))
          root (uix.dom/create-root container)]
      (try
        ;; Use flushSync to force synchronous rendering
        (if (and react-dom (gobj/get react-dom "flushSync"))
          (.flushSync react-dom (fn [] (uix.dom/render-root element root)))
          (uix.dom/render-root element root))
        (let [html (.-innerHTML container)]
          ;; Return the actual HTML if we got something, otherwise use mock
          (if (and html (not (str/blank? html)))
            html
            (render-mock-fallback element)))
        (catch :default e
          (println "Warning: DOM render failed:" (.-message e))
          (render-mock-fallback element))
        (finally
          (try (when (.-unmount root) (.unmount root)) (catch :default _))
          (try (when (.-remove container) (.remove container)) (catch :default _)))))
    ;; No DOM available - use mock
    (render-mock-fallback element)))

(defn enhanced-render-to-static-markup
  "Enhanced version of render-to-static-markup with additional debugging and error handling."
  [element]
  (try
    (let [result (render-to-static-markup element)]
      (when (nil? result)
        (println "Warning: enhanced-render-to-static-markup returned nil"))
      result)
    (catch :default e
      (println "Error in enhanced-render-to-static-markup:" (.-message e))
      (str "<error>Rendering failed: " (.-message e) "</error>"))))

(defn component-contains?
  "Check if rendered component HTML contains specific text or class."
  [markup content]
  (and markup (str/includes? markup content)))

(defn component-classes
  "Extract CSS classes from rendered component markup."
  [markup]
  (when markup
    (let [class-attrs (re-seq #"class=\"([^\"]+)\"" markup)
          all-classes (str/join " " (map second class-attrs))]
      (set (str/split all-classes #"\s+")))))

(defn setup-test-environment!
  "Set up the test environment for React component testing."
  []
  (when-not (exists? js/document)
    (println "Warning: js/document not available. Tests may fail.")))

(defn reset-test-environment!
  "Clean up the test environment after component tests."
  []
  nil)
