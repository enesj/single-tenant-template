(ns app.template.frontend.i18n
  "Facade: merges per-domain dictionaries and exposes translate + use-t."
  (:require
    [tongue.core :as tongue]
    [uix.re-frame :refer [use-subscribe]]
    [app.template.frontend.i18n.common   :as common]
    [app.template.frontend.i18n.auth     :as auth]
    [app.template.frontend.i18n.nav      :as nav]
    [app.template.frontend.i18n.tenant   :as tenant]
    [app.template.frontend.i18n.expenses :as expenses]
    [app.template.frontend.i18n.entities :as entities]
    [app.template.frontend.i18n.search   :as search]
    [app.template.frontend.i18n.mobile   :as mobile]
    [app.template.frontend.i18n.landing  :as landing]))

;; ---------------------------------------------------------------------------
;; Dictionary
;; Bosnian (:bs) is the default; English (:en) is the fallback.
;; Plain functions are used for interpolated strings: (t :key arg1 arg2).
;; Developer/console messages are intentionally kept in English.
;; ---------------------------------------------------------------------------

(def dicts
  (merge-with merge
    common/dicts
    auth/dicts
    nav/dicts
    tenant/dicts
    expenses/dicts
    entities/dicts
    search/dicts
    mobile/dicts
    landing/dicts))

;; ---------------------------------------------------------------------------
;; translate fn and use-t hook
;; ---------------------------------------------------------------------------

(def translate (tongue/build-translate dicts))

(defn use-t
  "React hook: subscribes to [:locale] and returns a curried translate fn.
   Components calling (use-t) re-render automatically on locale change.
   Usage: (let [t (use-t)] (t :login/welcome))"
  []
  (let [locale (use-subscribe [:locale])]
    (fn [key & args]
      (apply translate locale key args))))

(comment
  (translate :bs :login/welcome)        ;=> "Dobrodošli"
  (translate :en :login/welcome)        ;=> "Welcome"
  (translate :bs :tenant/welcome "Ana") ;=> "Dobrodošli, Ana!"
  :rcf)
