(ns app.admin.frontend.components.format
  "Formatting and export utilities for admin frontend components."
  (:require
    [app.shared.date :as date]
    [clojure.string :as str]
    [goog.object :as gobj]))

(defn react-element?
  "Check if a value is a React element"
  [value]
  (and (some? value) (object? value) (gobj/get value "$$typeof")))

(defn format-value
  "Format a value for display in admin components.
   Handles nil, booleans, keywords, strings, numbers, React elements, and vectors.

   Props:
   - value: The value to format
   - nil-text: Text to display for nil values (default: \"—\")
   - capitalize?: Whether to capitalize string values (default: true)"
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
  "Format ISO date string to readable format (YYYY-MM-DD HH:mm:ss)
   Handles parsing errors gracefully by returning the original string."
  [date-str]
  (when (and date-str (not= date-str "N/A") (not= date-str "—"))
    (try
      ;; Use shared date formatting if available, fall back to manual parsing
      (if-let [parsed-date (date/parse-date-string date-str)]
        (date/format-display-date parsed-date)
        ;; Manual parsing fallback
        (let [date-parts (.split date-str "T")
              date-part (first date-parts)
              time-parts (when (> (count date-parts) 1) (.split (second date-parts) "."))
              time-part (first time-parts)
              year (.substring date-part 0 4)
              month (.substring date-part 5 7)
              day (.substring date-part 8 10)
              hours (when time-part (.substring time-part 0 2))
              minutes (when time-part (.substring time-part 3 5))
              seconds (when time-part (.substring time-part 6 8))]
          (str year "-" month "-" day " " (or hours "00") ":" (or minutes "00") ":" (or seconds "00"))))
      (catch js/Error e
        (js/console.log "Date parsing error for:" date-str e)
        date-str))))

(defn format-relative-time
  "Format date as relative time (e.g., \"2 hours ago\", \"3 days ago\")
   Falls back to absolute date formatting for very old dates."
  [date-str]
  (when date-str
    (try
      (let [date (if (string? date-str)
                   (or (date/parse-date-string date-str)
                     (js/Date. date-str))
                   date-str)
            now (js/Date.)
            diff-ms (- (.getTime now) (.getTime date))
            diff-minutes (/ diff-ms (* 60 1000))
            diff-hours (/ diff-minutes 60)
            diff-days (/ diff-hours 24)]
        (cond
          (< diff-minutes 1) "Just now"
          (< diff-minutes 60) (str (Math/round diff-minutes) " minutes ago")
          (< diff-hours 24) (str (Math/round diff-hours) " hours ago")
          (< diff-days 7) (str (Math/round diff-days) " days ago")
          :else (format-date date-str)))
      (catch js/Error _
        (format-date date-str)))))

(defn user-initials
  "Return graceful initials based on full name or email.

   Props:
   - full-name: User's full name
   - email: User's email address"
  [full-name email]
  (let [source (or full-name email "")
        parts (->> (str/split source #"\s+")
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
  "Create a formatted tenant label with name and slug.

   Props:
   - tenant-name: Tenant name
   - tenant-slug: Tenant slug"
  [tenant-name tenant-slug]
  (cond
    (and tenant-name tenant-slug) (str tenant-name " (" tenant-slug ")")
    tenant-name tenant-name
    tenant-slug tenant-slug
    :else "—"))

(defn create-csv-export-data
  "Prepare data for CSV export with proper formatting.

   Props:
   - data: Vector of maps to export
   - headers: Vector of column headers
   - key-fn: Function to extract values from each map"
  [data headers key-fn]
  (let [header-row (str/join "," headers)
        data-rows (mapv (fn [item]
                          (str/join ","
                            (mapv (fn [header]
                                    (let [value (key-fn item header)]
                                      (cond
                                        (string? value) (str "\"" value "\"")
                                        (nil? value) ""
                                        :else (str value))))
                              headers)))
                    data)]
    (str header-row "\n" (str/join "\n" data-rows))))

(defn download-as-json
  "Download data as JSON file.

   Props:
   - data: Data to download
   - filename: Filename for the download"
  [data filename]
  (let [json-str (js/JSON.stringify (clj->js data) nil 2)
        blob (js/Blob. #js [json-str] #js {:type "application/json"})
        url (.createObjectURL js/URL blob)
        link (js/document.createElement "a")]
    (set! (.-href link) url)
    (set! (.-download link) filename)
    (.click js/document.body link)
    (.revokeObjectURL js/URL url)))
