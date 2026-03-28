(ns app.template.frontend.components.json-viewer
  "Reusable JSON viewer utilities and component.
   - json-copy-text: pretty JSON string for clipboard, with fallback
   - json-viewer: tree viewer with collapse/expand controls and indentation"
  (:require
    [uix.core :refer [$ defui use-state]]))

(defn- normalize-json
  "Return {:parsed normalized-json | nil, :json-str pretty-json | nil, :raw-str fallback-string | nil}.
   Accepts: JSON string, Clojure map/vector, JS object/array, or any other value."
  [data]
  (cond
    (string? data)
    (try
      (let [parsed (js/JSON.parse data)]
        {:parsed parsed
         :json-str (js/JSON.stringify parsed nil 2)
         :raw-str nil})
      (catch js/Error _
        {:parsed nil :json-str nil :raw-str data}))

    :else
    (try
      (let [json-ready (if (or (map? data) (vector? data) (seq? data) (set? data))
                         (clj->js data)
                         data)
            json-str (js/JSON.stringify json-ready nil 2)]
        (if (some? json-str)
          {:parsed (js/JSON.parse json-str)
           :json-str json-str
           :raw-str nil}
          {:parsed nil :json-str nil :raw-str (str data)}))
      (catch js/Error _
        {:parsed nil :json-str nil :raw-str (str data)}))))

(defn- js-array? [value]
  (array? value))

(defn- js-object? [value]
  (and (some? value)
    (= "object" (goog/typeOf value))
    (not (js-array? value))))

(defn- json-container? [value]
  (or (js-array? value) (js-object? value)))

(defn- container-brackets [value]
  (if (js-array? value)
    ["[" "]"]
    ["{" "}"]))

(defn- bracket-class [value]
  (if (js-array? value)
    "text-blue-600 dark:text-blue-400 font-semibold"
    "text-orange-600 dark:text-orange-400 font-semibold"))

(defn- container-entries [value]
  (cond
    (js-array? value)
    (mapv (fn [idx child] [idx child])
      (range (alength value))
      (array-seq value))

    (js-object? value)
    (mapv (fn [k] [k (aget value k)])
      (array-seq (js/Object.keys value)))

    :else
    []))

(defn- pluralize [n label]
  (str n " " label (when (not= 1 n) "s")))

(defn- container-summary [value]
  (let [entry-count (count (container-entries value))]
    (if (js-array? value)
      (pluralize entry-count "item")
      (pluralize entry-count "key"))))

(defn- value-class [value]
  (cond
    (string? value) "text-emerald-700 dark:text-emerald-300"
    (number? value) "text-amber-700 dark:text-amber-300"
    (boolean? value) "text-violet-700 dark:text-violet-300"
    (nil? value) "text-base-content/50"
    :else "text-base-content"))

(defn- render-json-key [key-name]
  ($ :span {:class "min-w-0"}
    ($ :span {:class "text-sky-700 dark:text-sky-300"}
      (pr-str (str key-name)))
    ($ :span {:class "text-base-content/50"} ": ")))

(defn- render-json-primitive [value]
  ($ :span {:class (value-class value)}
    (cond
      (string? value) (pr-str value)
      (nil? value) "null"
      :else (str value))))

