(ns app.template.frontend.pages.entity-manager
  "Shared entity management page wrapper."
  (:require
    [app.template.frontend.events.bootstrap :as bootstrap-events]
    [app.template.frontend.subs.core :as subs]
    [app.template.frontend.components.entity-nav :refer [entity-nav]]
    [app.template.frontend.components.list :refer [list-view]]
    [app.template.frontend.components.summary-cards :refer [summary-card-grid]]
    [app.template.frontend.events.list.crud :as crud-events]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui] :as uix]
    [uix.re-frame :refer [use-subscribe]]))

(defn- default-title
  [entity]
  (-> entity name (str/replace "_" " ") str/capitalize))

(defn- nav-items-from-set
  [entity-set title-fn]
  (map (fn [entity]
         {:id entity
          :label (title-fn entity)})
    (sort entity-set)))

(defui entity-page
  "Render an entity list page with shared routing, fetching, and list management.

   Expected props:
   - :entity-set        Set of allowed entity keywords
   - :default-entity    Optional default entity keyword (falls back to first in set)
   - :heading           Optional H1 heading text
   - :summary-items     Optional vector or function (entity -> items) for summary cards
   - :dashboard         Optional component for a custom dashboard when summary items not provided
   - :nav-items         Optional vector of {:id :label} maps (defaults derived from entity-set)
   - :nav-trailing      Optional element or function (entity -> element) rendered beside nav
   - :title-fn          Function to generate list title from entity (defaults to capitalized name)
   - :list-view-props   Map or function (context -> props) merged into list-view props
   - :container-class   Optional classes for outer container
   - :navigation-class  Optional classes for the nav container"
  [{:keys [entity-set default-entity heading summary-items dashboard nav-items nav-trailing
           title-fn list-view-props container-class navigation-class]}]
  (let [entity-type (use-subscribe [::subs/entity-type])
        current-route (use-subscribe [:current-route])
        entity-name-from-url (get-in current-route [:parameters :path :entity-name])
        url-entity (some-> entity-name-from-url keyword)
        entity-set (or entity-set #{})
        default-entity (or default-entity (first (sort entity-set)))
        valid-entity? (fn [entity] (and entity (contains? entity-set entity)))
        effective-entity (cond
                           (valid-entity? url-entity) url-entity
                           (valid-entity? entity-type) entity-type
                           :else default-entity)
        all-entity-specs (use-subscribe [:entity-specs])
        scoped-entity-specs (if (seq entity-set)
                              (select-keys all-entity-specs entity-set)
                              all-entity-specs)
        table-entity-spec (get scoped-entity-specs effective-entity)
        show-add-form? (use-subscribe [:app.template.frontend.subs.ui/show-add-form])
        title-fn (or title-fn default-title)
        resolved-summary-items (let [items summary-items]
                                 (cond
                                   (fn? items) (items effective-entity)
                                   (seq items) items
                                   :else nil))
        resolved-dashboard (when (and (not resolved-summary-items) dashboard)
                             (cond
                               (fn? dashboard) (dashboard effective-entity)
                               (vector? dashboard) dashboard
                               :else dashboard))
        derived-nav-items (or nav-items
                            (when (seq entity-set)
                              (vec (nav-items-from-set entity-set title-fn))))
        nav-trailing-node (cond
                            (fn? nav-trailing) (nav-trailing effective-entity)
                            (vector? nav-trailing) nav-trailing
                            (some? nav-trailing) nav-trailing
                            :else nil)
        context {:entity effective-entity
                 :entity-spec table-entity-spec
                 :show-add-form? show-add-form?}
        additional-list-props (cond
                                (fn? list-view-props) (or (list-view-props context) {})
                                (map? list-view-props) list-view-props
                                :else {})
        base-list-props {:entity-name effective-entity
                         :entity-spec table-entity-spec
                         :title (title-fn effective-entity)
                         :show-add-form? show-add-form?
                         :set-show-add-form! #(rf/dispatch [:app.template.frontend.events.config/set-show-add-form %])}
        merged-list-props (merge base-list-props additional-list-props)]
    (uix/use-effect
      (fn []
        ;; Sync entity type with URL parameter when applicable
        (when (and url-entity (valid-entity? url-entity) (not= entity-type url-entity))
          (rf/dispatch [::bootstrap-events/set-entity-type url-entity]))
        ;; Fetch entities once specs are available
        (when (and effective-entity table-entity-spec (keyword? effective-entity))
          (rf/dispatch [::crud-events/fetch-entities effective-entity]))
        js/undefined)
      [entity-type url-entity effective-entity table-entity-spec])

    ($ :div {:class (or container-class "container px-4")}
      (when heading
        ($ :h1 {:class "text-4xl font-bold mb-8"} heading))

      (when resolved-summary-items
        ($ summary-card-grid {:items resolved-summary-items}))

      (when (and (not resolved-summary-items) resolved-dashboard)
        resolved-dashboard)

      (when (seq derived-nav-items)
        ($ entity-nav {:items derived-nav-items
                       :active-entity effective-entity
                       :container-class navigation-class
                       :title-fn title-fn
                       :trailing nav-trailing-node}))

      (when table-entity-spec
        ($ list-view merged-list-props)))))
