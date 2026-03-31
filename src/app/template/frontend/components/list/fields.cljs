(ns app.template.frontend.components.list.fields
  (:require
    [app.template.frontend.utils.timestamp :as timestamp]
    [clojure.string :as str]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

(defn- format-timestamp
  "Format a timestamp value for display using canonical Created-style rendering."
  [value]
  (timestamp/render-timestamp value {:show-seconds? true
                                     :highlight-seconds? true}))

(defui select-field-value [{:keys [field value]}]
  (let [raw-options (:options field)
        is-dynamic-options? (and (vector? raw-options)
                              (= 2 (count raw-options))
                              (every? #(keyword? %) raw-options))
        dynamic-options (use-subscribe [:app.template.frontend.components.common/select-options
                                        (if is-dynamic-options? (first raw-options) :default)
                                        (if is-dynamic-options? (second raw-options) :default)])
        options (if is-dynamic-options?
                  dynamic-options
                  raw-options)
        option (first (filter #(= (:value %) value) options))
        label (or (:label option)
                  ;; Fallback: when options have not been loaded yet
                  ;; (e.g., user-scoped FKs like receipts), show the
                  ;; raw value instead of an empty cell.
                (when (some? value) (str value))
                "")]
    ($ :span label)))

(defn- truncate-text
  "Truncates text to specified length with ellipsis if needed."
  [text max-length]
  (when text
    (let [text-str (str text)]
      (if (<= (count text-str) max-length)
        text-str
        (str (subs text-str 0 max-length) "...")))))

(defui truncated-text-value
  "Component that renders text with truncation and hover tooltip for full content."
  [{:keys [text max-length field-type _field-id]}]
  (let [text-str (str (if (keyword? text) (name text) (or text "")))
        needs-truncation? (> (count text-str) max-length)
        display-text (truncate-text text-str max-length)
        ;; JSON detection based only on field type from database schema
        is-json? (or (= field-type "json")
                   (= field-type "jsonb"))]
    (if needs-truncation?
      ($ :span {:title text-str
                :class (str "cursor-help truncate-text "
                         (when is-json? "json-indicator"))
                :style {:display "block"
                        :width "100%"
                        :min-width "0"
                        :max-width (str "min(100%, " (* max-length 0.8) "ch)")
                        :overflow "hidden"
                        :text-overflow "ellipsis"
                        :white-space "nowrap"}}
        display-text
        (when is-json?
          ($ :span {:class "ml-1 text-xs text-gray-500"} "📄")))
      ($ :span display-text
        (when is-json?
          ($ :span {:class "ml-1 text-xs text-gray-500"} "📄"))))))

(defn- titleize-status
  [s]
  (->> (str/split (str s) #"[\s_-]+")
    (remove str/blank?)
    (map str/capitalize)
    (str/join " ")))

(defn- status->badge-variant
  [status-lower]
  (cond
    (contains? #{"posted"} status-lower)
    "ds-badge-primary"

    (contains? #{"active" "verified" "complete" "success" "extracted" "approved"} status-lower)
    "ds-badge-success"

    (contains? #{"review_required" "needs-review" "need-review" "needs review" "review" "warning"} status-lower)
    "ds-badge-warning"

    (contains? #{"failed" "error" "cancelled" "canceled"} status-lower)
    "ds-badge-error"

    (contains? #{"refining"} status-lower)
    "ds-badge-secondary"

    (contains? #{"uploaded" "pending" "parsing" "parsed" "extracting" "processing" "in-progress"} status-lower)
    "ds-badge-info"

    (contains? #{"inactive" "suspended" "archived" "purged"} status-lower)
    "ds-badge-ghost"

    :else
    "ds-badge-outline"))

(defn- normalize-field-key
  [field-key]
  (some-> (cond
            (keyword? field-key) (name field-key)
            (string? field-key) field-key
            (some? field-key) (str field-key)
            :else nil)
    (str/replace "_" "-")
    str/lower-case))

(defn- lookup-item-value
  [item field-key]
  (when (and (map? item) field-key)
    (when-let [normalized-key (normalize-field-key field-key)]
      (let [db-key (str/replace normalized-key "-" "_")
            candidates (remove nil? [field-key
                                     (keyword normalized-key)
                                     (keyword db-key)
                                     normalized-key
                                     db-key])]
        (or (some #(get item %) candidates)
          (some (fn [[k v]]
                  (when (and (keyword? k)
                          (= normalized-key (normalize-field-key (name k))))
                    v))
            item))))))

(defn normalize-formatter-id
  [formatter]
  (normalize-field-key formatter))

(defn resolve-backlog-badge
  "Resolve backlog formatter value into a badge label+variant map.

  Returns nil when the formatter is not one of the backlog badge formatters."
  [formatter value]
  (let [formatter-id (normalize-formatter-id formatter)
        value-str (some-> value str str/trim)
        value-lower (some-> value-str str/lower-case)]
    (case formatter-id
      "backlog-status-badge"
      {:label (if (seq value-str) (titleize-status value-str) "Unknown")
       :variant (cond
                  (contains? #{"waiting"} value-lower) "ds-badge-ghost"
                  (contains? #{"in progress" "in progres" "in-progress"} value-lower) "ds-badge-info"
                  (contains? #{"completed"} value-lower) "ds-badge-success"
                  (contains? #{"need improvements" "need improvments" "needs improvement" "needs improvements"} value-lower) "ds-badge-warning"
                  :else "ds-badge-outline")}

      "backlog-type-badge"
      {:label (if (seq value-str) (titleize-status value-str) "Unknown")
       :variant (cond
                  (contains? #{"issue"} value-lower) "ds-badge-error"
                  (contains? #{"feature"} value-lower) "ds-badge-success"
                  (contains? #{"refactoring"} value-lower) "ds-badge-warning"
                  (contains? #{"review"} value-lower) "ds-badge-info"
                  (contains? #{"improvement" "improvment"} value-lower) "ds-badge-secondary"
                  :else "ds-badge-outline")}

      nil)))

(defn- render-badge
  [{:keys [label variant]}]
  ($ :span {:class (str "ds-badge uppercase tracking-wide text-xs px-3 py-1 rounded-full border shadow-sm "
                     variant)}
    label))

(defn get-field-display-value
  "Gets the display value for a field, handling select fields specially and truncating long text content."
  ([field value]
   (get-field-display-value field value nil))
  ([field value item]
   (let [field-id (:id field)
         _normalized-field-id (normalize-field-key field-id)
         display-source-field (:display-source-field field)
         display-value (or (lookup-item-value item display-source-field) value)
         input-type (:input-type field)
         field-type (:type field)
         text-value (str (if (keyword? display-value)
                           (name display-value)
                           (or display-value "")))
         value-lower (some-> display-value str str/trim str/lower-case)
         formatter-id (normalize-formatter-id (:formatter field))
         formatter-badge (resolve-backlog-badge (:formatter field) display-value)
         raw-status-str (some-> value str str/trim not-empty)
         raw-status-lower (some-> raw-status-str str/lower-case)
         badge-label (or (some-> display-value str str/trim not-empty)
                       raw-status-str)
         status-badge (when (= formatter-id "status-badge")
                        (when badge-label
                          {:label badge-label
                           :variant (status->badge-variant raw-status-lower)}))
         status-field-id? (or (= field-id :status)
                            (= field-id "status")
                            (= (name field-id) "status"))
         type-field-id? (or (= field-id :type)
                          (= field-id "type")
                          (= (name field-id) "type"))
         backlog-badge-fallback (cond
                                  (and status-field-id?
                                    (contains? #{"waiting"
                                                 "in progress"
                                                 "in progres"
                                                 "in-progress"
                                                 "completed"
                                                 "need improvements"
                                                 "need improvments"
                                                 "needs improvement"
                                                 "needs improvements"}
                                      value-lower))
                                  (resolve-backlog-badge "backlog-status-badge" display-value)

                                  (and type-field-id?
                                    (contains? #{"issue"
                                                 "feature"
                                                 "refactoring"
                                                 "review"
                                                 "improvement"
                                                 "improvment"}
                                      value-lower))
                                  (resolve-backlog-badge "backlog-type-badge" display-value)

                                  :else
                                  nil)
         status-str (when status-field-id? raw-status-str)
         status-lower raw-status-lower
         is-json-field? (or (= field-type "json")
                          (= field-type "jsonb"))
         is-datetime-field? (or (= field-type :datetime)
                              (= field-type "datetime")
                              (= field-type :timestamp)
                              (= field-type "timestamp")
                              (= field-type :datetime-local)
                              (= field-type "datetime-local")
                              (= input-type "datetime-local")
                              (= input-type :datetime-local))
         _show-seconds? true
         should-truncate? (or
                            (= input-type "url")
                            (= input-type "email")
                            (= field-type "text")
                            (= field-type "textarea")
                            is-json-field?
                            (= field-id :avatar_url)
                            (= field-id :settings)
                            (= field-id :financial_settings)
                            (= field-id :metadata)
                            (= field-id :tags)
                            (= field-id :attachments)
                            (= field-id :changes)
                            (= field-id :calculation_details)
                            (= field-id :template_data))
         max-length (cond
                      is-json-field? 25
                      (= input-type "url") 40
                      (= input-type "email") 30
                      (= field-type "textarea") 100
                      (= field-type "text") 50
                      :else 30)]
     (cond
       formatter-badge
       (render-badge formatter-badge)

       backlog-badge-fallback
       (render-badge backlog-badge-fallback)

       status-badge
       (render-badge status-badge)

       status-str
       (render-badge {:label (or badge-label (titleize-status status-str))
                      :variant (status->badge-variant status-lower)})

       (and (= field-type "select")
         (:options field)
         display-value)
       ($ select-field-value {:field field :value display-value})

       is-datetime-field?
       (format-timestamp display-value)

       should-truncate?
       ($ truncated-text-value {:text text-value
                                :max-length max-length
                                :field-type field-type
                                :field-id field-id})

       :else
       text-value))))
