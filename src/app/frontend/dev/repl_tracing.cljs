(ns app.frontend.dev.repl-tracing
  "Convenient REPL utilities for inspecting re-frame traces.

   Usage from browser REPL:
   (require '[app.frontend.dev.repl-tracing :as repl-trace])
   (repl-trace/recent)  ; Return recent traces
   (repl-trace/events) ; Return only events
   (repl-trace/stats)  ; Return buffer statistics"

  (:require [app.frontend.dev.tracing :as tracing]
    [clojure.string :as str]))

;; ─── Pretty Printing Helpers ──────────────────────────────────────

(defn- format-trace
  "Format a single trace map for readable console output."
  [trace]
  (let [{:keys [op-type start duration tags]} trace
        event-vect (get-in tags [:event])
        sub-query (get-in tags [:query-v])]
    (cond-> {:op-type op-type
             :start start
             :duration-ms duration}
      event-vect (assoc :event event-vect)
      sub-query (assoc :subscription sub-query))))

(defn- format-traces
  "Format a collection of traces for readable output."
  [traces]
  (mapv format-trace traces))

;; ─── Convenience Functions for REPL Use ───────────────────────────

(defn recent
  "Return the most recent traces (default: 20, max: 100)."
  ([] (recent 20))
  ([n]
   (let [limited-n (min n 100)]
     (->> (tracing/get-recent limited-n)
       format-traces))))

(defn events
  "Return the most recent event traces (default: 20, max: 100)."
  ([] (events 20))
  ([n]
   (let [limited-n (min n 100)]
     (->> (tracing/get-events)
       (take-last limited-n)
       format-traces))))

(defn subscriptions
  "Return the most recent subscription traces (default: 20, max: 100)."
  ([] (subscriptions 20))
  ([n]
   (let [limited-n (min n 100)]
     (->> (tracing/get-subs)
       (take-last limited-n)
       format-traces))))

(defn by-operation
  "Return traces filtered by operation type (default: all, max: 100)."
  [op-type & [n]]
  (let [filtered (tracing/get-by-operation op-type)
        limited-n (if n (min n 100) 100)
        to-show (take-last limited-n filtered)]
    (->> to-show
      format-traces)))

(defn stats
  "Return trace buffer statistics."
  []
  (tracing/get-stats))

(defn clear
  "Clear the trace buffer."
  []
  (tracing/clear-history!))

(defn search
  "Search traces by event keyword or pattern (default: 50, max: 100)."
  [pattern & [n]]
  (let [pattern-fn (if (keyword? pattern)
                     #(= pattern (first (get-in % [:tags :event])))
                     #(and (string? pattern)
                        (clojure.string/includes? (str (get-in % [:tags :event])) pattern)))
        limited-n (min (or n 50) 100)
        matches (->> (tracing/get-history)
                  (filter pattern-fn)
                  (take-last limited-n))]
    (->> matches
      format-traces)))

(defn slow
  "Return traces that took longer than the threshold in milliseconds (default: 20, max: 100)."
  [threshold-ms & [n]]
  (let [limited-n (min (or n 20) 100)
        slow-traces (->> (tracing/get-history)
                      (filter #(>= (:duration %) threshold-ms))
                      (take-last limited-n))]
    (->> slow-traces
      format-traces)))

(defn help
  "Return available REPL tracing commands info."
  []
  "\n=== re-frame Tracing REPL Commands ===\nAll functions return ClojureScript data structures and include size limits (max: 100).\n\n  (repl-trace/recent)           - Return recent traces (default: 20)\n  (repl-trace/recent 50)        - Return 50 most recent traces\n  (repl-trace/events)           - Return event traces only (default: 20)\n  (repl-trace/subscriptions)    - Return subscription traces only (default: 20)\n  (repl-trace/by-operation :render) - Return traces by operation type (max: 100)\n  (repl-trace/search :initialize) - Search traces by keyword (default: 50, max: 100)\n  (repl-trace/slow 100)         - Return traces >100ms (default: 20, max: 100)\n  (repl-trace/stats)            - Return buffer statistics\n  (repl-trace/clear)            - Clear trace buffer\n  (repl-trace/help)             - Return this help\n")

;; Auto-show help when namespace is loaded
(comment
  (help))
