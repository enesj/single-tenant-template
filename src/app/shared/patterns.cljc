(ns app.shared.patterns
  "Cross-platform regex patterns for the hosting application.
   Provides consistent validation patterns and utilities
   that work in both Clojure and ClojureScript environments."
  )

;; -------------------------
;; Email Patterns
;; -------------------------

(def email-pattern
  "Basic email validation pattern - RFC 5322 compliant"
  #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$")

(def email-simple-pattern
  "Simple email pattern for basic validation"
  #".+@.+\..+")

#_(def email-local-part-pattern
  "Pattern for email local part (before @)"
  #"^[a-zA-Z0-9._%+-]+$")

#_(def email-domain-pattern
  "Pattern for email domain part (after @)"
  #"^[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$")

;; -------------------------
;; Date and Time Patterns
;; -------------------------

#_(def iso-date-pattern
  "ISO 8601 date format (YYYY-MM-DD)"
  #"^\d{4}-\d{2}-\d{2}$")

#_(def iso-datetime-pattern
  "ISO 8601 datetime format with optional timezone"
  #"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?(Z|[+-]\d{2}:\d{2})$")

#_(def iso-datetime-simple-pattern
  "Simplified ISO datetime pattern (YYYY-MM-DDTHH:mm:ss)"
  #"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$")

#_(def us-date-pattern
  "US date format (MM/DD/YYYY)"
  #"^\d{1,2}/\d{1,2}/\d{4}$")

#_(def us-date-strict-pattern
  "Strict US date format (MM/DD/YYYY with leading zeros)"
  #"^\d{2}/\d{2}/\d{4}$")

#_(def time-pattern
  "Time format (HH:MM or HH:MM:SS)"
  #"^\d{1,2}:\d{2}(:\d{2})?$")

#_(def time-12hour-pattern
  "12-hour time format with AM/PM"
  #"^\d{1,2}:\d{2}(:\d{2})?\s*(AM|PM|am|pm)$")

;; -------------------------
;; URL Patterns
;; -------------------------

#_(def url-pattern
  "URL validation for http, https, and ftp protocols"
  #"^(https?|ftp)://[^\s/$.?#].[^\s]*$")

#_(def http-url-pattern
  "HTTP/HTTPS URL validation"
  #"^https?://[^\s/$.?#].[^\s]*$")

#_(def domain-pattern
  "Domain name pattern"
  #"^[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$")

#_(def subdomain-pattern
  "Subdomain pattern (allows multiple levels)"
  #"^[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?(\.[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?)*$")

;; -------------------------
;; Identifier Patterns
;; -------------------------

#_(def uuid-pattern
  "UUID v4 format validation"
  #"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")

#_(def uuid-pattern-case-insensitive
  "UUID format validation (case insensitive)"
  #"(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")

#_(def slug-pattern
  "URL-friendly slug pattern (lowercase letters, numbers, hyphens)"
  #"^[a-z0-9-]+$")

#_(def slug-pattern-flexible
  "Flexible slug pattern (allows underscores and dots)"
  #"^[a-z0-9._-]+$")

#_(def identifier-pattern
  "General identifier pattern (alphanumeric with underscores)"
  #"^[a-zA-Z][a-zA-Z0-9_]*$")

;; -------------------------
;; Phone Number Patterns
;; -------------------------

#_(def phone-international-pattern
  "International phone number format"
  #"^\+?[\d\s\-()]{7,}$")

#_(def phone-us-pattern
  "US phone number format (XXX) XXX-XXXX"
  #"^\(\d{3}\)\s?\d{3}-\d{4}$")

#_(def phone-digits-only-pattern
  "Phone number with digits only (7-15 digits)"
  #"^\d{7,15}$")

#_(def phone-e164-pattern
  "E.164 international phone number format"
  #"^\+[1-9]\d{1,14}$")

;; -------------------------
;; Numeric Patterns
;; -------------------------

#_(def integer-pattern
  "Integer number pattern (positive, negative, or zero)"
  #"^-?\d+$")

#_(def positive-integer-pattern
  "Positive integer pattern"
  #"^\d+$")

#_(def decimal-pattern
  "Decimal number pattern"
  #"^-?\d+(\.\d+)?$")

#_(def positive-decimal-pattern
  "Positive decimal pattern"
  #"^\d+(\.\d+)?$")

#_(def percentage-pattern
  "Percentage pattern (0-100 with optional decimal)"
  #"^(100(\.0+)?|[0-9]?[0-9](\.\d+)?)$")

#_(def currency-pattern
  "Currency amount pattern (allows decimals and commas)"
  #"^\$?\d{1,3}(,\d{3})*(\.\d{2})?$")

;; -------------------------
;; Text Patterns
;; -------------------------

#_(def alphanumeric-pattern
  "Alphanumeric characters only"
  #"^[a-zA-Z0-9]+$")

#_(def alphanumeric-spaces-pattern
  "Alphanumeric characters and spaces"
  #"^[a-zA-Z0-9\s]+$")

#_(def name-pattern
  "Name pattern (letters, spaces, hyphens, apostrophes)"
  #"^[a-zA-Z\s\-']+$")

#_(def username-pattern
  "Username pattern (letters, numbers, underscores, hyphens)"
  #"^[a-zA-Z0-9_-]+$")

#_(def password-strong-pattern
  "Strong password pattern (8+ chars, uppercase, lowercase, number, special char)"
  #"^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$")

;; -------------------------
;; Code and Technical Patterns
;; -------------------------

#_(def hex-color-pattern
  "Hexadecimal color pattern (#RGB or #RRGGBB)"
  #"^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")

#_(def ip-address-pattern
  "IPv4 address pattern"
  #"^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$")

#_(def mac-address-pattern
  "MAC address pattern (XX:XX:XX:XX:XX:XX)"
  #"^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")

#_(def version-pattern
  "Semantic version pattern (X.Y.Z)"
  #"^\d+\.\d+\.\d+$")

#_(def version-pattern-extended
  "Extended semantic version pattern (X.Y.Z-beta.1+build.123)"
  #"^\d+\.\d+\.\d+(-[a-zA-Z0-9.-]+)?(\+[a-zA-Z0-9.-]+)?$")

;; -------------------------
;; Currency and Financial Patterns
;; -------------------------

#_(def currency-code-pattern
  "ISO 4217 currency code (3 uppercase letters)"
  #"^[A-Z]{3}$")

#_(def credit-card-pattern
  "Credit card number pattern (13-19 digits with optional spaces/hyphens)"
  #"^[\d\s\-]{13,19}$")

#_(def iban-pattern
  "IBAN pattern (basic format validation)"
  #"^[A-Z]{2}\d{2}[A-Z0-9]{4,30}$")

;; -------------------------
;; Geographic Patterns
;; -------------------------

#_(def postal-code-us-pattern
  "US postal code pattern (XXXXX or XXXXX-XXXX)"
  #"^\d{5}(-\d{4})?$")

#_(def postal-code-canada-pattern
  "Canadian postal code pattern (A1A 1A1)"
  #"^[A-Za-z]\d[A-Za-z]\s?\d[A-Za-z]\d$")

#_(def postal-code-uk-pattern
  "UK postal code pattern"
  #"^[A-Za-z]{1,2}\d[A-Za-z\d]?\s?\d[A-Za-z]{2}$")

;; -------------------------
;; Validation Functions
;; -------------------------

(defn matches-pattern?
  "Check if a string matches a given regex pattern"
  [pattern string]
  (when (and pattern string)
    (boolean (re-matches pattern string))))

#_(defn find-pattern
  "Find first occurrence of pattern in string"
  [pattern string]
  (when (and pattern string)
    (re-find pattern string)))

#_(defn extract-pattern
  "Extract all matches of pattern from string"
  [pattern string]
  (when (and pattern string)
    (re-seq pattern string)))

