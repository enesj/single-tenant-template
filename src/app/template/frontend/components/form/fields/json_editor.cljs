(ns app.template.frontend.components.form.fields.json-editor
  "JSON editor field component for jsonb values"
  (:require
    [app.template.frontend.components.common :as common]
    [app.template.frontend.components.form.validation :as validation]
    [app.template.frontend.components.json-highlight :refer [syntax-highlighted-json format-json-for-display]]
    [app.template.frontend.components.copy-button :refer [copy-to-clipboard-button]]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]
    [uix.core :refer [$ defui use-state]]))

(defn- safe-json-stringify
  "Safely stringify a value to JSON"
  [value]
  (try
    (if (string? value)
      value
      (js/JSON.stringify (clj->js value) nil 2))
    (catch :default e
      (log/warn "Failed to stringify JSON value:" e)
      (str value))))

(defn- safe-json-parse
  "Safely parse a JSON string"
  [json-str]
  (try
    (if (str/blank? json-str)
      {}
      (let [parsed (js->clj (js/JSON.parse json-str) :keywordize-keys true)]
        (log/debug "JSON parsed:" {:input json-str :result parsed})
        parsed))
    (catch :default e
      (log/warn "Failed to parse JSON:" e)
      ;; Rethrow so callers can present errors in the UI instead of
      ;; swallowing them and only logging to the console.
      (throw e))))

