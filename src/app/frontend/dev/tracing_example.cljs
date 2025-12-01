(ns app.frontend.dev.tracing-example
  "Example usage of the re-frame tracing system.

   This namespace demonstrates common patterns for using the tracing
   tools to debug re-frame applications. Copy and adapt these examples
   for your own debugging needs."
  (:require [app.frontend.dev.tracing :as tracing]
    [app.frontend.dev.repl-tracing :as repl-trace]))

;; ─── Example: Manual Trace Inspection ────────────────────────────────

(defn inspect-recent-events
  "Example: Get the 10 most recent event traces for manual inspection.
   Updated to include start timestamp for time-based sorting."
  []
  (->> (tracing/get-events)
    (take-last 10)
    (map (fn [trace]
           {:event (get-in trace [:tags :event])
            :op-type (:op-type trace)
            :start (:start trace)
            :duration-ms (:duration trace)}))
    vec))

;; ─── Example: Performance Analysis ──────────────────────────────────

(defn find-slow-events
  "Example: Find events that took longer than expected."
  [threshold-ms]
  (->> (tracing/get-history)
    (filter #(= (:op-type %) :event))
    (filter #(>= (:duration %) threshold-ms))
    (sort-by :duration >)
    (take 10)
    (map (fn [trace]
           {:event (get-in trace [:tags :event])
            :op-type (:op-type trace)
            :start (:start trace)
            :duration-ms (:duration trace)}))
    vec))

(defn event-frequency-analysis
  "Example: Analyze which events are called most frequently."
  []
  (->> (tracing/get-events)
    (map #(get-in % [:tags :event]))
    (frequencies)
    (sort-by val >)
    (take 10)))

;; ─── Example: Debugging Specific Issues ─────────────────────────────

(defn find-event-by-keyword
  "Example: Find all traces for a specific event keyword."
  [event-keyword]
  (->> (tracing/get-history)
    (filter #(= (first (get-in % [:tags :event])) event-keyword))))

(defn trace-subscription-lifecycle
  "Example: Follow the lifecycle of a specific subscription."
  [subscription-query]
  (->> (tracing/get-history)
    (filter #(= (:op-type %) :sub/create))
    (filter #(= (get-in % [:tags :query-v]) subscription-query))))

;; ─── Example: Custom Filtering and Analysis ────────────────────────

(defn get-traces-in-time-range
  "Example: Get traces that occurred within a specific time range."
  [start-ms end-ms]
  (->> (tracing/get-history)
    (filter #(and (>= (:start %) start-ms)
               (<= (:end %) end-ms)))))

(defn analyze-trace-patterns
  "Example: Look for patterns in trace data that might indicate issues."
  []
  (let [all-traces (tracing/get-history)]
    {:total-traces (count all-traces)
     :slow-events (count (filter #(and (= (:op-type %) :event)
                                    (> (:duration %) 100)) all-traces))
     :rapid-succession (count (filter #(and (= (:op-type %) :event)
                                         (< (:duration %) 1)) all-traces))
     :large-app-db-snapshots (count (filter :app-db-after all-traces))}))

;; ─── REPL Usage Examples ───────────────────────────────────────────

(comment
  ;; Basic inspection
  (repl-trace/recent)           ; Show recent traces
  (repl-trace/events)           ; Show only events
  (repl-trace/stats)            ; Show buffer statistics

  ;; Search and filter
  (repl-trace/search :initialize)  ; Find :initialize events
  (repl-trace/slow 50)             ; Find traces >50ms
  (repl-trace/by-operation :sub/create) ; Show subscription creation

  ;; Custom analysis
  (find-slow-events 100)         ; Find events slower than 100ms
  (event-frequency-analysis)     ; Most frequent events
  (analyze-trace-patterns)       ; Look for issues

  ;; Buffer management
  (repl-trace/clear)             ; Clear the buffer
  (tracing/get-stats))            ; Get detailed stats
