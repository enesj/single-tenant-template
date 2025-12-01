(ns app.shared.string
  "Cross-platform string utilities for both frontend and backend.

   Provides common string manipulation functions that work consistently
   across Clojure and ClojureScript environments."
  (:require
    [clojure.set]
    [clojure.string]))

;; ========================================
;; String Case Conversion
;; ========================================

(defn kebab-case
  "Convert string to kebab-case (e.g., 'Hello World' -> 'hello-world')"
  [s]
  (when s
    (-> s
      str
      (.toLowerCase)
      (clojure.string/replace #"[^a-zA-Z0-9]+" "-")
      (clojure.string/replace #"^-|-$" ""))))

(defn snake-case
  "Convert string to snake_case (e.g., 'Hello World' -> 'hello_world')"
  [s]
  (when s
    (-> s
      str
      (.toLowerCase)
      (clojure.string/replace #"[^a-zA-Z0-9]+" "_")
      (clojure.string/replace #"^_|_$" ""))))

(defn camel-case
  "Convert string to camelCase (e.g., 'hello-world' -> 'helloWorld')"
  [s]
  (when s
    (let [words (clojure.string/split (str s) #"[^a-zA-Z0-9]+")]
      (if (empty? words)
        ""
        (str (clojure.string/lower-case (first words))
          (clojure.string/join ""
            (map clojure.string/capitalize (rest words))))))))

(defn pascal-case
  "Convert string to PascalCase (e.g., 'hello-world' -> 'HelloWorld')"
  [s]
  (when s
    (let [words (clojure.string/split (str s) #"[^a-zA-Z0-9]+")]
      (clojure.string/join "" (map clojure.string/capitalize words)))))

;; ========================================
;; String Cleaning and Normalization
;; ========================================

(defn slugify
  "Convert string to URL-friendly slug"
  [s]
  (when s
    (-> s
      str
      (.toLowerCase)
      (clojure.string/replace #"[^\w\s-]" "")
      (clojure.string/replace #"[\s_-]+" "-")
      (clojure.string/replace #"^-|-$" ""))))

(defn clean-whitespace
  "Clean up whitespace in string (trim and normalize internal whitespace)"
  [s]
  (when s
    (-> s
      str
      clojure.string/trim
      (clojure.string/replace #"\s+" " "))))

(defn remove-non-alphanumeric
  "Remove all non-alphanumeric characters except spaces"
  [s]
  (when s
    (clojure.string/replace (str s) #"[^a-zA-Z0-9\s]" "")))

(defn normalize-phone
  "Normalize phone number to digits only"
  [phone]
  (when phone
    (clojure.string/replace (str phone) #"[^\d]" "")))

;; ========================================
;; String Truncation and Ellipsis
;; ========================================

(defn truncate
  "Truncate string to max-length, optionally adding suffix"
  ([s max-length]
   (truncate s max-length "..."))
  ([s max-length suffix]
   (when s
     (let [s-str (str s)]
       (if (<= (count s-str) max-length)
         s-str
         (str (subs s-str 0 (- max-length (count suffix))) suffix))))))

(defn truncate-words
  "Truncate string to max number of words"
  ([s max-words]
   (truncate-words s max-words "..."))
  ([s max-words suffix]
   (when s
     (let [words (clojure.string/split (str s) #"\s+")]
       (if (<= (count words) max-words)
         (clojure.string/join " " words)
         (str (clojure.string/join " " (take max-words words)) suffix))))))

(defn ellipsis-middle
  "Add ellipsis in middle of string if too long"
  [s max-length]
  (when s
    (let [s-str (str s)]
      (if (<= (count s-str) max-length)
        s-str
        (let [side-length (quot (- max-length 3) 2)
              start (subs s-str 0 side-length)
              end (subs s-str (- (count s-str) side-length))]
          (str start "..." end))))))

;; ========================================
;; String Interpolation and Templates
;; ========================================

(defn interpolate
  "Safe string interpolation using a map of replacements.

   Example: (interpolate 'Hello {{name}}!' {:name 'World'})
   Returns: 'Hello World!'"
  [template replacements]
  (when template
    (reduce-kv
      (fn [s k v]
        (clojure.string/replace s (str "{{" (name k) "}}") (str v)))
      (str template)
      (or replacements {}))))

(defn template-vars
  "Extract template variable names from a string.

   Example: (template-vars 'Hello {{name}} {{surname}}!')
   Returns: ['name' 'surname']"
  [template]
  (when template
    (mapv #(second %)
      (re-seq #"\{\{([^}]+)\}\}" (str template)))))

;; ========================================
;; String Validation Helpers
;; ========================================

(defn blank?
  "Check if string is nil, empty, or contains only whitespace"
  [s]
  (or (nil? s)
    (and (string? s) (clojure.string/blank? s))))

(defn not-blank?
  "Check if string is not blank"
  [s]
  (not (blank? s)))

(defn non-empty-string?
  "Check if value is a non-empty string"
  [s]
  (and (string? s) (not (clojure.string/blank? s))))

(defn starts-with-any?
  "Check if string starts with any of the given prefixes"
  [s prefixes]
  (when (and s (seq prefixes))
    (some #(clojure.string/starts-with? (str s) (str %)) prefixes)))

(defn ends-with-any?
  "Check if string ends with any of the given suffixes"
  [s suffixes]
  (when (and s (seq suffixes))
    (some #(clojure.string/ends-with? (str s) (str %)) suffixes)))

(defn contains-any?
  "Check if string contains any of the given substrings"
  [s substrings]
  (when (and s (seq substrings))
    (some #(clojure.string/includes? (str s) (str %)) substrings)))

;; ========================================
;; String Comparison Utilities
;; ========================================

(defn similarity-score
  "Calculate similarity score between two strings (0.0 to 1.0)
   Using simple character overlap ratio"
  [s1 s2]
  (when (and s1 s2)
    (let [str1 (str s1)
          str2 (str s2)
          chars1 (set str1)
          chars2 (set str2)
          intersection (clojure.set/intersection chars1 chars2)
          union (clojure.set/union chars1 chars2)]
      (if (empty? union)
        1.0
        (double (/ (count intersection) (count union)))))))

(defn fuzzy-match?
  "Check if strings are similar within threshold (0.0 to 1.0)"
  [s1 s2 threshold]
  (>= (similarity-score s1 s2) threshold))

;; ========================================
;; Number and Currency Formatting
;; ========================================

(defn format-currency
  "Format number as currency string"
  ([amount] (format-currency amount "$"))
  ([amount symbol]
   (when amount
     #?(:clj (format "%s%.2f" symbol (double amount))
        :cljs (str symbol (.toFixed (js/Number amount) 2))))))

(defn format-number
  "Format number with thousands separators"
  [n]
  (when n
    #?(:clj (format "%,d" (long n))
       :cljs (.toLocaleString (js/Number n)))))

(defn format-percentage
  "Format number as percentage"
  ([ratio] (format-percentage ratio 1))
  ([ratio decimals]
   (when ratio
     #?(:clj (format (str "%." decimals "f%%") (* ratio 100))
        :cljs (str (.toFixed (* (js/Number ratio) 100) decimals) "%")))))

;; ========================================
;; Platform-specific String Operations
;; ========================================

(defn safe-parse-int
  "Safely parse string to integer, returning nil on failure"
  [s]
  (when (and s (string? s))
    (try
      #?(:clj (Integer/parseInt (clojure.string/trim s))
         :cljs (let [n (js/parseInt (clojure.string/trim s))]
                 (when-not (js/isNaN n) n)))
      (catch #?(:clj Exception :cljs js/Error) _
        nil))))

(defn safe-parse-double
  "Safely parse string to double, returning nil on failure"
  [s]
  (when (and s (string? s))
    (try
      #?(:clj (Double/parseDouble (clojure.string/trim s))
         :cljs (let [n (js/parseFloat (clojure.string/trim s))]
                 (when-not (js/isNaN n) n)))
      (catch #?(:clj Exception :cljs js/Error) _
        nil))))

;; ========================================
;; Legacy Deprecation Warnings
;; ========================================

;; Function removed - use appropriate function from app.shared.string
