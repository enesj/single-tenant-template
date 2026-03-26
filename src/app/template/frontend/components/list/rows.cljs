(ns app.template.frontend.components.list.rows
  "Row rendering components for list views.

   This module uses reactive cell components from the cells module for selection
   and actions."
  (:require
    [app.template.frontend.components.filter.helpers :as filter-helpers]
    [app.template.frontend.utils.id :as id-utils]
    [app.shared.keywords :as kw]
    [app.shared.model-naming :as model-naming]
    [app.template.frontend.components.form :refer [form]]
    [app.template.frontend.components.list.cells :as cells]
    [app.template.frontend.components.list.fields :refer [get-field-display-value]]
    [app.template.frontend.events.form :as form-events]
    [app.template.frontend.events.list.crud :as crud-events]
    [app.template.frontend.events.list.filters :as filter-events]
    [app.template.frontend.utils.column-config :as column-config]
    [app.template.frontend.utils.timestamp :as timestamp]
    [re-frame.core :as rf]
    [uix.core :as uix :refer [$]]))

;; =============================================================================
;; Row Content Generation
;; =============================================================================

(defn- normalize-column-id
  [col]
  (model-naming/ensure-app-keyword (kw/ensure-name col)))

(defn- normalize-filterable-fields
  [filterable-fields]
  (cond
    (map? filterable-fields)
    (into {}
      (keep (fn [[k v]]
              (when-let [normalized-id (normalize-column-id k)]
                [normalized-id v])))
      filterable-fields)

    (coll? filterable-fields)
    (let [normalized-ids (into [] (keep normalize-column-id) filterable-fields)]
      (when (seq normalized-ids)
        (set normalized-ids)))

    :else nil))

