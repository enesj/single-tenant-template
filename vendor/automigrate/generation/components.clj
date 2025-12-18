(ns automigrate.generation.components
  "Component-specific migration generation for functions, triggers, policies, and views."
  (:require
    [automigrate.db.introspection :as db-intro]
    [clojure.string :as str]))

(defn generate-function-migrations
  "Generate migrations for database functions"
  [old-functions new-functions]
  (let [old-fn-names (set (map :function_name old-functions))
        new-fn-names (set (map #(name (first %)) new-functions))
        to-drop (remove new-fn-names old-fn-names)
        to-create (filter #(not (old-fn-names (name (first %)))) new-functions)]
    (concat
      (map #(db-intro/format-function-drop % nil) to-drop)
      (map #(get-in (second %) [:up]) to-create))))

(defn generate-trigger-migrations
  "Generate migrations for database triggers"
  [old-triggers new-triggers]
  (let [old-trigger-names (set (map :trigger_name old-triggers))
        new-trigger-names (set (map #(name (first %)) new-triggers))
        to-drop (remove new-trigger-names old-trigger-names)
        to-create (filter #(not (old-trigger-names (name (first %)))) new-triggers)]
    (concat
      (map #(format "DROP TRIGGER IF EXISTS %s;" %) to-drop)
      (map #(get-in (second %) [:up]) to-create))))

(defn generate-policy-migrations
  "Generate migrations for RLS policies"
  [old-policies new-policies]
  (let [old-policy-names (set (map :policy_name old-policies))
        new-policy-names (set (map #(name (first %)) new-policies))
        to-drop (remove new-policy-names old-policy-names)
        to-create (filter #(not (old-policy-names (name (first %)))) new-policies)]
    (concat
      (map #(format "DROP POLICY IF EXISTS %s;" %) to-drop)
      (map #(get-in (second %) [:up]) to-create))))

(defn generate-view-migrations
  "Generate migrations for database views"
  [old-views new-views]
  (let [old-view-names (set (map :view_name old-views))
        new-view-names (set (map #(name (first %)) new-views))
        to-drop (remove new-view-names old-view-names)
        to-create (filter #(not (old-view-names (name (first %)))) new-views)]
    (concat
      (map #(format "DROP VIEW IF EXISTS %s CASCADE;" %) to-drop)
      (map #(get-in (second %) [:up]) to-create))))
