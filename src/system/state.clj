(ns system.state
  "Runtime atoms that hold the currently running dev-system and helper process refs.
  Placed in `src/` so that it is available to tests and production code as a lightweight,
  side-effect-free dependency.  The same namespace was previously defined under `dev/`.
  Keeping an identical implementation avoids duplicate state – whichever source file
  happens to be loaded first wins, but the vars are `defonce`, so they are shared."
  (:require
    [clojure.tools.namespace.repl :refer [disable-reload! disable-unload!]]))

(disable-unload!)
(disable-reload!)

(defonce instance (atom (future ::never-run)))
(defonce state    (atom nil))
(defonce postcss-watcher  (atom nil))
(defonce backend-watcher (atom nil))

(defonce models-watcher (atom nil))

(defn clear-state! []
  (reset! instance (future ::never-run))
  (reset! state nil)
  (reset! postcss-watcher nil)
  (reset! backend-watcher nil)
  (reset! models-watcher nil))