;; -------------------------
;; Email Validation Functions
;; -------------------------

(defn valid-email?
  "Validate email address using standard pattern"
  [email]
  (matches-pattern? email-pattern email))

(defn valid-email-simple?
  "Validate email using simple pattern (less strict)"
  [email]
  (matches-pattern? email-simple-pattern email))

#_(defn extract-email-parts
  "Extract local and domain parts from email"
  [email]
  (when (valid-email? email)
    (let [[_ local domain] (re-matches #"^([^@]+)@(.+)$" email)]
      {:local local :domain domain})))

;; -------------------------
;; Date Validation Functions
;; -------------------------

#_(defn valid-iso-date?
  "Validate ISO date format (YYYY-MM-DD)"
  [date-str]
  (matches-pattern? iso-date-pattern date-str))

#_(defn valid-us-date?
  "Validate US date format (MM/DD/YYYY)"
  [date-str]
  (matches-pattern? us-date-pattern date-str))

#_(defn valid-iso-datetime?
  "Validate ISO datetime format"
  [datetime-str]
  (matches-pattern? iso-datetime-pattern datetime-str))

;; -------------------------
;; URL Validation Functions
;; -------------------------

#_(defn valid-url?
  "Validate URL format"
  [url]
  (matches-pattern? url-pattern url))

