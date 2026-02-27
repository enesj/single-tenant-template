(ns import-bh-cities
  (:require [clojure.string :as str]
            [clojure.edn :as edn])
  (:import [java.text Normalizer Normalizer$Form]
           [java.util UUID]))

(defn normalize-city-key
  "Normalize city name to a stable key for uniqueness checks."
  [city-name]
  (when-let [name* (some-> city-name str str/trim not-empty)]
    (-> name*
      (Normalizer/normalize Normalizer$Form/NFD)
      (str/replace #"\p{M}+" "")
      (str/replace #"Đ" "D")
      (str/replace #"đ" "d")
      str/lower-case
      (str/replace #"\s+" " ")
      str/trim)))

(defn parse-line [line]
  (let [parts (str/split (str/trim line) #"\s+")]
    (when (>= (count parts) 3)
      (let [zip (nth parts 1)
            name-parts (take-while #(not (re-matches #"\d{5}" %)) (drop 2 parts))
            name (str/join " " name-parts)]
        (when (re-matches #"\d{5}" zip)
          (let [norm (normalize-city-key name)]
            (when (not-empty norm)
              {:zip zip :name name :normalized_key norm})))))))

(defn generate-sql [data]
  (if (empty? data)
    "-- No data extracted"
    (let [header "INSERT INTO cities (id, name, normalized_key, zip, country, created_at, updated_at) VALUES\n"
          values (->> data
                      (map (fn [{:keys [name zip normalized_key]}]
                             (format "('%s', '%s', '%s', '%s', 'Bosnia and Herzegovina', NOW(), NOW())"
                                     (UUID/randomUUID)
                                     (str/replace name "'" "''")
                                     (str/replace normalized_key "'" "''")
                                     zip)))
                      (str/join ",\n"))
          footer "\nON CONFLICT (country, zip) DO NOTHING;"]
      (str header values footer))))

(let [raw-text (slurp *in*)
      lines (str/split-lines raw-text)
      extracted (keep parse-line lines)
      unique-cities-map (reduce (fn [acc {:keys [zip normalized_key] :as city}]
                                  (if (or (contains? (:zips acc) zip)
                                          (contains? (:norms acc) normalized_key))
                                    acc
                                    (-> acc
                                        (update :zips conj zip)
                                        (update :norms conj normalized_key)
                                        (assoc-in [:data zip] city))))
                                {:zips #{} :norms #{} :data {}}
                                extracted)
      data (vals (:data unique-cities-map))]
  (println (generate-sql data)))
