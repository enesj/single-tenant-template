(ns app.shared.frontend-config.validation
  "Semantic validation for frontend config bundles.

  Validates entities, form-fields, table-columns, and view-options
  against the DB schema and allowlists."
  (:require
    [app.shared.frontend-config.discovery :as discovery]
    [app.shared.frontend-config.schema :as schema]
    [app.shared.specs.entities :as entities-spec]
    [app.shared.specs.form-fields :as form-fields-spec]
    [app.shared.specs.table-columns :as table-columns-spec]
    [app.shared.specs.view-options :as view-options-spec]))

(def ^:private table-columns-list-keys
  [:available-columns :default-visible-columns :filterable-columns :sortable-columns :always-visible])

(def ^:private form-fields-list-keys
  [:create-fields :edit-fields :required-fields])

(defn- normalize-values
  [xs]
  (->> xs
    (map discovery/normalize-field-id)
    (remove nil?)
    set))

(defn computed-fields-by-entity
  [table-columns]
  (into {}
    (for [[entity cfg] table-columns]
      [(discovery/normalize-entity-id entity)
       (->> (:computed-fields cfg)
         keys
         (map discovery/normalize-field-id)
         (remove nil?)
         set)])))

(defn- spec-validate
  [kind scope data]
  (case kind
    :entities (if (= :domain scope)
                (entities-spec/validate-user-entities data)
                (entities-spec/validate-admin-entities-strict data))
    :form-fields (form-fields-spec/validate-form-fields-strict data)
    :table-columns (table-columns-spec/validate-table-columns-strict data)
    :view-options (view-options-spec/validate-view-options-strict data)
    {:valid? false :errors [(str "Unknown config kind: " kind)]}))

(defn- unknown-values
  [values allowed]
  (->> values
    (filter (fn [v]
              (let [normalized (discovery/normalize-field-id v)]
                (not (contains? allowed normalized)))))
    vec))

(defn- semantic-issues-table-columns
  [data schema-index computed allowlist]
  (reduce-kv
    (fn [acc entity cfg]
      (let [entity* (discovery/normalize-entity-id entity)
            allowed (schema/allowed-fields {:schema-index schema-index
                                            :entity entity*
                                            :computed computed
                                            :allowlist allowlist
                                            :include-computed? true})]
        (if-not (schema/known-entity? schema-index allowlist entity*)
          (update acc :unknown-entities conj entity)
          (let [unknown (into {}
                          (for [k table-columns-list-keys
                                :let [vals (get cfg k)
                                      unknown* (when (seq vals)
                                                 (unknown-values vals allowed))]
                                :when (seq unknown*)]
                            [k unknown*]))
                unknown-config (let [vals (keys (:column-config cfg))
                                     unknown* (when (seq vals)
                                                (unknown-values vals allowed))]
                                 (when (seq unknown*)
                                   {:column-config unknown*}))
                unknown (merge unknown unknown-config)
                available (normalize-values (:available-columns cfg))
                db-order (get-in schema-index [:entity->fields entity* :raw] [])
                missing (->> db-order
                          (remove (fn [f]
                                    (contains? available (discovery/normalize-field-id f))))
                          vec)
                acc (if (seq unknown)
                      (update acc :unknown-fields assoc entity unknown)
                      acc)
                acc (if (seq missing)
                      (update acc :missing-fields assoc entity missing)
                      acc)]
            acc))))
    {:unknown-entities []
     :unknown-fields {}
     :missing-fields {}}
    data))

