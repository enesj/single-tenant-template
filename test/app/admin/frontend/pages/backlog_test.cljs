(ns app.admin.frontend.pages.backlog-test
  (:require
    [app.admin.frontend.components.generic-admin-entity-page :as generic-page]
    [app.admin.frontend.pages.backlog :as backlog]
    [app.template.frontend.utils.test-utils :as test-utils]
    [cljs.test :refer [deftest is testing]]
    [clojure.string :as str]
    [uix.core :refer [$]]))

(test-utils/setup-test-environment!)

(deftest admin-backlog-page-renders-generic-admin-entity-page
  (testing "admin backlog page renders generic entity page with :backlog key and modal form display override"
    (let [captured-page-arg (atom nil)]
      (with-redefs [generic-page/generic-admin-entity-page
                    (fn [page-arg]
                      (reset! captured-page-arg page-arg)
                      ($ :div {:id "backlog-generic-admin-entity-page-stub"}
                        (str page-arg)))]
        (let [markup (test-utils/render-to-static-markup ($ backlog/admin-backlog-page))
              resolved-page-arg (let [page-arg @captured-page-arg]
                                  (cond
                                    (map? page-arg) page-arg
                                    (keyword? page-arg) {:children page-arg}
                                    :else (js->clj page-arg :keywordize-keys true)))
              resolved-entity-key (let [entity-key (:children resolved-page-arg)]
                                    (cond
                                      (keyword? entity-key) entity-key
                                      (string? entity-key) (keyword entity-key)
                                      :else entity-key))
              resolved-form-display (let [form-display (or (get-in resolved-page-arg [:list-overrides :form-display])
                                                         (get-in resolved-page-arg [:listOverrides :form-display])
                                                         (get-in resolved-page-arg [:listOverrides :formDisplay]))]
                                      (cond
                                        (keyword? form-display) form-display
                                        (string? form-display) (keyword form-display)
                                        :else form-display))]
          (is (= :backlog resolved-entity-key))
          (is (= :modal resolved-form-display))
          (is (str/includes? markup "backlog-generic-admin-entity-page-stub")))))))
