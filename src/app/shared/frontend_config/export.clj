(ns app.shared.frontend-config.export
  "Helpers for promoting DB-backed frontend config snapshots back into
  source-controlled EDN defaults."
  (:require
    [app.shared.frontend-config.discovery :as discovery]
    [app.shared.specs.entities :as entities-spec]
    [app.shared.specs.form-fields :as form-fields-spec]
    [app.shared.specs.table-columns :as table-columns-spec]
    [app.shared.specs.view-options :as view-options-spec]))

(def ^:private admin-kinds-list
  [:view-options :form-fields :table-columns :navigation])

(def ^:private user-kinds-list
  [:entities :view-options :form-fields :table-columns :navigation])

(defn admin-kinds []
  admin-kinds-list)

(defn user-kinds []
  user-kinds-list)

(defn bundle-key
  [{:keys [scope domain]}]
  (cond
    (= scope :admin) :admin
    (= domain "template") :template
    domain (keyword domain)
    :else :unknown))

(defn relevant-user-bundles
  [bundles kind]
  (->> bundles
    (filter #(= :domain (:scope %)))
    (filter #(contains? (:paths %) kind))
    vec))

(defn ownership-map
  "Return canonical entity-id -> bundle-key.

  Later bundles override earlier ones so domain bundles take precedence over the
  template bundle when both define the same entity."
  [bundles kind]
  (reduce
    (fn [acc bundle]
      (reduce
        (fn [inner entity-key]
          (assoc inner
            (discovery/normalize-entity-id entity-key)
            (bundle-key bundle)))
        acc
        (keys (get-in bundle [:data kind] {}))))
    {}
    bundles))

(defn default-user-owner
  "Default owner for runtime entries not present in any source EDN.

  Prefer the first non-template domain bundle when available so new user-facing
  entities land in the primary domain config rather than the template bundle."
  [bundles]
  (or (some (fn [bundle]
              (when (and (= :domain (:scope bundle))
                      (not= "template" (:domain bundle)))
                (bundle-key bundle)))
        bundles)
    :template))

(defn split-user-runtime-data
  "Split a merged user runtime snapshot back into template/domain-owned maps."
  [bundles kind runtime-data]
  (let [bundles (relevant-user-bundles bundles kind)
        initial (into {}
                  (map (fn [bundle]
                         [(bundle-key bundle) {}])
                    bundles))
        owners (ownership-map bundles kind)
        fallback-owner (default-user-owner bundles)]
    (reduce-kv
      (fn [acc entity-key entity-config]
        (let [owner (get owners
                      (discovery/normalize-entity-id entity-key)
                      fallback-owner)]
          (update acc owner assoc entity-key entity-config)))
      initial
      (or runtime-data {}))))

(defn validate-export-data
  [scope kind data]
  (case [scope kind]
    [:admin :view-options] (view-options-spec/validate-view-options-strict data)
    [:admin :form-fields] (form-fields-spec/validate-form-fields-strict data)
    [:admin :table-columns] (table-columns-spec/validate-table-columns-strict data)
    [:admin :navigation] {:valid? true}
    [:domain :entities] (entities-spec/validate-user-entities data)
    [:domain :view-options] (view-options-spec/validate-view-options-strict data)
    [:domain :form-fields] (form-fields-spec/validate-form-fields-strict data)
    [:domain :table-columns] (table-columns-spec/validate-table-columns-strict data)
    [:domain :navigation] {:valid? true}
    {:valid? false
     :errors [(str "Unsupported export target: " [scope kind])] }))

(defn export-plan
  "Build an export plan from runtime snapshots and discovered bundles.

  `runtime-config` shape:
    {:admin {:view-options ... :form-fields ... :table-columns ... :navigation ...}
     :user  {:entities ... :view-options ... :form-fields ... :table-columns ... :navigation ...}}"
  [bundles runtime-config]
  (let [bundles-by-key (into {}
                         (map (fn [bundle]
                                [(bundle-key bundle) bundle]))
                         bundles)
        admin-bundle (get bundles-by-key :admin)
        admin-plans (for [kind admin-kinds-list
                          :let [path (get-in admin-bundle [:paths kind])]
                          :when path
                          :let [data (get-in runtime-config [:admin kind] {})
                                validation (validate-export-data :admin kind data)]]
                      {:scope :admin
                       :domain nil
                       :kind kind
                       :path path
                       :data data
                       :validation validation})
        split-user (into {}
                     (map (fn [kind]
                            [kind (split-user-runtime-data bundles kind (get-in runtime-config [:user kind] {}))]))
                     user-kinds-list)
        user-plans (for [kind user-kinds-list
                         bundle (relevant-user-bundles bundles kind)
                         :let [target-key (bundle-key bundle)
                               path (get-in bundle [:paths kind])
                               data (get-in split-user [kind target-key] {})
                               validation (validate-export-data :domain kind data)]]
                     {:scope :domain
                      :domain (:domain bundle)
                      :kind kind
                      :path path
                      :data data
                      :validation validation})]
    (vec (concat admin-plans user-plans))))
