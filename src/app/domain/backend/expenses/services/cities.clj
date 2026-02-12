(ns app.domain.backend.expenses.services.cities
  "City ZIP-based lookup services.

  Provides ZIP normalization/extraction and city lookup by `country + zip`.
  Store processing should resolve `city_id` only from ZIPs present in source text."
  (:require
    [app.domain.backend.expenses.services.places-api :as places-api]
    [app.domain.backend.expenses.services.service-configs :as configs]
    [app.domain.backend.expenses.services.services-factory :as factory]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.text Normalizer Normalizer$Form]
    [java.time Instant]))

(defn normalize-city-key
  "Normalize city name to a stable key for uniqueness checks.
  
  Applies:
  - ASCII normalization (strip diacritics, handle Đ/đ)
  - Lowercase conversion
  - Whitespace trimming and collapsing
  
  Similar to normalize-store-key but tailored for city names.
  
  Examples:
    (normalize-city-key \"Sarajevo\") 
    ;; => \"sarajevo\"
    
    (normalize-city-key \"Banja Luka\") 
    ;; => \"banja luka\"
    
    (normalize-city-key \"MOSTAR\") 
    ;; => \"mostar\"
    
    (normalize-city-key \"  Tuzla  \") 
    ;; => \"tuzla\""
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

(defn find-city-by-normalized-key
  "Query cities table by normalized_key.
  
  Returns city row (map with :id, :name, :normalized_key, timestamps) or nil.
  
  Example:
    (find-city-by-normalized-key db \"sarajevo\")
    ;; => {:id #uuid \"...\", :name \"Sarajevo\", :normalized_key \"sarajevo\", ...}"
  [db normalized-key]
  (when-let [key* (some-> normalized-key str str/trim not-empty)]
    (jdbc/execute-one!
      db
      (sql/format {:select [:*]
                   :from [:cities]
                   :where [:= :normalized_key key*]
                   :limit 1})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn ensure-city!
  "Upsert city by normalized_key, return city id.
  
  Computes normalized_key from city-name and uses INSERT ... ON CONFLICT
  to ensure idempotency. Safe for concurrent use.
  
  Algorithm:
  1. Normalize city-name to compute normalized_key
  2. INSERT with ON CONFLICT (normalized_key) DO UPDATE SET updated_at = now()
  3. Return city id
  
  Examples:
    (ensure-city! db \"Sarajevo\")
    ;; => #uuid \"...\"  (creates or returns existing)
    
    (ensure-city! db \"sarajevo\")
    ;; => #uuid \"...\"  (same id, normalized to \"sarajevo\")
    
    (ensure-city! db \"SARAJEVO\")
    ;; => #uuid \"...\"  (same id, normalized)"
  [db city-name]
  (when-let [name* (some-> city-name str str/trim not-empty)]
    (let [normalized (normalize-city-key name*)
          row (jdbc/execute-one!
                db
                (sql/format {:insert-into :cities
                             :values [{:name name*
                                       :normalized_key normalized
                                       :created_at [:now]
                                       :updated_at [:now]}]
                             :on-conflict [:normalized_key]
                             :do-update-set {:updated_at [:now]}
                             :returning [:id]})
                {:builder-fn rs/as-unqualified-lower-maps})]
      (:id row))))

(def ^:private default-country
  "Bosnia and Herzegovina")

(defn normalize-zip
  "Normalize ZIP input to a strict 5-digit code.

  Accepts compact and spaced formats (e.g. `71000`, `71 000`).
  Returns nil for blank/invalid inputs."
  [zip-value]
  (when-let [zip* (some-> zip-value str str/trim not-empty)]
    (let [digits (str/replace zip* #"\D" "")]
      (when (= 5 (count digits))
        digits))))

(defn extract-zip-from-text
  "Extract the last valid 5-digit ZIP from free text.

  Behavior:
  - Normalizes spaced ZIPs (`71 000` -> `71000`)
  - Prefers the last valid ZIP when multiple ZIPs are present
  - Rejects suspicious numeric tails to avoid unsafe resolutions

  Returns ZIP string or nil."
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

(defn find-city-by-country-and-zip
  "Find a city row by exact `country + zip`.

  Country defaults to Bosnia and Herzegovina when nil/blank.
  ZIP is normalized via `normalize-zip`.

  Returns city row map or nil."
  [db country zip]
  (let [country* (or (some-> country str str/trim not-empty)
                   default-country)
        zip* (normalize-zip zip)]
    (when zip*
      (jdbc/execute-one!
        db
        (sql/format {:select [:id :name :country :zip]
                     :from [:cities]
                     :where [:and
                             [:= :country country*]
                             [:= :zip zip*]]
                     :limit 1})
        {:builder-fn rs/as-unqualified-lower-maps}))))

(defn- valid-city-fallback-candidate?
  "Validate whether text is safe to use as a city-name fallback candidate."
  [candidate]
  (when-let [candidate* (some-> candidate str str/trim not-empty)]
    (let [lower (str/lower-case candidate*)
          token-count (count (str/split candidate* #"\s+"))]
      (and (<= 2 (count candidate*) 50)
        (<= token-count 4)
        (re-find #"\p{L}" candidate*)
        (not (re-find #"\d" candidate*))
        (not (re-find #"(?i)\b(ul|ulica|bb|broj|br\.?)\b" lower))))))

(defn- extract-city-fallback-candidate
  "Extract a conservative city-name candidate from free text.

  Preference order:
  1) Segment immediately after the last 5-digit ZIP
  2) Last delimited segment in the input text"
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

(defn- places-query-from-text
  "Derive a safer Places query string from noisy OCR/store text.

  Tries to extract a street-like segment (e.g. containing `ulica`, `ul.`, `trg`)
  and combines it with the city candidate when present.

  Returns nil when no useful segment can be derived."
  [text candidate]
  (when-let [text* (some-> text str str/trim not-empty)]
    (let [candidate* (some-> candidate str str/trim not-empty)
          canonical (-> text*
                      ;; Keep line boundaries for segmentation; normalize only CRLF.
                      (str/replace #"\r\n?" "\n"))
          segments (->> (str/split canonical #"[,;|\n]+")
                     (map (fn [s]
                            (-> s
                              (str/replace #"\s+" " ")
                              str/trim)))
                     (remove str/blank?))
          street-raw (->> segments
                       (filter #(re-find #"(?i)\b(ulica|ul\.?|trg|bulevar)\b" %))
                       ;; Prefer the shortest matching segment to avoid swallowing whole OCR blobs.
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

(defn- candidate-normalized-keys
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

(defn- find-city-id-by-candidate
  "Resolve city id by normalized-key candidate with bounded token reduction.

  For compound city labels such as `Sarajevo Centar` or `Sarajevo Novi Grad`,
  tries exact normalized key first, then drops up to 2 trailing tokens.
  Returns nil when no existing city matches."
  [db candidate]
  (some (fn [normalized-key]
          (some-> (find-city-by-normalized-key db normalized-key)
            :id))
    (candidate-normalized-keys candidate)))

(defn resolve-city-id-from-text
  "Resolve `city_id` from free text with strict ZIP precedence.

  Arities:
  - (resolve-city-id-from-text db text)
  - (resolve-city-id-from-text db country text)

  Resolution order:
  1. If ZIP is detected, lookup existing city only by exact `country + zip`.
     Returns that city id or nil.
  2. If ZIP is not detected, derive a conservative city-name candidate from
     the text and lookup existing city by normalized key with bounded
     trailing-token reduction.

  A ZIP lookup miss does not trigger city-name fallback.
  This function never creates/upserts cities and returns nil when unresolved."
  ([db text]
   (resolve-city-id-from-text db default-country text))
  ([db country text]
   (if-let [zip (extract-zip-from-text text)]
     (some-> (find-city-by-country-and-zip db country zip)
       :id)
     (some-> (extract-city-fallback-candidate text)
       (#(find-city-id-by-candidate db %))))))

(defn- levenshtein-distance
  "Compute Levenshtein edit distance between two strings.

  Used conservatively to tolerate minor OCR noise (for example: lildza vs ilidza).
  Returns a non-negative integer."
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
  "Extract a city-like label from a Places displayName.

  This is a fallback for cases where `addressComponents` does not include
  `locality`/`postal_town`/`administrative_area_level_*` fields.

  Example:
    \"Opština Istočna Ilidža / Општина Источна Илиџа\" -> \"Istočna Ilidža\""
  [display-name]
  (when-let [s (some-> display-name str str/trim not-empty)]
    (let [first-part (some-> s
                       (str/split #"/")
                       first
                       str/trim
                       not-empty)
          without-prefix (some-> first-part
                           (str/replace #"(?i)^\s*(opština|opstina|općina|opcina|municipality\s+of|grad)\s+" "")
                           (str/replace #"\s+" " ")
                           str/trim
                           not-empty)]
      without-prefix)))

(defn- place-city-matches-candidate?
  "Return true when a Places city name is compatible with a candidate.

  Matching uses normalized forms and bounded trailing-token reduction on the
  candidate (for example: Sarajevo Centar matches Sarajevo).

  Additionally:
  - allow prefix matching when the candidate reduces to a shorter token form
    (for example: Istocna <-> Istocna Ilidza)
  - allow a very small Levenshtein distance for a single differing token to
    tolerate common OCR noise (for example: lildza <-> ilidza)

  This is intentionally conservative."
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
                                  diffs (->> pairs (remove (fn [[a b]] (= a b))))]
                              (when (= 1 (count diffs))
                                (let [[a b] (first diffs)]
                                  (<= (levenshtein-distance a b) 2))))))]
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
  "Return possible city names from Places `addressComponents` in priority order."
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

(defn- extract-city-from-components
  "Extract a city name from Places `addressComponents`.

  When `candidate` is provided, return the first component city name that matches
  the candidate normalization.

  When no suitable city-like component exists, optionally fall back to a cleaned
  Places display name (for example: \"Opština Istočna Ilidža / ...\").

  Returns nil when no safe match is found."
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

(defn- confirm-city-via-places
  "Confirm a city name for ZIP via Places API.

  Returns a confirmed city name string or nil.

  Safety constraints:
  - Places config must have API key
  - candidate must exist
  - Places postal code must match extracted ZIP
  - Places city must match candidate normalization"
  [zip candidate {:keys [places-cfg user-region] :as _opts}]
  (let [zip* (normalize-zip zip)
        candidate* (some-> candidate str str/trim not-empty)
        cfg (when (and (map? places-cfg)
                    (seq (:api-key places-cfg)))
              places-cfg)]
    (when (and zip* candidate* cfg)
      (let [query (str candidate* " " zip*)
            search-opts {:region-code (or user-region (:region-code cfg))
                         :language-code (:language-code cfg)
                         :max-results (or (:max-results cfg) 3)
                         :location-bias (:location-bias cfg)
                         :field-mask "places.displayName,places.id,places.formattedAddress,places.addressComponents"}
            places (:places (places-api/search-text! cfg query search-opts))]
        (some (fn [place]
                (let [place-id (some-> place :raw :id str str/trim not-empty)
                      search-components (get-in place [:raw :addressComponents])
                      search-formatted-address (get-in place [:raw :formattedAddress])]
                  (when (seq place-id)
                    (let [details (when-not (seq search-components)
                                    (places-api/get-place-details! cfg place-id))
                          components (or search-components
                                       (:address-components details))
                          formatted-address (or search-formatted-address
                                              (:formatted-address details))
                          place-display-name (or (:name place)
                                               (get-in place [:raw :displayName :text])
                                               (get-in place [:raw :displayName])
                                               (:display-name details))
                          place-city (some-> (extract-city-from-components components candidate* place-display-name)
                                       str
                                       str/trim
                                       not-empty)
                          place-zip (or
                                      (some-> (places-api/extract-postal-code-from-address-components components)
                                        normalize-zip)
                                      (some-> (extract-zip-from-text formatted-address)
                                        normalize-zip))]
                      (when (and (= zip* place-zip)
                              (place-city-matches-candidate? candidate* place-city))
                        place-city)))))
          places)))))

(defn- infer-city-and-zip-via-places
  "Infer {:city-name <string> :zip <string>} from free text via Places.

  This is used as a fallback when ZIP/candidate extraction is incomplete.

  Constraints:
  - Requires a Places API key
  - When `expected-zip` is provided, the inferred ZIP must match it
  - When `candidate` is provided, the inferred city must match the candidate
    normalization (with conservative prefix tolerance)
  - When `candidate` is absent, the inferred city must be mentioned in the
    original text (normalized contains check), OR (when an explicit `query-text`
    is provided) the returned place must match a street/route segment contained
    in the query text (via route component or formatted address).

  Returns nil when inference is unsafe or no match is found."
  [text {:keys [places-cfg user-region] :as _opts} & {:keys [expected-zip candidate query-text]}]
  (let [text* (some-> text str str/trim not-empty)
        explicit-query? (boolean (some-> query-text str str/trim not-empty))
        query* (some-> (or query-text text*) str str/trim not-empty)
        expected-zip* (some-> expected-zip normalize-zip)
        candidate* (some-> candidate str str/trim not-empty)
        cfg (when (and (map? places-cfg)
                    (seq (:api-key places-cfg)))
              places-cfg)
        normalized-text (some-> text* normalize-city-key)
        normalized-query (some-> query* normalize-city-key)]
    (when (and text* query* cfg)
      (let [search-opts {:region-code (or user-region (:region-code cfg))
                         :language-code (:language-code cfg)
                         :max-results (or (:max-results cfg) 3)
                         :location-bias (:location-bias cfg)
                         :field-mask "places.displayName,places.id,places.formattedAddress,places.addressComponents"}
            places (:places (places-api/search-text! cfg query* search-opts))]
        (some (fn [place]
                (let [place-id (some-> place :raw :id str str/trim not-empty)
                      search-components (get-in place [:raw :addressComponents])
                      search-formatted-address (get-in place [:raw :formattedAddress])
                      details (when (and (seq place-id) (not (seq search-components)))
                                (places-api/get-place-details! cfg place-id))
                      components (or search-components (:address-components details))
                      formatted-address (or search-formatted-address (:formatted-address details))
                      place-display-name (or (:name place)
                                           (get-in place [:raw :displayName :text])
                                           (get-in place [:raw :displayName])
                                           (:display-name details))
                      city (some-> (extract-city-from-components components candidate* place-display-name)
                             str
                             str/trim
                             not-empty)
                      zip (or
                            (some-> (places-api/extract-postal-code-from-address-components components)
                              normalize-zip)
                            (some-> (extract-zip-from-text formatted-address)
                              normalize-zip))
                      city-key (some-> city normalize-city-key)
                      route (when (sequential? components)
                              (some (fn [component]
                                      (when (and (map? component)
                                              (some #{"route"} (:types component)))
                                        (:longText component)))
                                components))
                      route-key (some-> route normalize-city-key)
                      formatted-key (some-> formatted-address normalize-city-key)
                      street-allowed? (and (seq normalized-query)
                                        (or
                                          (and (seq route-key)
                                            (str/includes? normalized-query route-key))
                                          (and (seq formatted-key)
                                            (str/includes? normalized-query formatted-key))))
                      city-allowed? (cond
                                      (and (seq candidate*) (seq city))
                                      (place-city-matches-candidate? candidate* city)

                                      (and (seq normalized-text) (seq city-key))
                                      (str/includes? normalized-text city-key)

                                      (and explicit-query? street-allowed?)
                                      true

                                      :else false)
                      zip-allowed? (cond
                                     (and (seq expected-zip*) (seq zip))
                                     (= expected-zip* zip)

                                     (seq expected-zip*)
                                     false

                                     :else
                                     (seq zip))]
                  (when (and (seq city)
                          (seq zip)
                          city-allowed?
                          zip-allowed?)
                    {:city-name city
                     :zip zip})))
          places)))))

(defn- ensure-city-by-country-and-zip!
  "Create/resolve a city row with conservative collision handling.

  Rules:
  - If country+zip row exists, return it
  - Else if normalized_key exists, only update it when country/zip are compatible
  - Else insert a new row"
  [db country zip city-name]
  (let [country* (or (some-> country str str/trim not-empty)
                   default-country)
        zip* (normalize-zip zip)
        city-name* (some-> city-name str str/trim not-empty)
        normalized* (some-> city-name* normalize-city-key)]
    (when (and zip* city-name* normalized*)
      (or
        (some-> (find-city-by-country-and-zip db country* zip*) :id)
        (when-let [by-key (find-city-by-normalized-key db normalized*)]
          (let [existing-country (some-> (:country by-key) str str/trim not-empty)
                existing-zip (some-> (:zip by-key) normalize-zip)
                compatible? (and (or (not (seq existing-country))
                                   (= existing-country country*))
                              (or (not (seq existing-zip))
                                (= existing-zip zip*)))
                id (:id by-key)]
            (when (and compatible? id)
              (some-> (jdbc/execute-one!
                        db
                        (sql/format {:update :cities
                                     :set {:country country*
                                           :zip zip*
                                           :updated_at [:now]}
                                     :where [:= :id id]
                                     :returning [:id]})
                        {:builder-fn rs/as-unqualified-lower-maps})
                :id))))
        (some-> (jdbc/execute-one!
                  db
                  (sql/format {:insert-into :cities
                               :values [{:name city-name*
                                         :normalized_key normalized*
                                         :country country*
                                         :zip zip*
                                         :created_at [:now]
                                         :updated_at [:now]}]
                               :returning [:id]})
                  {:builder-fn rs/as-unqualified-lower-maps})
          :id)))))

(defn resolve-city-id-from-text!
  "Resolve city id from free text, optionally confirming via Places and creating city rows.

  Keeps pure resolver behavior intact:
  - ZIP missing -> fallback lookup by city-name candidate in existing cities

  Additional side-effecting behavior:
  - ZIP present + ZIP missing in cities -> attempt Places inference and create/resolve a city row
  - ZIP missing + candidate missing/unmapped -> attempt Places inference and create/resolve a city row

  Places inference is intentionally conservative and will only create a city
  when it can infer both a city name and a 5-digit ZIP."
  ([db text]
   (resolve-city-id-from-text! db default-country text nil))
  ([db country-or-text text-or-opts]
   (if (or (map? text-or-opts) (nil? text-or-opts))
     (resolve-city-id-from-text! db default-country country-or-text text-or-opts)
     (resolve-city-id-from-text! db country-or-text text-or-opts nil)))
  ([db country text opts]
   (let [country* (or (some-> country str str/trim not-empty)
                    default-country)
         text* (some-> text str str/trim not-empty)]
     (when (seq text*)
       (if-let [zip (extract-zip-from-text text*)]
         (or
           (some-> (find-city-by-country-and-zip db country* zip)
             :id)
           ;; Prefer candidate+zip confirmation (most precise) when we have it.
           (when-let [candidate (extract-city-fallback-candidate text*)]
             (when-let [confirmed-city (confirm-city-via-places zip candidate opts)]
               (ensure-city-by-country-and-zip! db country* zip confirmed-city)))
           ;; Otherwise, infer city name from full text but require the same ZIP.
           (when-let [{:keys [city-name]} (infer-city-and-zip-via-places text* opts :expected-zip zip)]
             (ensure-city-by-country-and-zip! db country* zip city-name)))

         ;; ZIP missing: try existing cities first; if not found, attempt Places inference.
         (or
           (when-let [candidate (extract-city-fallback-candidate text*)]
             (or
               (find-city-id-by-candidate db candidate)
               (let [query-text (or (places-query-from-text text* candidate)
                                  candidate)]
                 (when-let [{:keys [zip city-name]} (infer-city-and-zip-via-places text* opts :candidate candidate :query-text query-text)]
                   (ensure-city-by-country-and-zip! db country* zip city-name)))))
           (when-let [{:keys [zip city-name]} (infer-city-and-zip-via-places text* opts :query-text (places-query-from-text text* nil))]
             (ensure-city-by-country-and-zip! db country* zip city-name))))))))

(defn backfill-store-cities!
  "Backfill stores.city_id using ZIP-only city resolution.

  Processes stores where city_id IS NULL and address/display_name is present.
  For each store:
  1. Extract ZIP from combined address/display_name text
  2. Lookup cities by exact `country + zip`
  3. Update stores.city_id only when lookup resolves to an existing city

  No cities are created/upserted by this flow.

  Options:
    :dry-run?  - When true, log what would be updated without writing (default: false)
    :limit     - Max rows to process per run (default: nil, process all)

  Returns:
    {:scanned N
     :updated M
     :skipped K
     :failed J
     :report-file tmp/backfill-city-ids-<timestamp>.edn}"
  [db & {:keys [dry-run? limit] :or {dry-run? false}}]
  (let [query {:select [:id :address :display_name]
               :from [:stores]
               :where [:and
                       [:is :city_id nil]
                       [:or
                        [:is-not :address nil]
                        [:is-not :display_name nil]]]}
        query-with-limit (if limit
                           (assoc query :limit limit)
                           query)
        rows (jdbc/execute!
               db
               (sql/format query-with-limit)
               {:builder-fn rs/as-unqualified-lower-maps})
        total-scanned (count rows)
        timestamp (str (Instant/now))
        report-file (str "tmp/backfill-city-ids-" timestamp ".edn")]

    (println (format "\n=== Backfill stores.city_id ==="))
    (println (format "Mode: %s" (if dry-run? "DRY-RUN" "LIVE")))
    (println (format "Scanned: %d stores with city_id = NULL" total-scanned))
    (println (format "Limit: %s\n" (or limit "none")))

    (let [result (reduce
                   (fn [acc {:keys [id address display_name]}]
                     (try
                       (let [source-text (->> [address display_name]
                                           (remove str/blank?)
                                           (str/join " "))
                             zip (extract-zip-from-text source-text)
                             city-id (resolve-city-id-from-text db source-text)]
                         (if city-id
                           (do
                             (when-not dry-run?
                               (jdbc/execute-one!
                                 db
                                 (sql/format {:update :stores
                                              :set {:city_id city-id
                                                    :updated_at [:now]}
                                              :where [:= :id id]})))
                             (when dry-run?
                               (println (format "[%s] Store %s: would set city_id=%s (zip=%s)"
                                          (:updated acc)
                                          id
                                          city-id
                                          zip)))
                             (update acc :updated inc))
                           (do
                             (when dry-run?
                               (println (format "[SKIP] Store %s: unresolved ZIP (zip=%s)" id (or zip "none"))))
                             (update acc :skipped inc))))
                       (catch Exception e
                         (println (format "[ERROR] Store %s: %s" id (.getMessage e)))
                         (update acc :failed inc))))
                   {:scanned total-scanned
                    :updated 0
                    :skipped 0
                    :failed 0}
                   rows)
          report (assoc result
                   :timestamp timestamp
                   :dry-run? dry-run?
                   :limit (or limit :all))]

      (io/make-parents report-file)
      (spit report-file (pr-str report))

      (println (format "\n=== Summary ==="))
      (println (format "Scanned:  %d stores" (:scanned report)))
      (println (format "Updated:  %d stores" (:updated report)))
      (println (format "Skipped:  %d stores (unresolved ZIP)" (:skipped report)))
      (println (format "Failed:   %d stores" (:failed report)))
      (println (format "Report:   %s\n" report-file))

      (assoc report :report-file report-file))))

(defn backfill-store-cities-with-places!
  "Backfill stores.city_id and optionally create missing `cities` rows via Places.

  This is intended as a one-off maintenance helper for stores that do not have
  a resolvable ZIP in the source text (OCR noise, truncated addresses, etc.).

  Safety defaults:
  - dry-run is enabled by default (no DB writes)
  - limit can be set to bound API calls

  Options (keyword args):
    :dry-run?    - When true, log what would be updated without writing (default: true)
    :limit       - Max rows to process per run (default: 25)
    :country     - Override country for city creation/lookup (default: Bosnia and Herzegovina)
    :user-region - Override Places region code (default: places-cfg :region-code)

  Returns a summary map like `backfill-store-cities!`, but may create cities."
  [db places-cfg & {:keys [dry-run? limit country user-region]
                    :or {dry-run? true
                         limit 25}}]
  (let [country* (or (some-> country str str/trim not-empty) default-country)
        query {:select [:id :address :display_name]
               :from [:stores]
               :where [:and
                       [:is :city_id nil]
                       [:or
                        [:is-not :address nil]
                        [:is-not :display_name nil]]]
               :limit limit}
        rows (jdbc/execute!
               db
               (sql/format query)
               {:builder-fn rs/as-unqualified-lower-maps})
        total-scanned (count rows)
        timestamp (str (Instant/now))
        report-file (str "tmp/backfill-city-ids-with-places-" timestamp ".edn")
        opts {:places-cfg places-cfg
              :user-region user-region}]

    (println (format "\n=== Backfill stores.city_id (with Places) ==="))
    (println (format "Mode: %s" (if dry-run? "DRY-RUN" "LIVE")))
    (println (format "Scanned: %d stores with city_id = NULL" total-scanned))
    (println (format "Limit: %s\n" (or limit "none")))

    (let [result (reduce
                   (fn [acc {:keys [id address display_name]}]
                     (try
                       (let [source-text (->> [display_name address]
                                           (remove str/blank?)
                                           (str/join "\n"))
                             city-id (resolve-city-id-from-text! db country* source-text opts)]
                         (if city-id
                           (do
                             (when-not dry-run?
                               (jdbc/execute-one!
                                 db
                                 (sql/format {:update :stores
                                              :set {:city_id city-id
                                                    :updated_at [:now]}
                                              :where [:= :id id]})))
                             (when dry-run?
                               (println (format "[OK] Store %s: would set city_id=%s" id city-id)))
                             (update acc :updated inc))
                           (do
                             (when dry-run?
                               (println (format "[SKIP] Store %s: unresolved" id)))
                             (update acc :skipped inc))))
                       (catch Exception e
                         (println (format "[ERROR] Store %s: %s" id (.getMessage e)))
                         (update acc :failed inc))))
                   {:scanned total-scanned
                    :updated 0
                    :skipped 0
                    :failed 0}
                   rows)
          report (assoc result
                   :timestamp timestamp
                   :dry-run? dry-run?
                   :limit (or limit :all)
                   :country country*)]

      (io/make-parents report-file)
      (spit report-file (pr-str report))

      (println (format "\n=== Summary ==="))
      (println (format "Scanned:  %d stores" (:scanned report)))
      (println (format "Updated:  %d stores" (:updated report)))
      (println (format "Skipped:  %d stores" (:skipped report)))
      (println (format "Failed:   %d stores" (:failed report)))
      (println (format "Report:   %s\n" report-file))

      (assoc report :report-file report-file))))

;; ============================================================================
;; Service Registration
;; ============================================================================

(def config
  (configs/get-entity-config :city))

;; ============================================================================
;; Generated CRUD Operations
;; ============================================================================

(def service
  (factory/build-entity-service config))

(comment
  ;; REPL validation examples

  ;; 1. Test city normalization
  (normalize-city-key "Sarajevo")
  ;; => "sarajevo"

  (normalize-city-key "Banja Luka")
  ;; => "banja luka"

  (normalize-city-key "MOSTAR")
  ;; => "mostar"

  ;; 2. Test city upsert (requires db connection)
  (require '[system.state :as system-state])
  (def db (:database @system-state/state))

  (ensure-city! db "Sarajevo")
  ;; => #uuid "..."

  (ensure-city! db "sarajevo")
  ;; => #uuid "..." (same id)

  (ensure-city! db "SARAJEVO")
  ;; => #uuid "..." (same id)

  ;; 3. Test city lookup
  (find-city-by-normalized-key db "sarajevo")
  ;; => {:id #uuid "...", :name "Sarajevo", :normalized_key "sarajevo", ...}

  ;; 4. Dry-run backfill (safe, no writes)
  (backfill-store-cities! db :dry-run? true :limit 10)
  ;; => {:scanned 10, :updated N, :skipped M, :failed 0, :report-file "tmp/..."}

  ;; 5. Real backfill (small limit first)
  (backfill-store-cities! db :limit 20)
  ;; => {:scanned 20, :updated N, :skipped M, :failed 0, :report-file "tmp/..."}

  ;; 6. Full backfill (all stores with NULL city_id)
  (backfill-store-cities! db)
  ;; => {:scanned N, :updated M, :skipped K, :failed 0, :report-file "tmp/..."}

  :rcf)
