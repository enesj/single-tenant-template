(ns app.admin.frontend.events.settings.navigation
	(:require
		[app.admin.frontend.events.settings.utils :as utils]
		[app.admin.frontend.utils.navigation-config :as nav-config]
		[re-frame.core :as rf]))

(rf/reg-event-db
	:app.admin.frontend.events.settings/set-navigation-title-draft
	(fn [db [_ title]]
		(update-in db [:admin :settings :navigation] nav-config/set-title title)))

(rf/reg-event-db
	:app.admin.frontend.events.settings/set-navigation-section-title-draft
	(fn [db [_ section-id title]]
		(update-in db [:admin :settings :navigation] nav-config/set-section-title section-id title)))

(rf/reg-event-db
	:app.admin.frontend.events.settings/set-navigation-item-label-draft
	(fn [db [_ item-id label]]
		(update-in db [:admin :settings :navigation] nav-config/set-item-label item-id label)))

(rf/reg-event-db
	:app.admin.frontend.events.settings/set-navigation-item-visible-draft
	(fn [db [_ item-id visible?]]
		(update-in db [:admin :settings :navigation] nav-config/set-item-visible item-id visible?)))

(rf/reg-event-db
	:app.admin.frontend.events.settings/move-navigation-item-draft
	(fn [db [_ item-id direction]]
		(update-in db [:admin :settings :navigation] nav-config/move-item item-id direction)))

(rf/reg-event-db
	:app.admin.frontend.events.settings/move-navigation-item-to-section-draft
	(fn [db [_ item-id section-id]]
		(update-in db [:admin :settings :navigation] nav-config/move-item-to-section item-id section-id)))

(rf/reg-event-db
	:app.admin.frontend.events.settings/reset-navigation-draft
	(fn [db _]
		(let [saved (utils/safe-map (get-in db [:admin :settings :navigation-saved]))]
			(assoc-in db [:admin :settings :navigation] saved))))