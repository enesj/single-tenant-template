(ns app.shared.schemas.primitives
  "Primitive and basic schemas used across the application.
   These are fundamental data types and patterns that are domain-agnostic.")

(def non-empty-string
  "A string that must contain at least one character"
  [:string {:min 1}])

(def color-schema
  "Flexible color representation supporting multiple formats"
  [:or
   string?                                                  ;; CSS color names, hex, rgb/rgba/hsl/hsla strings
   [:vector int?]                                           ;; [r g b] or [r g b a]
   keyword?                                                 ;; :red, :blue, etc.
   [:map                                                    ;; CSS color maps
    [:r {:optional true} :int]
    [:g {:optional true} :int]
    [:b {:optional true} :int]
    [:a {:optional true} [:double {:min 0 :max 1}]]]
   [:map-of :keyword :any]])

(def email-schema
  "Standard email address validation"
  [:and
   string?
   [:re #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$"]])

(def url-schema
  "URL validation for http, https, and ftp protocols"
  [:and
   string?
   [:re #"^(https?|ftp)://[^\s/$.?#].[^\s]*$"]])

(def uuid-schema
  "UUID v4 format validation"
  [:and
   string?
   [:re #"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"]])

(def date-schema
  "ISO 8601 date format (YYYY-MM-DD)"
  [:and
   string?
   [:re #"^\d{4}-\d{2}-\d{2}$"]])

(def datetime-schema
  "ISO 8601 datetime format with optional timezone"
  [:and
   string?
   [:re #"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?(Z|[+-]\d{2}:\d{2})$"]])

(def positive-int
  "An integer greater than zero"
  [:int {:min 1}])

(def percentage
  "A decimal value between 0 and 1 (inclusive)"
  [:double {:min 0 :max 1}])

(def coordinate
  "Geographic coordinate as [longitude, latitude] tuple"
  [:tuple
   [:double {:min -180 :max 180}]  ;; longitude
   [:double {:min -90 :max 90}]])   ;; latitude

(def phone-number
  "International phone number format"
  [:and
   string?
   [:re #"^\+?[\d\s\-()]{7,}$"]])

(def currency-code
  "ISO 4217 currency code (3 uppercase letters)"
  [:and
   string?
   [:re #"^[A-Z]{3}$"]])
