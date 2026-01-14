(ns app.admin.frontend.events.user-settings.view-options
  (:require
    [app.admin.frontend.events.user-settings.utils :as u]
    [re-frame.core :as rf]))

;; =============================================================================
;; Draft editing: view-options (defaults + locks)
;; =============================================================================

;; new-state schema:
;; - {:kind :inherit}
;; - {:kind :default :value boolean}
;; - {:kind :lock :value boolean}
(rf/reg-event-db
  :app.admin.frontend.events.user-settings/set-display-setting-draft
  (fn [db [_ entity setting-key new-state]]
    (let [entity-kw (u/normalize-kw entity)
          setting-kw (u/normalize-kw setting-key)
          defaults-path [:admin :user-settings :draft :view-options entity-kw :display-defaults]
          locks-path [:admin :user-settings :draft :view-options entity-kw :display-locks]
          kind (:kind new-state)
          value (:value new-state)
          ;; For :per-page, accept integer values; for other settings, accept booleans
          valid-value? (if (= setting-kw :per-page)
                         (and (integer? value) (pos? value))
                         (boolean? value))]
      (cond
        (or (nil? entity-kw) (nil? setting-kw))
        db

        (= kind :inherit)
        (-> db
          (update-in defaults-path (fnil dissoc {}) setting-kw)
          (update-in locks-path (fnil dissoc {}) setting-kw))

        (and (= kind :default) valid-value?)
        (-> db
            ;; A default only applies when not locked.
          (update-in locks-path (fnil dissoc {}) setting-kw)
          (assoc-in (conj defaults-path setting-kw) value))

        (and (= kind :lock) valid-value?)
        (-> db
            ;; A lock supersedes any default.
          (update-in defaults-path (fnil dissoc {}) setting-kw)
          (assoc-in (conj locks-path setting-kw) value))

        :else
        db))))

;; =============================================================================
;; Draft editing: column visibility policy (defaults + locks)
;; =============================================================================

;; new-state schema:
;; - {:kind :inherit}
;; - {:kind :default :value boolean}
;; - {:kind :lock :value boolean}
(rf/reg-event-db
  :app.admin.frontend.events.user-settings/set-column-visibility-setting-draft
  (fn [db [_ entity column-key new-state]]
    (let [entity-kw (u/normalize-kw entity)
          column-kw (u/normalize-kw column-key)
          defaults-path [:admin :user-settings :draft :view-options entity-kw :column-defaults]
          locks-path [:admin :user-settings :draft :view-options entity-kw :column-locks]
          kind (:kind new-state)
          value (:value new-state)]
      (cond
        (or (nil? entity-kw) (nil? column-kw))
        db

        (= kind :inherit)
        (-> db
          (update-in defaults-path (fnil dissoc {}) column-kw)
          (update-in locks-path (fnil dissoc {}) column-kw))

        (and (= kind :default) (boolean? value))
        (-> db
            ;; A default only applies when not locked.
          (update-in locks-path (fnil dissoc {}) column-kw)
          (assoc-in (conj defaults-path column-kw) value))

        (and (= kind :lock) (boolean? value))
        (-> db
            ;; A lock supersedes any default.
          (update-in defaults-path (fnil dissoc {}) column-kw)
          (assoc-in (conj locks-path column-kw) value))

        :else
        db))))

;; =============================================================================
;; Bulk helpers: column visibility defaults
;; =============================================================================

