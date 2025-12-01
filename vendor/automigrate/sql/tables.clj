(ns automigrate.sql.tables
  "Table operation SQL generation."
  (:require
   [automigrate.actions :as actions]
   [automigrate.sql.constraints :as sql-constraints]
   [automigrate.sql.core :refer [action->sql]]
   [automigrate.sql.fields :as sql-fields]
   [clojure.spec.alpha :as s]))

;; Table creation specs and implementation
(s/def ::create-table->sql
  (s/conformer
    (fn [{:keys [model-name fields]}]
      (let [create-table-q {:create-table [model-name]
                            :with-columns (sql-constraints/fields->columns {:fields fields
                                                                            :model-name model-name})}
            create-comments-q-vec (sql-fields/fields->comments-sql model-name fields)]
        (if (seq create-comments-q-vec)
          (concat [create-table-q] (vec create-comments-q-vec))
          create-table-q)))))

(defmethod action->sql actions/CREATE-TABLE-ACTION
  [_]
  (s/and
    (s/keys
      :req-un [::actions/action
               ::actions/model-name
               ::sql-fields/fields])
    ::create-table->sql))

;; Table deletion specs and implementation
(s/def ::drop-table->sql
  (s/conformer
    (fn [value]
      {:drop-table [:if-exists (:model-name value)]})))

(defmethod action->sql actions/DROP-TABLE-ACTION
  [_]
  (s/and
    (s/keys
      :req-un [::actions/action
               ::actions/model-name])
    ::drop-table->sql))
