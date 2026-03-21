(ns app.template.frontend.utils.timestamp
  "Canonical read-only timestamp formatting used across frontend surfaces.

  Default output matches the existing `Created` list column style:
  `Mon DD HH:mm:ss` in local time using en-US short month names."
  (:require
    [uix.core :refer [$]]))

(defn- parse-date
  [value]
  (cond
    (instance? js/Date value) value
    (string? value) (js/Date. value)
    (number? value) (js/Date. value)
    :else nil))

(defn- valid-date?
  [date]
  (and (instance? js/Date date)
    (not (js/isNaN (.getTime date)))))

(defn- pad2
  [n]
  (if (< n 10)
    (str "0" n)
    (str n)))

(defn- date-parts
  [date use-utc?]
  (let [month-options (if use-utc?
                        #js {:month "short" :timeZone "UTC"}
                        #js {:month "short"})
        month (.toLocaleString date "en-US" month-options)
        day ((if use-utc? #(.getUTCDate %) #(.getDate %)) date)
        hours ((if use-utc? #(.getUTCHours %) #(.getHours %)) date)
        minutes ((if use-utc? #(.getUTCMinutes %) #(.getMinutes %)) date)
        seconds ((if use-utc? #(.getUTCSeconds %) #(.getSeconds %)) date)]
    {:month month
     :day day
     :hh (pad2 hours)
     :mm (pad2 minutes)
     :ss (pad2 seconds)}))

(defn format-timestamp-string
  "Format a timestamp into canonical Created-style text.

  Options:
  - `:use-utc?`  -> deterministic UTC formatting for tests (default false)
  - `:nil-text`  -> text for nil/unparseable values (default nil)

  Returns the original string representation when input is non-nil but invalid."
  ([value]
   (format-timestamp-string value {}))
  ([value {:keys [use-utc? nil-text]
           :or {use-utc? false
                nil-text nil}}]
   (if-let [date (parse-date value)]
     (if (valid-date? date)
       (let [{:keys [month day hh mm ss]} (date-parts date use-utc?)]
         (str month " " day " " hh ":" mm ":" ss))
       (or (some-> value str) nil-text))
     nil-text)))

(defn render-timestamp
  "Render canonical Created-style timestamp markup.

  Options:
  - `:use-utc?`          deterministic UTC formatting for tests
  - `:show-seconds?`     include `:ss` segment (default true)
  - `:highlight-seconds?` render seconds in warning color (default true)
  - `:nil-text`          fallback display for nil/unparseable values (default nil)"
  ([value]
   (render-timestamp value {}))
  ([value {:keys [use-utc? show-seconds? highlight-seconds? nil-text]
           :or {use-utc? false
                show-seconds? true
                highlight-seconds? true
                nil-text nil}}]
   (if-let [date (parse-date value)]
     (if (valid-date? date)
       (let [{:keys [month day hh mm ss]} (date-parts date use-utc?)]
         ($ :span {:class "whitespace-nowrap"}
           ($ :span (str month " " day))
           ($ :span {:class "ml-1"} (str hh ":" mm))
           (when show-seconds?
             (if highlight-seconds?
               ($ :span {:class "text-warning"} (str ":" ss))
               ($ :span (str ":" ss))))))
       ($ :span (or (some-> value str) nil-text)))
     (when (some? nil-text)
       ($ :span nil-text)))))