(rf/reg-event-db
  :app.admin.frontend.events.user-settings/set-column-defaults-bulk
  (fn [db [_ entity column-keys value]]
    (let [entity-kw (u/normalize-kw entity)
          cols (->> (or column-keys [])
                 (keep u/normalize-kw)
                 vec)
          value (boolean value)
          defaults-path [:admin :user-settings :draft :view-options entity-kw :column-defaults]
          locks-path [:admin :user-settings :draft :view-options entity-kw :column-locks]]
      (cond
        (or (nil? entity-kw) (empty? cols))
        db

        :else
        (reduce (fn [db' col]
                  (assoc-in db' (conj defaults-path col) value))
          (update-in db locks-path (fnil (fn [m] (apply dissoc m cols)) {}))
          cols)))))

;; =============================================================================
;; Bulk helpers: apply tristate to many display settings / columns
;; =============================================================================

(rf/reg-event-db
  :app.admin.frontend.events.user-settings/set-display-settings-bulk
  (fn [db [_ entity setting-keys new-state]]
    (let [entity-kw (u/normalize-kw entity)
          keys (->> (or setting-keys [])
                 (keep u/normalize-kw)
                 vec)
          kind (:kind new-state)
          value (:value new-state)
          defaults-path [:admin :user-settings :draft :view-options entity-kw :display-defaults]
          locks-path [:admin :user-settings :draft :view-options entity-kw :display-locks]]
      (cond
        (or (nil? entity-kw) (empty? keys))
        db

        (= kind :inherit)
        (-> db
          (update-in defaults-path (fnil (fn [m] (apply dissoc m keys)) {}))
          (update-in locks-path (fnil (fn [m] (apply dissoc m keys)) {})))

        (and (= kind :default) (boolean? value))
        (reduce (fn [db' k]
                  (assoc-in db' (conj defaults-path k) value))
          (update-in db locks-path (fnil (fn [m] (apply dissoc m keys)) {}))
          keys)

        (and (= kind :lock) (boolean? value))
        (reduce (fn [db' k]
                  (assoc-in db' (conj locks-path k) value))
          (update-in db defaults-path (fnil (fn [m] (apply dissoc m keys)) {}))
          keys)

        :else
        db))))

(rf/reg-event-db
  :app.admin.frontend.events.user-settings/set-column-visibility-bulk
  (fn [db [_ entity column-keys new-state]]
    (let [entity-kw (u/normalize-kw entity)
          cols (->> (or column-keys [])
                 (keep u/normalize-kw)
                 vec)
          kind (:kind new-state)
          value (:value new-state)
          defaults-path [:admin :user-settings :draft :view-options entity-kw :column-defaults]
          locks-path [:admin :user-settings :draft :view-options entity-kw :column-locks]]
      (cond
        (or (nil? entity-kw) (empty? cols))
        db

        (= kind :inherit)
        (-> db
          (update-in defaults-path (fnil (fn [m] (apply dissoc m cols)) {}))
          (update-in locks-path (fnil (fn [m] (apply dissoc m cols)) {})))

        (and (= kind :default) (boolean? value))
        (reduce (fn [db' c]
                  (assoc-in db' (conj defaults-path c) value))
          (update-in db locks-path (fnil (fn [m] (apply dissoc m cols)) {}))
          cols)

        (and (= kind :lock) (boolean? value))
        (reduce (fn [db' c]
                  (assoc-in db' (conj locks-path c) value))
          (update-in db defaults-path (fnil (fn [m] (apply dissoc m cols)) {}))
          cols)

        :else
        db))))

(rf/reg-event-db
  :app.admin.frontend.events.user-settings/reset-entity-display-draft
  (fn [db [_ entity]]
    (let [entity-kw (u/normalize-kw entity)
          saved-defaults (get-in db [:admin :user-settings :saved :view-options entity-kw :display-defaults])
          saved-locks (get-in db [:admin :user-settings :saved :view-options entity-kw :display-locks])
          saved-col-defaults (get-in db [:admin :user-settings :saved :view-options entity-kw :column-defaults])
          saved-col-locks (get-in db [:admin :user-settings :saved :view-options entity-kw :column-locks])]
      (cond
        (nil? entity-kw)
        db

        :else
        (-> db
          (cond->
            (map? saved-defaults)
            (assoc-in [:admin :user-settings :draft :view-options entity-kw :display-defaults] saved-defaults)

            (not (map? saved-defaults))
            (update-in [:admin :user-settings :draft :view-options entity-kw] dissoc :display-defaults))

          (cond->
            (map? saved-locks)
            (assoc-in [:admin :user-settings :draft :view-options entity-kw :display-locks] saved-locks)

            (not (map? saved-locks))
            (update-in [:admin :user-settings :draft :view-options entity-kw] dissoc :display-locks))

          (cond->
            (map? saved-col-defaults)
            (assoc-in [:admin :user-settings :draft :view-options entity-kw :column-defaults] saved-col-defaults)

            (not (map? saved-col-defaults))
            (update-in [:admin :user-settings :draft :view-options entity-kw] dissoc :column-defaults))

          (cond->
            (map? saved-col-locks)
            (assoc-in [:admin :user-settings :draft :view-options entity-kw :column-locks] saved-col-locks)

            (not (map? saved-col-locks))
            (update-in [:admin :user-settings :draft :view-options entity-kw] dissoc :column-locks)))))))

