(ns fork.re-frame
  (:require
   [fork.core :as core]
   [re-frame.core :as rf]
   [taoensso.timbre :as log]
   [uix.core :refer [$ defui use-callback use-effect use-state]]
   [uix.re-frame :as urf]))

(rf/reg-event-db
  ::init-form
  (fn [db [_ path]]
    (if (get-in db path)
      db
      (assoc-in db path {}))))

(defn set-waiting
  [db path input-name bool]
  (core/set-waiting db path input-name bool))

(defn set-submitting
  [state path bool]
  (core/set-submitting state path bool))

(defn set-server-message
  [db path message]
  (core/set-server-message db path message))

(defn set-error
  [state path input-name message]
  (core/set-error state path input-name message))

(defn retrieve-event-value
  [evt]
  (core/element-value evt))

(rf/reg-event-db
  ::server-dispatch-logic
  (fn [db [config path]]
    (let [set-waiting? (core/config-set-waiting? config)
          input-names (:clean-on-refetch config)]
      (cond-> db
        (seq input-names) (update-in path (fn [m] (apply update m :server dissoc input-names)))
        set-waiting? (assoc-in (concat path [:server (:name config) :waiting?]) true)))))

(rf/reg-event-db
  ::server-set-waiting
  (fn [db [path input-key bool]]
    (assoc-in db (concat path [:server input-key :waiting?]) bool)))

(rf/reg-sub
  ::db
  (fn [db [_ path]]
    (get-in db path)))

(rf/reg-event-db
  ::clean
  (fn [db [_ path]]
    (if (= 1 (count path))
      (dissoc db (first path))
      (update-in db (butlast path) dissoc (last path)))))

(defn field-array
  [props component]
  [core/field-array props component])

(defui form-component [{:keys [component-props children] :as props}]
  (children component-props))

(defui form
  [{:keys [children initial-values] :as props}]
  (let [[state set-state] (use-state (core/initialize-state {:props props
                                                             :initial-values initial-values}))
        p (:path props)
        path (cond
               (and p (vector? p)) p
               (keyword? p) (vector p)
               :else [::global])
        form-id (or (:form-id props) (str (gensym)))
        db (urf/use-subscribe [::db path])

        touched-fn (use-callback #(get (:touched state) %) [state])
        set-touched-fn (use-callback #(core/set-touched [state set-state] %1) [state])
        set-untouched-fn (use-callback #(core/set-untouched [state set-state] %1) [state])
        set-values-fn (use-callback #(core/set-values [state set-state] %1) [state])
        disable-fn (use-callback #(set-state (fn [s] (core/disable-logic s %1))) [])
        enable-fn (use-callback #(set-state (fn [s] (core/enable-logic s %1))) [])
        disabled-fn (use-callback #(core/disabled? state %) [state])
        normalize-name-fn (use-callback #(core/normalize-name % {:props props}) [props])
        set-handle-change-fn (use-callback
                               (fn [params]
                                 (core/set-handle-change [state set-state] params))
                               [state])
        set-handle-blur-fn (use-callback
                             (fn [params]
                               (core/set-handle-blur [state set-state] params))
                             [state])
        handle-change-fn (use-callback
                           (fn [e]
                             (core/handle-change [state set-state] e))
                           [state])
        handle-blur-fn (use-callback #(core/handle-blur [state] %1) [state])
        send-server-request-fn (use-callback
                                 (fn [config callback]
                                   (core/send-server-request
                                     callback (merge config
                                                {:props props}
                                                {:path path
                                                 :state [state set-state]
                                                 :server-dispatch-logic
                                                 #(rf/dispatch [::server-dispatch-logic config path])})))
                                 [props path state])
        reset-fn (use-callback
                   (fn [& [m]]
                     (set-state (fn [_]
                                  (merge
                                    (when (:keywordize-keys props)
                                      {:keywordize-keys true}
                                      {:values {} :touched #{}}
                                      m))))
                     (rf/dispatch [::clean path]))
                   [props path])

        handlers {:touched touched-fn
                  :set-touched set-touched-fn
                  :set-untouched set-untouched-fn
                  :set-values set-values-fn
                  :disable disable-fn
                  :enable enable-fn
                  :disabled? disabled-fn
                  :normalize-name normalize-name-fn
                  :set-handle-change set-handle-change-fn
                  :set-handle-blur set-handle-blur-fn
                  :handle-change handle-change-fn
                  :handle-blur handle-blur-fn
                  :send-server-request send-server-request-fn
                  :reset reset-fn}

        ;; Create a ref to track if component-did-mount has been called
        init-done (use-state false)
        [has-initialized, set-initialized] init-done]

    ;; Effect to run component-did-mount only once
    (use-effect
      (fn []
        ;; Initialize the form

;; Only run component-did-mount if not already initialized
        (when (and (not has-initialized) (:component-did-mount props))
          ;; Mark as initialized immediately to prevent duplicate calls
          (rf/dispatch [::init-form path])
          (set-initialized true)
          ;; Call the component-did-mount function with the handlers
          ((:component-did-mount props) handlers))

        ;; Cleanup on unmount
        (fn []
          (when (:clean-on-unmount? props)
            (rf/dispatch [::clean path]))))

      ;; Include all dependencies to satisfy React's rules
      ;; The has-initialized guard ensures component-did-mount is still only called once
      [path props has-initialized set-initialized handlers])

    (if-not db
      ($ :div {} "Loading...")
      (let [validation (when-let [val-fn (:validation props)]
                         (core/handle-validation state val-fn))
            server-validation (core/resolve-server-validation (:server db))
            on-submit-server-message (:server-message db)
            component-props {:props props
                             :state [state set-state]
                             :db db
                             :path path
                             :form-id form-id
                             :values (:values state)
                             :dirty (core/dirty (:values state) (merge (:initial-values state)
                                                                  (:touched-values state)))
                             :errors validation
                             :server-errors server-validation
                             :on-submit-server-message on-submit-server-message
                             :touched (:touched handlers)
                             :set-touched (:set-touched handlers)
                             :set-untouched (:set-untouched handlers)
                             :submitting? (:submitting? db)
                             :attempted-submissions (or (:attempted-submissions state) 0)
                             :successful-submissions (or (:successful-submissions state) 0)
                             :set-values (:set-values handlers)
                             :disable (:disable handlers)
                             :enable (:enable handlers)
                             :disabled? (:disabled? handlers)
                             :normalize-name (:normalize-name handlers)
                             :set-handle-change (:set-handle-change handlers)
                             :set-handle-blur (:set-handle-blur handlers)
                             :handle-change (:handle-change handlers)
                             :handle-blur (:handle-blur handlers)
                             :send-server-request (:send-server-request handlers)
                             :reset (:reset handlers)
                             :handle-submit (fn [e]
                                              (core/handle-submit
                                                [state set-state]
                                                {:server (:server db)
                                                 :on-submit (:on-submit props)
                                                 :prevent-default? true
                                                 :path path
                                                 :validation validation
                                                 :already-submitting? (:submitting? db)
                                                 :reset (:reset handlers)}
                                                e))}]
        ($ form-component {:component-props component-props} children)))))
