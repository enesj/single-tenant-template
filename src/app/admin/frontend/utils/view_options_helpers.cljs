(ns app.admin.frontend.utils.view-options-helpers
  "Pure db-mutation helpers for tristate view-options editing.

  These are path-parametric: callers pass `base-path` — the vector up to and
  including the entity key (e.g. [:admin :settings :view-options :expenses]) —
  and the helpers append :display-defaults / :display-locks / :column-defaults /
  :column-locks as needed.

  All functions are pure (db in, db out) and have no re-frame dependency.")

;; =============================================================================
;; Display-setting tristate helpers
;; =============================================================================

(defn apply-display-setting
  "Apply a single tristate display-setting mutation.

  new-state schema:
  - {:kind :inherit}
  - {:kind :default :value boolean|pos-int}  (:per-page uses pos-int)
  - {:kind :lock   :value boolean|pos-int}"
  [db base-path setting-kw kind value valid-value?]
  (let [defaults-path (conj base-path :display-defaults)
        locks-path (conj base-path :display-locks)]
    (cond
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

      :else db)))

(defn apply-display-settings-bulk
  "Apply a tristate state to many display settings at once."
  [db base-path ks kind value]
  (let [defaults-path (conj base-path :display-defaults)
        locks-path (conj base-path :display-locks)]
    (cond
      (= kind :inherit)
      (-> db
        (update-in defaults-path (fnil (fn [m] (apply dissoc m ks)) {}))
        (update-in locks-path (fnil (fn [m] (apply dissoc m ks)) {})))

      (and (= kind :default) (boolean? value))
      (reduce (fn [db' k]
                (assoc-in db' (conj defaults-path k) value))
        (update-in db locks-path (fnil (fn [m] (apply dissoc m ks)) {}))
        ks)

      (and (= kind :lock) (boolean? value))
      (reduce (fn [db' k]
                (assoc-in db' (conj locks-path k) value))
        (update-in db defaults-path (fnil (fn [m] (apply dissoc m ks)) {}))
        ks)

      :else db)))

;; =============================================================================
;; Column-visibility tristate helpers
;; =============================================================================

(defn apply-column-visibility-setting
  "Apply a single tristate column-visibility mutation."
  [db base-path column-kw kind value]
  (let [defaults-path (conj base-path :column-defaults)
        locks-path (conj base-path :column-locks)]
    (cond
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

      :else db)))

(defn apply-column-defaults-bulk
  "Set column-defaults for `cols` to `value`, clearing any existing locks."
  [db base-path cols value]
  (let [defaults-path (conj base-path :column-defaults)
        locks-path (conj base-path :column-locks)]
    (reduce (fn [db' col]
              (assoc-in db' (conj defaults-path col) value))
      (update-in db locks-path (fnil (fn [m] (apply dissoc m cols)) {}))
      cols)))

(defn apply-column-visibility-bulk
  "Apply a tristate state to many column-visibility settings at once."
  [db base-path cols kind value]
  (let [defaults-path (conj base-path :column-defaults)
        locks-path (conj base-path :column-locks)]
    (cond
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

      :else db)))
