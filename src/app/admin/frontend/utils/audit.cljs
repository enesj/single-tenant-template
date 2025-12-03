(ns app.admin.frontend.utils.audit
  "UI formatting utilities for audit logs.
   
   These helpers are used by audit components for display formatting.")

(defn format-timestamp
  "Format a timestamp for display."
  [timestamp]
  (when timestamp
    (let [date (js/Date. timestamp)]
      (.toLocaleString date))))

(defn format-changes
  "Format a changes map for display as a comma-separated string."
  [changes]
  (when (seq changes)
    (clojure.string/join ", "
      (for [[k v] changes]
        (str (name k) ": " v)))))

(defn get-action-badge-class
  "Return CSS classes for action type badge styling."
  [action]
  (case (keyword action)
    :create "bg-green-100 text-green-800"
    :update "bg-yellow-100 text-yellow-800"
    :delete "bg-red-100 text-red-800"
    "bg-gray-100 text-gray-800"))

(defn get-entity-icon
  "Return icon hiccup for entity type."
  [entity-type]
  (case (keyword entity-type)
    :user [:icon {:name "user" :class "w-4 h-4 text-gray-500"}]
    :subscription [:icon {:name "credit-card" :class "w-4 h-4 text-gray-500"}]
    [:icon {:name "question-mark-circle" :class "w-4 h-4 text-gray-500"}]))
