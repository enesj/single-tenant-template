(ns app.template.frontend.auto-test-data
  "Auto-generation system for frontend test data based on models.edn.
   This system eliminates hardcoded test data and generates everything
   dynamically based on database schema definitions."
  (:require
    [clojure.set :as set]
    [clojure.string :as str]))

;; =============================================================================
;; Models Definition - Inline for ClojureScript
;; =============================================================================

;; Since ClojureScript can't read files at runtime, we include the models inline
;; This should be kept in sync with resources/db/models.edn
(def models
  {:tenants {:fields [[:id :uuid {:primary-key true :default [:gen_random_uuid]}]
                      [:name [:varchar 255] {:null false}]
                      [:slug [:varchar 100] {:null false :unique true}]
                      [:status [:enum :tenant-status] {:null false :default "active"}]
                      [:settings :jsonb {:default {}}]
                      [:subscription_tier [:enum :subscription-tier] {:default "free"}]
                      [:subscription_status [:enum :subscription-status] {:default "trialing"}]
                      [:trial_ends_at :timestamptz]
                      [:billing_email [:varchar 255]]
                      [:stripe_customer_id [:varchar 255] {:unique true}]
                      [:stripe_subscription_id [:varchar 255]]
                      [:created_at :timestamptz {:default [:now]}]
                      [:updated_at :timestamptz {:default [:now]}]]
             :types [[:tenant-status :enum {:choices ["active" "suspended" "cancelled"]}]
                     [:subscription-tier :enum {:choices ["free" "starter" "professional" "enterprise"]}]
                     [:subscription-status :enum {:choices ["trialing" "active" "past_due" "cancelled"]}]]}
   :users {:fields [[:id :uuid {:primary-key true :default [:gen_random_uuid]}]
                    [:tenant_id :uuid {:foreign-key :tenants/id :null false :on-delete :cascade}]
                    [:email [:varchar 255] {:null false}]
                    [:full_name [:varchar 255]]
                    [:avatar_url :text]
                    [:auth_provider [:enum :auth-provider] {:null false}]
                    [:provider_user_id [:varchar 255]]
                    [:role [:enum :user-role] {:null false :default "member"}]
                    [:status [:enum :user-status] {:null false :default "active"}]
                    [:last_login_at :timestamptz]
                    [:created_at :timestamptz {:default [:now]}]
                    [:updated_at :timestamptz {:default [:now]}]]
           :types [[:auth-provider :enum {:choices ["google" "email" "apple"]}]
                   [:user-role :enum {:choices ["owner" "admin" "member" "viewer"]}]
                   [:user-status :enum {:choices ["active" "inactive" "suspended"]}]]}
   :properties {:fields [[:id :uuid {:primary-key true :default [:gen_random_uuid]}]
                         [:tenant_id :uuid {:foreign-key :tenants/id :null false :on-delete :cascade}]
                         [:owner_id :uuid {:foreign-key :users/id :null false :on-delete :cascade}]
                         [:name [:varchar 255] {:null false}]
                         [:address :text]
                         [:property_type [:enum :property-type] {:default "apartment"}]
                         [:status [:enum :property-status] {:default "active"}]
                         [:settings :jsonb {:default {}}]
                         [:financial_settings :jsonb {:default {}}]
                         [:created_at :timestamptz {:default [:now]}]
                         [:updated_at :timestamptz {:default [:now]}]]
                :types [[:property-type :enum {:choices ["apartment" "house" "room" "commercial" "other"]}]
                        [:property-status :enum {:choices ["active" "inactive" "archived"]}]]}
   :transaction_types {:fields [[:id :uuid {:primary-key true :default [:gen_random_uuid]}]
                                [:tenant_id :uuid {:foreign-key :tenants/id :on-delete :cascade}]
                                [:name [:varchar 255] {:null false}]
                                [:flow [:enum :flow] {:null false}]
                                [:is_global :boolean {:default false}]
                                [:parent_category_id :uuid {:foreign-key :transaction_types/id}]
                                [:icon [:varchar 50]]
                                [:color [:varchar 7]]
                                [:created_at :timestamptz {:default [:now]}]
                                [:updated_at :timestamptz {:default [:now]}]]
                       :types [[:flow :enum {:choices ["income" "expense"]}]]}
   :transactions {:fields [[:id :uuid {:primary-key true :default [:gen_random_uuid]}]
                           [:tenant_id :uuid {:foreign-key :tenants/id :null false :on-delete :cascade}]
                           [:property_id :uuid {:foreign-key :properties/id :on-delete :cascade}]
                           [:transaction_type_id :uuid {:foreign-key :transaction_types/id :null false}]
                           [:description :text {:null false :check [:> [:length :description] 3]}]
                           [:amount [:decimal 15 2] {:null false :check [:> :amount 0]}]
                           [:date :date {:null false}]
                           [:paid_by_user_id :uuid {:foreign-key :users/id :on-delete :set-null}]
                           [:tags :jsonb {:default []}]
                           [:attachments :jsonb {:default []}]
                           [:metadata :jsonb {:default {}}]
                           [:created_by :uuid {:foreign-key :users/id :null false :on-delete :cascade}]
                           [:updated_by :uuid {:foreign-key :users/id :on-delete :set-null}]
                           [:created_at :timestamptz {:default [:now]}]
                           [:updated_at :timestamptz {:default [:now]}]]}})

