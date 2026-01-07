(ns app.template.backend.routes.admin.settings-io-test
  (:require
    [app.template.backend.routes.admin.settings-io :as settings-io]
    [app.shared.specs.form-fields :as form-fields-spec]
    [app.shared.specs.table-columns :as table-columns-spec]
    [app.shared.specs.view-options :as view-options-spec]
    [clojure.java.io :as io]
    [clojure.test :refer [deftest is testing]]))

(defn- temp-dir []
  (.toFile
    (java.nio.file.Files/createTempDirectory
      "settings-io-test"
      (make-array java.nio.file.attribute.FileAttribute 0))))

(defn- tmp-path [^java.io.File dir filename]
  (.getPath (io/file dir filename)))

(defn- write-edn! [path data]
  (spit path (pr-str data)))

(deftest read-view-options-admin-overrides-domain
  (testing "admin file overrides domain view-options for the same entity"
    (let [dir (temp-dir)
          admin-path (tmp-path dir "admin-view-options.edn")
          domain-path (tmp-path dir "domain-view-options.edn")]
      (write-edn! admin-path {:receipts {:display-locks {:show-edit? true}}})
      (write-edn! domain-path {:receipts {:display-locks {:show-edit? false}}})
      (clojure.core/with-redefs-fn {#'settings-io/view-options-path admin-path
                                    #'settings-io/domain-admin-config-paths [{:view-options domain-path}]
                                    #'view-options-spec/validate-view-options-strict (constantly {:valid? true})}
        (fn []
          (is (= true (get-in (settings-io/read-view-options)
                        [:receipts :display-locks :show-edit?]))))))))

(deftest read-form-fields-admin-overrides-domain
  (testing "admin file overrides domain form-fields for the same entity"
    (let [dir (temp-dir)
          admin-path (tmp-path dir "admin-form-fields.edn")
          domain-path (tmp-path dir "domain-form-fields.edn")]
      (write-edn! admin-path {:receipts {:create [:admin]}})
      (write-edn! domain-path {:receipts {:create [:domain]}})
      (clojure.core/with-redefs-fn {#'settings-io/form-fields-path admin-path
                                    #'settings-io/domain-admin-config-paths [{:form-fields domain-path}]
                                    #'form-fields-spec/validate-form-fields-strict (constantly {:valid? true})}
        (fn []
          (is (= [:admin] (get-in (settings-io/read-form-fields) [:receipts :create]))))))))

(deftest read-table-columns-admin-overrides-domain
  (testing "admin file overrides domain table-columns for the same entity"
    (let [dir (temp-dir)
          admin-path (tmp-path dir "admin-table-columns.edn")
          domain-path (tmp-path dir "domain-table-columns.edn")]
      (write-edn! admin-path {:receipts {:available-columns [:id]}})
      (write-edn! domain-path {:receipts {:available-columns [:domain-id]}})
      (clojure.core/with-redefs-fn {#'settings-io/table-columns-path admin-path
                                    #'settings-io/domain-admin-config-paths [{:table-columns domain-path}]
                                    #'table-columns-spec/validate-table-columns-strict (constantly {:valid? true})}
        (fn []
          (is (= [:id] (get-in (settings-io/read-table-columns)
                         [:receipts :available-columns]))))))))