(defui json-editor
  [{:keys [id label error required class inline on-change value fork-errors] :as all-props}]

  (let [base-class "ds-textarea ds-textarea-primary font-mono text-sm"
        label-class "ds-label"
        ;; Get errors from either the direct error prop or from Fork validation errors
        field-error (or error (when fork-errors (validation/get-field-errors fork-errors (keyword id))))
        ;; Convert value to JSON string for display
        initial-json-str (safe-json-stringify value)
        [json-str, set-json-str] (use-state initial-json-str)
        [parse-error, set-parse-error] (use-state nil)
        ;; Always enable preview and copy features by default for better UX
        [show-preview, set-show-preview!] (use-state true)

        handle-change (fn [e]
                        (let [new-value (.. e -target -value)]
                          (log/debug "JSON editor change:" {:id id :new-value new-value})
                          (js/console.log "JSON Editor Change:" {:id id :new-value new-value})
                          (set-json-str new-value)
                          ;; Try to parse and validate the JSON
                          (try
                            (let [parsed (safe-json-parse new-value)]
                              (set-parse-error nil)
                              (log/debug "JSON editor on-change calling with parsed value:" {:id id :parsed parsed})
                              ;; Always mark JSON fields as dirty when they change
                              (rf/dispatch [:app.template.frontend.events.form/set-dirty-fields
                                            (keyword (first (str/split id #"\.")))
                                            #{(keyword id)}])
                              (on-change parsed))
                            (catch :default e
                              (let [error-msg (.-message e)
                                    user-friendly-error (cond
                                                          (str/includes? error-msg "Unterminated string") "Unterminated string - check for missing quotes"
                                                          (str/includes? error-msg "Unexpected end") "Unexpected end of JSON - check for missing braces/brackets"
                                                          (str/includes? error-msg "Unexpected token") "Unexpected token - check for commas, braces, or brackets"
                                                          :else "Invalid JSON syntax")]
                                (set-parse-error user-friendly-error)
                                (log/warn "JSON parse error in editor:" {:id id :error error-msg :user-friendly user-friendly-error :value new-value})
                                (js/console.error "JSON Editor Error:" user-friendly-error)
                                ;; Still call on-change with the string value
                                (on-change new-value))))))

        display-error (or field-error parse-error)

        ;; Debug: log error state on every render
        _ (js/console.log "JSON Editor Render State:" {:id id :json-str json-str :parse-error parse-error :field-error field-error :display-error display-error})
        ;; Debug: log when we have errors
        _ (when display-error
            (log/debug "JSON Editor displaying error:" {:id id :error display-error :field-error field-error :parse-error parse-error})
            (js/console.log "JSON Editor Error Display:", display-error))
        ;; Debug: add a visible indicator when we have errors
        _ (when display-error
            (js/setTimeout
              (fn []
                (when-let [el (js/document.getElementById id)]
                  (let [debug-div (js/document.createElement "div")]
                    (set! (.-innerHTML debug-div) (str "🔥 JSON ERROR: " display-error))
                    (set! (.-style debug-div) "background: red; color: white; padding: 5px; margin: 5px 0; font-weight: bold;")
                    (.insertBefore (.parentElement el) debug-div el))))
              100))
        ;; Get formatted JSON for display (handles parsing errors gracefully)
        formatted-json (format-json-for-display json-str)
        ;; Check if current JSON is valid for preview display
        json-valid? (and (not display-error) (seq json-str))

        props (-> all-props
                (assoc
                  :class (str base-class " " class (when display-error " border-error"))
                  :value json-str
                  :on-change handle-change
                  :rows 6
                  :placeholder "Enter JSON object: {\"key\": \"value\"}")
                (dissoc :error :input-type :disabled? :validate-server? :inline :fork-errors
                  :formId :form-id :label :required :on-change-raw :show-preview? :show-copy?))]

    ($ :div {:class (str "mb-4" (if inline " flex flex-row items-start gap-4"
                                  " flex flex-col items-start gap-4"))}
      ($ common/label {:text (str label " (JSON)")
                       :for id
                       :required required
                       :class (str label-class (when inline " mb-0 min-w-[150px] text-left"))})

      ($ :div {:class (when inline "flex-1 text-left")}
        ;; Editor textarea with copy button
        ($ :div {:class "relative"}
          ($ :textarea props)
          ;; Copy button in top-right corner
          (when (seq json-str)
            ($ copy-to-clipboard-button
              {:text json-str
               :class "absolute top-2 right-2 z-10 opacity-60 hover:opacity-100 transition-opacity"
               :title "Copy JSON content"})))

        ;; Error display (enhanced visibility)
        (when display-error
          ($ :div {:class "mt-2 p-3 border-2 border-error/30 bg-error/10 rounded-lg text-sm font-bold shadow-sm"
                   :role "alert"
                   :style {:background-color "#fef2f2" :border-color "#ef4444" :color "#dc2626"}}
            ($ :div {:class "flex items-center gap-2"}
              ($ :span {:class "text-lg"} "⚠️")
              ($ :span {} display-error))
            ;; Debug info
            ($ :div {:class "text-xs mt-1 opacity-60"}
              ($ :span {} "JSON Parse Error"))))

        ;; Help text
        ($ :div {:class "text-xs text-gray-500 mt-1 flex justify-between items-center"}
          ($ :span {} "Enter valid JSON format, e.g., {\"setting\": \"value\"}")

          ;; Preview toggle button (visible, button-styled)
          (when (seq json-str)
            ($ :button {:type "button"
                        :class "ds-btn ds-btn-ghost ds-btn-sm text-primary"
                        :on-click #(set-show-preview! (not show-preview))}
              (if show-preview "Hide Preview" "Show Preview"))))

        ;; Syntax-highlighted preview section
        (when (and show-preview json-valid? (seq formatted-json))
          ($ :div {:class "mt-3 border border-base-300 rounded-lg overflow-hidden"}
            ;; Preview header
            ($ :div {:class "bg-base-200 px-3 py-2 flex justify-between items-center border-b border-base-300"}
              ($ :span {:class "text-sm font-medium text-base-content/70"} "JSON Preview")
              ;; Copy button for formatted JSON
              ($ copy-to-clipboard-button
                {:text formatted-json
                 :size "w-4 h-4"
                 :title "Copy formatted JSON"}))

            ;; Syntax-highlighted content
            ($ :div {:class "bg-base-50 p-3 max-h-60 overflow-y-auto"}
              ($ syntax-highlighted-json
                {:json-str formatted-json
                 :class "text-xs leading-relaxed"}))))))))

;; =============================================================================
;; Enhanced JSON Editor Usage Examples
;; =============================================================================
;;
;; Basic usage (backward compatible):
;; ($ json-editor
;;   {:id "config-json"
;;    :label "Configuration"
;;    :value {:setting1 "value1" :nested {:key "value"}}
;;    :on-change (fn [parsed-value]
;;                  (rf/dispatch [:update-config parsed-value]))})
;;
;; Enhanced usage with features (enabled by default):
;; ($ json-editor
;;   {:id "advanced-config"
;;    :label "Advanced Configuration"
;;    :show-preview? false     ; Optional: Disable preview (enabled by default)
;;    :show-copy? false        ; Optional: Disable copy buttons (enabled by default)
;;    :value initial-config
;;    :on-change handle-config-change})
;;
;; Features (enabled by default):
;; - Syntax highlighting preview : Shows color-coded JSON with toggle button
;; - Copy-to-clipboard buttons : Floating copy button for both raw and formatted JSON
;; - Enhanced error handling : Invalid JSON shows helpful error messages
;; - Responsive design : Works in both inline and block layouts
;; - Visual feedback : Syntax highlighting with color-coded JSON structure
;; - Dual copy options : Copy raw JSON content or formatted/syntax-highlighted version
;;
;; To disable features:
;; - :show-preview? false : Hide syntax-highlighted preview section
;; - :show-copy? false : Hide copy-to-clipboard buttons
;;
;; The component integrates seamlessly with the shared components:
;; - syntax-highlighted-json : For beautiful JSON display with color coding
;; - copy-to-clipboard-button : For one-click copying with visual feedback
;; - format-json-for-display : For consistent JSON formatting across the app
