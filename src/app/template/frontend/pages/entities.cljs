(ns app.template.frontend.pages.entities
  (:require
    [app.template.frontend.events.bootstrap :as bootstrap-events]
    [app.template.frontend.subs.core :as subs]
    [app.template.frontend.components.button :refer [nav-button]]
    [app.template.frontend.components.list :refer [list-view]]
    [app.template.frontend.events.list.crud :as crud-events]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]
    [uix.core :refer [$ defui] :as uix]
    [uix.re-frame :refer [use-subscribe] :as urf]))

(defui entities-page [props]
  (let [entity-type (use-subscribe [::subs/entity-type])
        auth-status (use-subscribe [:auth-status])
        current-route (use-subscribe [:current-route])
        entity-name-from-url (get-in current-route [:parameters :path :entity-name])
        effective-entity-type (if (and entity-name-from-url
                                    (or (nil? entity-type)
                                      (not= (name entity-type) entity-name-from-url)))
                                (keyword entity-name-from-url)
                                entity-type)
        ;; Use entity-specs for table display (includes tenant/owner fields)
        all-entity-specs (use-subscribe [:entity-specs])
        pages-to-show (:pages-to-show props)
        base-entity-specs (if pages-to-show
                            (select-keys all-entity-specs pages-to-show)
                            all-entity-specs)
        ;; Hide sensitive entities like tenants from unauthenticated visitors
        entity-specs (cond-> base-entity-specs
                       (not (:authenticated auth-status)) (dissoc :tenants))
        show-add-form? (use-subscribe [:app.template.frontend.subs.ui/show-add-form])]

    ;; Combined effect to update entity type and fetch data
    (uix/use-effect
      (fn []
        ;; First, update entity type if URL parameter exists and differs from current type
        (when (and entity-name-from-url
                (or (nil? entity-type)
                  (not= (name entity-type) entity-name-from-url)))
          (rf/dispatch [::bootstrap-events/set-entity-type (keyword entity-name-from-url)]))

        ;; Then fetch entities if we have both entity type and specs
        (when (and effective-entity-type
                (seq entity-specs)
                (keyword? effective-entity-type))

          (rf/dispatch [::crud-events/fetch-entities effective-entity-type]))

        js/undefined)
      [entity-type entity-name-from-url effective-entity-type entity-specs])

    ($ :div
      {:class "container px-4"}
      ($ :h1
        {:class "text-4xl font-bold mb-8"}
        "Rental Bookkeeping")
      ($ :div
        {:class "flex justify-between mb-8"}
        ($ :div
          {:class "flex space-x-4"}
          (for [[page-key _] entity-specs]
            ($ nav-button
              {:key page-key
               :entity-type effective-entity-type
               :target-page page-key}))))
      ;; Pass the table entity spec (includes tenant/owner) to list-view
      ;; The list-view component will handle using form-entity-specs for forms internally
      (when-let [table-entity-spec (get entity-specs effective-entity-type)]
        ($ list-view
          {:entity-name effective-entity-type
           :entity-spec table-entity-spec                   ; This is for table display (includes tenant/owner)
           :title (str/capitalize (name effective-entity-type))
           :show-add-form? show-add-form?
           :set-show-add-form! #(rf/dispatch [:app.template.frontend.events.config/set-show-add-form %])})))))
