(ns app.frontend.dev.tracing
  (:require
    [re-frame.trace :as trace]))

;; ─── Tracing Configuration ────────────────────────────────────────

(defonce ^:private history* (atom (clojure.core/vec [])))
(defonce ^:private max-items 5000)

;; ─── Buffer Management ─────────────────────────────────────────────

(defn- keep-last [n coll]
  (let [cnt (count coll)]
    (if (<= cnt n)
      (vec coll)
      (subvec (vec coll) (- cnt n) cnt))))

;; ─── Trace Processing ─────────────────────────────────────────────

(defn- slim-trace
  "Reduce trace size by removing large data that's rarely needed for debugging."
  [t]
  (cond-> t
    true (dissoc :app-db-after)        ; Often huge; keep if you really need it
    true (update :tags dissoc :app-db))) ; Tags may include big data

(defn- on-traces
  "Receives a vector of trace maps from re-frame's tracing system.
   This function filters, processes, and stores traces in our in-memory buffer."
  [traces]
  (when (seq traces)
    (swap! history*
      (fn [h]
        (keep-last max-items
          (-> h
            (into (map slim-trace traces))
            vec))))))

;; ─── Tracing Control ───────────────────────────────────────────────

(defn start!
  "Register our callback with re-frame's tracing system.
   Safe to call multiple times due to idempotent registration."
  []
  (when-not (trace/is-trace-enabled?)
    (js/console.warn "[tracing] re-frame tracing is disabled; set re-frame.trace.trace-enabled? to true in build config"))
  (trace/register-trace-cb :app/dev-tracer on-traces)
  (js/console.info "[tracing] re-frame trace callback registered"))

(defn stop!
  "Remove our callback from re-frame's tracing system."
  []
  (trace/remove-trace-cb :app/dev-tracer)
  (js/console.info "[tracing] re-frame trace callback removed"))

;; ─── Trace Inspection API ───────────────────────────────────────────

(defn get-history
  "Return the complete trace history."
  []
  @history*)

(defn get-recent
  "Return the most recent n traces (default: 50)."
  ([] (get-recent 50))
  ([n] (take-last n @history*)))

(defn get-events
  "Return only event traces (filtering out subs, reactions, etc.)."
  []
  (filter #(= (:op-type %) :event) @history*))

(defn get-subs
  "Return only subscription traces."
  []
  (filter #(= (:op-type %) :sub/create) @history*))

(defn get-by-operation
  "Return traces filtered by operation type (:event, :sub/create, :render, etc.)."
  [op-type]
  (filter #(= (:op-type %) op-type) @history*))

(defn clear-history!
  "Clear the in-memory trace buffer."
  []
  (reset! history* [])
  (js/console.info "[tracing] trace history cleared"))

(defn get-stats
  "Return basic statistics about the current trace buffer."
  []
  {:total-count (count @history*)
   :event-count (count (get-events))
   :sub-count (count (get-subs))
   :buffer-size max-items
   :buffer-utilization (/ (count @history*) max-items)})

;; ─── Auto-start (dev only) ─────────────────────────────────────────

;; Since this namespace will be preloaded in dev, auto-start tracing
(start!)

(js/console.info "[tracing] re-frame tracing system initialized"
  (clj->js {:buffer-size max-items
            :tracing-enabled true}))
