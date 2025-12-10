(ns app.template.frontend.components.form.validation
  "Form validation utilities"
  (:require
   [app.shared.validation.builder :as validation-builder]
   [app.shared.validation.core :as validation-core]
   [app.shared.validation.fork :as validation-fork]
   [app.template.frontend.events.form :as form-events]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [re-frame.core :as rf]))

(defn- parse-field-value
  "Parses field value based on input type"
  [input-type raw-value]
  (case input-type
    "number" (when-not (str/blank? raw-value)
               (js/parseFloat raw-value))
    "object" (try
               (edn/read-string raw-value)
               (catch :default _
                 raw-value))
    raw-value))

(defn- validate-on-server
  "Sends a field value to the server for validation"
  [{:keys [id value entity-name]} send-server-request]
  (when send-server-request
    (send-server-request
      {:name id
       :value value
       :set-waiting? true
       :clean-on-refetch [id]
       :debounce 500}
      #(rf/dispatch [::form-events/set-server-field-error
                     {:dirty {id value}
                      :entity-name entity-name}]))))

(defn- get-initial-value [state field-id]
  (when (and state field-id)
    (if (satisfies? IDeref state)
      (get-in @state [:initial-values field-id])
      (get-in state [:initial-values field-id]))))

;; This function is now handled directly in set-handle-value-change
;; through the use of should-validate-field? and validate-single-field

(defn- handle-server-validation [state form-props field entity-name parsed-value send-server-request]
  (let [{:keys [id validate-server?]} field
        {:keys [server-validation?]} form-props
        initial-value (get-initial-value state (keyword id))]
    (when (and server-validation? validate-server? send-server-request (not= initial-value parsed-value))
      (validate-on-server {:id id
                           :value parsed-value
                           :entity-name entity-name}
        send-server-request))))

(defn set-handle-value-change
  "Create a handler for field value changes with integrated validation.
   This handler is responsible for:
   1. Parsing field values based on input type
   2. Updating Fork form state
   3. Tracking dirty fields
   4. Triggering client/server validation as needed"
  [field form-props models-data]
  (let [{:keys [id input-type]} field
        {:keys [set-handle-change send-server-request entity-name state server-validation? editing set-dirty]} form-props
        field-id (keyword id)
        dirty (:dirty form-props)
        dirty? (seq dirty)
        ;; Extract the actual models structure that validation expects
        validation-models-data (or (:data models-data) models-data)
        validators (when validation-models-data
                     (let [raw-validators (validation-builder/create-enhanced-validators validation-models-data)
                           ;; CRITICAL FIX: Don't use js->clj conversion since validators are already ClojureScript maps
                           ;; The structure is: outer keys = keywords, inner keys = strings
                           cljs-validators raw-validators]
;; DEBUG: Log what validators are actually created

                       cljs-validators))]
    (if set-handle-change
      (fn [e]
        (let [raw-value (if (and e (.-target e))
                          (.. e -target -value)
                          e)
              parsed-value (parse-field-value input-type raw-value)
              ;; Get the initial value for comparison
              initial-value (get-initial-value state field-id)]
          ;; Update the value in Fork state
          (set-handle-change {:value parsed-value
                              :path field-id})

          ;; Only mark as dirty if value changed from initial
          (when (and editing (not= initial-value parsed-value))
            ;; Directly set the field as dirty in Fork state (if available)
            (when (fn? set-dirty)
              (set-dirty field-id parsed-value))

            ;; Also update re-frame state (for our existing validation system)
            (rf/dispatch [::form-events/set-dirty-fields entity-name #{field-id}]))

          ;; After changing the field value, update the dirty-fields in app-db
          (when dirty?
            (rf/dispatch [::form-events/set-dirty-fields entity-name (keys dirty)]))

;; Handle client-side validation if validators are available
          (let [entity-name-kw (keyword entity-name)
                field-id-kw (keyword id)]  ;; CRITICAL FIX: Convert string field ID to keyword
            (when (and validators (validation-core/should-validate-field? validators entity-name-kw field-id-kw parsed-value))
              ;; Handle validation
              (let [error (when validators
                            (validation-fork/validate-single-field validators entity-name-kw field-id-kw parsed-value))]
                (if error
                  (rf/dispatch [::form-events/set-field-error
                                entity-name
                                field-id
                                {:message error}])
                  (rf/dispatch [::form-events/clear-field-error entity-name field-id])))))

          ;; Handle server validation if needed (independent of client-side validation)
          (when server-validation?
            (handle-server-validation state form-props field entity-name parsed-value send-server-request))))
      nil)))

(defn get-field-errors
  "Get validation errors for a specific field from Fork form errors"
  [errors field-id]
  (get errors field-id))
