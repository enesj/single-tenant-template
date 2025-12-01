(ns fork.core
  (:require
   [clojure.data :as data]))

(defn- vec-remove
  [coll pos]
  (vec (concat (subvec coll 0 pos) (subvec coll (inc pos)))))

(defn vec-insert-at
  [coll pos element]
  (vec (concat (subvec coll 0 pos) [element] (subvec coll pos))))

(defn touched
  [state k]
  (or (:attempted-submissions state)
    (get (:touched state) k)))

(defn initialize-state
  [{:keys [state keywordize-keys initial-values initial-touched]}]
  (let [values (if (and initial-values (map? initial-values))
                 initial-values
                 {})
        initialized-state {:keywordize-keys keywordize-keys
                           :initial-values values
                           :initial-touched (or initial-touched {})
                           :values values
                           :touched (into #{} (keys initial-touched))}]
    initialized-state))

(defn element-value
  [evt]
  (let [type (-> evt .-target .-type)]
    (case type
      "checkbox" (-> evt .-target .-checked)
      "radio" (-> evt .-target .-value)
      "select-multiple" (let [select-el (-> evt .-target)
                              options (array-seq (.-options select-el))]
                          (->> options
                            (filter #(.-selected %))
                            (map #(.-value %))
                            (vec)))
      "file" (let [files (-> evt .-target .-files)]
               (when (pos? (.-length files))
                 (array-seq files)))
      (-> evt .-target .-value))))

(defn element-name
  [t v keywordize?]
  (let [el-name (case t
                  :evt (-> v .-target (.getAttribute "name"))
                  :node (-> v (.getAttribute "name"))
                  v)]
    (if keywordize? (keyword el-name) el-name)))

(defn normalize-name
  [k {:keys [keywordize-keys]}]
  (if (and keywordize-keys (keyword? k))
    (subs (str k) 1)
    k))

(defn set-values
  [[state set-state] new-values]
  (set-state (fn [s] (-> s
                       (update :values merge new-values)
                       (update :touched into (keys new-values))))))

(defn vectorize-path
  [path]
  (if (vector? path) path [path]))

(defn set-submitting
  [db path bool]
  (assoc-in db (concat (vectorize-path path) [:submitting?]) bool))

(defn set-waiting
  [db path input-name bool]
  (assoc-in db (concat (vectorize-path path) [:server input-name :waiting?]) bool))

(defn set-server-message
  [db path message]
  (assoc-in db (concat (vectorize-path path) [:server-message]) message))

(defn set-error
  [db path input-name message]
  (assoc-in db (concat (vectorize-path path) [:server input-name :errors]) message))

(defn resolve-server-validation
  [m]
  (not-empty
    (into {}
      (keep (fn [[k v]]
              (when-let [err (:errors v)]
                {k err}))
        m))))

(defn config-set-waiting?
  [config]
  (let [x (get config :set-waiting? :no-key)]
    (if (= :no-key x) true x)))

(defn set-touched
  [[state set-state] names]
  (set-state (fn [s] (update s :touched (fn [x] (apply conj x names))))))

(defn set-untouched
  [[state set-state] names]
  (set-state (fn [s] (update s :touched (fn [x] (apply disj x names))))))

(defn disable-logic
  [current-set ks]
  (apply conj ((fnil into #{}) current-set) ks))

(defn enable-logic
  [current-set ks]
  (apply disj current-set ks))

(defn disabled?
  [state k]
  (get (:disabled? state) k))

(defn handle-validation
  [state validation]
  (let [resolved (validation state)]
    (when-not (every? empty? resolved) resolved)))

(defn handle-change
  [[state set-state] evt]
  (let [input-key (element-name :evt evt (:keywordize-keys state))
        input-value (element-value evt)]
    (set-state (fn [s] (-> s
                         (update :values assoc input-key input-value)
                         (update :touched conj input-key))))))

(defn handle-blur
  [[state set-state] evt]
  (let [input-key (element-name :evt evt (:keywordize-keys state))]
    (set-state (fn [s] (update s :touched conj input-key)))))

(defn handle-focus
  [[state set-state] evt]
  (let [input-key (element-name :evt evt (:keywordize-keys state))]
    (set-state (fn [s] (assoc-in s [:focused input-key] true)))))

(defn fieldarray-handle-change
  [[state set-state] evt vec-field-array-key idx]
  (let [path (conj vec-field-array-key idx)
        input-key (element-name :evt evt (:keywordize-keys state))
        input-value (element-value evt)]
    (set-state (fn [s] (assoc-in s (cons :values path) (assoc (get-in s (cons :values path)) input-key input-value))))))

(defn fieldarray-handle-blur
  [[state set-state] evt vec-field-array-key idx]
  (let [input-key (element-name :evt evt (:keywordize-keys state))
        path (conj vec-field-array-key idx input-key)]
    (set-state (fn [s] (assoc-in s [:touched path] true)))))

(defn- set-handle-change-one
  [deref-state {:keys [value path]}]
  (let [path (vectorize-path path)
        current-value (get-in deref-state (cons :values path))
        new-value (if (fn? value) (value current-value) value)
        resolved-new-value (if (seq? new-value)
                             (doall new-value)
                             new-value)]
    ;;(tap> [path "set-handle-change-one" path resolved-new-value])

    (assoc-in deref-state (cons :values path) resolved-new-value)))

(defn set-handle-change
  [[state set-state] params]
  (cond
    (map? params)
    (set-state (fn [s] (set-handle-change-one s params)))

    (sequential? params)
    (set-state (fn [s] (->> (remove nil? params)
                         (reduce
                           (fn [acc item]
                             (set-handle-change-one acc item))
                           s))))

    :else (js/console.error "set-handle-change was called with the wrong
    params. Provide either a map or a sequential collection")))

(defn set-handle-blur
  [[state set-state] {:keys [value path]}]
  (let [path (vectorize-path path)]
    (set-state (fn [s] (assoc-in s [:touched path] (if value true false))))))

(defn fieldarray-insert
  [[state set-state] vec-field-array-key m]
  (set-state (fn [s] (update-in s (cons :values vec-field-array-key) (fnil conj []) m))))

(defn- fieldarray-update-touched
  [touched path idx]
  (let [path-count (count path)]
    (->> touched
      (keep (fn [touched-el]
              (if ;; to filter out only the relevant fieldarray groups
               (and (vector? touched-el)
                 (= path (vec (take path-count touched-el))))
                (let [[position curr-idx] (->> touched-el
                                            (map-indexed #(when (number? %2) [%1 %2]))
                                            (remove nil?)
                                            (last))]
                  (cond
                    (> curr-idx idx) (update touched-el position dec)
                    (< curr-idx idx) touched-el
                       ;; remove field array group being deleted
                    :else nil))
                touched-el)))
      (into #{}))))

(defn fieldarray-remove
  [[state set-state] vec-field-array-key idx]
  (set-state (fn [s] (-> s
                       (update-in (cons :values vec-field-array-key) vec-remove idx)
                       (update :touched #(fieldarray-update-touched % vec-field-array-key idx))))))

(defn dirty
  [values initial-values]
  (first (data/diff values (or initial-values {}))))

(defn handle-submit
  [[state set-state] {:keys [server on-submit prevent-default?
                             path validation already-submitting? reset]} evt]
  (when prevent-default? (.preventDefault evt))
  (set-state (fn [s] (assoc s :attempted-submissions (inc (:attempted-submissions s)))))
  (when (and (not already-submitting?)
          (nil? validation)
          (nil? (resolve-server-validation server))
          (every? #(false? (:waiting? %)) (vals server)))
    (set-state (fn [s] (assoc s :successful-submissions (inc (:successful-submissions s)))))
    (on-submit
      {:state state
       :path path
       :values (:values state)
       :dirty (dirty (:values state) (merge (:initial-values state)
                                       (:touched-values state)))
       :reset reset})))

(defn send-server-request
  [http-fn
   [[state set-state] {:keys [validation evt name value path
                              server-dispatch-logic debounce throttle]}]]
  (let [input-key name
        input-value value
        values (merge
                 (:values state)
                 (when input-value
                   {input-key input-value}))
        touched (if (:on-blur evt)
                  (conj (:touched state) input-key)
                  (:touched state))
        props {:path path
               :dirty (dirty values (merge (:initial-values state)
                                      (:touched-values state)))
               :errors (when validation
                         ;;(println "handle-validation" values)
                         (handle-validation {:values values}
                           validation))
               :values values
               :touched touched
               :state state}]
    (server-dispatch-logic)
    (cond
      debounce (do
                 (js/clearTimeout (get-in state [:debounce input-key]))
                 (set-state (fn [s] (assoc-in s [:debounce input-key]
                                              (js/setTimeout
                                                #(http-fn props) debounce)))))
      throttle (when (not (get-in state [:throttle input-key]))
                 (set-state (fn [s] (assoc-in s [:throttle input-key]
                                              (js/setTimeout
                                                #(do
                                                   (http-fn props)
                                                   (set-state (fn [s] (dissoc s [:throttle input-key]))))
                                                throttle)))))
      :else
      (http-fn props))))

(defn fieldarray-touched
  [[state _] vec-field-array-key idx input-key]
  (or (:attempted-submissions state)
    (get (:touched state) (conj vec-field-array-key idx input-key))))

(defn handle-drag-start
  [[state set-state] k idx]
  (set-state (fn [s] (-> s
                       (dissoc :drag-and-drop)
                       (assoc-in [:drag-and-drop k :idx-of-item-being-dragged] idx)))))

(defn handle-drag-end
  [[state set-state]]
  (set-state (fn [s] (dissoc s :drag-and-drop))))

(defn handle-drag-over [e] (.preventDefault e))

(defn handle-drag-enter
  [[state set-state] k idx]
  (set-state (fn [s] (assoc-in s [:drag-and-drop k :idx-of-element-droppable-location] idx))))

(defn handle-drop
  [[state set-state] k vec-field-array-key]
  (let [dragged-idx (get-in state [:drag-and-drop k :idx-of-item-being-dragged])
        dropped-idx (get-in state [:drag-and-drop k :idx-of-element-droppable-location])]
    (set-state (fn [s] (-> s
                         (update-in (cons :values vec-field-array-key)
                           #(-> %
                              (vec-remove dragged-idx)
                              (vec-insert-at dropped-idx (get % dragged-idx))))
                         (dissoc :drag-and-drop))))))

(defn current-target-idx
  [[state _] k]
  (some-> state :drag-and-drop k :idx-of-element-droppable-location))

(defn current-dragged-idx
  [[state _] k]
  (some-> state :drag-and-drop k :idx-of-item-being-dragged))

(defn field-array
  [props _]
  (let [[state set-state] (get-in props [:props :state])
        field-array-key (:name props)
        vec-field-array-key (vectorize-path field-array-key)
        handlers {:set-handle-change
                  #(set-handle-change [state set-state] %)
                  :set-handle-blur
                  #(set-handle-blur [state set-state] %)
                  :handle-change
                  (fn [evt idx] (fieldarray-handle-change [state set-state] evt vec-field-array-key idx))
                  :handle-blur
                  (fn [evt idx] (fieldarray-handle-blur [state set-state] evt vec-field-array-key idx))
                  :remove
                  (fn [idx] (fieldarray-remove [state set-state] vec-field-array-key idx))
                  :insert
                  (fn [m] (fieldarray-insert [state set-state] vec-field-array-key m))
                  :touched
                  (fn [idx input-key] (fieldarray-touched [state set-state] vec-field-array-key idx input-key))
                  :current-target-idx
                  (fn [k] (current-target-idx [state set-state] k))
                  :current-dragged-idx
                  (fn [k] (current-dragged-idx [state set-state] k))
                  :next-droppable-target?
                  (fn [k idx]
                    (and (= idx (current-target-idx [state set-state] k))
                      (> idx (current-dragged-idx [state set-state] k))))
                  :prev-droppable-target?
                  (fn [k idx]
                    (and (= idx (current-target-idx [state set-state] k))
                      (< idx (current-dragged-idx [state set-state] k))))
                  :drag-and-drop-handlers
                  (fn [k idx]
                    (when (or (nil? (:drag-and-drop state))
                            (current-dragged-idx [state set-state] k))
                      {:draggable true
                       :on-drag-start
                       (fn [_] (handle-drag-start [state set-state] k idx))
                       :on-drag-end
                       (fn [_] (handle-drag-end [state set-state]))
                       :on-drag-over
                       (fn [evt] (handle-drag-over evt))
                       :on-drag-enter
                       (fn [_] (handle-drag-enter [state set-state] k idx))
                       :on-drop
                       (fn [_] (handle-drop [state set-state] k vec-field-array-key))}))}]
    (fn [{:keys [props] :as args} component]
      (let [fields (get-in (:values props) vec-field-array-key)]
        [component props
         {:fieldarray/name field-array-key
          :fieldarray/options (:options args)
          :fieldarray/fields fields
          :fieldarray/touched (:touched handlers)
          :fieldarray/insert (:insert handlers)
          :fieldarray/remove (:remove handlers)
          :fieldarray/set-handle-change (:set-handle-change handlers)
          :fieldarray/set-handle-blur (:set-handle-blur handlers)
          :fieldarray/handle-change (:handle-change handlers)
          :fieldarray/handle-blur (:handle-blur handlers)
          :fieldarray/current-target-idx (:current-target-idx handlers)
          :fieldarray/current-dragged-idx (:current-dragged-idx handlers)
          :fieldarray/next-droppable-target? (:next-droppable-target? handlers)
          :fieldarray/prev-droppable-target? (:prev-droppable-target? handlers)
          :fieldarray/drag-and-drop-handlers (:drag-and-drop-handlers handlers)}]))))
