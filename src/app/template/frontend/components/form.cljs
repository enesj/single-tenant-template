(ns app.template.frontend.components.form
  "Form component namespace providing form validation and rendering functionality"
  (:require
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.form.base :as base]
    [app.template.frontend.components.form.fields.array-input :refer [array-input]]
    [app.template.frontend.components.form.fields.checkbox :refer [checkbox-input]]
    [app.template.frontend.components.form.fields.input :refer [input
                                                                input-width]]
    [app.template.frontend.components.form.fields.json-editor :refer [json-editor]]
    [app.template.frontend.components.form.fields.number :refer [number-input]]
    [app.template.frontend.components.form.fields.select :refer [select-input]]
    [app.template.frontend.components.form.fields.textarea :refer [textarea-input]]
    [app.template.frontend.components.form.validation :refer [set-handle-value-change]]
    [app.template.frontend.components.icons :refer [cancel-icon save-icon]]
    [app.template.frontend.components.messages :as messages]
    [app.template.frontend.events.form :as form-events]
    [app.template.frontend.events.list.crud :as crud-events]
    [app.template.frontend.subs.form :as form-subs]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]
    [uix.core :as uix :refer [$ defui use-effect]]
    [uix.re-frame :as urf]))

;; Helper function for memoized version
(defn- create-field-props-impl
  [{:keys [field-spec disabled? handle-change label success inline] :as props} error]
  (let [field-id (keyword (:id field-spec))
        field-value (get-in props [:values field-id])
        input-type (:input-type field-spec)
        required (:required field-spec)
        ;; Safe width lookup with fallback to default
        width (if input-type
                (get input-width (keyword input-type) (:default input-width))
                (:default input-width))]

    (merge
      (select-keys field-spec [:id :required :type :input-type :options :label :step])
      {:disabled disabled?
       :value field-value
       :class width
       :error error
       :data-xss-protection true
       :success success
       :inline inline
       :auto-complete (or (:autocomplete field-spec) "on")  ;; Use validation metadata autocomplete
       :placeholder (:placeholder field-spec)               ;; Use validation metadata placeholder
       :on-change handle-change
       :aria-label (str "Enter " label)
       :aria-required required
       :aria-invalid (boolean error)
       :validate-server? (= (:validate-server? field-spec) "unique")})))

;; Primary memoized function
(def create-field-props
  "Creates props for a form field with memoization for performance"
  (memoize (fn [props error]
             (create-field-props-impl props error))))

