(ns app.admin.frontend.components.settings-views.utils)

(defn next-tristate
  "Cycle nil → true → false → nil"
  [v]
  (cond
    (nil? v) true
    (true? v) false
    (false? v) nil
    :else nil))

(defn default-badge-props
  "Badge for a default value (nil/true/false or :mixed)."
  [default-val]
  (cond
    (= default-val :mixed)
    {:class "ds-badge ds-badge-ghost ds-badge-sm" :text "Mixed"}

    (true? default-val)
    {:class "ds-badge ds-badge-success ds-badge-sm" :text "Default On"}

    (false? default-val)
    {:class "ds-badge ds-badge-error ds-badge-sm" :text "Default Off"}

    :else
    {:class "ds-badge ds-badge-ghost ds-badge-sm" :text "Inherit"}))

(defn lock-badge-props
  "Badge for a lock value.

   Options:
   - lock-style :admin | :user
   - immutable? / immutable-val: when immutable, display as Enforced and disable controls"
  [{:keys [lock-val lock-style immutable? immutable-val]}]
  (let [lock-style (or lock-style :user)
        effective-lock (if immutable? immutable-val lock-val)]
    (cond
      (= effective-lock :mixed)
      {:class "ds-badge ds-badge-ghost ds-badge-sm"
       :text "Mixed"}

      immutable?
      {:class (str "ds-badge ds-badge-sm "
                (if (true? effective-lock) "ds-badge-success" "ds-badge-error"))
       :text (str "Enforced " (if (true? effective-lock) "On" "Off"))}

      (true? effective-lock)
      {:class "ds-badge ds-badge-success ds-badge-sm"
       :text (case lock-style
               :admin "Enabled"
               "Locked On")}

      (false? effective-lock)
      {:class "ds-badge ds-badge-error ds-badge-sm"
       :text (case lock-style
               :admin "Disabled"
               "Locked Off")}

      :else
      {:class "ds-badge ds-badge-ghost ds-badge-sm"
       :text (case lock-style
               :admin "Not set"
               "Inherit")})))

(defn tristate-hint
  "Small helper text showing the next value in the cycle."
  [{:keys [kind current-val lock-style]}]
  (let [lock-style (or lock-style :user)
        next-val (next-tristate (if (= current-val :mixed) nil current-val))]
    (case kind
      :default
      (str "→ " (cond
                  (true? next-val) "Default On"
                  (false? next-val) "Default Off"
                  :else "Inherit"))

      :lock
      (str "→ " (cond
                  (true? next-val) (case lock-style :admin "Enabled" "Locked On")
                  (false? next-val) (case lock-style :admin "Disabled" "Locked Off")
                  :else (case lock-style :admin "Not set" "Inherit")))
      nil)))

(defn uniform-or-mixed
  "If all values are the same, returns that value; otherwise returns :mixed.

  Values are expected to be one of nil/true/false."
  [vals]
  (let [vals (vec vals)]
    (cond
      (empty? vals) nil
      (apply = vals) (first vals)
      :else :mixed)))

