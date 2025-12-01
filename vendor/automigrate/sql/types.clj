(ns automigrate.sql.types
  "Type operation SQL generation with complex enum alteration logic."
  (:require
   [automigrate.actions :as actions]
   [automigrate.sql.core :refer [action->sql]]
   [automigrate.util.spec :as spec-util]
   [clojure.spec.alpha :as s]
   [slingshot.slingshot :refer [throw+]]))

;; Type creation specs and implementation
(s/def ::create-type->sql
  (s/conformer
    (fn [value]
      (when (= :enum (get-in value [:options :type]))
        (let [options (:options value)
              type-action actions/CREATE-TYPE-ACTION]
          {type-action [(:type-name value) :as (cons :enum (:choices options))]})))))

(defmethod action->sql actions/CREATE-TYPE-ACTION
  [_]
  (s/and
    (s/keys
      :req-un [::actions/action
               ::actions/type-name
               ::actions/model-name
               :automigrate.actions.types/options])
    ::create-type->sql))

;; Type deletion specs and implementation
(s/def ::drop-type->sql
  (s/conformer
    (fn [value]
      ; TODO: try to remove vector
      {:drop-type [(:type-name value)]})))

(defmethod action->sql actions/DROP-TYPE-ACTION
  [_]
  (s/and
    (s/keys
      :req-un [::actions/action
               ::actions/type-name
               ::actions/model-name])
    ::drop-type->sql))

;; Complex enum alteration logic
(defn- ->alter-type-action
  [{:keys [type-name new-value position existing-value]}]
  {:pre [(spec-util/assert! keyword? type-name)
         (spec-util/assert! string? new-value)
         (spec-util/assert! #{:before :after} position)
         (spec-util/assert! string? existing-value)]}
  {:alter-type [type-name :add-value new-value position existing-value]})

(defn- get-actions-for-new-choices
  [type-name initial-value choices-to-add position]
  {:pre [(spec-util/assert! #{:before :after} position)]}
  (loop [prev-value initial-value
         [new-value & rest-choices] choices-to-add
         actions []]
    (if-not new-value
      actions
      (let [next-action (->alter-type-action {:type-name type-name
                                              :new-value new-value
                                              :position position
                                              :existing-value prev-value})]
        (recur new-value rest-choices (conj actions next-action))))))

(defn- last-item?
  [idx items]
  (= (inc idx) (count items)))

(defn- get-actions-for-enum-choices-changes
  [type-name
   from-choices
   to-choices
   result-actions
   [from-idx from-value]]
  (let [value-in-to-idx (.indexOf to-choices from-value)]
    ; It is not possible to remove choices from the enum type
    (when (< value-in-to-idx 0)
      (throw+ {:type ::alter-type-missing-old-choice
               :message "Missing old choice value in new enum type definition"
               :data {:from-idx from-idx
                      :from-value from-value}}))

    (let [prev-value-count-drop (if (= from-idx 0)
                                  0
                                  (->> (nth from-choices (dec from-idx) 0)
                                    (.indexOf to-choices)
                                    ; inc index to get count of  items from the start of vec
                                    (inc)))
          before-reversed (-> to-choices
                            (subvec prev-value-count-drop value-in-to-idx)
                            (reverse))
          new-actions-before (get-actions-for-new-choices
                               type-name
                               from-value
                               before-reversed
                               :before)
          new-actions-after (if (last-item? from-idx from-choices)
                              (get-actions-for-new-choices
                                type-name
                                from-value
                                ; the rest of the `to-choices` vec
                                (subvec to-choices (inc value-in-to-idx))
                                :after)
                              [])]
      (vec (concat result-actions new-actions-before new-actions-after)))))

;; Type alteration specs and implementation
(s/def ::alter-type->sql
  (s/conformer
    (fn [action]
      ; Currently there is implementation for enum type
      (let [{:keys [from to]} (get-in action [:changes :choices])]
        (reduce
          (partial get-actions-for-enum-choices-changes (:type-name action) from to)
          []
          (map-indexed vector from))))))

(defmethod action->sql actions/ALTER-TYPE-ACTION
  [_]
  (s/and
    (s/keys
      :req-un [::actions/action
               ::actions/type-name
               ::actions/model-name
               :automigrate.actions.types/options
               :automigrate.actions.types/changes])
    ::alter-type->sql))
