(ns app.shared.field-types
  (:require
    #?(:clj [taoensso.timbre :as log]
       :cljs [taoensso.timbre :as log])
    [clojure.string :as str]))

(def base-input-types
  {:text {:type "input" :input-type "text"}
   :varchar {:type "input" :input-type "text"}
   :email {:type "input" :input-type "email" :validation {:email true}}
   :integer {:type "number" :input-type "integer" :step "1"}
   :decimal {:type "number" :input-type "decimal" :step "0.01"}
   :boolean {:type "checkbox" :input-type "boolean"}
   :timestamp {:type "input" :input-type "datetime-local" :step "1"}
   :timestamptz {:type "input" :input-type "datetime-local" :step "1"}
   :date {:type "input" :input-type "date"}
   :jsonb {:type "json" :input-type "jsonb"}
   :json {:type "json" :input-type "json"}
   :map {:type "json" :input-type "jsonb"}
   :array {:type "array" :input-type "array"}
   :enum {:type "select" :input-type "select"}
   :serial {:type "number" :input-type "integer" :step "1"}
   :uuid {:type "input" :input-type "text"}
   :inet {:type "input" :input-type "text"}
   :bigint {:type "number" :input-type "integer" :step "1"}
   :numeric {:type "number" :input-type "decimal" :step "0.01"}})

(defprotocol FieldType
  (get-input-type [this])
  (get-default-value [this])
  (get-options [this type-info]))

#?(:cljs
   (defn format-date [date]
     (let [year (.getFullYear date)
           month (inc (.getMonth date))
           day (.getDate date)]
       (str year "-"
         (if (< month 10) "0" "") month "-"
         (if (< day 10) "0" "") day))))

;; Implement type-specific behaviors
(defrecord BasicField [type]
  FieldType
  (get-input-type [_]
    (let [kw-type (if (keyword? type) type (keyword type))]
      (get base-input-types kw-type)))
  (get-default-value [_]
    (let [kw-type (if (keyword? type) type (keyword type))
          default-value (case kw-type
                          :text ""
                          :varchar ""
                          :email ""
                          :integer 0
                          :decimal 0.0
                          :boolean false
                          ;; Important: Do NOT default timestamps to Date objects.
                          ;; This breaks HTML "datetime-local" inputs which expect a
                          ;; formatted string (YYYY-MM-DDTHH:mm[:ss]) and also risks
                          ;; sending invalid empty casts to the DB when creating records.
                          ;; Default to nil so forms omit the field unless a user sets it.
                          :timestamp nil
                          :timestamptz nil
                          :date #?(:clj (java.util.Date.)
                                   :cljs (format-date (js/Date.)))
                          :jsonb {}
                          :json {}
                          :map {}
                          :array []
                          :uuid ""
                          :inet ""
                          :serial 1
                          :bigint 0
                          :numeric 0.0
                          nil)]
      default-value))
  (get-options [_ _] nil))

(defrecord EnumField [choices]
  FieldType
  (get-input-type [_] (:enum base-input-types))
  (get-default-value [_]
    (let [default-value (first choices)]
      default-value))
  (get-options [_ _]
    (mapv (fn [choice]
            {:value choice
             :label (str/capitalize choice)})
      choices)))

(defrecord ForeignKeyField [reference-entity unique-field]
  FieldType
  (get-input-type [_] {:type "select" :input-type "select"})
  (get-default-value [_]
    nil)
  (get-options [_ _] [reference-entity unique-field]))
