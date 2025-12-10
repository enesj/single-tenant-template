(ns app.template.frontend.components.list.fields
  (:require
   [uix.core :refer [$ defui]]
   [uix.re-frame :refer [use-subscribe]]))

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
        option (first (filter #(= (:value %) value) options))]
    ($ :span (or (:label option) ""))))

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

(defn get-field-display-value
  "Gets the display value for a field, handling select fields specially and truncating long text content."
  [field value]
  (if (and (= (:type field) "select")
        (:options field)
        value)
    ($ select-field-value {:field field :value value})
    (let [field-id (:id field)
          input-type (:input-type field)
          field-type (:type field)
          text-value (str (if (keyword? value) (name value) (or value "")))

          ;; JSON detection based only on field type from database schema
          is-json-field? (or (= field-type "json")
                           (= field-type "jsonb"))

          ;; Determine if this field should be truncated based on type or ID
          should-truncate? (or
                             (= input-type "url")          ; URL fields
                             (= input-type "email")        ; Email fields
                             (= field-type "text")         ; Text fields
                             (= field-type "textarea")     ; Textarea fields
                             is-json-field?               ; JSON fields based on schema
                             (= field-id :avatar_url)      ; Specific field IDs
                             (= field-id :settings)        ; JSON fields that might be long
                             (= field-id :financial_settings)
                             (= field-id :metadata)
                             (= field-id :tags)
                             (= field-id :attachments)
                             (= field-id :changes)         ; Audit log changes field
                             (= field-id :calculation_details) ; Balance calculation details
                             (= field-id :template_data))   ; Template data

          max-length (cond
                      ;; JSON fields get very aggressive truncation as they can be huge
                       is-json-field? 25
                      ;; URLs get shorter truncation as they're typically very long
                       (= input-type "url") 40
                      ;; Emails get medium truncation
                       (= input-type "email") 30
                      ;; Text areas get generous truncation
                       (= field-type "textarea") 100
                      ;; General text fields
                       (= field-type "text") 50
                      ;; Default for other truncatable fields
                       :else 30)]

      (if should-truncate?
        ($ truncated-text-value {:text text-value
                                 :max-length max-length
                                 :field-type field-type
                                 :field-id field-id})
        ($ :span text-value)))))
