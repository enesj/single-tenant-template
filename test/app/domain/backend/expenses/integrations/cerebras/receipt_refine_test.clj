(ns app.domain.backend.expenses.integrations.cerebras.receipt-refine-test
  (:require
    [app.domain.backend.expenses.integrations.cerebras.receipt-refine :as receipt-refine]
    [clojure.test :refer [deftest is testing]]))

(defn- schema-nodes
  "Return a lazy seq of all nodes (maps + scalars) in a JSON-schema-shaped structure." 
  [x]
  (letfn [(branch? [v]
            (or (map? v)
                (sequential? v)))
          (children [v]
            (cond
              (map? v) (vals v)
              (sequential? v) v
              :else nil))]
    (tree-seq branch? children x)))

(deftest receipt-extraction-schema-uses-anyof-not-type-lists
  (testing "Cerebras structured outputs does not accept `type: [..]` (list-of-types)"
    (let [schema receipt-refine/receipt-extraction-json-schema
          maps (filter map? (schema-nodes schema))
          offending (->> maps
                      (keep (fn [m]
                              (when (sequential? (get m "type"))
                                m)))
                      vec)]
      (is (empty? offending)
        (str "Found schema nodes using list-of-types under `type`: " (pr-str offending))))))

(deftest receipt-extraction-schema-avoids-minimum
  (testing "Cerebras structured outputs rejects numeric constraints like `minimum`"
    (let [schema receipt-refine/receipt-extraction-json-schema
          maps (filter map? (schema-nodes schema))
          offending (->> maps
                      (keep (fn [m]
                              (when (contains? m "minimum")
                                m)))
                      vec)]
      (is (empty? offending)
        (str "Found schema nodes using unsupported `minimum`: " (pr-str offending))))))

(deftest receipt-extraction-schema-has-anyof-for-known-nullables
  (testing "Sanity-check: top-level known nullable fields have anyOf"
    (let [schema receipt-refine/receipt-extraction-json-schema
          props (get schema "properties")]
      (is (contains? (get props "merchant") "anyOf"))
      (is (contains? (get props "purchased_at") "anyOf"))
      (is (contains? (get props "currency") "anyOf")))))