(defn- collect-collapsible-paths [value path]
  (if (json-container? value)
    (let [entries (container-entries value)]
      (if (seq entries)
        (reduce (fn [paths [child-key child-value]]
                  (into paths (collect-collapsible-paths child-value (conj path child-key))))
          #{path}
          entries)
        #{}))
    #{}))

(defn- render-json-node
  [value {:keys [path depth key-name is-last? collapsed-paths set-collapsed-paths! indent-size]}]
  (let [node-key (str "json-node-" (pr-str path))
        row-style {:paddingLeft (str (* depth indent-size) "px")}
        row-class "flex items-start gap-2 rounded px-2 py-0.5 hover:bg-base-200/50 min-w-0"
        comma (when-not is-last?
                ($ :span {:class "text-base-content/40"} ","))
        show-key? (some? key-name)]
    (if (json-container? value)
      (let [entries (container-entries value)
            empty? (zero? (count entries))
            collapsed? (contains? collapsed-paths path)
            [open close] (container-brackets value)
            bracket-classes (bracket-class value)
            toggle! (fn [e]
                      (.preventDefault e)
                      (.stopPropagation e)
                      (set-collapsed-paths! (fn [paths]
                                              (if (contains? paths path)
                                                (disj paths path)
                                                (conj paths path)))))]
        (cond
          empty?
          ($ :div {:key node-key}
            ($ :div {:class row-class :style row-style}
              ($ :span {:class "inline-flex w-4 flex-shrink-0"})
              (when show-key?
                (render-json-key key-name))
              ($ :span {:class bracket-classes} open)
              ($ :span {:class bracket-classes} close)
              comma))

          collapsed?
          ($ :div {:key node-key}
            ($ :div {:class row-class :style row-style}
              ($ :button {:type "button"
                          :class "inline-flex w-4 flex-shrink-0 justify-center text-base-content/60 hover:text-base-content"
                          :aria-label "Expand JSON node"
                          :on-click toggle!}
                "▸")
              (when show-key?
                (render-json-key key-name))
              ($ :span {:class bracket-classes} open)
              ($ :span {:class "text-base-content/60 italic"}
                (str " " (container-summary value) " "))
              ($ :span {:class bracket-classes} close)
              comma))

          :else
          ($ :div {:key node-key :class "w-full"}
            ($ :div {:class row-class :style row-style}
              ($ :button {:type "button"
                          :class "inline-flex w-4 flex-shrink-0 justify-center text-base-content/60 hover:text-base-content"
                          :aria-label "Collapse JSON node"
                          :on-click toggle!}
                "▾")
              (when show-key?
                (render-json-key key-name))
              ($ :span {:class bracket-classes} open))
            (map-indexed
              (fn [idx [child-key child-value]]
                (render-json-node child-value
                  {:path (conj path child-key)
                   :depth (inc depth)
                   :key-name (when (js-object? value) child-key)
                   :is-last? (= idx (dec (count entries)))
                   :collapsed-paths collapsed-paths
                   :set-collapsed-paths! set-collapsed-paths!
                   :indent-size indent-size}))
              entries)
            ($ :div {:class row-class :style row-style}
              ($ :span {:class "inline-flex w-4 flex-shrink-0"})
              ($ :span {:class bracket-classes} close)
              comma))))

      ($ :div {:key node-key}
        ($ :div {:class row-class :style row-style}
          ($ :span {:class "inline-flex w-4 flex-shrink-0"})
          (when show-key?
            (render-json-key key-name))
          (render-json-primitive value)
          comma)))))

(defn json-copy-text
  "Best-effort pretty JSON for clipboard; falls back to (str data)."
  [data]
  (let [{:keys [json-str raw-str]} (normalize-json data)]
    (or json-str raw-str "")))

(defui json-viewer
  "Render JSON as a tree viewer with collapse controls when valid; otherwise raw pre/code.
   Props:
   - :data any
   - :class optional extra classes for the viewer wrapper
   - :indent-size optional indentation width in pixels (default 16)"
  [{:keys [data class indent-size]
    :or {indent-size 16}}]
  (let [{:keys [parsed json-str raw-str]} (normalize-json data)
        [collapsed-paths set-collapsed-paths!] (use-state #{})
        collapsible-paths (if json-str
                            (disj (collect-collapsible-paths parsed []) [])
                            #{})]
    (if json-str
      ($ :div {:class (str "w-full font-mono text-xs leading-relaxed " class)}
        (when (seq collapsible-paths)
          ($ :div {:class "mb-2 flex items-center justify-end gap-2 border-b border-base-200/80 pb-2"}
            ($ :button {:type "button"
                        :class "ds-btn ds-btn-ghost ds-btn-xs"
                        :disabled (= collapsed-paths collapsible-paths)
                        :on-click (fn [e]
                                    (.preventDefault e)
                                    (set-collapsed-paths! collapsible-paths))}
              "Collapse all")
            ($ :button {:type "button"
                        :class "ds-btn ds-btn-ghost ds-btn-xs"
                        :disabled (empty? collapsed-paths)
                        :on-click (fn [e]
                                    (.preventDefault e)
                                    (set-collapsed-paths! #{}))}
              "Expand all")))
        ($ :div {:class "space-y-0.5"}
          (render-json-node parsed
            {:path []
             :depth 0
             :key-name nil
             :is-last? true
             :collapsed-paths collapsed-paths
             :set-collapsed-paths! set-collapsed-paths!
             :indent-size indent-size})))
      ($ :pre {:class (str "text-xs leading-relaxed text-base-content/80 w-full whitespace-pre-wrap break-words " class)}
        ($ :code {:class "w-full"}
          (or raw-str ""))))))
