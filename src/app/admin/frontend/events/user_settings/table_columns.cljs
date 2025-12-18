(ns app.admin.frontend.events.user-settings.table-columns
  (:require
    [app.admin.frontend.events.user-settings.utils :as u]
    [re-frame.core :as rf]))

;; =============================================================================
;; Draft editing: table-columns defaults
;; =============================================================================

(rf/reg-event-db
  :app.admin.frontend.events.user-settings/toggle-column-visibility-draft
  (fn [db [_ entity column-name]]
    (let [entity-kw (u/normalize-kw entity)
          column-kw (u/normalize-kw column-name)
          entity-config (u/safe-map (get-in db [:admin :user-settings :draft :table-columns entity-kw]))
          available (u/normalize-cols (:available-columns entity-config))
          always-visible-set (into #{} (u/normalize-cols (:always-visible entity-config)))
          has-default-visible? (contains? entity-config :default-visible-columns)
          default-visible (u/normalize-cols (if has-default-visible?
                                             (:default-visible-columns entity-config)
                                             available))
          visible-set (into #{} default-visible)
          visible-set (into visible-set always-visible-set)]
      (cond
        (or (nil? entity-kw) (nil? column-kw))
        db

        (not (some #{column-kw} available))
        db

        (contains? always-visible-set column-kw)
        db

        :else
        (let [currently-visible? (contains? visible-set column-kw)
              new-visible-set (if currently-visible?
                                (disj visible-set column-kw)
                                (conj visible-set column-kw))
              ;; Preserve ordering based on available-columns
              new-visible (->> available
                               (filter new-visible-set)
                               vec)]
          (assoc-in db
                    [:admin :user-settings :draft :table-columns entity-kw :default-visible-columns]
                    new-visible))))))

(rf/reg-event-db
  :app.admin.frontend.events.user-settings/reset-columns-draft
  (fn [db [_ entity]]
    (let [entity-kw (u/normalize-kw entity)
          saved-entity-config (get-in db [:admin :user-settings :saved :table-columns entity-kw])]
      (if (nil? entity-kw)
        db
        (assoc-in db [:admin :user-settings :draft :table-columns entity-kw] (u/safe-map saved-entity-config))))))

;; =============================================================================
;; Draft editing: table-columns config (structural, not policy)
;; =============================================================================

(rf/reg-event-db
  :app.admin.frontend.events.user-settings/set-table-column-list-draft
  (fn [db [_ entity list-type columns]]
    (let [entity-kw (u/normalize-kw entity)
          list-type-kw (u/normalize-kw list-type)]
      (if (or (nil? entity-kw) (nil? list-type-kw))
        db
        (assoc-in db [:admin :user-settings :draft :table-columns entity-kw list-type-kw] (vec columns))))))

(rf/reg-event-db
  :app.admin.frontend.events.user-settings/toggle-table-column-in-list-draft
  (fn [db [_ entity list-type column-name]]
    (let [entity-kw (u/normalize-kw entity)
          list-type-kw (u/normalize-kw list-type)
          col-str (if (keyword? column-name) (name column-name) (str column-name))
          path [:admin :user-settings :draft :table-columns entity-kw list-type-kw]
          current-cols (vec (or (get-in db path) []))
          col-set (set current-cols)]
      (if (or (nil? entity-kw) (nil? list-type-kw))
        db
        (let [new-cols (if (contains? col-set col-str)
                         (vec (remove #{col-str} current-cols))
                         (conj current-cols col-str))]
          (assoc-in db path new-cols))))))

