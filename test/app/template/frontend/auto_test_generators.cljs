(ns app.template.frontend.auto-test-generators
  "Core logic for generating randomized test values based on field definitions."
  (:require
    [app.template.frontend.auto-test-utils :as utils]
    [clojure.string :as str]))

;; = :::: Random Number Generation (Seedable)
;; =============================================================================

;; Simple seedable random number generator for consistent test data
(defn make-random [seed]
  (let [state (atom seed)]
    {:next-int (fn [max]
                 (swap! state #(mod (+ (* % 1103515245) 12345) 2147483648))
                 (mod @state max))
     :next-double (fn []
                    (swap! state #(mod (+ (* % 1103515245) 12345) 2147483648))
                    (/ @state 2147483648.0))}))

;; Global random generator with fixed seed for reproducibility
(def ^:dynamic *random* (make-random 12345))

;; Global state for consistent test data generation
(def ^:dynamic *data-context* (atom {}))

;; Counter for unique IDs
(def ^:dynamic *id-counter* (atom 1000))

;; =============================================================================
;; Smart Value Generation
;; =============================================================================

(defn generate-unique-id "Generate unique identifier for test data"
  []
  (swap! *id-counter* inc))

(defn generate-string-by-context
  "Generate contextually appropriate strings based on field name."
  [field-name _length]
  (let [base-id (generate-unique-id)
        field-str (name field-name)]
    (cond
      (str/includes? field-str "name") (str "Auto-Name-" base-id)
      (str/includes? field-str "description") (str "Auto-Description-" base-id " with detailed context")
      (str/includes? field-str "title") (str "Auto-Title-" base-id)
      (str/includes? field-str "email") (str "auto-email-" base-id "@test.com")
      (str/includes? field-str "phone") (str "+1-555-" (.substring (str "0000" (mod base-id 10000)) -4))
      (str/includes? field-str "address") (str base-id " Auto Street, Test City")
      :else (str "Auto-" (str/capitalize field-str) "-" base-id))))

(defn generate-decimal-with-constraints "Generate decimal values respecting constraints"
  [_field-name constraints valid?]
  (if valid?
    (let [min-val (if-let [check (:check constraints)]
                    (cond
                      ; Handle [:> :field-name number] format
                      (and (vector? check)
                        (= (first check) :>)
                        (= (count check) 3)
                        (keyword? (nth check 1))
                        (number? (nth check 2)))
                      (+ (nth check 2) 0.01)

                      ; Handle [:>= :field-name number] format
                      (and (vector? check)
                        (= (first check) :>=)
                        (= (count check) 3)
                        (keyword? (nth check 1))
                        (number? (nth check 2)))
                      (nth check 2)

                      ; Handle simple [> number] format
                      (and (vector? check) (= (first check) :>) (number? (last check)))
                      (+ (last check) 0.01)

                      ; Handle simple [>= number] format
                      (and (vector? check) (= (first check) :>=) (number? (last check)))
                      (last check)

                      :else 0.01)
                    0.01)
          max-val 1000.00
          raw-value (+ min-val (* ((:next-double *random*)) (- max-val min-val)))
          ; Round to 2 decimal places
          rounded-value (/ (Math/round (* raw-value 100.0)) 100.0)]
      rounded-value)
    ; For invalid case, generate a number that violates the constraint
    (if-let [check (:check constraints)]
      (cond
        ; If constraint is [:> :field-name number], generate number <= the limit
        (and (vector? check)
          (= (first check) :>)
          (= (count check) 3)
          (number? (nth check 2)))
        (- (nth check 2) 0.01)

        ; If constraint is [:>= :field-name number], generate number < the limit
        (and (vector? check)
          (= (first check) :>=)
          (= (count check) 3)
          (number? (nth check 2)))
        (- (nth check 2) 0.01)

        :else -1.0)
      -1.0)))

(defn generate-date-value "Generate realistic date values using JavaScript Date"
  [_field-name]
  (let [base-date (js/Date. 2025 0 15)                      ; January 15, 2025
        days-offset ((:next-int *random*) 365)
        result-date (js/Date. (.getTime base-date))]
    (.setDate result-date (+ (.getDate result-date) days-offset))
    (.toISOString result-date)))

(defn generate-enum-value "Generate enum values from type definitions"
  [field-type entity-types valid?]
  (let [enum-name (if (vector? field-type) (second field-type) field-type)
        enum-def (first (filter #(= (first %) enum-name) entity-types))
        choices (when enum-def (get-in enum-def [2 :choices]))]
    (if (and valid? (seq choices))
      (nth choices ((:next-int *random*) (count choices)))
      (if valid?
        "valid-enum-value"
        "invalid-enum-value"))))

(defn generate-json-value "Generate appropriate JSON structures based on field context"
  [field-name]
  (let [field-str (name field-name)]
    (cond
      (str/includes? field-str "description")
      {:category "auto-generated"
       :details (str "Auto JSON for " field-str)
       :timestamp (.toISOString (js/Date.))}

      (str/includes? field-str "config")
      {:enabled true
       :settings {:auto-generated true}
       :test-mode true}

      :else {})))

(defn resolve-foreign-key-value "Resolve foreign key values from already generated data"
  [fk-ref data-context valid?]
  (let [referenced-entity (-> fk-ref namespace keyword)
        referenced-entities (get @data-context referenced-entity)]
    (if (and valid? (seq referenced-entities))
      ; Get ID from generated entity - should be a UUID string
      (or (:id (nth referenced-entities ((:next-int *random*) (count referenced-entities))))
        (let [id (generate-unique-id)
              padded (str "000000000000" id)]
          (str "550e8400-e29b-41d4-a716-" (.substring padded (- (count padded) 12)))))
      (if valid?
        ; Generate a valid UUID string for fallback
        (let [id (generate-unique-id)
              padded (str "000000000000" id)]
          (str "550e8400-e29b-41d4-a716-" (.substring padded (- (count padded) 12))))
        "invalid-uuid-ref"))))                              ; Invalid foreign key

;; =============================================================================
;; Field Value Generation Engine
;; =============================================================================

(defn generate-field-value "Generate appropriate value for any field based on its definition"
  [field-def entity-types data-context valid?]
  (let [field-name (utils/get-field-name field-def)
        field-type (utils/get-field-type field-def)
        constraints (utils/get-field-constraints field-def)]

    ; Skip system-managed fields
    (when-not (#{:id :created_at :updated_at} field-name)
      [field-name
       (cond
         ; Foreign key fields
         (:foreign-key constraints)
         (resolve-foreign-key-value (:foreign-key constraints) data-context valid?)

         ; Enum fields
         (and (vector? field-type) (= (first field-type) :enum))
         (generate-enum-value field-type entity-types valid?)

         ; String/text fields
         (#{:text :varchar} (if (vector? field-type) (first field-type) field-type))
         (let [min-length (when-let [check (:check constraints)]
                            (cond
                              ; Handle [:> [:length :field-name] number] format
                              (and (vector? check)
                                (= (first check) :>)
                                (vector? (second check))
                                (= (first (second check)) :length)
                                (= (count check) 3)
                                (number? (nth check 2)))
                              (+ (nth check 2) 1)

                              :else 5))]
           (if valid?
             (generate-string-by-context field-name (max min-length 10))
             ""))

         ; Decimal/numeric fields - handle both :decimal and [:decimal precision scale]
         (or (= field-type :decimal)
           (and (vector? field-type) (= (first field-type) :decimal)))
         (generate-decimal-with-constraints field-name constraints valid?)

         ; Integer fields (non-foreign key)
         (= field-type :integer)
         (if valid?
           ((:next-int *random*) 1000)
           "invalid-integer")

         ; Date fields
         (= field-type :date)
         (if valid?
           (.substring (generate-date-value field-name) 0 10) ; YYYY-MM-DD format
           "invalid-date")

         ; JSON fields
         (= field-type :jsonb)
         (if valid?
           (generate-json-value field-name)
           "invalid-json-string")

         ; UUID fields
         (= field-type :uuid)
         (if valid?
           (let [id (generate-unique-id)
                 padded (str "000000000000" id)]
             (str "550e8400-e29b-41d4-a716-" (.substring padded (- (count padded) 12))))
           "invalid-uuid")

         ; Boolean fields
         (= field-type :boolean)
         (if valid?
           (< ((:next-double *random*)) 0.5)
           "invalid-boolean")

         ; Timestamp fields (timestamptz)
         (= field-type :timestamptz)
         (if valid?
           (.toISOString (js/Date.))
           "invalid-timestamp")

         ; Array fields
         (and (vector? field-type) (= (first field-type) :array))
         (if valid?
           [] ; Empty array as default
           "invalid-array")

         ; Default fallback
         :else
         (if valid?
           (str "auto-" (name field-name) "-" (generate-unique-id))
           "invalid-value"))])))
