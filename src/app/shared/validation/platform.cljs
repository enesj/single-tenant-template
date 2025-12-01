(ns app.shared.validation.platform
  "Platform-specific validation utilities for ClojureScript (Browser)."
  (:require
    [clojure.string :as str]
    [malli.core :as m]
    [malli.error :as me]))

;; ========================================
;; Browser-specific validation functions
;; ========================================

(defn validate-file-path
  "Validate file path (browser implementation - limited)."
  [path]
  (when path
    (and (string? path)
      (not (str/blank? path))
      (not (str/includes? path ".."))))) ; Basic security check

(defn validate-uuid-format
  "Validate UUID format using JavaScript regex."
  [uuid-str]
  (when uuid-str
    (let [pattern #"^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"]
      (boolean (re-matches pattern (str/lower-case uuid-str))))))

(defn validate-email-format
  "Validate email format using JavaScript regex."
  [email]
  (when email
    (let [pattern #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$"]
      (boolean (re-matches pattern email)))))

(defn validate-phone-number
  "Validate phone number format (browser implementation)."
  [phone]
  (when phone
    (let [cleaned (str/replace phone #"[^\d]" "")]
      (and (>= (count cleaned) 10)
        (<= (count cleaned) 15)))))

(defn validate-url-format
  "Validate URL format using browser URL constructor."
  [url]
  (when url
    (try
      (js/URL. url)
      true
      (catch js/Error _
        false))))

;; ========================================
;; Browser-specific validation
;; ========================================

(defn validate-dom-element-id
  "Validate DOM element ID format."
  [id]
  (when id
    (and (string? id)
      (not (str/blank? id))
      (re-matches #"^[a-zA-Z][a-zA-Z0-9_-]*$" id))))

(defn validate-css-class
  "Validate CSS class name format."
  [class-name]
  (when class-name
    (and (string? class-name)
      (not (str/blank? class-name))
      (re-matches #"^[a-zA-Z_-][a-zA-Z0-9_-]*$" class-name))))

(defn validate-color-hex
  "Validate hexadecimal color format."
  [color]
  (when color
    (re-matches #"^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$" color)))

(defn validate-media-query
  "Validate CSS media query format."
  [query]
  (when query
    (and (string? query)
      (str/includes? query "@media"))))

;; ========================================
;; Client-side validation utilities
;; ========================================

(defn validate-json-string
  "Validate JSON string format."
  [json-str]
  (when json-str
    (try
      (js/JSON.parse json-str)
      true
      (catch js/Error _
        false))))

(defn validate-local-storage-key
  "Validate localStorage key format."
  [key]
  (when key
    (and (string? key)
      (not (str/blank? key))
      (< (count key) 1024)))) ; Browser storage key limit

;; ========================================
;; Performance utilities
;; ========================================

(defn validate-with-timeout
  "Validate with timeout (browser implementation using setTimeout)."
  [validator value timeout-ms]
  (js/Promise.
    (fn [resolve _reject]
      (let [timeout-id (js/setTimeout
                         #(resolve {:valid? false :error "Validation timeout"})
                         timeout-ms)]
        (try
          (let [result (validator value)]
            (js/clearTimeout timeout-id)
            (resolve {:valid? result}))
          (catch js/Error e
            (js/clearTimeout timeout-id)
            (resolve {:valid? false :error (str "Validation error: " (.-message e))})))))))

;; ========================================
;; Browser API validation
;; ========================================

(defn validate-geolocation-coords
  "Validate geolocation coordinates."
  [coords]
  (when coords
    (and (map? coords)
      (number? (:latitude coords))
      (number? (:longitude coords))
      (>= (:latitude coords) -90)
      (<= (:latitude coords) 90)
      (>= (:longitude coords) -180)
      (<= (:longitude coords) 180))))

(defn validate-file-input
  "Validate file input from browser."
  [file-info]
  (when file-info
    (and (map? file-info)
      (string? (:name file-info))
      (string? (:type file-info))
      (number? (:size file-info))
      (pos? (:size file-info)))))

;; ========================================
;; Malli integration
;; ========================================

(defn create-platform-schema
  "Create platform-specific Malli schema."
  []
  [:map
   [:file-path {:optional true} [:fn validate-file-path]]
   [:uuid {:optional true} [:fn validate-uuid-format]]
   [:email {:optional true} [:fn validate-email-format]]
   [:phone {:optional true} [:fn validate-phone-number]]
   [:url {:optional true} [:fn validate-url-format]]
   [:dom-id {:optional true} [:fn validate-dom-element-id]]
   [:css-class {:optional true} [:fn validate-css-class]]
   [:color-hex {:optional true} [:fn validate-color-hex]]
   [:json-string {:optional true} [:fn validate-json-string]]
   [:geolocation {:optional true} [:fn validate-geolocation-coords]]
   [:file-input {:optional true} [:fn validate-file-input]]])

(defn validate-with-platform-schema
  "Validate data using platform-specific schema."
  [data]
  (let [schema (create-platform-schema)]
    (if (m/validate schema data)
      {:valid? true :data data}
      {:valid? false :errors (me/humanize (m/explain schema data))})))
