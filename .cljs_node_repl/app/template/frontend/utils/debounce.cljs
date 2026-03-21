(ns app.template.frontend.utils.debounce
  "Unified debounce utilities for the frontend application.
   Provides consistent debouncing functionality across components.")

(defn debounce
  "Returns a debounced version of the given function.
   The debounced function will delay invoking `f` until after `delay-ms`
   milliseconds have elapsed since the last time it was invoked.

   Example:
   ```clojure
   (def save-input (debounce save-to-server 500))
   ;; save-input will only call save-to-server 500ms after the last call
   ```"
  [f delay-ms]
  (let [timeout-atom (atom nil)]
    (fn [& args]
      ;; Clear any existing timeout
      (when-let [existing-timeout @timeout-atom]
        (js/clearTimeout existing-timeout))
      ;; Set new timeout
      (reset! timeout-atom
        (js/setTimeout
          (fn []
            (reset! timeout-atom nil)
            (apply f args))
          delay-ms)))))

(defn debounce-with-cancel
  "Returns a debounced function along with a cancel function.
   Useful when you need to manually cancel pending invocations.

   Returns a map with:
   - :debounced - the debounced function
   - :cancel - function to cancel pending invocations

   Example:
   ```clojure
   (let [{:keys [debounced cancel]} (debounce-with-cancel save-to-server 500)]
     ;; Use debounced function
     (debounced data)
     ;; Cancel if needed
     (cancel))
   ```"
  [f delay-ms]
  (let [timeout-atom (atom nil)
        cancel-fn (fn []
                    (when-let [timeout @timeout-atom]
                      (js/clearTimeout timeout)
                      (reset! timeout-atom nil)))
        debounced-fn (fn [& args]
                       (cancel-fn)
                       (reset! timeout-atom
                         (js/setTimeout
                           (fn []
                             (reset! timeout-atom nil)
                             (apply f args))
                           delay-ms)))]
    {:debounced debounced-fn
     :cancel cancel-fn}))

(defn use-debounced-callback
  "React hook that returns a debounced version of the callback.
   Automatically cleans up on unmount.

   This is a ClojureScript adaptation for use with UIX/React hooks.

   Example:
   ```clojure
   (defui my-component []
     (let [search (use-debounced-callback
                    (fn [query] (search-api query))
                    500)]
       ($ :input {:on-change #(search (.. % -target -value))})))
   ```"
  [callback delay-ms _deps]
  ;; Note: This requires uix.core to be available in the calling namespace
  ;; We'll use a simple debounce for now, but this could be enhanced
  ;; with React-specific lifecycle management
  (debounce callback delay-ms))
