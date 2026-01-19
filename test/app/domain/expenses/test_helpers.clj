(ns app.domain.expenses.test-helpers
  "Test helpers for the expenses domain.

  These helpers smooth over common setup steps (e.g. payer types) so integration
  tests can focus on domain behavior rather than schema wiring."
  (:require
    [app.domain.backend.expenses.services.payer-types :as payer-types]
    [app.domain.backend.expenses.services.payers :as payers]
    [clojure.string :as str]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))

(defn- payer-type-label
  "Derive a stable payer type label from legacy `:type` strings.

  The production system uses payer types as a table, while some tests still use
  enum-like strings such as \"cash\"/\"card\"/\"account\"."
  [type]
  (let [s (-> (or type "cash") str str/trim str/lower-case)]
    (if (str/blank? s)
      "Cash"
      (str/capitalize s))))

(defn ensure-payer-type!
  "Ensure a payer type exists (by label) and return the row." 
  [db label]
  (or (jdbc/execute-one!
        db
  ["select * from payer_types where label = ? limit 1" label]
  {:builder-fn rs/as-unqualified-lower-maps})
    (payer-types/create-payer-type! db {:label label :is_default false})))

(defn create-payer!
  "Create a payer, accepting either:

  - `:payer_type_id` (preferred), or
  - legacy `:type` (string) which is mapped to a payer type label.

  Returns the created payer row." 
  [db {:keys [type payer_type_id] :as payer-data}]
  (let [payer-type-id (or payer_type_id
                          (:id (ensure-payer-type! db (payer-type-label type))))]
    (payers/create-payer!
      db
      (-> payer-data
        (dissoc :type)
        (assoc :payer_type_id payer-type-id)))))
