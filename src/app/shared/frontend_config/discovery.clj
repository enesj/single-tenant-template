(ns app.shared.frontend-config.discovery
  "Config discovery and loading utilities.

  Discovers admin, template-user, and domain config bundles (entities,
  view-options, form-fields, table-columns) and loads them as EDN."
  (:require
    [app.shared.frontend-config.template-user :as template-user]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.string :as str]))

(def ^:private standard-config-files
  {:entities "entities.edn"
   :view-options "view-options.edn"
   :form-fields "form-fields.edn"
  :table-columns "table-columns.edn"
  :navigation "navigation.edn"})

(defn- file-exists?
  [path]
  (.exists (io/file path)))

(defn- canonicalize-name
  [s]
  (some-> s (str/replace "_" "-")))

(defn normalize-id
  "Return a canonical string identifier for keywords/strings.

  Normalizes separators so '_' and '-' are treated as equivalent."
  [x]
  (cond
    (keyword? x) (canonicalize-name (name x))
    (string? x) (canonicalize-name x)
    :else nil))

(defn normalize-entity-id
  [x]
  (normalize-id x))

(defn normalize-field-id
  [x]
  (normalize-id x))

(defn read-edn-file
  [path]
  (try
    (edn/read-string (slurp path))
    (catch Exception e
      (throw (ex-info "Failed to read EDN"
               {:path path
                :cause (.getMessage e)}
               e)))))

(defn discover-domain-names
  "Return domain names under root that contain a config/ directory.

  Options:
  - :only [\"expenses\" ...] include only these domains
  - :skip [\"expenses\" ...] exclude these domains"
  ([root]
   (discover-domain-names root {}))
  ([root {:keys [only skip]}]
   (let [root-dir (io/file root)
         only-set (set (map str only))
         skip-set (set (map str skip))]
     (if-not (.exists root-dir)
       []
       (->> (.listFiles root-dir)
         (filter #(.isDirectory %))
         (map #(.getName %))
         (filter (fn [name]
                   (-> (io/file root-dir name "config") .isDirectory)))
         (filter (fn [name]
                   (or (empty? only-set) (contains? only-set name))))
         (remove (fn [name]
                   (contains? skip-set name)))
         sort
         vec)))))

(defn config-bundles
  "Discover admin + template-user + domain config bundles.

  Options:
  - :admin-root          (default src/app/admin/frontend/config)
  - :template-user-root  (default src/app/template/frontend/config)
  - :domain-root         (default src/app/domain/frontend)
  - :only                (domain allowlist)
  - :skip                (domain denylist)"
  ([] (config-bundles {}))
  ([{:keys [admin-root template-user-root domain-root only skip]}]
   (let [admin-root (or admin-root "src/app/admin/frontend/config")
         template-user-root (or template-user-root template-user/root-dir)
         domain-root (or domain-root "src/app/domain/frontend")
         admin-paths (into {}
                       (for [[kind filename] standard-config-files
                             :let [path (str (io/file admin-root filename))]
                             :when (file-exists? path)]
                         [kind path]))
         admin-bundle (when (seq admin-paths)
                        {:scope :admin
                         :domain nil
                         :paths admin-paths})
         template-user-paths (into {}
                               (for [[kind filename] standard-config-files
                                     :let [path (str (io/file template-user-root filename))]
                                     :when (file-exists? path)]
                                 [kind path]))
         template-user-bundle (when (seq template-user-paths)
                                {:scope :domain
                                 :domain "template"
                                 :paths template-user-paths})
         domains (discover-domain-names domain-root {:only only :skip skip})
         domain-bundles (for [domain domains
                              :let [config-dir (str (io/file domain-root domain "config"))
                                    paths (into {}
                                            (for [[kind filename] standard-config-files
                                                  :let [path (str (io/file config-dir filename))]
                                                  :when (file-exists? path)]
                                              [kind path]))]
                              :when (seq paths)]
                          {:scope :domain
                           :domain domain
                           :paths paths})]
     (vec (concat
            (when admin-bundle [admin-bundle])
            (when template-user-bundle [template-user-bundle])
            domain-bundles)))))

(defn load-bundles
  [bundles]
  (mapv (fn [bundle]
          (assoc bundle :data
            (into {}
              (for [[kind path] (:paths bundle)]
                [kind (read-edn-file path)]))))
    bundles))
