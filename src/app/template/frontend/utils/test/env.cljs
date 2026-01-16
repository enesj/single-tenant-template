(ns app.template.frontend.utils.test.env
  (:require
    [clojure.string :as str]
    [goog.object :as gobj]
    [re-frame.db :as rf-db]
    [re-frame.registrar :as rf-registrar]))

(defn safe-subscribe
  "Safely subscribe to a re-frame subscription, returning nil on failure.
   This works outside of reactive context for testing purposes."
  [sub-key]
  (try
    ;; Try to directly call the subscription handler from the registry
    (let [db @rf-db/app-db
          handler (get-in @rf-registrar/kind->id->handler [:sub sub-key])]
      (when handler
        (let [result (handler db [sub-key])
              ;; The handler may return a Reaction - deref it to get the value
              value (cond
                      ;; If result implements IDeref, deref it
                      (satisfies? IDeref result) @result
                      ;; Otherwise use directly
                      :else result)]
          value)))
    (catch :default _ nil)))

(defn deep-js->clj
  "Recursively convert JS object to Clojure map, handling nested objects.
   Handles both camelCase and kebab-case keys, converting to kebab-case keywords."
  [obj]
  (cond
    (nil? obj) nil
    (keyword? obj) obj
    (string? obj) obj
    (number? obj) obj
    (boolean? obj) obj
    (array? obj) (mapv deep-js->clj obj)
    (object? obj)
    (let [keys (js/Object.keys obj)]
      (into {}
        (for [k keys
              :let [v (gobj/get obj k)]
              :when (not (fn? v))]
          ;; Convert both camelCase and kebab-case to kebab-case keywords
          [(keyword (-> k
                      ;; Convert camelCase to kebab-case
                      (str/replace #"([a-z])([A-Z])" "$1-$2")
                      str/lower-case))
           (deep-js->clj v)])))
    :else obj))

(defn setup-test-environment!
  "Set up the test environment for React component testing."
  []
  (when-not (exists? js/document)
    (println "Warning: js/document not available. Tests may fail.")))