(defn- semantic-issues-form-fields
  [data schema-index allowlist]
  (reduce-kv
    (fn [acc entity cfg]
      (let [entity* (discovery/normalize-entity-id entity)
            allowed (schema/allowed-fields {:schema-index schema-index
                                            :entity entity*
                                            :computed {}
                                            :allowlist allowlist
                                            :include-computed? false})]
        (if-not (schema/known-entity? schema-index allowlist entity*)
          (update acc :unknown-entities conj entity)
          (let [unknown (into {}
                          (for [k form-fields-list-keys
                                :let [vals (get cfg k)
                                      unknown* (when (seq vals)
                                                 (unknown-values vals allowed))]
                                :when (seq unknown*)]
                            [k unknown*]))
                unknown-config (let [vals (keys (:field-config cfg))
                                     unknown* (when (seq vals)
                                                (unknown-values vals allowed))]
                                 (when (seq unknown*)
                                   {:field-config unknown*}))
                unknown (merge unknown unknown-config)
                acc (if (seq unknown)
                      (update acc :unknown-fields assoc entity unknown)
                      acc)]
            acc))))
    {:unknown-entities []
     :unknown-fields {}
     :missing-fields {}}
    data))

(defn- semantic-issues-view-options
  [data schema-index computed allowlist]
  (reduce-kv
    (fn [acc entity cfg]
      (let [entity* (discovery/normalize-entity-id entity)
            allowed (schema/allowed-fields {:schema-index schema-index
                                            :entity entity*
                                            :computed computed
                                            :allowlist allowlist
                                            :include-computed? true})]
        (if-not (schema/known-entity? schema-index allowlist entity*)
          (update acc :unknown-entities conj entity)
          (let [unknown-defaults (unknown-values (keys (:column-defaults cfg)) allowed)
                unknown-locks (unknown-values (keys (:column-locks cfg)) allowed)
                unknown (cond-> {}
                          (seq unknown-defaults) (assoc :column-defaults unknown-defaults)
                          (seq unknown-locks) (assoc :column-locks unknown-locks))
                acc (if (seq unknown)
                      (update acc :unknown-fields assoc entity unknown)
                      acc)]
            acc))))
    {:unknown-entities []
     :unknown-fields {}
     :missing-fields {}}
    data))

(defn- semantic-issues-entities
  [data schema-index allowlist]
  (let [unknown (->> (keys data)
                  (remove #(schema/known-entity? schema-index allowlist %))
                  vec)]
    {:unknown-entities unknown
     :unknown-fields {}
     :missing-fields {}}))

(defn validate-bundles
  "Validate all config bundles. Returns per-file results.

  Each result contains:
  {:scope :admin|:domain
   :domain \"expenses\"|nil
   :kind :entities|:form-fields|:table-columns|:view-options
   :path \"...\"
   :valid? boolean
   :errors []
   :warnings []
   :semantic {:unknown-entities [] :unknown-fields {} :missing-fields {}}}"
  [bundles schema-index allowlist]
  (let [allowlist (schema/normalize-allowlist allowlist)]
    (mapcat
      (fn [bundle]
        (let [data (:data bundle)
              computed (computed-fields-by-entity (:table-columns data))]
          (for [[kind value] data
                :let [spec-result (spec-validate kind (:scope bundle) value)
                      semantic (if (:valid? spec-result)
                                 (case kind
                       :entities (semantic-issues-entities value schema-index allowlist)
                                   :form-fields (semantic-issues-form-fields value schema-index allowlist)
                                   :table-columns (semantic-issues-table-columns value schema-index computed allowlist)
                                   :view-options (semantic-issues-view-options value schema-index computed allowlist)
                                   {:unknown-entities [] :unknown-fields {} :missing-fields {}})
                                 {:unknown-entities [] :unknown-fields {} :missing-fields {}})
                      semantic-errors? (or (seq (:unknown-entities semantic))
                                         (seq (:unknown-fields semantic)))
                      warnings (vec (concat (:warnings spec-result)
                                      (when (seq (:missing-fields semantic))
                                        [{:missing-fields (:missing-fields semantic)}])))
                      errors (vec (concat (:errors spec-result)
                                    (when semantic-errors?
                                      [(str "Schema alignment issues: "
                                         (pr-str (select-keys semantic
                                                   [:unknown-entities :unknown-fields])))])))]]
            (merge bundle
              {:kind kind
               :path (get-in bundle [:paths kind])
               :valid? (and (:valid? spec-result) (not semantic-errors?))
               :errors errors
               :warnings warnings
               :semantic semantic}))))
      bundles)))
