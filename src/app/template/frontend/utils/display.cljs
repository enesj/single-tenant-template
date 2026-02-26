(ns app.template.frontend.utils.display
  "Shared display/formatting utilities for ClojureScript UIs.

  Note: this was extracted from admin UI formatting helpers so domain/template/admin
  code can share the same formatting behavior without depending on admin namespaces."
  (:require
    [app.template.frontend.utils.timestamp :as timestamp]
    [clojure.string :as str]
    [goog.object :as gobj]))

(defn react-element?
  "Best-effort check if a value is a React element."
  [value]
  (and (some? value) (object? value) (gobj/get value "$$typeof")))

(defn format-value
  "Format a value for display.

  Handles nil, booleans, keywords, strings, numbers, React elements, and vectors.

  Args:
  - value: value to format
  - nil-text: text to display for nil values (default: \"—\")
  - capitalize?: whether to capitalize keyword/string values (default: true)"
  ([value]
   (format-value value "—" true))
  ([value nil-text]
   (format-value value nil-text true))
  ([value nil-text capitalize?]
   (cond
     (nil? value) nil-text
     (react-element? value) value
     (vector? value) value
     (boolean? value) (if value "Yes" "No")
     (keyword? value) (-> value
                        name
                        (str/replace "-" " ")
                        ((if capitalize? str/capitalize identity)))
     (string? value) ((if capitalize? identity str/trim) value)
     (number? value) value
     :else (str value))))

(defn format-date
  "Format an ISO date string to canonical Created-style display format."
  [date-str]
  (when (and date-str (not= date-str "N/A") (not= date-str "—"))
    (timestamp/format-timestamp-string date-str)))

(defn user-initials
  "Return graceful initials based on full name or email."
  [full-name email]
  (let [source (or full-name email "")
        parts (->> (str/split source #"\\s+")
                (remove str/blank?)
                (take 2))
        initials (->> parts
                   (map #(subs % 0 (min 1 (count %))))
                   (str/join "")
                   (str/upper-case))]
    (cond
      (seq initials) initials
      (and email (not (str/blank? email))) (-> email (subs 0 1) str/upper-case)
      :else "U")))

(defn tenant-label
  "Create a formatted tenant label with name and slug."
  [tenant-name tenant-slug]
  (cond
    (and tenant-name tenant-slug) (str tenant-name " (" tenant-slug ")")
    tenant-name tenant-name
    tenant-slug tenant-slug
    :else "—"))
