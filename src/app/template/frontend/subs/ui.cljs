(ns app.template.frontend.subs.ui
  (:require
    [app.admin.frontend.config.loader :as config-loader]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(rf/reg-sub
  ::recently-updated-entities
  (fn [db [_ entity-type]]
    (let [updated-ids (get-in db [:ui :recently-updated entity-type])]
      updated-ids)))

(rf/reg-sub
  ::recently-created-entities
  (fn [db [_ entity-type]]
    (let [created-ids (get-in db [:ui :recently-created entity-type])]
      created-ids)))

;; Helper functions to get entity-specific or default settings
(defn get-entity-setting
  "Get entity-specific setting or fallback to defaults, then globals, then provided default."
  [db entity-name path default-value]
  (let [sentinel ::not-found
        entity-path (into [:ui :entity-configs entity-name] path)
        default-path (into [:ui :defaults] path)
        global-path (into [:ui] path)
        entity-value (get-in db entity-path sentinel)
        default-value-from-defaults (get-in db default-path sentinel)
        global-value (get-in db global-path sentinel)]
    (cond
      (and (not= entity-value sentinel) (some? entity-value)) entity-value
      (and (not= default-value-from-defaults sentinel) (some? default-value-from-defaults)) default-value-from-defaults
      (and (not= global-value sentinel) (some? global-value)) global-value
      :else default-value)))

(rf/reg-sub
  ::entity-display-settings
  (fn [db [_ entity-name]]
    (let [show-timestamps? (get-entity-setting db entity-name [:show-timestamps?] true)]
      {:show-timestamps? show-timestamps?
       :show-edit? (get-entity-setting db entity-name [:show-edit?] true)
       :show-delete? (get-entity-setting db entity-name [:show-delete?] true)
       :show-highlights? (get-entity-setting db entity-name [:show-highlights?] true)
       :show-select? (get-entity-setting db entity-name [:show-select?] false)
       :show-filtering? (get-entity-setting db entity-name [:show-filtering?] true)
       :show-pagination? (get-entity-setting db entity-name [:show-pagination?] true)
       :controls {:show-timestamps-control? (get-entity-setting db entity-name [:controls :show-timestamps-control?] true)
                  :show-edit-control? (get-entity-setting db entity-name [:controls :show-edit-control?] true)
                  :show-delete-control? (get-entity-setting db entity-name [:controls :show-delete-control?] true)
                  :show-highlights-control? (get-entity-setting db entity-name [:controls :show-highlights-control?] true)
                  :show-select-control? (get-entity-setting db entity-name [:controls :show-select-control?] false)
                  :show-filtering-control? (get-entity-setting db entity-name [:controls :show-filtering-control?] true)
                  :show-invert-selection? (get-entity-setting db entity-name [:controls :show-invert-selection?] false)
                  :show-delete-selected? (get-entity-setting db entity-name [:controls :show-delete-selected?] false)}})))

(rf/reg-sub
  ::show-add-form
  (fn [db _]
    (get-in db [:ui :show-add-form])))

(rf/reg-sub
  ::editing
  (fn [db _]
    (get-in db [:ui :editing])))

;; Subscription to get the list of filterable fields for an entity
;; Uses vector-config only (no legacy fallback)
(rf/reg-sub
  ::filterable-fields
  (fn [_ [_ entity-name]]
    (when-let [vector-config (config-loader/get-table-config entity-name)]
      (:filterable-columns vector-config))))
;; Note: We intentionally do not fall back to [:ui :entity-configs]
;; to avoid mixing legacy settings with vector-config.

;; Subscription to get the list of visible columns for an entity
(rf/reg-sub
  ::visible-columns
  (fn [db [_ entity-name]]
    ;; Get the visible columns map from the entity config, or nil if not set
    (get-entity-setting db entity-name [:visible-columns] nil)))
;; Note: We don't provide a default map here, instead we handle defaults at rendering time
;; This allows us to properly track which columns have been explicitly set vs. using defaults
