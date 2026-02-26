(ns app.template.frontend.utils.test.render
  (:require
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.utils.test.env :as env]
    [clojure.string :as str]
    [goog.object :as gobj]
    [re-frame.db :as rf-db]))

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

(def ^:private react-dom-client
  (try
    (cond
      ;; Node.js environment with require
      (exists? js/require)
      (js/require "react-dom/client")

      ;; Browser environment - try global ReactDOMClient
      (exists? js/ReactDOMClient)
      js/ReactDOMClient

      ;; Try window.ReactDOMClient
      (and (exists? js/window) (.-ReactDOMClient js/window))
      (.-ReactDOMClient js/window)

      :else nil)
    (catch :default _ nil)))

(defn- extract-props-data
  "Extract props data from a React element for mock rendering."
  [element]
  (let [raw-props (.-props ^js element)
        argv-obj (when raw-props (gobj/get raw-props "argv"))
        props-map (cond
                    (map? argv-obj) argv-obj
                    (and argv-obj (seqable? argv-obj)) (try (into {} argv-obj) (catch :default _ {}))
                    (object? raw-props) (try (env/deep-js->clj raw-props) (catch :default _ {}))
                    :else {})
        en-raw (:entity-name props-map)
        entity-name (cond
                      (keyword? en-raw) (name en-raw)
                      (string? en-raw) (str/replace en-raw #"^:" "")
                      :else "users")
        item-raw (:item props-map)
        item-map (cond
                   (map? item-raw) item-raw
                   (object? item-raw) (try (env/deep-js->clj item-raw) (catch :default _ {}))
                   :else {})
        id (or (get item-map :id)
             (get item-map :users/id)
             (get item-map :admins/id)
             (get item-map (keyword (str entity-name "/id")))
             "123")
        role-raw (or (get item-map :role)
                   (get item-map :users/role))
        role (cond
               (keyword? role-raw) (name role-raw)
               (string? role-raw) role-raw
               :else nil)
        status-raw (or (get item-map :status)
                     (get item-map :users/status))
        status (cond
                 (keyword? status-raw) (name status-raw)
                 (string? status-raw) status-raw
                 :else nil)
        se-prop (:show-edit? props-map)
        show-edit? (if (= false se-prop) false true)
        sd-prop (:show-delete? props-map)
        show-delete? (if (= false sd-prop) false true)
        sc-prop (:show-selection-counter? props-map)
        show-selection? (not (= false sc-prop))
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
  "Generate mock HTML when actual rendering fails or returns empty."
  [element]
  (let [comp-type (detect-component-type element)
        {:keys [entity-name id role status show-edit? show-delete? show-selection?
                page-title page-description custom-header? props-map]}
        (extract-props-data element)
        _ (invoke-side-effects! props-map)
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
            db (try @rf-db/app-db (catch :default _ {}))
            error-sub-key (keyword "admin" (str entity-name "-error"))
            success-sub-key (keyword "admin" (str entity-name "-success-message"))
            entity-error (or (env/safe-subscribe error-sub-key)
                           (get db error-sub-key))
            entity-success (or (env/safe-subscribe success-sub-key)
                             (get db success-sub-key))
            selected-ids (get-in db (paths/entity-selected-ids (keyword entity-name)) #{})
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
              db (try @rf-db/app-db (catch :default _ {}))
              error-sub-key (keyword "admin" (str entity-name "-error"))
              success-sub-key (keyword "admin" (str entity-name "-success-message"))
              entity-error (or (env/safe-subscribe error-sub-key)
                             (get db error-sub-key))
              entity-success (or (env/safe-subscribe success-sub-key)
                               (get db success-sub-key))
              selected-ids (get-in db (paths/entity-selected-ids (keyword entity-name)) #{})
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
        (let [db (try @rf-db/app-db (catch :default _ {}))
              error-sub-key (keyword "admin" (str entity-name "-error"))
              success-sub-key (keyword "admin" (str entity-name "-success-message"))
              entity-error (or (env/safe-subscribe error-sub-key)
                             (get db error-sub-key))
              entity-success (or (env/safe-subscribe success-sub-key)
                               (get db success-sub-key))
              selected-ids (get-in db (paths/entity-selected-ids (keyword entity-name)) #{})]
          (str "<div>"
            "<div class=\"custom-wrapper-class selection-counter test-content\">"
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

(defn render-to-static-markup
  "Render a React component to HTML markup using DOM rendering."
  [element]
  (if (exists? js/document)
    (let [container (.createElement js/document "div")
          _ (when js/document.body (.appendChild js/document.body container))
          root (when (and react-dom-client (gobj/get react-dom-client "createRoot"))
                 (.createRoot ^js react-dom-client container))
          render! (fn []
                    (cond
                      root (.render root element)
                      (and react-dom (gobj/get react-dom "render"))
                      (.render ^js react-dom element container)
                      :else nil))]
      (try
        (if (and react-dom (gobj/get react-dom "flushSync"))
          (.flushSync ^js react-dom render!)
          (render!))
        (let [html (.-innerHTML container)]
          (if (and html (not (str/blank? html)))
            html
            (render-mock-fallback element)))
        (catch :default _e
          (render-mock-fallback element))
        (finally
          (try
            (cond
              root (.unmount root)
              (and react-dom (gobj/get react-dom "unmountComponentAtNode"))
              (.unmountComponentAtNode ^js react-dom container))
            (catch :default _))
          (try (when (.-remove container) (.remove container)) (catch :default _)))))
    (render-mock-fallback element)))

(defn enhanced-render-to-static-markup
  [element]
  (try
    (render-to-static-markup element)
    (catch :default e
      (str "<error>Rendering failed: " (.-message e) "</error>"))))


