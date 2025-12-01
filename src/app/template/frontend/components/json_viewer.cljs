(ns app.template.frontend.components.json-viewer
  "Reusable JSON viewer utilities and component.
   - json-copy-text: pretty JSON string for clipboard, with fallback
   - json-viewer: highlight valid JSON; show raw text otherwise"
  (:require
    [uix.core :refer [$ defui]]
    [app.template.frontend.components.json-highlight :refer [syntax-highlighted-json]]))

(defn- normalize-json
  "Return {:json-str pretty-json | nil, :raw-str fallback-string | nil}
   Accepts: JSON string, Clojure map/vector, JS object/array, or any other value."
  [data]
  (cond
    (string? data)
    (try
      (let [parsed (js/JSON.parse data)]
        {:json-str (js/JSON.stringify parsed nil 2) :raw-str nil})
      (catch js/Error _
        {:json-str nil :raw-str data}))

    (or (map? data) (vector? data))
    (try
      {:json-str (js/JSON.stringify (clj->js data) nil 2) :raw-str nil}
      (catch js/Error _
        {:json-str nil :raw-str (str data)}))

    :else {:json-str nil :raw-str (str data)}))

(defn json-copy-text
  "Best-effort pretty JSON for clipboard; falls back to (str data)."
  [data]
  (let [{:keys [json-str raw-str]} (normalize-json data)]
    (or json-str raw-str "")))

(defui json-viewer
  "Render JSON with syntax highlighting when valid; otherwise raw pre/code.
   Props:
   - :data any
   - :class optional extra classes for pre/code wrapper"
  [{:keys [data class]}]
  (let [{:keys [json-str raw-str]} (normalize-json data)]
    (if json-str
      ($ syntax-highlighted-json {:json-str json-str})
      ($ :pre {:class (str "text-xs leading-relaxed text-base-content/80 w-full whitespace-pre-wrap break-words " class)}
        ($ :code {:class "w-full"}
          (or raw-str ""))))))
