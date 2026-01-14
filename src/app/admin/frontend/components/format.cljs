(ns app.admin.frontend.components.format
  "Formatting and export utilities for admin frontend components."
  (:require
    [app.template.frontend.utils.display :as display]
    [clojure.string :as str]))

;; Delegate shared formatting behavior to the template layer so admin/domain/template
;; UIs stay consistent without duplicating logic.
(def react-element? display/react-element?)

(def format-value display/format-value)

(def format-date display/format-date)

(def format-relative-time display/format-relative-time)

(def user-initials display/user-initials)

(def tenant-label display/tenant-label)

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
