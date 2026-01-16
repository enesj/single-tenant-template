(ns app.shared.schemas.primitives
  "Primitive and basic schemas used across the application.
   These are fundamental data types and patterns that are domain-agnostic.")

(def non-empty-string
  "A string that must contain at least one character"
  [:string {:min 1}])


(def percentage
  "A decimal value between 0 and 1 (inclusive)"
  [:double {:min 0 :max 1}])

(def coordinate
  "Geographic coordinate as [longitude, latitude] tuple"
  [:tuple
   [:double {:min -180 :max 180}]  ;; longitude
   [:double {:min -90 :max 90}]])   ;; latitude


(def currency-code
  "ISO 4217 currency code (3 uppercase letters)"
  [:and
   string?
   [:re #"^[A-Z]{3}$"]])