;; =============================================================================
;; Random Number Generation (Seedable)
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
;; Model Parsing Utilities
;; =============================================================================

(defn get-field-constraints "Extract constraints from field definition: [name type constraints]"
  [field-def]
  (when (>= (count field-def) 3)
    (nth field-def 2)))

(defn get-field-type "Extract field type from definition: [name type constraints]"
  [field-def]
  (nth field-def 1))

(defn get-field-name "Extract field name from definition: [name type constraints]"
  [field-def]
  (first field-def))

;; =============================================================================
;; Dependency Resolution System
;; =============================================================================

(defn extract-foreign-keys "Extract all foreign key relationships from models"
  [models]
  (reduce
    (fn [acc [entity-name entity-def]]
      (let [fields (:fields entity-def)
            fks (keep (fn [field-def]
                        (let [constraints (get-field-constraints field-def)
                              fk (:foreign-key constraints)]
                          (when fk
                            {:entity entity-name
                             :field (get-field-name field-def)
                             :references fk})))
                  fields)]
        (concat acc fks)))
    []
    models))

(defn build-dependency-graph "Build dependency graph showing which entities depend on others"
  [models]
  (let [fks (extract-foreign-keys models)]
    (reduce
      (fn [graph fk]
        (let [dependent (:entity fk)
              referenced (-> fk :references namespace keyword)]
          (update graph dependent (fnil conj #{}) referenced)))
      {}
      fks)))

(defn topological-sort "Sort entities in dependency order (referenced entities first)"
  [dependency-graph all-entities]
  (loop [sorted []
         remaining (set all-entities)
         deps dependency-graph]
    (if (empty? remaining)
      sorted
      (let [no-deps (set/difference remaining (set (keys deps)))
            next-items (if (seq no-deps)
                         no-deps
                         (take 1 remaining))                ; Fallback to avoid infinite loop
            new-sorted (concat sorted next-items)
            new-remaining (set/difference remaining next-items)
            new-deps (reduce dissoc deps next-items)]
        (recur new-sorted new-remaining new-deps)))))

;; =============================================================================
;; Smart Value Generation
;; =============================================================================

(defn generate-unique-id "Generate unique identifier for test data"
  []
  (swap! *id-counter* inc))

(defn generate-string-by-context
  "Generate contextually appropriate strings based on field name. The optional
  `length` parameter is currently ignored but kept for API parity with the
  backend generator so callers can pass a desired minimum length without
  breaking."
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
  (let [field-name (get-field-name field-def)
        field-type (get-field-type field-def)
        constraints (get-field-constraints field-def)]

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

;; =============================================================================
;; Entity Data Generation
;; =============================================================================

(defn generate-entity-data "Generate complete entity data based on models definition"
  [entity-keyword constraint-type]
  (let [entity-def (get models entity-keyword)
        fields (:fields entity-def)
        entity-types (:types entity-def)
        valid? (not= constraint-type :invalid)]

    (->> fields
      (keep #(generate-field-value % entity-types *data-context* valid?))
      (into {}))))

(defn generate-entity
  ([entity-type] (generate-entity entity-type {}))
  ([entity-type overrides]
   (let [generated (generate-entity-data entity-type :valid)
         ;; Generate UUID string for ID if not provided
         entity-id (or (:id overrides)
                     (let [id (generate-unique-id)
                           padded (str "000000000000" id)]
                       (str "550e8400-e29b-41d4-a716-" (.substring padded (- (count padded) 12)))))
         base-entity (assoc generated :id entity-id)]
     ;; Merge after inserting the id so callers can override any field
     (merge base-entity overrides))))

(defn generate-entities
  ([entity-type count] (generate-entities entity-type count {}))
  ([entity-type count overrides]
   (repeatedly count #(generate-entity entity-type overrides))))

(defn generate-invalid-entity
  "Generate an entity with invalid data"
  [entity-type]
  (generate-entity-data entity-type :invalid))

;; =============================================================================
;; Complete Test Data Generation
;; =============================================================================

(defn generate-comprehensive-test-data
  ([] (generate-comprehensive-test-data {}))
  ([options]
   (let [entities (keys models)
         dependency-graph (build-dependency-graph models)
         sorted-entities (topological-sort dependency-graph entities)
         seed-count (get options :seed-count 4)
         data-context (atom {})]

     (binding [*data-context* data-context]
       (loop [remaining sorted-entities
              result {}]
         (if (empty? remaining)
           result
           (let [entity-keyword (first remaining)
                 seed-data (vec (generate-entities entity-keyword seed-count))
                 new-data (generate-entity entity-keyword)
                 update-data (generate-entity entity-keyword)
                 invalid-data (generate-invalid-entity entity-keyword)]
             ;; Store in context for foreign key resolution
             (swap! data-context assoc entity-keyword seed-data)
             ;; Continue with updated result
             (recur (rest remaining)
               (assoc result entity-keyword
                 (hash-map :seed-data seed-data
                   :new-data new-data
                   :update-data update-data
                   :invalid-create-data invalid-data))))))))))

;; =============================================================================
;; Public API
;; =============================================================================

(defn generate-test-data-fixed
  "Fixed version of comprehensive test data generation"
  ([] (generate-test-data-fixed {}))
  ([options]
   (let [entities (keys models)
         dependency-graph (build-dependency-graph models)
         sorted-entities (topological-sort dependency-graph entities)
         seed-count (get options :seed-count 4)
         data-context (atom {})
         result (atom {})]

     (binding [*data-context* data-context]
       ;; Process each entity
       (doseq [entity-keyword sorted-entities]
         (let [seed-data (vec (generate-entities entity-keyword seed-count))]
           ;; Store seed data for foreign key resolution
           (swap! data-context assoc entity-keyword seed-data)
           ;; Generate all data types for this entity
           (swap! result assoc entity-keyword
             {:seed-data seed-data
              :new-data (generate-entity entity-keyword)
              :update-data (generate-entity entity-keyword)
              :invalid-create-data (generate-invalid-entity entity-keyword)})))
       @result))))

#_{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}
(defn get-auto-generated-data
  "Main entry point for getting auto-generated test data"
  ([]
   (get-auto-generated-data {}))
  ([options]
   (generate-test-data-fixed options)))

#_{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}
(defn reset-id-counter!
  "Reset the ID counter for test isolation"
  []
  (reset! *id-counter* 1000))
