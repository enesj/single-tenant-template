(ns app.template.frontend.components.list.fields
  (:require
    [clojure.string :as str]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

(defn- format-timestamp
  "Format a timestamp value for display, matching Created/Updated column formatting.

   When `:show-seconds?` is true, renders seconds (highlighted) to make rapid changes
   easier to spot in list views."
  ([value]
   (format-timestamp value {:show-seconds? false}))
  ([value {:keys [show-seconds?]}]
   (when (and value (not= value "") (not= value "—"))
     (try
       (let [date (js/Date. value)]
         (when-not (js/isNaN (.getTime date))
           (let [month (.toLocaleString date "en-US" #js {:month "short"})
                 day (.getDate date)
                 hours (.getHours date)
                 minutes (.getMinutes date)
                 seconds (.getSeconds date)
                 hh (str (when (< hours 10) "0") hours)
                 mm (str (when (< minutes 10) "0") minutes)
                 ss (str (when (< seconds 10) "0") seconds)]
             ($ :span {:class "whitespace-nowrap"}
               ($ :span (str month " " day))
               ($ :span {:class "ml-1"} (str hh ":" mm))
               (when show-seconds?
                 ($ :span {:class "text-warning"} (str ":" ss)))))))
       (catch js/Error _
         ($ :span (str value)))))))

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
                :style {:display "inline-block"
                        :max-width (str (* max-length 0.8) "ch")
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

    (contains? #{"uploaded" "pending" "parsing" "parsed" "extracting" "processing" "in-progress"} status-lower)
    "ds-badge-info"

    (contains? #{"inactive" "suspended" "archived"} status-lower)
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

(defn get-field-display-value
  "Gets the display value for a field, handling select fields specially and truncating long text content."
  ([field value]
   (get-field-display-value field value nil))
  ([field value item]
   (if (and (= (:type field) "select")
         (:options field)
         value)
     ($ select-field-value {:field field :value value})
     (let [field-id (:id field)
           normalized-field-id (normalize-field-key field-id)
           display-source-field (:display-source-field field)
           display-value (or (lookup-item-value item display-source-field) value)
           input-type (:input-type field)
           field-type (:type field)
           text-value (str (if (keyword? display-value)
                             (name display-value)
                             (or display-value "")))
           status-field-id? (or (= field-id :status)
                              (= field-id "status")
                              (= (name field-id) "status"))
           status-str (when status-field-id? (some-> text-value str/trim not-empty))
           status-lower (some-> status-str str/lower-case)
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
           show-seconds? (contains? #{"created-at" "updated-at"} normalized-field-id)
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
         status-str
         ($ :span {:class (str "ds-badge uppercase tracking-wide text-xs px-3 py-1 rounded-full border shadow-sm "
                            (status->badge-variant status-lower))}
           (titleize-status status-str))

         is-datetime-field?
         ($ :span {:class "whitespace-nowrap"}
           (format-timestamp display-value {:show-seconds? show-seconds?}))

         should-truncate?
         ($ truncated-text-value {:text text-value
                                  :max-length max-length
                                  :field-type field-type
                                  :field-id field-id})

         :else
         ($ :span text-value))))))
