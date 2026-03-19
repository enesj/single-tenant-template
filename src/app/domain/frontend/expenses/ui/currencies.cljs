(ns app.domain.frontend.expenses.ui.currencies
  "Helpers for frontend currency option lists driven by profile/global settings."
  (:require
    [clojure.string :as str]))

(def fallback-currency-options
  [{:label "BAM" :value "BAM"}
   {:label "EUR" :value "EUR"}
   {:label "USD" :value "USD"}])

(defn enabled-currency-options
  [profile]
  (let [currencies (or (:enabled-currencies profile)
                     (:enabled_currencies profile))
        options (->> currencies
                  (keep (fn [currency]
                          (let [code (some-> (or (:code currency)
                                               (:enabled_currencies/code currency))
                                       str
                                       str/trim
                                       not-empty)
                                name (some-> (or (:name currency)
                                               (:enabled_currencies/name currency))
                                       str
                                       str/trim
                                       not-empty)]
                            (when code
                              {:label (or code name)
                               :value code}))))
                  (distinct)
                  vec)]
    (if (seq options)
      options
      fallback-currency-options)))

(defn default-currency
  [profile]
  (or (get-in profile [:settings :default-currency])
    (get-in profile [:settings :default_currency])
    "BAM"))

(defn has-enabled-currencies?
  [profile]
  (boolean (seq (or (:enabled-currencies profile)
                  (:enabled_currencies profile)))))
