(ns automigrate.sql.columns
  "Column operation SQL generation with complex alteration logic."
  (:require
   [automigrate.actions :as actions]
   [automigrate.constraints :as constraints]
   [automigrate.sql.constraints :as sql-constraints]
   [automigrate.sql.core :refer [action->sql]]
   [automigrate.sql.fields :as sql-fields]
   [automigrate.util.model :as model-util]
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [spec-dict :as d]))

;; Column addition specs and implementation
(s/def ::options
  ::sql-fields/options->sql)

(s/def ::add-column->sql
  (s/conformer
    (fn [{:keys [model-name field-name options]}]
      (let [add-column {:alter-table model-name
                        :add-column (first (sql-constraints/fields->columns {:model-name model-name
                                                                             :fields [[field-name options]]}))}]
        (if (some? (:comment options))
          [add-column
           (sql-fields/create-comment-on-field-raw
             {:model-name model-name
              :field-name field-name
              :comment-val (:comment options)})]
          add-column)))))

(defmethod action->sql actions/ADD-COLUMN-ACTION
  [_]
  (s/and
    (s/keys
      :req-un [::actions/action
               ::actions/field-name
               ::actions/model-name
               ::options])
    ::add-column->sql))

;; Column alteration - complex logic
(s/def ::changes
  (s/and
    (d/dict*
      (d/->opt (model-util/generate-type-option :automigrate.sql.option->sql/type))
      (d/->opt (model-util/generate-changes sql-fields/options-specs)))
    #(> (count (keys %)) 0)))

(defn- foreign-key-changes
  "Return full option changes or nil if option should be dropped."
  [options changes-to-add changes-to-drop]
  (let [keys-to-add (set (keys changes-to-add))]
    (if (contains? changes-to-drop :foreign-key)
      nil
      (when (or (some keys-to-add [:foreign-key :on-delete :on-update])
              (some changes-to-drop [:on-delete :on-update]))
        (:foreign-key options)))))

(defn- get-changes
  [action]
  (let [changes-to-add (model-util/changes-to-add (:changes action))
        changes-to-drop (model-util/changes-to-drop (:changes action))
        foreign-key (foreign-key-changes (:options action) changes-to-add changes-to-drop)]
    {:changes-to-add (cond-> changes-to-add
                       true (dissoc :on-delete :on-update)
                       (some? foreign-key) (assoc :foreign-key foreign-key))
     :changes-to-drop (disj changes-to-drop :on-delete :on-update)}))

(defn- alter-foreign-key->edn
  [{:keys [model-name field-name field-value action-changes]}]
  (let [from-value-empty? (= :EMPTY (get-in action-changes [:foreign-key :from]))
        drop-constraint {:drop-constraint
                         [[:raw "IF EXISTS"]
                          (constraints/foreign-key-constraint-name model-name field-name)]}
        add-constraint {:add-constraint
                        (concat [(constraints/foreign-key-constraint-name model-name field-name)
                                 [:foreign-key field-name]]
                          field-value)}]
    (cond-> []
      (not from-value-empty?) (conj drop-constraint)
      true (conj add-constraint))))

(defn- alter-check->edn
  [{:keys [model-name field-name field-value action-changes]}]
  (let [from-value-empty? (= :EMPTY (get-in action-changes [:check :from]))
        drop-constraint {:drop-constraint
                         [[:raw "IF EXISTS"]
                          (constraints/check-constraint-name model-name field-name)]}
        add-constraint {:add-constraint
                        [(constraints/check-constraint-name model-name field-name)
                         field-value]}]
    (cond-> []
      (not from-value-empty?) (conj drop-constraint)
      true (conj add-constraint))))

(defn- ->alter-column
  [field-name option {:keys [changes options] :as _action}]
  (when (or (= option :type)
          (and (not (contains? changes :type))
            (= option :array)))
    (let [type-sql (sql-fields/field-type->sql options)
          field-name-str (-> field-name (name) (str/replace #"-" "_"))]
      {:alter-column
       (concat
         [field-name :type]
         type-sql
         ; always add `using` to be able to convert different types
         [:using [:raw field-name-str] [:raw "::"]]
         type-sql)})))

(defn- alter-primary-key->edn
  [model-name field-name]
  {:add-constraint
   [(constraints/primary-key-constraint-name model-name)
    [:primary-key field-name]]})

(defn- alter-unique->edn
  [model-name field-name]
  {:add-constraint
   [(constraints/unique-constraint-name model-name field-name)
    [:unique nil field-name]]})

(s/def ::alter-column->sql
  (s/conformer
    (fn [{:keys [field-name model-name] :as action}]
      (let [{:keys [changes-to-add changes-to-drop]} (get-changes action)
            changes (for [[option value] changes-to-add
                          :when (not= option :comment)]
                      (condp contains? option
                        #{:type :array} (->alter-column field-name option action)
                        #{:null} (let [operation (if (nil? value) :drop :set)]
                                   {:alter-column [field-name operation [:not nil]]})
                        #{:default} {:alter-column [field-name :set value]}
                        #{:unique} (alter-unique->edn model-name field-name)
                        #{:primary-key} (alter-primary-key->edn model-name field-name)
                        #{:foreign-key} (alter-foreign-key->edn
                                          {:model-name model-name
                                           :field-name field-name
                                           :field-value value
                                           :action-changes (:changes action)})
                        #{:check} (alter-check->edn
                                    {:model-name model-name
                                     :field-name field-name
                                     :field-value value
                                     :action-changes (:changes action)})))
            ; remove nil if options type and array have been changed
            changes* (->> changes (remove nil?) (flatten))

            dropped (for [option changes-to-drop
                          :when (not= option :comment)]
                      (case option
                        :array (->alter-column field-name option action)
                        :null {:alter-column [field-name :drop [:not nil]]}
                        :default {:alter-column [field-name :drop :default]}
                        :unique {:drop-constraint (constraints/unique-constraint-name
                                                    model-name
                                                    field-name)}
                        :primary-key {:drop-constraint (constraints/primary-key-constraint-name
                                                         model-name)}
                        :foreign-key {:drop-constraint (constraints/foreign-key-constraint-name
                                                         model-name
                                                         field-name)}
                        :check {:drop-constraint (constraints/check-constraint-name
                                                   model-name
                                                   field-name)}))
            dropped* (remove nil? dropped)
            all-actions (concat changes* dropped*)
            alter-table-sql {:alter-table (cons model-name all-actions)}

            new-comment-val (-> action :changes :comment :to)
            comment-sql (when (some? new-comment-val)
                          (sql-fields/create-comment-on-field-raw
                            {:model-name model-name
                             :field-name field-name
                             :comment-val (when (not= new-comment-val :EMPTY)
                                            new-comment-val)}))]
        (if (and (seq all-actions) (not (seq comment-sql)))
          ; for compatibility with existing tests
          alter-table-sql
          (cond-> []
            (seq all-actions) (conj alter-table-sql)
            (seq comment-sql) (conj comment-sql)))))))

(defmethod action->sql actions/ALTER-COLUMN-ACTION
  [_]
  (s/and
    (s/keys
      :req-un [::actions/action
               ::actions/field-name
               ::actions/model-name
               ::options
               ::changes])
    ::alter-column->sql))

;; Column deletion specs and implementation
(s/def ::drop-column->sql
  (s/conformer
    (fn [value]
      {:alter-table (:model-name value)
       :drop-column (:field-name value)})))

(defmethod action->sql actions/DROP-COLUMN-ACTION
  [_]
  (s/and
    (s/keys
      :req-un [::actions/action
               ::actions/field-name
               ::actions/model-name])
    ::drop-column->sql))
