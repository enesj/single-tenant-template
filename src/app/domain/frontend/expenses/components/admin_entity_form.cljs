(ns app.domain.frontend.expenses.components.admin-entity-form
  "Reusable admin modal form wrapper for simple expenses entities."
  (:require
    [app.template.frontend.components.form :refer [form]]
    [app.template.frontend.subs.form :as form-subs]
    [uix.core :refer [$ defui use-effect use-memo use-state]]
    [uix.re-frame :refer [use-subscribe]]))

(defn- normalize-field-specs
  [spec]
  (cond
    (sequential? spec) spec
    (map? spec) (->> (vals spec)
                  (filter map?)
                  (filter #(contains? % :id)))
    :else []))

(defn- default-values-from-spec
  [spec]
  (reduce
    (fn [acc field]
      (let [default-value (if (contains? field :default-value)
                            (:default-value field)
                            (:default field))]
        (if (some? default-value)
          (assoc acc (keyword (:id field)) default-value)
          acc)))
    {}
    (normalize-field-specs spec)))

^{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}
(defui entity-form-modal
  [{:keys [entity-name entity-spec editing? initial-values on-success on-cancel button-text]}]
  (let [entity-key (if (keyword? entity-name) entity-name (keyword entity-name))
        form-entity-spec (use-subscribe [:form-entity-specs/by-name entity-key])
        effective-spec (or form-entity-spec entity-spec)
        default-values (use-memo #(default-values-from-spec effective-spec) [effective-spec])
        initial-values* (merge default-values (or initial-values {}))
        form-success? (use-subscribe [::form-subs/form-success entity-key])
        submitted? (use-subscribe [::form-subs/submitted? entity-key])
        [callback-fired? set-callback-fired!] (use-state false)]

    (use-effect
      (fn []
        (when (and form-success? submitted? (not callback-fired?))
          (set-callback-fired! true)
          (when (fn? on-success)
            (on-success)))
        (when (and (not submitted?) callback-fired?)
          (set-callback-fired! false))
        js/undefined)
      [form-success? submitted? callback-fired? on-success])

    ($ form {:entity-name entity-key
             :entity-spec effective-spec
             :editing (true? editing?)
             :initial-values initial-values*
             :on-cancel on-cancel
             :button-text button-text})))
