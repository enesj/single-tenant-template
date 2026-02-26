(ns app.template.frontend.components.batch-edit.hooks
  (:require
    [app.template.frontend.components.batch-edit.utils :as utils]
    [uix.core :refer [use-effect use-state]]))

(defn use-initial-values
  "Hook to manage initial values calculation and updates"
  [items entity-spec & [entity-name]]
  (let [[initial-values set-initial-values] (use-state
                                              (if (and (seq items) (seq entity-spec))
                                                (utils/find-identical-values items entity-spec entity-name)
                                                {}))
        [original-values _set-original-values] (use-state initial-values)]

    ;; Effect to update initial values when items or entity spec changes
    (use-effect
      (fn []
        (when (and (seq items) (seq entity-spec))
          (set-initial-values (utils/find-identical-values items entity-spec entity-name)))
        ;; No cleanup function needed
        (fn [] nil))
      [items entity-spec entity-name])

    {:initial-values initial-values
     :set-initial-values set-initial-values
     :original-values original-values}))

(defn use-batch-form-state
  "Hook to manage batch form state including values and dirty tracking"
  [initial-values]
  (let [[form-values set-form-values] (use-state initial-values)
        [dirty-fields set-dirty-fields] (use-state #{})]

    {:form-values form-values
     :set-form-values set-form-values
     :dirty-fields dirty-fields
     :set-dirty-fields set-dirty-fields}))


