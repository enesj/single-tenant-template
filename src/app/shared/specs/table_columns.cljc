(ns app.shared.specs.table-columns
  "Malli specs for table-columns configuration files.

  Used by:
  - src/app/admin/frontend/config/table-columns.edn
  - src/app/domain/frontend/expenses/config/table-columns.edn" 
  (:require
    [clojure.set :as set]
    [malli.core :as m]
    [malli.error :as me]))

;; =============================================================================
;; Common
;; =============================================================================

(def ColumnId
  "Column identifiers are sometimes strings (JSON-style) and sometimes keywords." 
  [:or :keyword :string])

(def ColumnIdVec
  [:vector ColumnId])

(def ColumnFormatter
  [:or :keyword :string])

(def ColumnConfig
  [:map {:closed false}
   [:width {:optional true} :string]
   [:formatter {:optional true} ColumnFormatter]
   [:computed-field {:optional true} :boolean]])

(def ComputedField
  "Computed field metadata.

  Admin audit logs use string-based configuration here." 
  [:map {:closed false}
   [:type {:optional true} [:or :keyword :string]]
   [:compute-fn {:optional true} [:or :keyword :string]]
   [:dependencies {:optional true} [:vector [:or :keyword :string]]]])

(def EntityTableColumns
  [:map {:closed false}
   [:available-columns {:optional true} ColumnIdVec]
   [:default-visible-columns {:optional true} ColumnIdVec]
   [:filterable-columns {:optional true} ColumnIdVec]
   [:sortable-columns {:optional true} ColumnIdVec]
   [:always-visible {:optional true} ColumnIdVec]
   [:computed-fields {:optional true} [:map-of ColumnId ComputedField]]
   [:column-config {:optional true} [:map-of ColumnId ColumnConfig]]])

(def TableColumnsFile
  [:map-of :keyword EntityTableColumns])

(defn validate-table-columns
  "Validate an entire table-columns.edn data structure." 
  [data]
  (if (m/validate TableColumnsFile data)
    {:valid? true :data data}
    {:valid? false
     :errors (me/humanize (m/explain TableColumnsFile data))}))

;; =============================================================================
;; Strict checks
;; =============================================================================

(defn- normalize-id
  [x]
  (cond
    (keyword? x) (name x)
    (string? x) x
    :else nil))

(defn- ids->set
  [xs]
  (->> xs
       (map normalize-id)
       (remove nil?)
       set))

(defn- check-column-subsets
  "Ensure key vectors are subsets of :available-columns (when available).

  Returns nil if OK, otherwise returns a seq of problems." 
  [data]
  (let [problems
        (->> data
             (mapcat
               (fn [[entity cfg]]
                 (let [available (ids->set (:available-columns cfg))
                       checks [[:default-visible-columns (:default-visible-columns cfg)]
                               [:filterable-columns (:filterable-columns cfg)]
                               [:sortable-columns (:sortable-columns cfg)]
                               [:always-visible (:always-visible cfg)]]]
                   (when (seq available)
                     (for [[k xs] checks
                           :let [xs-set (ids->set xs)
                                 missing (->> (set/difference xs-set available)
                                              sort
                                              vec)]
                           :when (seq missing)]
                       {:entity entity
                        :problem (str k " contains columns not in :available-columns: " missing)
                        :fix "Add the columns to :available-columns, or remove them from the list."})))) )
             vec)]
    (when (seq problems)
      problems)))

(defn- check-no-duplicate-columns
  "Ensure vectors don't contain duplicates (after normalizing keyword/string IDs)." 
  [data]
  (let [problems
        (->> data
             (mapcat
               (fn [[entity cfg]]
                 (let [checks [[:available-columns (:available-columns cfg)]
                               [:default-visible-columns (:default-visible-columns cfg)]
                               [:filterable-columns (:filterable-columns cfg)]
                               [:sortable-columns (:sortable-columns cfg)]
                               [:always-visible (:always-visible cfg)]]]
                   (for [[k xs] checks
                         :when (seq xs)
                         :let [normalized (map normalize-id xs)
                               dupes (->> normalized
                                          (remove nil?)
                                          frequencies
                                          (filter (fn [[_ n]] (> n 1)))
                                          (map first)
                                          sort
                                          vec)]
                         :when (seq dupes)]
                     {:entity entity
                      :problem (str "Duplicate entries in " k ": " dupes)
                      :fix "Remove duplicates from the vector."}))))
             vec)]
    (when (seq problems)
      problems)))

(defn validate-table-columns-strict
  "Validate table-columns with additional consistency checks." 
  [data]
  (let [schema-result (validate-table-columns data)
        subset-issues (check-column-subsets data)
        dupes (check-no-duplicate-columns data)
        issues (vec (concat subset-issues dupes))]
    (cond
      (not (:valid? schema-result))
      schema-result

      (seq issues)
      {:valid? false
       :errors [(str "Table-columns consistency issues: " issues)]
       :warnings issues
       :data data}

      :else
      schema-result)))