;; Component implementation
(defui render-field
  [{:keys [field-spec entity-name editing errors form-id] :as props}]

  (let [field-id (:id field-spec)
        component-id (when (and form-id field-id)
                       (str form-id "-" field-id))
        server-validation (urf/use-subscribe [::form-subs/field-server-validation-state field-id entity-name])
        field-validation (urf/use-subscribe [::form-subs/field-validation-state entity-name field-id])
        error (or (:error field-validation) (:error server-validation))
        field-props (create-field-props
                      (if editing
                        (assoc props :inline true)
                        props)
                      error)
        ;; Normalize field type from spec (accept strings like "input" and keywords like :string/:uuid)
        raw-type (:type field-spec)
        type-kw (cond
                  (keyword? raw-type) raw-type
                  (string? raw-type) (keyword raw-type)
                  :else nil)
        ui-type (or (when (#{:input :number :checkbox :textarea :select :json :array} type-kw)
                      type-kw)
                  (cond
                    (#{:string :text :varchar :uuid :timestamp :date} type-kw) :input
                    (#{:integer :decimal :bigint :float :double :number} type-kw) :number
                    (#{:boolean} type-kw) :checkbox
                    (#{:json :jsonb} type-kw) :json
                    (#{:array} type-kw) :array
                    :else nil))]

    ($ :div {:id component-id}
      (let [input-type (:input-type field-spec)]
        (case ui-type
          :input ($ input (assoc field-props :fork-errors errors :form-id form-id))
          :number ($ number-input (assoc field-props :fork-errors errors :form-id form-id))
          :checkbox ($ checkbox-input (assoc field-props :fork-errors errors :form-id form-id))
          :textarea ($ textarea-input (assoc field-props :form-id form-id))
          :select ($ select-input (assoc field-props :form-id form-id))
          :json ($ json-editor (assoc field-props :fork-errors errors :form-id form-id))
          :array ($ array-input (assoc field-props :fork-errors errors :form-id form-id))
          ;; Handle nil/null types by detecting input-type or defaulting to input
          nil (cond
                ;; Number types
                (#{:integer :decimal "integer" "decimal" "number"} input-type)
                (do
                  (log/debug "Field" (:id field-spec) "has nil type but number input-type, using number-input")
                  ($ number-input (assoc field-props :fork-errors errors :form-id form-id)))
                ;; Boolean types
                (#{:boolean "boolean"} input-type)
                (do
                  (log/debug "Field" (:id field-spec) "has nil type but boolean input-type, using checkbox-input")
                  ($ checkbox-input (assoc field-props :fork-errors errors :form-id form-id)))
                ;; JSON types
                (#{:jsonb "jsonb" "json"} input-type)
                (do
                  (log/debug "Field" (:id field-spec) "has nil type but json input-type, using json-editor")
                  ($ json-editor (assoc field-props :fork-errors errors :form-id form-id)))
                ;; Array types
                (#{:array "array"} input-type)
                (do
                  (log/debug "Field" (:id field-spec) "has nil type but array input-type, using array-input")
                  ($ array-input (assoc field-props :fork-errors errors :form-id form-id)))
                ;; Default to input
                :else
                (do
                  (log/debug "Field" (:id field-spec) "has nil/unknown type, defaulting to input")
                  ($ input (assoc field-props :fork-errors errors :form-id form-id))))
          ;; Default case for any other unrecognized field type
          ($ input (assoc field-props :fork-errors errors :form-id form-id)))))))

(defui form-fields
  [{:keys [editing set-dirty form-id] :as props}]
  (let [entity-name (:entity-name props)
        ;; Use enhanced field specs with validation metadata from subscription
        enhanced-field-specs (urf/use-subscribe [:form-entity-specs/by-name entity-name])
        ;; Fallback to raw entity-spec if enhanced specs aren't available yet
        raw-spec (:entity-spec props)

        ;; Prefer enhanced specs, fallback to raw specs
        spec-to-use (or enhanced-field-specs raw-spec)

        ;; Normalize field specs: accept either a map of fields or a seq of field maps
        excluded-field-ids #{:id :created-at :updated-at}
        field-specs (cond
                      (map? spec-to-use) (->> (vals spec-to-use)
                                           (filter map?)
                                           (filter #(contains? % :id))
                                           (remove #(excluded-field-ids (:id %)))
                                           (sort-by (fn [f] (or (:display-order f) 0))))
                      (sequential? spec-to-use) (->> spec-to-use
                                                  (remove #(excluded-field-ids (:id %))))
                      :else [])
        models-data (urf/use-subscribe [:models-data])]

    ;; Debug log to see which specs are being used

    ($ :fieldset {:class "fieldset border-1 border-base-300 p-4 mb-4 shadow text-left"
                  :id (when form-id (str form-id "-fieldset"))}
      ($ :legend {:class "legend font-bold mb-4 ml-4 text-left"}
        (str (if editing "Edit " "Add ") (or (str/capitalize (name (:entity-name props))) (:legend props) "")))
      (map (fn [field-spec]
             (let [field-id (:id field-spec)
                   safe-label (or (:label field-spec)
                                (when field-id
                                  (-> (name field-id)
                                    (str/replace "_" " ")
                                    (str/capitalize))))]
               ($ render-field
                 {:key field-id
                  :label safe-label
                  :field-spec field-spec
                  :editing editing
                  :inline (if editing true true)
                  :entity-name (:entity-name props)
                  :form-id form-id
                  :handle-change (set-handle-value-change field-spec (assoc props :set-dirty set-dirty) models-data)
                  :values (:values props)
                  :errors (:errors props)})))
        field-specs))))

(def form-props
  {:on-cancel {:type :function :required false}
   :button-text {:type :string :required false}
   :entity-spec {:type :array :required true}
   :entity-name {:type :string :required true}
   :editing {:type :boolean :required false}
   :initial-values {:type :map :required false}
   :set-editing! {:type :function :required false}})

(defui form
  "Renders a form with fields based on entity specification"
  {:prop-types form-props}
  [{:keys [on-cancel button-text entity-spec entity-name editing initial-values set-editing!] :as _props}]
  (let [form-success? (urf/use-subscribe [::form-subs/form-success entity-name])
        submitted? (urf/use-subscribe [::form-subs/submitted? entity-name])
        form-errors (urf/use-subscribe [::form-subs/form-errors entity-name])]

    ;; Effect to handle form success
    (use-effect
      (fn []
        (when (and form-success? submitted?)
          ;; Delay close to allow animation
          (js/setTimeout
            (fn []
              ;; For edit forms, close the form
              ;; For add forms, just reset success state and keep the form with default values

              (rf/dispatch [::form-events/set-submitted entity-name false])
              (rf/dispatch [::crud-events/clear-error (keyword entity-name)])
              (rf/dispatch [::form-events/clear-form-errors entity-name]))
            1000)))
      [form-success? submitted? entity-name])

    ($ base/initialize-form
      {:render-fn (fn [{:keys [form-id handle-submit dirty submitting? values errors on-submit-server-message] :as form-props}]
                    (let [subscription-valid? (urf/use-subscribe [::form-subs/all-fields-valid? entity-name entity-spec editing values])
                          all-fields-valid? (if (some? errors)
                                              (empty? errors)
                                              subscription-valid?)]
                      ($ :form {:id form-id
                                :on-submit handle-submit}
                        (when-let [form-error (get-in form-errors [:form])]
                          ($ messages/error-alert
                            {:error form-error
                             :entity-name entity-name}))
                        (when (and form-success? submitted?)
                          ($ messages/success-alert
                            {:message (str (if editing "Updated" "Created") " successfully!")
                             :entity-name entity-name}))
                        ($ form-fields
                          (merge form-props
                            {:entity-name entity-name
                             :editing editing
                             :values values
                             :form-id form-id
                             :entity-spec entity-spec}))

                        ($ :div
                          ($ :div {:class "flex justify-end gap-2"}

                            ($ button
                              {:btn-type :cancel
                               :id "btn-cancel"
                               :type "button"
                               :disabled submitting?
                               :on-click (fn []
                                           (rf/dispatch [::form-events/set-submitted entity-name false])
                                           (rf/dispatch [::crud-events/clear-error (keyword entity-name)])
                                           (rf/dispatch [::form-events/clear-form-errors entity-name])
                                           ;; Always dispatch the cancel-form event to ensure batch editing works
                                           (rf/dispatch [::form-events/cancel-form entity-name])
                                           ;; Only call the on-cancel callback if it's a function (not for batch editing)
                                           (when (fn? on-cancel)
                                             (on-cancel)))
                               :children ["Cancel" ($ cancel-icon)]})
                            ($ button
                              {:btn-type :save
                               :id (if editing "btn-update" "btn-save")
                               :type "submit"
                               ;; Adjust the disabled condition based on form type
                               :disabled (or
                                           submitting?
                                           (nil? dirty)
                                           (and (map? dirty) (empty? dirty))
                                           (and (set? dirty) (empty? dirty))
                                           (not all-fields-valid?))
                               :children [(or button-text (if editing "Update" "Save")) ($ save-icon)]}))
                          (when on-submit-server-message
                            ($ :div {:class (str "message "
                                              (if (str/includes? on-submit-server-message "Success")
                                                "success"
                                                "error"))}
                              on-submit-server-message))))))
       :initial-values initial-values
       :entity-name entity-name
       :entity-spec entity-spec
       :editing editing
       :path [:forms (keyword entity-name)]
       :keywordize-keys true
       :prevent-default? true
       :clean-on-unmount? false
       :on-submit #(do
                     (let [dirty (:dirty %)
                           values (:values %)
                           reset (:reset %)
                           _ (log/debug "Form submission:"
                               {:entity-name entity-name
                                :editing editing
                                :dirty dirty
                                :values values
                                :settings-value (get values :settings)})
                           changed-values (-> values
                                            (select-keys (cons :id (keys dirty))))]
                       (log/debug "Form submission - changed values:"
                         {:changed-values changed-values
                          :settings-in-changed (contains? changed-values :settings)})
                       (rf/dispatch [::form-events/submit-form
                                     (assoc %
                                       :values (if editing changed-values values)
                                       :entity-name entity-name
                                       :editing editing)])
                       (if editing (set-editing! nil)
                         (reset {:values initial-values
                                 :touched (set (keys initial-values))}))
                       (rf/dispatch [::form-events/set-submitted entity-name true])))

       :on-cancel #(do
                     (rf/dispatch [::form-events/set-submitted entity-name false])
                     (if (fn? on-cancel)
                       (on-cancel)
                       (rf/dispatch [on-cancel])))})))
