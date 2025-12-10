(ns app.template.frontend.components.common
  (:require
   [app.template.frontend.events.list.crud :as crud-events]
   [re-frame.core :as rf]
   [uix.core :refer [$ defui] :as uix]
   [uix.hooks.alpha :as hooks]
   [uix.re-frame :as urf]))

(def label-props
  {:text {:type :string :required true}
   :required {:type :boolean :required false}
   :class {:type :string :required false}
   :for {:type :string :required false}
   :id {:type :string :required false}})

(defui label
  {:prop-types label-props}
  [{:keys [text required class for id] :as props}]
  ($ :label
    (merge
      {:class (str "ds-label font-bold" (when class (str " " class)))
       :id id
       :for (or for id)}
      (dissoc props :text :required :class :for :id))
    ($ :span text)
    (when required
      ($ :span {:class "text-red-500 ml-1"} "*"))))

(def checkbox-props
  {:id {:type :string :required true}
   :label {:type :string :required true}
   :checked {:type :boolean :required false}
   :on-change {:type :function :required true}
   :class {:type :string :required false}
   :indeterminate? {:type :boolean :required false}})

(defui checkbox
  "A simple checkbox component"
  {:prop-types checkbox-props}
  [{:keys [id label checked on-change class indeterminate?] :as props}]
  (let [set-indeterminate (hooks/use-callback
                            (fn [el]
                              (when el
                                (set! (.-indeterminate el) (boolean indeterminate?))))
                            #js [indeterminate?])
        clean-props (dissoc props :label :indeterminate? :formId)]

    ($ :div {:class "flex items-center"}
      ($ :input
        (merge clean-props
          {:type "checkbox"
           :id id
           :checked (boolean checked)
           :on-change on-change
           :ref set-indeterminate
           :class (str "ds-checkbox ds-checkbox-primary " class)}))
      ($ :label
        {:for id
         :class "ds-label"}
        ($ :span label)))))

(def input-props
  {:id {:type :string :required false}
   :form-id {:type :string :required false}
   :value {:type :any :required false}
   :on-change {:type :function :required false}
   :class {:type :string :required false}})

(defui input
  {:prop-types input-props}
  [{:keys [id form-id] :as props}]
  (when props
    ($ :div
      ($ :input
        (-> props
          (dissoc :error :input-type :disabled? :validate-server? :form-id :formId)
          (assoc :class "ds-input ds-input-bordered")
          (assoc :id (or id (when form-id (str form-id "-input")))))))))

(def select-props
  {:options {:type :array :required true}
   :id {:type :string :required false}
   :form-id {:type :string :required false}
   :value {:type :any :required false}
   :on-change {:type :function :required false}})

(defui select
  {:prop-types select-props}
  [{:keys [options id form-id show-mixed-values?] :as props}]
  (let [[entity-name field-name] (if (and (vector? options)
                                       (= 2 (count options))
                                       (every? #(or (keyword? %) (string? %)) options))
                                   options
                                   [nil nil])
        ;; Get current entity type to avoid circular fetches
        current-entity-type (urf/use-subscribe [:app.template.frontend.subs.list/current-entity-type])
        entity-subscription (seq (urf/use-subscribe [::select-options
                                                     (or entity-name :default)
                                                     (or field-name :default)]))
        final-options (or
                        entity-subscription
                        options)
        ;; Fix 1: Ensure value is never null, use empty string instead
        value-prop (:value props)
        safe-value-prop (if (nil? value-prop) "" value-prop)

        is-foreign-key? (and entity-name field-name)
        entity-data (urf/use-subscribe [:app.template.frontend.subs.entity/entities entity-name])

        ;; Check for entity with matching name
        matching-items (when (and is-foreign-key?
                               (string? safe-value-prop)
                               (not (re-matches #"^\d+$" safe-value-prop))) ; If value looks like a name, not an ID
                         (filter (fn [[_ item]]
                                   (= (get item field-name) safe-value-prop))
                           (:data entity-data)))

        entity-id-by-name (when (seq matching-items)
                            (first (first matching-items)))

        resolved-value (if entity-id-by-name
                         entity-id-by-name                  ; Use the ID we found by name lookup
                         safe-value-prop)                   ; Use the safe value (never null)

        ;; Fix 2: Always ensure the value prop is not null
        resolved-props (-> props
                         (assoc :value resolved-value)
                         (dissoc :error :disabled? :validate-server? :form-id :formId :show-mixed-values?)
                         (assoc :class "ds-select")
                         (assoc :id (or id (when form-id (str form-id "-select")))))]

    (uix/use-effect
      (fn []
        ;; Only fetch entities if we're not already on the same entity page (to avoid infinite loops)
        (when (and entity-name
                (keyword? entity-name)
                (not (= entity-name current-entity-type)))
          (rf/dispatch [::crud-events/fetch-entities entity-name]))
        js/undefined)
      [entity-name current-entity-type])

    ($ :div
      ($ :select resolved-props
        (concat
          ;; Show different default option based on mixed values state
          [($ :option {:key "default-empty-option" :value ""}
             (if show-mixed-values? "(Mixed values)" "Select..."))]
          ;; Fix 3: Use unique ID-based keys and ensure they're never empty
          (map-indexed
            (fn [idx {:keys [value label]}]
              ($ :option
                ;; Use index to ensure uniqueness even when value/label are empty
                {:key (str "option-" idx "-" (or value "") "-" (or label ""))
                 :value (or value "")}
                (or label "")))
            (or final-options [])))))))

(def text-area-props
  {:error {:type :string :required false}
   :id {:type :string :required false}
   :form-id {:type :string :required false}
   :value {:type :string :required false}
   :on-change {:type :function :required false}})

(defui text-area
  {:prop-types text-area-props}
  [{:keys [error id form-id] :as props}]
  ($ :div
    ($ :textarea
      (merge
        (-> props
          (dissoc :error :form-id :formId)
          (assoc :class "ds-textarea ds-textarea-bordered w-full"
            :id (or id (when form-id (str form-id "-textarea")))))
        {:class (str (or (:class props) "")
                  (when error " textarea-error"))}))
    (when error
      ($ :div {:class "text-error text-sm mt-1"} error))))

;; Re-frame subscription for select options
(rf/reg-sub
  ::select-options
  (fn [[_ entity-name]]
    (if (not= entity-name :default)
      (rf/subscribe [:app.template.frontend.subs.entity/entities entity-name])
      []))
  (fn [entities [_ _entity-name field-name]]
    (if entities
      (->> entities
        (mapv (fn [entity]
                (let [val (:id entity)
                      lbl (get entity field-name)]
                  {:value val
                   :label (or lbl val)})))
        (sort-by :label))
      [])))
