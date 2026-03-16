(ns app.domain.backend.expenses.services.cities-normalize
  "Normalization and text extraction helpers for city resolution."
  (:require
    [clojure.string :as str])
  (:import
    [java.text Normalizer Normalizer$Form]))

(def default-country
  "Bosnia and Herzegovina")

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

(defn normalize-zip
  "Normalize ZIP input to a strict 5-digit code."
  [zip-value]
  (when-let [zip* (some-> zip-value str str/trim not-empty)]
    (let [digits (str/replace zip* #"\D" "")]
      (when (= 5 (count digits))
        digits))))

(defn extract-zip-from-text
  "Extract the last valid 5-digit ZIP from free text."
  [text]
  (when-let [text* (some-> text str str/trim not-empty)]
    (let [normalized (-> text*
                       (str/replace #"[\r\n\t]+" " ")
                       (str/replace #"(\d{2})\s+(\d{3})" "$1$2"))
          matcher (re-matcher #"(?<!\d)(\d{5})(?!\d)" normalized)]
      (loop [last-zip nil]
        (if (.find matcher)
          (let [zip (.group matcher 1)
                tail (-> (subs normalized (.end matcher))
                       (str/replace #"^[\s,;:.\-]+" "")
                       str/trim)
                noisy-tail? (boolean (re-find #"^\d{2,}" tail))]
            (recur (if noisy-tail? last-zip zip)))
          last-zip)))))

(defn- valid-city-fallback-candidate?
  [candidate]
  (when-let [candidate* (some-> candidate str str/trim not-empty)]
    (let [lower (str/lower-case candidate*)
          token-count (count (str/split candidate* #"\s+"))]
      (and (<= 2 (count candidate*) 50)
        (<= token-count 4)
        (re-find #"\p{L}" candidate*)
        (not (re-find #"\d" candidate*))
        (not (re-find #"(?i)\b(ul|ulica|bb|broj|br\.?)\b" lower))))))

(defn extract-city-fallback-candidate
  "Extract a conservative city-name candidate from free text."
  [text]
  (when-let [text* (some-> text str str/trim not-empty)]
    (let [normalized (-> text*
                       (str/replace #"[\r\n\t]+" " ")
                       (str/replace #"\s+" " ")
                       str/trim)
          matcher (re-matcher #"(?<!\d)(\d{5})(?!\d)" normalized)
          last-zip-end (loop [last-end nil]
                         (if (.find matcher)
                           (recur (.end matcher))
                           last-end))
          after-zip-segment (when last-zip-end
                              (some-> (subs normalized last-zip-end)
                                (str/replace #"^[\s,;:\-]+" "")
                                (str/split #"[,;|]")
                                first
                                str/trim
                                not-empty))
          tail-segment (some-> normalized
                         (str/split #"[,;|]")
                         last
                         str/trim
                         not-empty)
          candidate (some-> (or after-zip-segment tail-segment)
                      (str/replace #"(?<!\d)\d{5}(?!\d)" "")
                      (str/replace #"^[\s,;:\-]+|[\s,;:\-]+$" "")
                      (str/replace #"\s+" " ")
                      str/trim
                      not-empty)]
      (when (valid-city-fallback-candidate? candidate)
        candidate))))

(defn extract-city-fallback-candidates
  "Returns an ordered seq of unique city-name candidates from free text.
   Tries all segments after the last ZIP code, then the tail segment of the full
   text — so that addresses like '71103 Centar, Sarajevo' yield both
   'Centar' and 'Sarajevo' rather than only the first segment."
  [text]
  (when-let [text* (some-> text str str/trim not-empty)]
    (let [normalized (-> text*
                       (str/replace #"[\r\n\t]+" " ")
                       (str/replace #"\s+" " ")
                       str/trim)
          matcher (re-matcher #"(?<!\d)(\d{5})(?!\d)" normalized)
          last-zip-end (loop [last-end nil]
                         (if (.find matcher)
                           (recur (.end matcher))
                           last-end))
          normalize-seg (fn [s]
                          (some-> s
                            (str/replace #"(?<!\d)\d{5}(?!\d)" "")
                            (str/replace #"^[\s,;:\-]+|[\s,;:\-]+$" "")
                            (str/replace #"\s+" " ")
                            str/trim
                            not-empty
                            (#(when (valid-city-fallback-candidate? %) %))))
          after-zip-segs (when last-zip-end
                           (some->> (subs normalized last-zip-end)
                             (#(str/replace % #"^[\s,;:\-]+" ""))
                             not-empty
                             (#(str/split % #"[,;|]"))
                             (keep normalize-seg)
                             seq))
          tail-seg (some-> normalized
                     (str/split #"[,;|]")
                     last
                     normalize-seg)]
      (not-empty
        (distinct
          (remove nil?
            (concat (or after-zip-segs [])
              (when tail-seg [tail-seg]))))))))

(defn places-query-from-text
  "Derive a safer Places query string from noisy OCR/store text."
  [text candidate]
  (when-let [text* (some-> text str str/trim not-empty)]
    (let [candidate* (some-> candidate str str/trim not-empty)
          canonical (-> text*
                      (str/replace #"\r\n?" "\n"))
          segments (->> (str/split canonical #"[,;|\n]+")
                     (map (fn [segment]
                            (-> segment
                              (str/replace #"\s+" " ")
                              str/trim)))
                     (remove str/blank?))
          street-raw (->> segments
                       (filter #(re-find #"(?i)\b(ulica|ul\.?|trg|bulevar)\b" %))
                       (sort-by count)
                       first)
          street-segment (some-> street-raw
                           (str/replace #"(?i)\bdo\s+broja\s+(\d+)\b" "$1")
                           (str/replace #"\s+" " ")
                           str/trim
                           not-empty)]
      (cond
        (and street-segment candidate*)
        (str street-segment ", " candidate*)

        street-segment
        street-segment

        :else
        nil))))

(defn candidate-normalized-keys
  "Build normalized candidate keys with bounded trailing-token reduction."
  [candidate]
  (when-let [normalized (some-> candidate normalize-city-key)]
    (let [tokens (str/split normalized #"\s+")
          token-count (count tokens)
          max-drops (min 2 (dec token-count))]
      (->> (range 0 (inc max-drops))
        (map (fn [drop-count]
               (->> tokens
                 (take (- token-count drop-count))
                 (str/join " "))))
        (remove str/blank?)
        distinct
        vec))))

(defn- levenshtein-distance
  [a b]
  (let [a* (str (or a ""))
        b* (str (or b ""))
        n (count a*)
        m (count b*)]
    (cond
      (zero? n) m
      (zero? m) n
      :else
      (let [prev (int-array (inc m))
            curr (int-array (inc m))]
        (dotimes [j (inc m)]
          (aset-int prev j j))
        (dotimes [i n]
          (aset-int curr 0 (inc i))
          (dotimes [j m]
            (let [cost (if (= (.charAt a* i) (.charAt b* j)) 0 1)
                  del (inc (aget prev (inc j)))
                  ins (inc (aget curr j))
                  sub (+ (aget prev j) cost)]
              (aset-int curr (inc j) (min del ins sub))))
          (System/arraycopy curr 0 prev 0 (inc m)))
        (aget prev m)))))

(defn- normalize-place-display-name->city
  [display-name]
  (when-let [string-value (some-> display-name str str/trim not-empty)]
    (let [first-part (some-> string-value
                       (str/split #"/")
                       first
                       str/trim
                       not-empty)]
      (some-> first-part
        (str/replace #"(?i)^\s*(opština|opstina|općina|opcina|municipality\s+of|grad)\s+" "")
        (str/replace #"\s+" " ")
        str/trim
        not-empty))))

(defn place-city-matches-candidate?
  [candidate place-city-name]
  (let [candidate-keys (some-> candidate candidate-normalized-keys set)
        place-key (some-> place-city-name normalize-city-key)]
    (and (seq candidate-keys)
      (seq place-key)
      (or
        (contains? candidate-keys place-key)
        (some (fn [candidate-key]
                (when (seq candidate-key)
                  (let [token-fuzzy-match?
                        (let [cand-toks (str/split candidate-key #"\s+")
                              place-toks (str/split place-key #"\s+")]
                          (when (and (<= 2 (count cand-toks) 4)
                                  (= (count cand-toks) (count place-toks)))
                            (let [pairs (map vector cand-toks place-toks)
                                  diffs (->> pairs (remove (fn [[left right]] (= left right))))]
                              (when (= 1 (count diffs))
                                (let [[left right] (first diffs)]
                                  (<= (levenshtein-distance left right) 2))))))]
                    (or
                      (str/starts-with? place-key (str candidate-key " "))
                      (str/starts-with? candidate-key (str place-key " "))
                      token-fuzzy-match?
                      (when (and (not (str/includes? candidate-key " "))
                              (not (str/includes? place-key " "))
                              (<= (Math/abs (- (count candidate-key) (count place-key))) 2))
                        (let [dist (levenshtein-distance candidate-key place-key)
                              max-dist (if (<= (max (count candidate-key) (count place-key)) 6) 1 2)]
                          (<= dist max-dist)))))))
          candidate-keys)))))

(def ^:private place-city-type->rank
  {"locality" 0
   "postal_town" 1
   "administrative_area_level_3" 2
   "administrative_area_level_4" 3
   "administrative_area_level_2" 4})

(defn- city-candidates-from-address-components
  [address-components]
  (when (sequential? address-components)
    (->> address-components
      (keep (fn [component]
              (when (map? component)
                (let [types (:types component)
                      rank (some->> types
                             (keep place-city-type->rank)
                             seq
                             (apply min))
                      name (some-> (:longText component) str str/trim not-empty)]
                  (when (and (some? rank) (seq name))
                    {:rank rank :name name})))))
      (sort-by :rank)
      (map :name)
      distinct
      vec)))

(defn extract-city-from-components
  "Extract a city name from Places `addressComponents`."
  ([address-components candidate]
   (extract-city-from-components address-components candidate nil))
  ([address-components candidate place-display-name]
   (let [names (city-candidates-from-address-components address-components)
         candidate* (some-> candidate str str/trim not-empty)
         from-components (cond
                           (and (seq names) (seq candidate*))
                           (some (fn [name]
                                   (when (place-city-matches-candidate? candidate* name)
                                     name))
                             names)

                           (seq names)
                           (first names)

                           :else
                           nil)
         from-display (when-not (seq from-components)
                        (normalize-place-display-name->city place-display-name))]
     (cond
       (seq from-components)
       from-components

       (and (seq from-display) (seq candidate*))
       (when (place-city-matches-candidate? candidate* from-display)
         from-display)

       (seq from-display)
       from-display

       :else
       nil))))
