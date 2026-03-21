(ns app.shared.string
  "String casing/slug/email helpers; avoid ad hoc logic."
  (:require
    [clojure.string :as str]))

;; ========================================
;; String Case Conversion
;; ========================================

(defn kebab-case
  "Convert a value to a kebab-case string.

  Examples:
  - 'Hello World' -> 'hello-world'
  - 'hello_world' -> 'hello-world'"
  [s]
  (when (some? s)
    (-> (str s)
      (str/lower-case)
      (str/replace #"[_\s]+" "-")
      (str/replace #"[^a-z0-9-]+" "-")
      (str/replace #"-+" "-")
      (str/replace #"^-|-$" ""))))

(defn snake-case
  "Convert a value to a snake_case string.

  Examples:
  - 'Hello World' -> 'hello_world'
  - 'hello-world' -> 'hello_world'"
  [s]
  (when (some? s)
    (-> (str s)
      (str/lower-case)
      (str/replace #"[-\s]+" "_")
      (str/replace #"[^a-z0-9_]+" "_")
      (str/replace #"_+" "_")
      (str/replace #"^_|_$" ""))))

(defn camel-case
  "Convert string to camelCase (e.g., 'hello-world' -> 'helloWorld')"
  [s]
  (when s
    (let [words (str/split (str s) #"[^a-zA-Z0-9]+")]
      (if (empty? words)
        ""
        (str (str/lower-case (first words))
          (str/join ""
            (map str/capitalize (rest words))))))))

;; ========================================
;; String Cleaning and Normalization
;; ========================================

(defn slugify
  "Convert a value to a URL-friendly slug (kebab-case, alnum + dash)."
  [s]
  (when (some? s)
    (-> (str s)
      (str/lower-case)
      (str/replace #"[^a-z0-9\s_-]" "")
      (str/replace #"[\s_]+" "-")
      (str/replace #"-+" "-")
      (str/replace #"^-|-$" ""))))

(defn clean-whitespace
  "Clean up whitespace in a value (trim and normalize internal whitespace)."
  [s]
  (when (some? s)
    (-> (str s)
      (str/trim)
      (str/replace #"\s+" " "))))

;; ========================================
;; String Validation Helpers
;; ========================================

(defn blank?
  "True when s is nil, empty, or contains only whitespace.

  Non-string values are treated as non-blank."
  [s]
  (or (nil? s)
    (and (string? s) (str/blank? s))))

(defn not-blank?
  "Negation of `blank?`."
  [s]
  (not (blank? s)))

(defn non-empty-string?
  "True when s is a non-blank string."
  [s]
  (and (string? s) (not (str/blank? s))))

;; ========================================
;; Platform-specific String Operations
;; ========================================

(defn safe-parse-int
  "Safely parse a string to an integer, returning nil on failure.

  Notes:
  - Accepts leading/trailing whitespace
  - Rejects empty/blank strings"
  [s]
  (when (and (string? s) (not (str/blank? s)))
    (try
      #?(:clj (Long/parseLong (str/trim s))
         :cljs (let [n (js/parseInt (str/trim s) 10)]
                 (when-not (js/isNaN n) n)))
      (catch #?(:clj Exception :cljs js/Error) _
        nil))))

(defn safe-parse-double
  "Safely parse string to double, returning nil on failure"
  [s]
  (when (and s (string? s))
    (try
      #?(:clj (Double/parseDouble (str/trim s))
         :cljs (let [n (js/parseFloat (str/trim s))]
                 (when-not (js/isNaN n) n)))
      (catch #?(:clj Exception :cljs js/Error) _
        nil))))

;; ========================================
;; Legacy Deprecation Warnings
;; ========================================

;; (intentionally empty)
