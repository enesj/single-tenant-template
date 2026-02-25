(ns app.domain.backend.expenses.services.stores.city
  (:require
    [app.domain.backend.expenses.services.places-api :as places-api]
    [clojure.string :as str]
    [taoensso.timbre :as log]))

(defn- title-case [s]
  (when s
    (->> (str/split s #"\s+")
      (map str/capitalize)
      (str/join " "))))

(defn- clean-city-source [s]
  (when-let [text (some-> s str str/trim not-empty)]
    (-> text
      (str/replace #"[\r\n\t]+" " ")
      (str/replace #"\s{2,}" " ")
      (str/replace #",{2,}" ",")
      (str/replace #"^[\"']|[\"']$" "")
      (str/replace #"(\d{2})\s+(\d{3})" "$1$2")
      str/trim
      not-empty)))

(defn- normalize-city-candidate [candidate]
  (when-let [text (some-> candidate str str/trim not-empty)]
    (-> text
      (str/replace #"^[,.\-;:]+|[,.\-;:]+$" "")
      str/trim
      (str/replace #"(?i)\s+(broj|br\.?)\s+\d+$" "")
      str/trim
      (str/replace #"\s{2,}" " ")
      title-case
      not-empty)))

(defn- valid-city-candidate? [candidate]
  (when-let [text (some-> candidate str str/trim not-empty)]
    (let [lower-text (str/lower-case text)
          token-count (count (str/split text #"\s+"))
          has-legal-entity? (or (str/includes? lower-text "d.o.o")
                              (str/includes? lower-text "doo")
                              (str/includes? lower-text "ltd")
                              (str/includes? lower-text "llc"))
          noise-tokens #{"tropic" "maloprodaja" "prodavnica" "hipemarket" "market"}
          has-noise-token? (some #(str/includes? lower-text %) noise-tokens)]
      (and (>= (count text) 2)
        (<= (count text) 40)
        (re-find #"\p{L}" text)
        (not (re-find #"\d" text))
        (not (re-find #"[\r\n\t]" text))
        (not has-legal-entity?)
        (not has-noise-token?)
        (<= token-count 5)))))

(defn- extract-city-from-display-name [display-name]
  (when-let [cleaned (clean-city-source display-name)]
    (or
      (when-let [match (re-find #"^(ogranak|podružnica|podruznica|poslovnica|centar)[\s:.\-]+(.+)$"
                         (str/lower-case cleaned))]
        (let [tail (nth match 2)
              candidate (normalize-city-candidate tail)]
          (when (valid-city-candidate? candidate)
            candidate)))
      (when (re-find #"[\-,/|]" cleaned)
        (let [segments (str/split cleaned #"[\-,/|]")
              last-seg (some-> segments last str/trim not-empty)
              candidate (normalize-city-candidate last-seg)]
          (when (valid-city-candidate? candidate)
            candidate)))
      (when (and (< (count cleaned) 50)
              (not (re-find #"[\-,/|]" cleaned)))
        (let [tokens (str/split cleaned #"\s+")
              last-token (some-> tokens last str/trim not-empty)
              candidate (normalize-city-candidate last-token)]
          (when (valid-city-candidate? candidate)
            candidate))))))

(defn- extract-city-via-places-api [place-id places-config]
  (when (and (some-> place-id str str/trim not-empty)
          (map? places-config)
          (seq (:api-key places-config)))
    (try
      (when-let [details (places-api/get-place-details! places-config place-id)]
        (when-let [raw-city (places-api/extract-city-from-address-components
                              (:address-components details))]
          (let [normalized (normalize-city-candidate raw-city)]
            (when (valid-city-candidate? normalized)
              normalized))))
      (catch Exception e
        (log/warn e "Places API fallback failed for city extraction" {:place-id place-id})
        nil))))

(defn extract-city-from-address
  "Extract city from address, with optional display-name and Places fallback."
  ([address]
   (extract-city-from-address address nil nil nil))
  ([address display-name]
   (extract-city-from-address address display-name nil nil))
  ([address display-name place-id places-config]
   (or
     (when-let [addr (some-> address clean-city-source)]
       (let [postal-codes (re-seq #"\d{5}" addr)]
         (when (seq postal-codes)
           (let [last-postal (last postal-codes)
                 idx (.lastIndexOf addr last-postal)
                 after-postal (when (>= idx 0)
                                (subs addr (+ idx (count last-postal))))
                 cleaned (some-> after-postal
                           str/trim
                           (str/replace #"^[,\s]+" "")
                           str/trim
                           not-empty)]
             (when (seq cleaned)
               (let [segments (str/split cleaned #"[,\n]")
                     valid-segment (some (fn [seg]
                                           (let [candidate (normalize-city-candidate seg)]
                                             (when (valid-city-candidate? candidate)
                                               candidate)))
                                     (reverse segments))]
                 valid-segment))))))
     (when display-name
       (extract-city-from-display-name display-name))
     (when (and place-id places-config)
       (extract-city-via-places-api place-id places-config)))))
