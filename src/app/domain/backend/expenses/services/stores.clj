(ns app.domain.backend.expenses.services.stores
  "Store (branch/location) services.

  Stores are owned by a supplier, and represent a specific physical location.

  This is intentionally kept separate from `suppliers` so that item aliasing can
  remain supplier-level (article_aliases.supplier_id) while expenses/receipts can
  carry store context."
  (:require
    [app.domain.backend.expenses.services.places-api :as places-api]
    [app.domain.backend.expenses.services.service-configs :as configs]
    [app.domain.backend.expenses.services.services-factory :as factory]
    [app.domain.backend.expenses.services.stores.matching :as store-matching]
    [app.domain.backend.expenses.services.stores.receipt-fingerprint :as receipt-fp]
    [app.shared.type-conversion :as type-conv]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.util UUID]))

(def config (configs/get-entity-config :store))

(def service (factory/build-entity-service config))

(def ^:private try-uuid type-conv/try-parse-uuid)

(defn- title-case
  "Convert string to title case (capitalize each word)."
  [s]
  (when s
    (->> (str/split s #"\s+")
      (map str/capitalize)
      (str/join " "))))

(defn- clean-city-source
  "Normalize OCR-ish text before processing city extraction.
  
  - Trims whitespace and collapses multiple spaces to single space
  - Replaces repeated commas with single comma
  - Strips control characters (newlines, tabs, etc.)
  - Removes surrounding quotes if present
  - Normalizes postal codes by removing spaces (e.g., \"71 000\" -> \"71000\")
  - Preserves diacritics (Serbian/Bosnian characters)
  
  Returns cleaned string or nil if input is nil/blank."
  [s]
  (when-let [text (some-> s str str/trim not-empty)]
    (-> text
        ;; Strip control characters
      (str/replace #"[\r\n\t]+" " ")
        ;; Collapse multiple spaces
      (str/replace #"\s{2,}" " ")
        ;; Collapse multiple commas
      (str/replace #",{2,}" ",")
        ;; Remove surrounding quotes
      (str/replace #"^[\"']|[\"']$" "")
        ;; Normalize postal codes: "71 000" -> "71000"
      (str/replace #"(\d{2})\s+(\d{3})" "$1$2")
      str/trim
      not-empty)))

(defn- normalize-city-candidate
  "Clean up a candidate city segment extracted from address/name.
  
  - Trims whitespace
  - Strips leading/trailing punctuation (, . ; : -)
  - Strips numeric suffixes like \"BROJ 126\", \"BR. 3\", \"BR 90\"
  - Collapses internal whitespace to single space
  - Applies title-case normalization
  
  Returns normalized string or nil if result is blank."
  [candidate]
  (when-let [text (some-> candidate str str/trim not-empty)]
    (-> text
        ;; Strip leading/trailing punctuation
      (str/replace #"^[,.\-;:]+|[,.\-;:]+$" "")
      str/trim
        ;; Strip numeric suffixes (BROJ/BR patterns)
      (str/replace #"(?i)\s+(broj|br\.?)\s+\d+$" "")
      str/trim
        ;; Collapse internal whitespace
      (str/replace #"\s{2,}" " ")
        ;; Apply title-case
      title-case
      not-empty)))

(defn- valid-city-candidate?
  "Validate whether a candidate string looks like a real city name.
  
  Rejects candidates that:
  - Are nil or blank
  - Contain digits
  - Are too long (> 40 characters)
  - Contain control characters
  - Contain legal entity tokens (d.o.o, doo, ltd, llc)
  - Contain known OCR noise tokens (tropic, maloprodaja, prodavnica, etc.)
  - Have suspicious token count (> 5 suggests not a simple city name)
  
  Requires:
  - At least one letter
  - Reasonable length (2-40 chars)
  
  Returns boolean."
  [candidate]
  (when-let [text (some-> candidate str str/trim not-empty)]
    (let [lower-text (str/lower-case text)
          token-count (count (str/split text #"\s+"))
          ;; Legal entity patterns
          has-legal-entity? (or (str/includes? lower-text "d.o.o")
                              (str/includes? lower-text "doo")
                              (str/includes? lower-text "ltd")
                              (str/includes? lower-text "llc"))
          ;; OCR noise tokens (standalone or dominant)
          noise-tokens #{"tropic" "maloprodaja" "prodavnica" "hipemarket" "market"}
          has-noise-token? (some #(str/includes? lower-text %) noise-tokens)]
      (and
       ;; Basic validation
        (>= (count text) 2)
        (<= (count text) 40)
       ;; Must have at least one letter
        (re-find #"\p{L}" text)
       ;; Must not contain digits
        (not (re-find #"\d" text))
       ;; Must not contain control characters
        (not (re-find #"[\r\n\t]" text))
       ;; Must not have legal entity tokens
        (not has-legal-entity?)
       ;; Must not have OCR noise tokens
        (not has-noise-token?)
       ;; Token count should be reasonable for a city name
        (<= token-count 5)))))

(defn- bad-city?
  "Check if an existing city value is polluted/invalid.
  
  Returns true if the city appears to be corrupted by OCR noise or contains
  suspicious patterns that indicate it's not a real city name.
  
  Detects:
  - Contains digits (e.g., postal codes leaked into city field)
  - Too long (> 50 characters suggests address leakage)
  - Contains known noise tokens (tropic, maloprodaja, prodavnica, hipemarket, merkur, d.o.o, doo)
  - Contains \"centar\" as standalone/dominant token (but allows \"City Centar\" patterns)
  - Contains newline or control characters
  - Contains multiple sentences (periods followed by capitals)
  - All uppercase with length > 15 (likely OCR artifact)
  
  Returns boolean."
  [city]
  (when-let [text (some-> city str str/trim not-empty)]
    (let [lower-text (str/lower-case text)
          tokens (str/split text #"\s+")
          token-count (count tokens)
          ;; Known noise tokens that indicate OCR pollution
          noise-tokens #{"tropic" "maloprodaja" "prodavnica" "hipemarket" "merkur" "market"}
          has-noise-token? (some #(str/includes? lower-text %) noise-tokens)
          ;; Check for "centar" contextually:
          ;; - Allow if it's part of "City Centar" pattern (2 tokens, second is centar)
          ;; - Reject if standalone or dominant
          has-bad-centar? (and (str/includes? lower-text "centar")
                            (not (and (= token-count 2)
                                   (= (str/lower-case (last tokens)) "centar"))))
          ;; Legal entity patterns
          has-legal-entity? (or (str/includes? lower-text "d.o.o")
                              (str/includes? lower-text "doo")
                              (str/includes? lower-text "ltd")
                              (str/includes? lower-text "llc"))
          ;; Multiple sentences (e.g., "SARAJEVO. Some other text")
          has-multiple-sentences? (re-find #"\.\s+[A-Z]" text)
          ;; All uppercase and long (OCR artifact pattern)
          all-uppercase-long? (and (= text (str/upper-case text))
                                (> (count text) 15))]
      (or
       ;; Contains digits
        (re-find #"\d" text)
       ;; Too long
        (> (count text) 50)
       ;; Has noise tokens
        has-noise-token?
       ;; Has bad centar usage
        has-bad-centar?
       ;; Has legal entity markers
        has-legal-entity?
       ;; Control characters
        (re-find #"[\r\n\t]" text)
       ;; Multiple sentences
        has-multiple-sentences?
       ;; All uppercase and suspiciously long
        all-uppercase-long?))))

(defn- extract-city-from-display-name
  "Extract city from store/branch display name.
  
  Tries multiple heuristics in order, returning first valid match:
  1. Prefix pattern - matches branch/store prefixes like 'PODRUŽNICA SARAJEVO'
  2. Separator-tail heuristic - takes last segment after separators (-, /, |, etc.)
  3. Last token heuristic - for simple cases like 'BINGO Sarajevo'
  
  All candidates are validated with valid-city-candidate? before returning.
  
  Examples:
    \"PODRUŽNICA SARAJEVO\" -> \"Sarajevo\"
    \"BINGO Sarajevo\" -> \"Sarajevo\"
    \"CENTAR Banja Luka\" -> \"Banja Luka\"
    \"Store - Mostar\" -> \"Mostar\"
    \"TROPIC MALOPRODAJA d.o.o.\" -> nil (legal entity rejected)
    nil -> nil"
  [display-name]
  (when-let [cleaned (clean-city-source display-name)]
    (or
;; Pattern 1: Prefix pattern (ogranak, podružnica, poslovnica, centar)
      ;; Note: lowercase first because (?i) flag doesn't work reliably with Unicode (ž)
      (when-let [match (re-find #"^(ogranak|podružnica|podruznica|poslovnica|centar)[\s:.\-]+(.+)$"
                         (str/lower-case cleaned))]
        (let [tail (nth match 2)
              candidate (normalize-city-candidate tail)]
          (when (valid-city-candidate? candidate)
            candidate)))

      ;; Pattern 2: Separator-tail heuristic
      (when (re-find #"[\-,/|]" cleaned)
        (let [segments (str/split cleaned #"[\-,/|]")
              last-seg (some-> segments last str/trim not-empty)
              candidate (normalize-city-candidate last-seg)]
          (when (valid-city-candidate? candidate)
            candidate)))

      ;; Pattern 3: Last token(s) heuristic (conservative)
      (when (and (< (count cleaned) 50)
              (not (re-find #"[\-,/|]" cleaned)))
        (let [tokens (str/split cleaned #"\s+")
              last-token (some-> tokens last str/trim not-empty)
              candidate (normalize-city-candidate last-token)]
          (when (valid-city-candidate? candidate)
            candidate))))))

(defn- extract-city-via-places-api
  "Extract city using Places API as fallback.
  
  Given a place-id and places-config, fetches place details from Google Places API
  and extracts city from addressComponents.
  
  Algorithm:
  1. Check if places-config has API key (return nil if missing)
  2. Call places-api/get-place-details! with place-id
  3. Extract city from addressComponents using places-api/extract-city-from-address-components
  4. Normalize with normalize-city-candidate
  5. Validate with valid-city-candidate?
  
  Returns normalized city string or nil.
  
  Example:
    (extract-city-via-places-api \"ChIJQR...\" places-cfg)
    ;; => \"Sarajevo\" (if found and valid)
    ;; => nil (if API error, no city in components, or validation fails)"
  [place-id places-config]
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
        ;; Log but don't throw - this is a fallback, failures should be graceful
        (println (format "Places API fallback failed for place-id %s: %s"
                   place-id (.getMessage e)))
        nil))))

(defn extract-city-from-address
  "Extract city from address string, with optional display-name fallback and Places API fallback.
  
  Arities:
  - (extract-city-from-address address) - backwards compatible, address-only
  - (extract-city-from-address address display-name) - tries address first, then display-name
  - (extract-city-from-address address display-name place-id places-config) - adds Places API fallback
  
  Address extraction algorithm:
  1. Clean address with clean-city-source
  2. Find last 5-digit postal code in address (pattern: \\d{5})
  3. Extract text after the postal code
  4. Parse after-postal text: split by commas/newlines, evaluate segments right-to-left
  5. Pick last segment that passes valid-city-candidate?
  6. Apply normalize-city-candidate to selected segment
  
  Display-name fallback (when address extraction yields nothing):
  - Uses extract-city-from-display-name with validation
  
  Places API fallback (when heuristic extraction yields nothing and place-id is present):
  - Uses extract-city-via-places-api to fetch addressComponents from Google Places
  - Extracts city from locality/postal_town/administrative_area_level_2 types
  
  Examples:
    (extract-city-from-address \"ul. Kolodvorska br.12, 71000 Sarajevo\")
    ;; => \"Sarajevo\"
    
    (extract-city-from-address \"Street, 71103 SARAJEVO CENTAR\")
    ;; => \"Sarajevo Centar\"
    
    (extract-city-from-address nil \"BINGO Sarajevo\")
    ;; => \"Sarajevo\"
    
    (extract-city-from-address \"ul. Broj 126\" \"PODRUŽNICA SARAJEVO\" \"ChIJQR...\" places-cfg)
    ;; => \"Sarajevo\" (from Places API if heuristic fails)"
  ([address]
   (extract-city-from-address address nil nil nil))
  ([address display-name]
   (extract-city-from-address address display-name nil nil))
  ([address display-name place-id places-config]
   (or
     ;; Step 1: Try address-based extraction first
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
               ;; Parse the after-postal text: split by commas/newlines, evaluate right-to-left
               (let [segments (str/split cleaned #"[,\n]")
                     ;; Try segments in reverse order (last to first)
                     valid-segment (some (fn [seg]
                                           (let [candidate (normalize-city-candidate seg)]
                                             (when (valid-city-candidate? candidate)
                                               candidate)))
                                     (reverse segments))]
                 valid-segment))))))

     ;; Step 2: Fallback to display-name extraction when address yielded nothing
     (when display-name
       (extract-city-from-display-name display-name))

     ;; Step 3: Fallback to Places API when heuristic extraction failed and place-id is present
     (when (and place-id places-config)
       (extract-city-via-places-api place-id places-config)))))

(defn get-store
  [db store-id]
  (jdbc/execute-one!
    db
    (sql/format {:select [:*]
                 :from [:stores]
                 :where [:= :id store-id]
                 :limit 1})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn find-by-supplier-and-normalized-key
  [db supplier-id normalized-key]
  (jdbc/execute-one!
    db
    (sql/format {:select [:*]
                 :from [:stores]
                 :where [:and
                         [:= :supplier_id supplier-id]
                         [:= :normalized_key normalized-key]]
                 :limit 1})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn find-by-supplier-and-place-id
  [db supplier-id place-id]
  (let [place-id* (some-> place-id str str/trim not-empty)]
    (when place-id*
      (jdbc/execute-one!
        db
        (sql/format {:select [:*]
                     :from [:stores]
                     :where [:and
                             [:= :supplier_id supplier-id]
                             [:= :place_id place-id*]]
                     :limit 1})
        {:builder-fn rs/as-unqualified-lower-maps}))))

(defn update-store!
  "Patch a store row and return the updated store.

  Accepts snake_case or kebab-case keys: display_name/display-name, address, place_id/place-id."
  [db store-id {:keys [display_name display-name address place_id place-id]}]
  (let [display-name* (some-> (or display_name display-name) str str/trim not-empty)
        address* (some-> address str str/trim not-empty)
        place-id* (some-> (or place_id place-id) str str/trim not-empty)
        ;; If address is being updated, compute city
        city* (when address* (extract-city-from-address address* display-name*))
        set-map (cond-> {:updated_at [:now]}
                  (some? display-name*) (assoc :display_name display-name*)
                  (some? address*) (assoc :address address*)
                  (some? place-id*) (assoc :place_id place-id*)
                  (some? address*) (assoc :city city*))]
    (jdbc/execute-one!
      db
      (sql/format {:update :stores
                   :set set-map
                   :where [:= :id store-id]
                   :returning [:*]})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- merge-duplicate-stores-by-place-id!
  "Merge stores for a supplier that resolve to the same Places `place_id`.

  Keeps `keep-store-id`, rewires foreign keys, and deletes duplicates.

  This is safe to run during OCR ingestion."
  [db supplier-id place-id keep-store-id]
  (let [place-id* (some-> place-id str str/trim not-empty)]
    (when (and supplier-id (seq place-id*) keep-store-id)
      (let [rows (jdbc/execute!
                   db
                   (sql/format {:select [:id]
                                :from [:stores]
                                :where [:and
                                        [:= :supplier_id supplier-id]
                                        [:= :place_id place-id*]]})
                   {:builder-fn rs/as-unqualified-lower-maps})
            ids (->> rows (map :id) (remove nil?) distinct vec)
            keep-id keep-store-id
            drop-ids (->> ids (remove #(= % keep-id)) vec)]
        (when (seq drop-ids)
          ;; Update all dependent references first.
          (jdbc/execute!
            db
            (sql/format {:update :expenses
                         :set {:store_id keep-id}
                         :where [:in :store_id drop-ids]}))
          (jdbc/execute!
            db
            (sql/format {:update :store_aliases
                         :set {:store_id keep-id
                               :updated_at [:now]}
                         :where [:in :store_id drop-ids]}))
          ;; Finally remove duplicates.
          (jdbc/execute!
            db
            (sql/format {:delete-from :stores
                         :where [:in :id drop-ids]})))
        {:kept keep-id
         :merged (count drop-ids)}))))

(defn find-or-create-store!
  "Idempotently create a store row.

  Keys accepted (snake or kebab):
  - supplier_id / supplier-id (required)
  - display_name / display-name (required)
  - normalized_key / normalized-key (optional, used when :place_id is not present)
  - address (optional)
  - place_id / place-id (optional)

  Returns the store row."
  [db {:keys [supplier_id supplier-id
              display_name display-name
              normalized_key normalized-key
              address
              place_id place-id]}]
  (let [supplier-id* (try-uuid (or supplier_id supplier-id))
        display-name* (some-> (or display_name display-name) str str/trim not-empty)
        address* (some-> address str str/trim not-empty)
        place-id* (some-> (or place_id place-id) str str/trim not-empty)
        normalized* (some-> (or normalized_key normalized-key) str str/trim not-empty)]
    (when-not supplier-id*
      (throw (ex-info "supplier_id is required" {:status 400 :field :supplier_id})))
    (when-not display-name*
      (throw (ex-info "display_name is required" {:status 400 :field :display_name})))
    (let [normalized (cond
                       place-id*
                       (configs/normalize-store-key (str "place " place-id*))

                       normalized*
                       (configs/normalize-store-key normalized*)

                       :else
                       (configs/normalize-store-key (str display-name* " " (or address* ""))))
          city (extract-city-from-address address* display-name*)
          row {:id (UUID/randomUUID)
               :supplier_id supplier-id*
               :display_name display-name*
               :normalized_key normalized
               :address address*
               :place_id place-id*
               :city city
               :created_at [:now]
               :updated_at [:now]}
          sql-map {:insert-into :stores
                   :values [row]
                   :on-conflict [:supplier_id :normalized_key]
                   :do-update-set (cond-> {:display_name :excluded/display_name
                                           :updated_at [:now]}
                                    (some? address*) (assoc :address :excluded/address
                                                       :city :excluded/city)
                                    (some? place-id*) (assoc :place_id :excluded/place_id))
                   :returning [:*]}]
      (jdbc/execute-one!
        db
        (sql/format sql-map)
        {:builder-fn rs/as-unqualified-lower-maps}))))

(defn- list-stores-for-supplier
  [db supplier-id]
  (jdbc/execute!
    db
    (sql/format {:select [:id :normalized_key :display_name :address :place_id]
                 :from [:stores]
                 :where [:= :supplier_id supplier-id]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- store-match-key
  [{:keys [normalized_key display_name address place_id] :as _store}]
  (let [normalized* (some-> normalized_key str str/trim not-empty)
        place-id* (some-> place_id str str/trim not-empty)
        address* (some-> address str str/trim not-empty)
        address-key (some-> address* configs/normalize-store-key)
        pj-or-places-key? (and (seq normalized*)
                            (or (str/starts-with? normalized* "pj-")
                              (str/starts-with? normalized* "place-")))
        display-name* (some-> display_name str str/trim not-empty)
        label (->> [display-name* address*]
                (remove str/blank?)
                (str/join " "))
        derived (some-> label not-empty configs/normalize-store-key)]
    (cond
      ;; For PJ/Places-keyed stores, prefer an address-derived key so we can still
      ;; match future OCR alias variants that don't include PJ/Places data.
      (and pj-or-places-key? (seq address-key))
      address-key

      ;; For normal stores, use the stored normalized_key as the primary match key.
      (seq normalized*)
      normalized*

      ;; Fallbacks.
      (seq address-key)
      address-key

      (and (seq place-id*) (seq derived))
      derived

      :else
      nil)))

(defn- store-candidates-for-supplier
  [db supplier-id]
  (mapv (fn [{:keys [id] :as row}]
          {:id id
           :key (store-match-key row)})
    (list-stores-for-supplier db supplier-id)))

(defn resolve-store-from-merchant
  "Best-effort store resolution from merchant data.

  `merchant` is expected to contain:
  - :name (supplier-level)
  - :store_name (branch label)
  - :address (store disambiguator)

  If Places is configured, we try to canonicalize stores by `place_id` so that
  multiple OCR variants (store_name/address noise) can converge on the same
  store row.

  Additionally:
  - when `:receipt-markdown` is present in opts, we try to derive a stable store
    key from the receipt header (e.g. `PJ` branch number) to avoid duplicates
  - when `:store-alias-normalized` is present in opts, treat it as the primary
    alias key for matching/heuristics (script-style)

  Returns {:store <row> :store-id <uuid> :store-alias-label <string>} or nil when
  it cannot infer a store label."
  ([db supplier-id merchant]
   (resolve-store-from-merchant db supplier-id merchant nil))
  ([db supplier-id {:keys [name store_name address] :as _merchant}
    {:keys [places-cfg user-region receipt-markdown
            store-alias-normalized store-alias-raw-label]
     :as _opts}]
   (let [supplier-id* (try-uuid supplier-id)
         supplier-name* (some-> name str str/trim not-empty)
         address* (some-> address str str/trim not-empty)
         store-name* (some-> store_name str str/trim not-empty)
         store-label (or address* store-name*)
         store-display-basic (or store-name* address* "Store")
         alias-key (or (some-> store-alias-normalized str str/trim not-empty)
                     (some-> store-label configs/normalize-store-key))
         pj-source (->> [receipt-markdown store-alias-raw-label store-name* address*]
                     (remove str/blank?)
                     (str/join "\n"))
         pj (some-> (receipt-fp/pj-number pj-source) str str/trim not-empty)
         pj-key (when pj
                  (configs/normalize-store-key (str "pj " pj)))
         store-key (or pj-key alias-key)
         places-enabled? (and (map? places-cfg)
                           (seq (:api-key places-cfg))
                           (not (seq pj-key))
                           (not (seq store-alias-normalized)))
         places-query (when (and places-enabled? (seq store-label))
                        (->> [supplier-name* store-name* address*]
                          (remove str/blank?)
                          (str/join " ")))
         places-opts {:region-code (or user-region (:region-code places-cfg))
                      :language-code (:language-code places-cfg)
                      :max-results (or (:max-results places-cfg) 3)
                      :location-bias (:location-bias places-cfg)
                      :field-mask "places.displayName,places.id,places.formattedAddress"}
         place (when (seq places-query)
                 (first (:places (places-api/search-text! places-cfg places-query places-opts))))
         place-id (some-> place :raw :id str str/trim not-empty)
         place-name (some-> place :name str str/trim not-empty)
         place-address (some-> place :raw :formattedAddress str str/trim not-empty)
         effective-alias-label (or address* place-address store-name*)
         effective-address (or place-address address*)
         effective-display (or store-name* effective-address place-name store-display-basic)]
     (when (and supplier-id* (seq effective-alias-label))
       (let [store (cond
                     (seq place-id)
                     (or
                       (when-let [existing (find-by-supplier-and-place-id db supplier-id* place-id)]
                         (update-store! db (:id existing)
                           {:display_name effective-display
                            :address effective-address
                            :place_id place-id}))

                       ;; If we already created a store without Places (script-style
                       ;; or PJ-key), attach place_id now.
                       (when-let [existing (and (seq store-key)
                                             (find-by-supplier-and-normalized-key db supplier-id* store-key))]
                         (update-store! db (:id existing)
                           {:display_name effective-display
                            :address effective-address
                            :place_id place-id}))

                       ;; Legacy key format (before script-style normalized_key).
                       (when-let [legacy (find-by-supplier-and-normalized-key
                                           db
                                           supplier-id*
                                           (configs/normalize-store-key (str store-display-basic " " (or address* ""))))]
                         (update-store! db (:id legacy)
                           {:display_name effective-display
                            :address effective-address
                            :place_id place-id}))

                       (find-or-create-store!
                         db
                         {:supplier_id supplier-id*
                          :display_name effective-display
                          :address effective-address
                          :place_id place-id}))

                     :else
                     (or
                       ;; Prefer stable store key (PJ branch number) when available.
                       (when (seq store-key)
                         (find-by-supplier-and-normalized-key db supplier-id* store-key))

                       ;; In case store-key came from PJ, also allow exact alias-key
                       ;; matches to reuse pre-existing stores.
                       (when (and (seq alias-key) (not= alias-key store-key))
                         (find-by-supplier-and-normalized-key db supplier-id* alias-key))

                       (when-let [store-id (receipt-fp/match-store-id db supplier-id* pj-source)]
                         (get-store db store-id))

                       (when-let [{:keys [store-id]} (store-matching/match-store
                                                       alias-key
                                                       (store-candidates-for-supplier db supplier-id*))]
                         (get-store db store-id))

                       (find-or-create-store!
                         db
                         {:supplier_id supplier-id*
                          :display_name (or store-display-basic
                                          (some-> store-alias-raw-label str str/trim not-empty)
                                          store-key
                                          alias-key
                                          "Store")
                          :address address*
                          :normalized_key store-key})))
             _ (when (and (seq place-id) (:id store))
                 (merge-duplicate-stores-by-place-id! db supplier-id* place-id (:id store)))]
         {:store store
          :store-id (:id store)
          :store-alias-label effective-alias-label})))))

(defn backfill-store-place-ids!
  "Backfill `stores.place_id` using Google Places, and merge duplicates by place_id.

  Intended for REPL/admin maintenance after enabling Places store canonicalization.

  opts:
  - :limit (default 200)
  - :region-code / :language-code

  Returns a summary map."
  ([db places-cfg]
   (backfill-store-place-ids! db places-cfg nil))
  ([db places-cfg {:keys [limit region-code language-code] :or {limit 200}}]
   (if-not (and (map? places-cfg) (seq (:api-key places-cfg)))
     {:scanned 0 :updated 0 :no-match 0 :failed 0 :reason :missing-places-api-key}
     (let [rows (jdbc/execute!
                  db
                  (sql/format {:select [[:st.id :store_id]
                                        [:st.supplier_id :supplier_id]
                                        [:st.display_name :store_display_name]
                                        [:st.address :store_address]
                                        [:sp.display_name :supplier_name]]
                               :from [[:stores :st]]
                               :join [[:suppliers :sp] [:= :sp.id :st.supplier_id]]
                               :where [:and
                                       [:is :st.place_id nil]
                                       [:is-not :st.address nil]
                                       [:<> :st.address ""]]
                               :limit limit})
                  {:builder-fn rs/as-unqualified-lower-maps})
           search-opts {:region-code (or region-code (:region-code places-cfg))
                        :language-code (or language-code (:language-code places-cfg))
                        :max-results (or (:max-results places-cfg) 3)
                        :location-bias (:location-bias places-cfg)
                        :field-mask "places.displayName,places.id,places.formattedAddress"}]
       (reduce
         (fn [acc {:keys [store_id supplier_id store_display_name store_address supplier_name]}]
           (try
             (let [query (->> [supplier_name store_display_name store_address]
                           (remove (fn [s] (str/blank? (str s))))
                           (map (fn [s] (str/trim (str s))))
                           (str/join " "))
                   place (when (seq query)
                           (first (:places (places-api/search-text! places-cfg query search-opts))))
                   place-id (some-> place :raw :id str str/trim not-empty)
                   place-address (some-> place :raw :formattedAddress str str/trim not-empty)]
               (if (seq place-id)
                 (do
                   (update-store!
                     db
                     store_id
                     {:place_id place-id
                      :address (or place-address store_address)
                      :display_name store_display_name})
                   (merge-duplicate-stores-by-place-id! db supplier_id place-id store_id)
                   (update acc :updated inc))
                 (update acc :no-match inc)))
             (catch Exception _
               (update acc :failed inc))))
         {:scanned (count rows)
          :updated 0
          :no-match 0
          :failed 0}
         rows)))))

(defn backfill-store-cities!
  "Backfill city column for existing stores with NULL or polluted cities.
  
  Extracts city from address and display_name using extract-city-from-address.
  When heuristic extraction fails, falls back to Places API if place_id is present.
  
  Safe to re-run (updates only rows with NULL or bad cities).
  
  A \"bad city\" is detected by bad-city? and includes:
  - Cities with digits (postal codes leaked in)
  - Too long (> 50 chars, likely address leakage)
  - Contains OCR noise tokens (tropic, maloprodaja, merkur, etc.)
  - Control characters or multiple sentences
  
  Options:
    :limit            - Max rows to process per run (default: no limit)
    :dry-run?         - When true, only show what would be updated (default: false)
    :fix-bad-cities?  - When true, also fix polluted cities (default: true)
    :places-config    - Places API config for fallback (optional, fetched from DI container if not provided)
  
  Returns:
    {:scanned N, :updated M, :no-match K, :failed J, :places-api-used P}
  
  Example:
    ;; Dry run first (includes bad cities)
    (backfill-store-cities! db {:dry-run? true})
    
    ;; Run actual backfill
    (backfill-store-cities! db)
    
    ;; Process only NULL cities
    (backfill-store-cities! db {:fix-bad-cities? false})
    
    ;; Process in batches
    (backfill-store-cities! db {:limit 10})
    
    ;; With Places API fallback
    (backfill-store-cities! db {:places-config places-cfg})"
  ([db]
   (backfill-store-cities! db nil))
  ([db {:keys [limit dry-run? fix-bad-cities? places-config]
        :or {dry-run? false
             fix-bad-cities? true}}]
   (let [;; Query for stores that have address OR display_name, now including place_id
         query {:select [:id :address :display_name :city :place_id]
                :from [:stores]
                :where [:or
                        [:is-not :address nil]
                        [:is-not :display_name nil]]}
         query-with-limit (if limit
                            (assoc query :limit limit)
                            query)
         all-rows (jdbc/execute!
                    db
                    (sql/format query-with-limit)
                    {:builder-fn rs/as-unqualified-lower-maps})
         ;; Filter rows: NULL cities OR (fix-bad-cities? AND bad city)
         rows (filter
                (fn [{:keys [city]}]
                  (or (nil? city)
                    (and fix-bad-cities? (bad-city? city))))
                all-rows)
         total-scanned (count rows)]
     (println (format "Backfill stores.city: scanned %d rows (dry-run: %s, fix-bad-cities: %s, places-api: %s)"
                total-scanned
                (if dry-run? "true" "false")
                (if fix-bad-cities? "true" "false")
                (if places-config "enabled" "disabled")))
     (reduce
       (fn [acc {:keys [id address display_name city place_id]}]
         (try
           (let [;; Check if current city is bad (for logging)
                 current-city-bad? (and city (bad-city? city))
                 ;; Extract new city from address and/or display_name, with Places API fallback
                 new-city (extract-city-from-address address display_name place_id places-config)
                 ;; Check if Places API was used (heuristic failed but Places succeeded)
                 heuristic-city (extract-city-from-address address display_name)
                 places-api-used? (and (nil? heuristic-city)
                                    (some? new-city)
                                    (some? place_id)
                                    (some? places-config))
                 ;; Only update if we got something and it's different
                 should-update? (and (seq new-city)
                                  (or (nil? city)
                                    (not= new-city city)))]
             (if should-update?
               (do
                 (when-not dry-run?
                   (jdbc/execute-one!
                     db
                     (sql/format {:update :stores
                                  :set {:city new-city
                                        :updated_at [:now]}
                                  :where [:= :id id]})))
                 (when dry-run?
                   (println (format "Would update store %s: %s -> %s%s%s"
                              id
                              (or city "NULL")
                              new-city
                              (if current-city-bad? " (bad city)" "")
                              (if places-api-used? " [Places API]" ""))))
                 (cond-> (update acc :updated inc)
                   places-api-used? (update :places-api-used inc)))
               (do
                 (when dry-run?
                   (println (format "No update for store %s: current=%s, extracted=%s"
                              id (or city "NULL") (or new-city "none"))))
                 (update acc :no-match inc))))
           (catch Exception e
             (println (format "Failed to process store %s: %s" id (.getMessage e)))
             (update acc :failed inc))))
       {:scanned total-scanned
        :updated 0
        :no-match 0
        :failed 0
        :places-api-used 0}
       rows))))
