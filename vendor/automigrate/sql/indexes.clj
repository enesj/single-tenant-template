(ns automigrate.sql.indexes
  "Index operation SQL generation."
  (:require
   [automigrate.actions :as actions]
   [automigrate.sql.core :refer [action->sql]]
   [clojure.spec.alpha :as s]))

(def ^:private DEFAULT-INDEX :btree)

;; Index creation specs and implementation
(s/def ::create-index->sql
  (s/conformer
    (fn [value]
      (let [options (:options value)
            index-type (or (:type options) DEFAULT-INDEX)
            index-action (if (true? (:unique options))
                           :create-unique-index
                           :create-index)
            index-name (:index-name value)
            ;; Base spec; idempotency handled at execution layer
            base-spec [index-name
                       :on (:model-name value)
                       :using (cons index-type (:fields options))]
            base-spec (cond-> base-spec
                         (seq (:where options)) (concat [:where (:where options)]))]
        {index-action base-spec}))))

(defmethod action->sql actions/CREATE-INDEX-ACTION
  [_]
  (s/and
    (s/keys
      :req-un [::actions/action
               ::actions/index-name
               ::actions/model-name
               :automigrate.actions.indexes/options])
    ::create-index->sql))

;; Index deletion specs and implementation
(s/def ::drop-index->sql
  (s/conformer
    (fn [value]
      {:drop-index (:index-name value)})))

(defmethod action->sql actions/DROP-INDEX-ACTION
  [_]
  (s/and
    (s/keys
      :req-un [::actions/action
               ::actions/index-name
               ::actions/model-name])
    ::drop-index->sql))

;; Index alteration specs and implementation
(s/def ::alter-index->sql
  (s/conformer
    (fn [value]
      [(s/conform ::drop-index->sql (assoc value :action actions/DROP-INDEX-ACTION))
       (s/conform ::create-index->sql (assoc value :action actions/CREATE-INDEX-ACTION))])))

(defmethod action->sql actions/ALTER-INDEX-ACTION
  [_]
  (s/and
    (s/keys
      :req-un [::actions/action
               ::actions/index-name
               ::actions/model-name
               :automigrate.actions.indexes/options])
    ::alter-index->sql))
