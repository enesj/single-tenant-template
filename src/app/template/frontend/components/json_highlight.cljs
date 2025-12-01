(ns app.template.frontend.components.json-highlight
  "JSON syntax highlighting component for UI display"
  (:require
    [clojure.string :as str]
    [taoensso.timbre :as log]
    [uix.core :refer [$ defui use-state]]))

;; =============================================================================
;; JSON Syntax Highlighting
;; ============================================================================

(defn- json-syntax-highlight-line
  "Apply syntax highlighting to a single line of JSON text"
  [line]
  (map-indexed
    (fn [char-idx char]
      (case char
        \{ ($ :span {:key (str "brace-" char-idx) :class "text-orange-600 dark:text-orange-400 font-semibold"} char)
        \} ($ :span {:key (str "brace-" char-idx) :class "text-orange-600 dark:text-orange-400 font-semibold"} char)
        \[ ($ :span {:key (str "bracket-" char-idx) :class "text-blue-600 dark:text-blue-400 font-semibold"} char)
        \] ($ :span {:key (str "bracket-" char-idx) :class "text-blue-600 dark:text-blue-400 font-semibold"} char)
        \, ($ :span {:key (str "comma-" char-idx) :class "text-gray-500 dark:text-gray-400"} char)
        \: ($ :span {:key (str "colon-" char-idx) :class "text-gray-600 dark:text-gray-400 mx-1"} char)
        \" ($ :span {:key (str "quote-" char-idx) :class "text-green-600 dark:text-green-400"} char)
        ($ :span {:key (str "char-" char-idx)} char)))
    (map str line)))

(defui syntax-highlighted-json
  "Render JSON string with syntax highlighting for UI display.

   Args:
     json-str: JSON string to highlight

   Returns:
     UIX component with syntax-highlighted JSON"
  [{:keys [json-str class max-height]
    :or {class "text-sm font-mono leading-relaxed"
         max-height "max-h-80"}}]
  (let [formatted-json (try
                         (let [parsed (js/JSON.parse json-str)]
                           (js/JSON.stringify parsed nil 2))
                         (catch js/Error _
                           json-str))]
    ($ :div {:class class}
      (map-indexed
        (fn [idx line]
          ($ :div {:key (str "line-" idx) :class "hover:bg-base-200/50 -mx-2 px-2 py-0.5 rounded transition-colors"}
            (json-syntax-highlight-line line)))
        (.split formatted-json "\n")))))

;; =============================================================================
;; JSON Formatting and Display Utilities
;; ============================================================================

(defn format-json-for-display
  "Format JSON value for display with proper indentation.

   Args:
     json-value: JSON value (object, string, or primitive)

   Returns:
     Formatted JSON string"
  [json-value]
  (try
    (let [json-str (cond
                     (string? json-value) json-value
                     :else (js/JSON.stringify (clj->js json-value)))]
      (if (str/blank? json-str)
        "{}"
        (let [parsed (js/JSON.parse json-str)]
          (js/JSON.stringify parsed nil 2))))
    (catch js/Error e
      (log/warn "Failed to format JSON for display:" e)
      (str json-value))))

(defn safe-parse-json
  "Safely parse JSON string with error handling.

   Args:
     json-str: JSON string to parse

   Returns:
     Parsed JSON object or original string if parsing fails"
  [json-str]
  (when (and json-str (not (str/blank? json-str)))
    (try
      (js/JSON.parse json-str)
      (catch js/Error e
        (log/warn "Failed to parse JSON string:" e)
        json-str))))

(defn safe-stringify-json
  "Safely convert value to JSON string with error handling.

   Args:
     value: Value to convert to JSON
     pretty?: Whether to format with indentation (default: false)

   Returns:
     JSON string or original value if stringification fails"
  ([value]
   (safe-stringify-json value false))
  ([value pretty?]
   (try
     (cond
       (string? value) value
       pretty? (js/JSON.stringify (clj->js value) nil 2)
       :else (js/JSON.stringify (clj->js value)))
     (catch js/Error e
       (log/warn "Failed to stringify JSON value:" e)
       (str value)))))

;; =============================================================================
;; JSON Validation
;; ============================================================================

(defn valid-json?
  "Check if a string contains valid JSON.

   Args:
     json-str: String to validate

   Returns:
     true if valid JSON, false otherwise"
  [json-str]
  (when (and json-str (not (str/blank? json-str)))
    (try
      (js/JSON.parse json-str)
      true
      (catch js/Error _
        false))))

;; =============================================================================
;; UI Component Helpers
;; ============================================================================

(defui json-display-card
  "Create a card component for displaying JSON with syntax highlighting.

   Args:
     json-value: JSON value to display
     opts: Options map with keys:
       :title - Card title (default: \"JSON Data\")
       :class - Additional CSS classes
       :show-copy? - Whether to show copy button (default: true)
       :max-height - Max height for scrollable area (default: \"max-h-80\")"
  [{:keys [json-value title class show-copy? max-height]
    :or {title "JSON Data"
         show-copy? true
         max-height "max-h-80"}}]
  (let [json-str (format-json-for-display json-value)
        [copied? set-copied!] (use-state false)]

    ($ :div {:class (str "ds-card ds-card-bordered bg-base-100 shadow-lg p-4 " class)}
      ;; Header
      ($ :div {:class "flex items-center justify-between gap-2 mb-3"}
        ($ :div {:class "flex items-center gap-2"}
          ($ :div {:class "w-1 h-4 rounded-full bg-primary"})
          ($ :h3 {:class "text-base font-semibold text-base-content"} title))
        (when show-copy?
          ($ :button
            {:class "ds-btn ds-btn-xs ds-btn-ghost ds-btn-circle opacity-60 hover:opacity-100 transition-opacity"
             :title "Copy to clipboard"
             :on-click (fn [e]
                         (.stopPropagation e)
                         (.writeText js/navigator.clipboard json-str)
                         (set-copied! true)
                         (js/setTimeout #(set-copied! false) 2000))}
            (if copied?
              ($ :svg {:class "w-3 h-3 text-green-600" :fill "none" :stroke "currentColor" :view-box "0 0 24 24"}
                ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M5 13l4 4L19 7"}))
              ($ :svg {:class "w-3 h-3" :fill "none" :stroke "currentColor" :view-box "0 0 24 24"}
                ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                          :d "M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"}))))))

      ;; JSON Content
      ($ :div {:class (str "bg-base-200/50 rounded-md p-4 " max-height " overflow-y-auto border border-base-200")}
        ($ :pre {:class "text-xs leading-relaxed"}
          ($ syntax-highlighted-json {:json-str json-str}))))))
