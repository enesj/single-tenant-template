(ns app.template.frontend.components.form.fields.input
  "Input field components"
  (:require
    [app.template.frontend.components.common :as common]
    [app.template.frontend.components.form.fields.date-picker :refer [date-picker]]
    [app.template.frontend.components.form.validation :as validation]
    [app.template.frontend.i18n :as i18n]
    [clojure.string :as str]
    [uix.core :refer [$ defui]]))

(defn- coerce-datetime-local-value
  "HTML datetime-local inputs require `YYYY-MM-DDTHH:mm` (no seconds, no TZ).
   API timestamps arrive as ISO-Z strings; strip the seconds and any trailing
   `Z`/offset so the browser will populate the control."
  [v]
  (cond
    (not (string? v)) v
    (str/blank? v) v
    :else (let [no-tz (-> v
                        (str/replace #"Z$" "")
                        (str/replace #"[+-]\d{2}:?\d{2}$" ""))
                ;; keep only up to minutes: YYYY-MM-DDTHH:mm
                [date time] (str/split no-tz #"T" 2)]
            (if (and date time)
              (str date "T" (subs time 0 (min (count time) 5)))
              no-tz))))

(def input-width
  {:text "w-1/2"
   :number "w-1/3"
   :date "w-1/3"
   :datetime-local "w-1/3"
   :time "w-1/3"
   :date-time "w-1/3"
   :email "w-1/3"
   :password "w-1/3"
   :search "w-1/3"
   :tel "w-1/3"
   :url "w-1/3"
   :select "w-1/3"
   :textarea "w-full"
   :week "w-1/3"
   :default "w-full"})

(defui input
  [{:keys [id label input-type error required class inline on-change value fork-errors formId mixed-value?] :as all-props}]

  (let [t (i18n/use-t)
        ;; Generate ID from formId if not explicitly provided
        field-id (or id (when formId (str formId "-input")))
        error-id (when field-id (str field-id "-error"))
        base-class "ds-input ds-input-primary"
        label-class "ds-label"
        error-class "text-error"
        ;; Get errors from either the direct error prop or from Fork validation errors
        field-error (or error (when fork-errors (validation/get-field-errors fork-errors (keyword id))))
        normalized-input-type (cond
                                (keyword? input-type) (name input-type)
                                (string? input-type) input-type
                                :else "text")
        datetime-like-input? (contains? #{"datetime-local" "date" "time"} normalized-input-type)
        mixed-values-label (t :common/mixed-values)
        localized-placeholder (when mixed-value? mixed-values-label)
        props (-> all-props
                (assoc
                  :class (str base-class " " class)
                  :id field-id)
                (dissoc :error :input-type :disabled? :validate-server? :inline :fork-errors :formId :label :required :mixed-value?))]

    ;; For date type inputs, use the date-picker component
    (if (= (or input-type "text") "date")
      ($ date-picker
        (cond-> all-props
          true (assoc
                 :id field-id
                 :mode "single"
                 :highlighted-dates #{}                     ;; Empty set to avoid test data
                 :highlighted-class "ds-bg-secondary"       ;; Using DaisyUI class
                 :selected-date value                       ;; Only use value, don't provide default
                 :on-date-change #(on-change %))))

      ;; For all other input types, use the existing implementation
      ($ :div {:class (str "mb-4" (if inline " flex flex-row items-start gap-4"
                                    " flex flex-col items-start gap-4"))}
        ($ common/label {:text label
                         :for field-id
                         :required required
                         :class (str label-class (when inline " mb-0 min-w-[150px] text-left"))})
        ($ :div {:class (when inline "flex-1 text-left")}
          (let [datetime-local? (= "datetime-local" normalized-input-type)]
            ($ common/input
              (cond-> props
                localized-placeholder
                (assoc :placeholder localized-placeholder)
                (:input-type all-props)
                (assoc :type (or input-type "text"))
                datetime-local?
                (assoc :value (coerce-datetime-local-value value))
                (:step all-props)
                (assoc :step (or (:step all-props) "0.01"))
                (:min all-props)
                (assoc :min (:min all-props))
                (:max all-props)
                (assoc :max (:max all-props)))))
          (when (and mixed-value?
                  datetime-like-input?
                  (or (nil? value)
                    (and (string? value) (str/blank? value))))
            ($ :div {:class "text-xs text-base-content/60 mt-1"}
              mixed-values-label))
          (when field-error
            ($ :div {:class error-class
                     :id error-id
                     :role "alert"}
              ($ :div (if (string? field-error)
                        field-error
                        (:message field-error))))))))))
