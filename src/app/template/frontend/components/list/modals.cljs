(ns app.template.frontend.components.list.modals
  (:require
    [app.shared.keywords :as kw]
    [app.template.frontend.components.form :refer [form]]
    [app.template.frontend.components.modal-wrapper :refer [modal-wrapper]]
    [app.template.frontend.events.form :as form-events]
    [app.template.frontend.utils.id :as id-utils]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$]]))

(defn- kebab->snake [s] (str/replace s #"-" "_"))
(defn- snake->kebab [s] (str/replace s #"_" "-"))

(defn- key-variants
  "Given a simple keyword, return both its snake_case and kebab-case keyword forms.
   Field-spec ids authored in either style will then resolve into :values."
  [k]
  (if (keyword? k)
    (let [n (name k)]
      (cond-> #{k}
        (re-find #"-" n) (conj (keyword (kebab->snake n)))
        (re-find #"_" n) (conj (keyword (snake->kebab n)))))
    #{k}))

(defn- normalize-edit-initial-values
  "Strip namespaces from item keys and expose every value under BOTH snake_case
   and kebab-case keys so form field lookups succeed regardless of the
   convention used in the field-spec :id."
  [item]
  (reduce-kv
    (fn [acc k v]
      (let [simple-key (if (and (keyword? k) (namespace k))
                         (keyword (name k))
                         k)]
        (reduce (fn [m variant] (assoc m variant v))
          acc
          (key-variants simple-key))))
    {}
    item))

(defn render-add-modal
  [{:keys [add-modal-open?
           add-modal-title
           entity-name
           entity-kw
           entity-spec
           form-entity-spec
           has-custom-add-form?
           render-add-form
           handle-add-modal-close
           handle-add-modal-success]}]
  (when add-modal-open?
    ($ modal-wrapper
      {:visible? true
       :title add-modal-title
       :size :large
       :on-close handle-add-modal-close
       :close-button-id (str "btn-close-add-modal-" (kw/ensure-name entity-name))}
      (if has-custom-add-form?
        (render-add-form {:entity-name entity-name
                          :entity-spec entity-spec
                          :on-success handle-add-modal-success
                          :on-cancel handle-add-modal-close})
        (let [effective-form-spec (or form-entity-spec
                                    (when (vector? entity-spec) entity-spec)
                                    (when (map? entity-spec)
                                      (let [fields (:fields entity-spec)]
                                        (cond
                                          (vector? fields) fields
                                          (sequential? fields) (vec fields)
                                          :else nil)))
                                    [])
              default-values (reduce (fn [acc field-spec]
                                       (if-let [default-value (:default-value field-spec)]
                                         (assoc acc (keyword (:id field-spec)) default-value)
                                         acc))
                               {}
                               effective-form-spec)]
          ($ form
            {:key (str "modal-add-" (kw/ensure-name entity-name))
             :entity-name entity-kw
             :entity-spec effective-form-spec
             :editing false
             :initial-values default-values
             :on-cancel handle-add-modal-close}))))))

(defn render-edit-modal
  [{:keys [edit-modal-open?
           edit-modal-item
           edit-modal-title
           entity-name
           entity-kw
           entity-spec
           form-entity-spec
           form-entity-spec-edit
           has-custom-edit-form?
           render-edit-form
           handle-edit-modal-close
           handle-edit-modal-success]}]
  (when (and edit-modal-open? edit-modal-item)
    (let [item-clj (if (map? edit-modal-item)
                     edit-modal-item
                     (js->clj edit-modal-item :keywordize-keys true))
          item-id (id-utils/extract-entity-id item-clj)
          initial-values (normalize-edit-initial-values item-clj)
          effective-form-spec (or form-entity-spec-edit
                                form-entity-spec
                                entity-spec)
          handle-default-submit (fn [{:keys [dirty values] :as payload}]
                                  (let [changed-values (-> values
                                                         (select-keys (cons :id (keys dirty))))]
                                    (rf/dispatch [::form-events/submit-form
                                                  (assoc payload
                                                    :values changed-values
                                                    :entity-name entity-kw
                                                    :editing true)])
                                    (rf/dispatch [::form-events/set-submitted entity-kw true])))]
      ($ modal-wrapper
        {:visible? true
         :title edit-modal-title
         :size :large
         :draggable? true
         :on-close handle-edit-modal-close
         :close-button-id (str "btn-close-edit-modal-" (kw/ensure-name entity-name))}
        (if has-custom-edit-form?
          (render-edit-form item-clj
            {:entity-name entity-name
             :entity-spec entity-spec
             :on-success handle-edit-modal-success
             :on-cancel handle-edit-modal-close})
          ($ form
            {:key (str "modal-edit-" (kw/ensure-name entity-name) "-" (or item-id "unknown"))
             :entity-name entity-kw
             :entity-spec effective-form-spec
             :editing true
             :initial-values initial-values
             :on-cancel handle-edit-modal-close
             :on-submit handle-default-submit}))))))