#_(defn valid-http-url?
  "Validate HTTP/HTTPS URL format"
  [url]
  (matches-pattern? http-url-pattern url))

#_(defn extract-domain
  "Extract domain from URL"
  [url]
  (when url
    (second (re-find #"://([^/]+)" url))))

;; -------------------------
;; Identifier Validation Functions
;; -------------------------

#_(defn valid-uuid?
  "Validate UUID format"
  [uuid]
  (matches-pattern? uuid-pattern (str uuid)))

#_(defn valid-slug?
  "Validate URL slug format"
  [slug]
  (matches-pattern? slug-pattern slug))

#_(defn valid-identifier?
  "Validate programming identifier format"
  [identifier]
  (matches-pattern? identifier-pattern identifier))

;; -------------------------
;; Phone Validation Functions
;; -------------------------

#_(defn valid-phone-international?
  "Validate international phone number"
  [phone]
  (matches-pattern? phone-international-pattern phone))

#_(defn valid-phone-us?
  "Validate US phone number format"
  [phone]
  (matches-pattern? phone-us-pattern phone))

#_(defn normalize-phone-digits
  "Extract only digits from phone number"
  [phone]
  (when phone
    (apply str (re-seq #"\d" phone))))

;; -------------------------
;; Numeric Validation Functions
;; -------------------------

#_(defn valid-integer?
  "Validate integer format"
  [value]
  (matches-pattern? integer-pattern (str value)))

#_(defn valid-decimal?
  "Validate decimal number format"
  [value]
  (matches-pattern? decimal-pattern (str value)))

#_(defn valid-percentage?
  "Validate percentage format (0-100)"
  [value]
  (matches-pattern? percentage-pattern (str value)))

;; -------------------------
;; Utility Functions
;; -------------------------

#_(defn clean-pattern-input
  "Clean input string for pattern matching (trim, lowercase)"
  [input & {:keys [lowercase? trim?] :or {lowercase? false trim? true}}]
  (when input
    (let [cleaned (if trim? (clojure.string/trim input) input)]
      (if lowercase? (clojure.string/lower-case cleaned) cleaned))))

#_(defn pattern-test-suite
  "Test multiple patterns against a value (useful for debugging)"
  [value patterns]
  (into {} (map (fn [[name pattern]]
                  [name (matches-pattern? pattern value)])
             patterns)))

;; -------------------------
;; Common Pattern Collections
;; -------------------------

#_(def common-validation-patterns
  "Map of common validation patterns"
  {:email email-pattern
   :url url-pattern
   :uuid uuid-pattern
   :phone phone-international-pattern
   :iso-date iso-date-pattern
   :us-date us-date-pattern
   :slug slug-pattern
   :integer integer-pattern
   :decimal decimal-pattern})

#_(def strict-validation-patterns
  "Map of strict validation patterns"
  {:email email-pattern
   :phone phone-e164-pattern
   :iso-date iso-date-pattern
   :uuid uuid-pattern
   :currency-code currency-code-pattern
   :positive-integer positive-integer-pattern})

;; -------------------------
;; Pattern Compilation Utilities (CLJ only)
;; -------------------------

#_#?(:clj
     (defn compile-pattern
       "Compile a regex pattern for better performance (JVM only)"
       [pattern]
       (if (instance? java.util.regex.Pattern pattern)
         pattern
         (java.util.regex.Pattern/compile (str pattern)))))

#_#?(:clj
     (defn compile-patterns
       "Compile a map of patterns for better performance (JVM only)"
       [pattern-map]
       (into {} (map (fn [[k v]] [k (compile-pattern v)]) pattern-map))))