(defn- row-content
  "Generates the content for a table row."
  [{:keys [entity-spec
           item
           actions
           visible-columns
           entity-name
           selected-ids
           on-select-change
           column-order
           show-filtering?
           filterable-fields
           user-filterable-settings]}]

  (let [entity-fields (cond
                        (and (map? entity-spec) (:fields entity-spec))
                        (let [fields (:fields entity-spec)]
                          (if (sequential? fields)
                            fields
                            []))

                        (map? entity-spec)
                        (->> (vals entity-spec)
                          (filter map?)
                          (filter #(contains? % :id))
                          (sort-by #(or (get-in % [:admin :display-order]) 999)))

                        (sequential? entity-spec) entity-spec
                        :else [])

        ordered-entity-fields (column-config/order-fields entity-fields column-order)
        normalized-user-filterable-settings
        (into {}
          (keep (fn [[k v]]
                  (when-let [normalized-id (normalize-column-id k)]
                    [normalized-id v])))
          (or user-filterable-settings {}))
        normalized-filterable-fields (normalize-filterable-fields filterable-fields)
        field-filterable?
        (fn [field-id]
          (let [normalized-field-id (normalize-column-id field-id)
                user-setting (get normalized-user-filterable-settings normalized-field-id ::not-found)]
            (if (not= user-setting ::not-found)
              user-setting
              (cond
                (map? normalized-filterable-fields)
                (boolean (get normalized-filterable-fields normalized-field-id true))

                (set? normalized-filterable-fields)
                (contains? normalized-filterable-fields normalized-field-id)

                :else true))))
        field-specs-by-id (into {}
                            (map (fn [field]
                                   [(normalize-column-id (:id field)) field]))
                            (filter map? ordered-entity-fields))
        timestamp-field-ids (into #{}
                              (keep (comp normalize-column-id :id))
                              (filter map? ordered-entity-fields))
        filtered-entity-fields (remove (fn [field]
                                         (contains? #{:created-at :updated-at}
                                           (normalize-column-id (:id field))))
                                 ordered-entity-fields)
        build-filterable-cell
        (fn [{:keys [field raw-value display-value rendered-content]}]
          (let [field-id (normalize-column-id (:id field))
                filter-value (when (and show-filtering?
                                     (field-filterable? field-id))
                               (filter-helpers/cell-filter-value
                                 {:field-spec field
                                  :item item
                                  :value raw-value
                                  :display-value display-value}))]
            (if filter-value
              (fn []
                ($ :div {:class "w-full cursor-pointer"
                         :title "Double-click to filter by this value"
                         :on-double-click (fn [e]
                                            (.stopPropagation e)
                                            (rf/dispatch [::filter-events/apply-filter
                                                          entity-name
                                                          field-id
                                                          filter-value
                                                          false]))}
                  rendered-content))
              rendered-content)))
        field-values (mapv (fn [field]
                             (let [raw-id (:id field)
                                   field-id (normalize-column-id raw-id)
                                   is-column-visible? (let [user-setting (get visible-columns field-id ::not-found)]
                                                        (if (not= user-setting ::not-found)
                                                          user-setting
                                                          true))]
                               (when is-column-visible?
                                 (let [entity-ns (kw/ensure-name entity-name)
                                       raw-name (kw/ensure-name raw-id)
                                       raw-kw (kw/ensure-keyword raw-id)
                                       namespaced-raw (when (and entity-ns raw-name)
                                                        (keyword entity-ns raw-name))
                                       namespaced-canonical (when (and entity-ns (kw/ensure-name field-id))
                                                              (keyword entity-ns (kw/ensure-name field-id)))
                                       value (or (get item field-id)
                                               (when raw-kw (get item raw-kw))
                                               (when namespaced-raw (get item namespaced-raw))
                                               (when namespaced-canonical (get item namespaced-canonical)))
                                       display-value (or (some-> (:display-source-field field)
                                                           (filter-helpers/get-item-field-value item))
                                                       value)
                                       rendered-content (get-field-display-value field value item)]
                                   (build-filterable-cell {:field field
                                                           :raw-value value
                                                           :display-value display-value
                                                           :rendered-content rendered-content})))))
                       filtered-entity-fields)
        filtered-field-values (filterv some? field-values)
        timestamps (let [created-key :created-at
                         updated-key :updated-at
                         legacy-map {created-key :created-at
                                     updated-key :updated-at}
                         sentinel ::not-found
                         resolve-setting (fn [settings key]
                                           (let [legacy (get legacy-map key)
                                                 current (get settings key sentinel)
                                                 legacy-val (if legacy (get settings legacy sentinel) sentinel)]
                                             (cond
                                               (not= current sentinel) current
                                               (not= legacy-val sentinel) legacy-val
                                               :else nil)))
                         column-visible? (fn [key]
                                           (let [value (resolve-setting visible-columns key)]
                                             (if (nil? value) true value)))
                         fetch-value (fn [entity key]
                                       (let [legacy (get legacy-map key)
                                             direct (or (get entity key)
                                                      (get entity (keyword (str (kw/ensure-name entity-name)
                                                                             "/"
                                                                             (kw/ensure-name key)))))
                                             legacy-val (when legacy
                                                          (or (get entity legacy)
                                                            (get entity (keyword (str (kw/ensure-name entity-name)
                                                                                   "/"
                                                                                   (kw/ensure-name legacy))))))]
                                         (or direct legacy-val)))
                         render-timestamp (fn [value]
                                            (timestamp/render-timestamp value {:show-seconds? true
                                                                               :highlight-seconds? true}))
                         timestamp-cell (fn [field-id raw-value]
                                          (let [field-spec (or (get field-specs-by-id field-id)
                                                             {:id field-id
                                                              :input-type "datetime"})]
                                            (build-filterable-cell {:field field-spec
                                                                    :raw-value raw-value
                                                                    :display-value raw-value
                                                                    :rendered-content (render-timestamp raw-value)})))
                         has-created? (contains? timestamp-field-ids created-key)
                         has-updated? (contains? timestamp-field-ids updated-key)]
                     [(when (and has-created? (column-visible? created-key))
                        (let [raw-value (fetch-value item created-key)]
                          (timestamp-cell created-key raw-value)))
                      (when (and has-updated? (column-visible? updated-key))
                        (let [raw-value (fetch-value item updated-key)]
                          (timestamp-cell updated-key raw-value)))])
        filtered-timestamps (filterv some? timestamps)]

    (vec (concat [(fn [] ($ cells/reactive-selection-cell
                           {:entity-name entity-name
                            :item item
                            :selected-ids selected-ids
                            :on-select-change on-select-change}))]
           filtered-field-values
           filtered-timestamps
           [(fn [] actions)]))))

;; =============================================================================
;; Row Rendering
;; =============================================================================

(defn render-row
  "Renders a single row in the list, either as a form or as a table row."
  [{:keys [entity-spec
           editing
           set-editing!
           entity-name
           recently-updated-ids
           recently-created-ids
           selected-ids
           on-select-change
           visible-columns
           column-order
           show-filtering?
           filterable-fields
           user-filterable-settings]
    :as props}
   {:keys [item]}]
  (let [props-map (js->clj props :keywordize-keys true)
        effective-entity-spec entity-spec
        form-entity-spec-edit (or (:form-entity-spec-edit props-map)
                                (:form-entity-spec props-map))
        effective-form-entity-spec (or form-entity-spec-edit effective-entity-spec)
        item-clj (if (map? item) item (js->clj item :keywordize-keys true))
        row-props (merge props-map
                    {:initial-values (into {}
                                       (map (fn [[k v]]
                                              (let [simple-key (if (and (keyword? k) (namespace k))
                                                                 (keyword (name k))
                                                                 k)]
                                                [simple-key v]))
                                         item-clj))
                     :entity-spec effective-form-entity-spec
                     :on-cancel #(do
                                   (rf/dispatch [::crud-events/clear-error (keyword (:entity-name props))])
                                   (rf/dispatch [::form-events/clear-form-errors (keyword (:entity-name props))])
                                   (set-editing! nil))
                     :item item-clj})
        item-id (id-utils/extract-entity-id item-clj)
        item-id-int (if (string? item-id) (js/parseInt item-id) item-id)
        item-id-str (str item-id)
        is-uuid? (fn [s]
                   (and (string? s)
                     (re-matches #"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$" s)))
        is-editing (and (some? item-id)
                     (some? editing)
                     (cond
                       (and (is-uuid? item-id) (is-uuid? editing))
                       (= item-id editing)

                       (and (number? item-id) (number? editing))
                       (= item-id editing)

                       (and (string? item-id) (number? editing))
                       (= (js/parseInt item-id) editing)

                       (and (number? item-id) (string? editing))
                       (= item-id (js/parseInt editing))

                       (and (string? item-id) (string? editing))
                       (= item-id editing)

                       :else
                       (= item-id editing)))
        updated-ids (or recently-updated-ids #{})
        created-ids (or recently-created-ids #{})
        recently-updated? (and (set? updated-ids)
                            (or (contains? updated-ids item-id)
                              (contains? updated-ids item-id-int)
                              (contains? updated-ids item-id-str)))
        recently-created? (and (set? created-ids)
                            (or (contains? created-ids item-id)
                              (contains? created-ids item-id-int)
                              (contains? created-ids item-id-str)))
        selected-id-set (set (or selected-ids #{}))
        selected? (or (contains? selected-id-set item-id)
                    (contains? selected-id-set item-id-int)
                    (contains? selected-id-set item-id-str))
        disallowed-mode (or (:disallowed-action-mode props) :hide)
        disable-mode? (= disallowed-mode :disable)
        allowed-edit? (not (false? (:allow-edit? props)))
        allowed-delete? (not (false? (:allow-delete? props)))
        policy-show-edit? (not (false? (:show-edit? props)))
        policy-show-delete? (not (false? (:show-delete? props)))
        show-edit? (and policy-show-edit? (or allowed-edit? disable-mode?))
        show-delete? (and policy-show-delete? (or allowed-delete? disable-mode?))
        edit-disabled? (and policy-show-edit? disable-mode? (not allowed-edit?))
        delete-disabled? (and policy-show-delete? disable-mode? (not allowed-delete?))]

    (if is-editing
      (if-let [custom-render-edit (:render-edit-form props)]
        [(custom-render-edit item-clj
           {:entity-name entity-name
            :entity-spec entity-spec
            :on-success (fn []
                          (set-editing! nil)
                          (when-let [on-success (:on-edit-success props)]
                            (on-success)))
            :on-cancel #(set-editing! nil)})]
        ($ form (assoc row-props :editing editing)))
      {:cells (row-content {:entity-spec effective-entity-spec
                            :item item
                            :actions (if-let [override (:actions-override props)]
                                       (override (assoc item-clj
                                                   :show-edit? show-edit?
                                                   :show-delete? show-delete?
                                                   :edit-disabled? edit-disabled?
                                                   :delete-disabled? delete-disabled?
                                                   :on-edit-click (:on-edit-click props)))
                                       ($ cells/action-buttons
                                         {:item item-clj
                                          :entity-name (:entity-name props)
                                          :show-edit? show-edit?
                                          :show-delete? show-delete?
                                          :edit-disabled? edit-disabled?
                                          :delete-disabled? delete-disabled?
                                          :custom-actions (:custom-actions props)
                                          :on-edit-click (:on-edit-click props)}))
                            :visible-columns visible-columns
                            :column-order column-order
                            :entity-name entity-name
                            :selected-ids (or selected-ids #{})
                            :on-select-change on-select-change
                            :show-filtering? show-filtering?
                            :filterable-fields filterable-fields
                            :user-filterable-settings user-filterable-settings})
       :recently-updated? recently-updated?
       :recently-created? recently-created?
       :selected? selected?
       :is-api-failure? (boolean (:is-api-failure item-clj))})))
