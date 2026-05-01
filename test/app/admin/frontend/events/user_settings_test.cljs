(ns app.admin.frontend.events.user-settings-test
  (:require
    [app.admin.frontend.events.user-settings :as user-settings]
    [app.admin.frontend.test-setup :as setup]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(defn- example-response
  ([] (example-response {}))
  ([overrides]
   (merge
     {:entities {:expenses {:title "My Expenses"}}
      :view-options {:expenses {:display-defaults {:show-edit? true}
                                :display-locks {:show-delete? false}
                                :column-defaults {:notes false}
                                :column-locks {:purchased_at true}}}
      :form-fields {:expenses {:create-fields [:purchased_at]
                               :edit-fields [:purchased_at]}}
      :table-columns {:expenses {:available-columns [:purchased_at :notes]
                                 :default-visible-columns [:purchased_at]}}}
     overrides)))

(deftest init-triggers-load-request
  (testing "::init triggers GET to user-ui-config endpoint"
    (setup/reset-db!)
    (setup/install-http-stub!)

    (rf/dispatch-sync [::user-settings/init])

    (let [req (setup/last-http-request)
          db @rf-db/app-db]
      (is (= :get (:method req)))
      (is (= "/admin/api/settings/user-ui-config" (:uri req)))
      (is (true? (get-in db [:admin :user-settings :loading?])))
      (is (= "view-options" (get-in db [:admin :user-settings :tab]))))))

(deftest load-success-populates-domain-config-and-editor-state
  (testing "::load-success syncs [:domain :config] and editor draft/saved"
    (setup/reset-db!)
    (setup/install-http-stub!)

    (rf/dispatch-sync [::user-settings/init])
    (setup/respond-success! (example-response))

    (let [db @rf-db/app-db]
      (is (= {:expenses {:title "My Expenses"}}
            (get-in db [:domain :config :entities])))
      (is (= {:expenses {:display-defaults {:show-edit? true}
                         :display-locks {:show-delete? false}
                         :column-defaults {:notes false}
                         :column-locks {:purchased_at true}}}
            (get-in db [:domain :config :view-options])))
      (is (= {:expenses {:available-columns [:purchased_at :notes]
                         :default-visible-columns [:purchased_at]}}
            (get-in db [:domain :config :table-columns])))

      (is (= (get-in db [:admin :user-settings :draft])
            (get-in db [:admin :user-settings :saved])))
      (is (false? (get-in db [:admin :user-settings :loading?] false)))
      (is (nil? (get-in db [:admin :user-settings :error]))))))

(deftest display-setting-draft-set-and-clear
  (testing "::set-display-setting-draft updates :display-defaults/:display-locks in draft"
    (setup/reset-db!)
    (setup/install-http-stub!)

    (rf/dispatch-sync [::user-settings/init])
    (setup/respond-success! (example-response))

    (rf/dispatch-sync [::user-settings/set-display-setting-draft
                       :expenses
                       :show-edit?
                       {:kind :default :value false}])
    (is (= false (get-in @rf-db/app-db
                   [:admin :user-settings :draft :view-options :expenses :display-defaults :show-edit?])))

    ;; inherit means remove from both defaults and locks
    (rf/dispatch-sync [::user-settings/set-display-setting-draft
                       :expenses
                       :show-edit?
                       {:kind :inherit}])
    (is (nil? (get-in @rf-db/app-db
                [:admin :user-settings :draft :view-options :expenses :display-defaults :show-edit?])))
    (is (nil? (get-in @rf-db/app-db
                [:admin :user-settings :draft :view-options :expenses :display-locks :show-edit?])))))

(deftest column-visibility-setting-draft-set-and-clear
  (testing "::set-column-visibility-setting-draft updates :column-defaults/:column-locks in draft"
    (setup/reset-db!)
    (setup/install-http-stub!)

    (rf/dispatch-sync [::user-settings/init])
    (setup/respond-success! (example-response))

    ;; Set a lock to false (hidden)
    (rf/dispatch-sync [::user-settings/set-column-visibility-setting-draft
                       :expenses
                       :notes
                       {:kind :lock :value false}])
    (is (= false (get-in @rf-db/app-db
                   [:admin :user-settings :draft :view-options :expenses :column-locks :notes])))

    ;; inherit means remove from both defaults and locks
    (rf/dispatch-sync [::user-settings/set-column-visibility-setting-draft
                       :expenses
                       :notes
                       {:kind :inherit}])
    (is (nil? (get-in @rf-db/app-db
                [:admin :user-settings :draft :view-options :expenses :column-defaults :notes])))
    (is (nil? (get-in @rf-db/app-db
                [:admin :user-settings :draft :view-options :expenses :column-locks :notes])))))

(deftest toggle-column-visibility-updates-default-visible-columns
  (testing "::toggle-column-visibility-draft edits :default-visible-columns"
    (setup/reset-db!)
    (setup/install-http-stub!)

    (rf/dispatch-sync [::user-settings/init])
    (setup/respond-success! (example-response))

    ;; Hide :purchased_at too
    (rf/dispatch-sync [::user-settings/toggle-column-visibility-draft :expenses :purchased_at])
    (is (= []
          (get-in @rf-db/app-db [:admin :user-settings :draft :table-columns :expenses :default-visible-columns])))

    ;; Unhide :notes
    (rf/dispatch-sync [::user-settings/toggle-column-visibility-draft :expenses :notes])
    (is (= [:notes]
          (get-in @rf-db/app-db [:admin :user-settings :draft :table-columns :expenses :default-visible-columns])))))

(deftest removing-available-column-prunes-dependent-lists-and-maps
  (testing "Changing :available-columns prunes dependent table-columns config"
    (setup/reset-db!)
    (setup/install-http-stub!)

    (rf/dispatch-sync [::user-settings/init])
    (setup/respond-success!
      (example-response
        {:table-columns
         {:expenses {:available-columns [:purchased_at :notes :supplier_display_name]
                     :default-visible-columns [:purchased_at :supplier_display_name]
                     :filterable-columns [:notes :supplier_display_name]
                     :sortable-columns [:supplier_display_name]
                     :always-visible [:purchased_at]
                     :computed-fields {:supplier_display_name {:type "join"}}
                     :column-config {:supplier_display_name {:width "100px"}
                                     :notes {:width "80px"}}
                     :column-metadata {:supplier_display_name {:label "Supplier"}
                                       :notes {:label "Notes"}}}}}))

    ;; Remove supplier_display_name from :available-columns
    (rf/dispatch-sync [::user-settings/toggle-table-column-in-list-draft
                       :expenses :available-columns :supplier_display_name])

    (let [cfg (get-in @rf-db/app-db [:admin :user-settings :draft :table-columns :expenses])]
      (is (= ["purchased_at" "notes"] (:available-columns cfg)))
      (is (= ["purchased_at"] (:default-visible-columns cfg)))
      (is (= ["notes"] (:filterable-columns cfg)))
      (is (= [] (:sortable-columns cfg)))
      (is (= ["purchased_at"] (:always-visible cfg)))
      (is (= {} (:computed-fields cfg)))
      (is (= {:notes {:width "80px"}} (:column-config cfg)))
      (is (= {:notes {:label "Notes"}} (:column-metadata cfg))))))

(deftest setting-available-columns-prunes-dependent-lists-and-maps
  (testing "::set-table-column-list-draft cleans up when list-type is :available-columns"
    (setup/reset-db!)
    (setup/install-http-stub!)

    (rf/dispatch-sync [::user-settings/init])
    (setup/respond-success!
      (example-response
        {:table-columns
         {:expenses {:available-columns [:purchased_at :notes]
                     :default-visible-columns [:purchased_at :notes]
                     :filterable-columns [:notes]
                     :sortable-columns [:purchased_at]
                     :always-visible [:purchased_at]
                     :computed-fields {:notes {:type "noop"}}
                     :column-config {:notes {:width "80px"}
                                     :purchased_at {:width "120px"}}
                     :column-metadata {:notes {:label "Notes"}
                                       :purchased_at {:label "Purchased"}}}}}))

    (rf/dispatch-sync [::user-settings/set-table-column-list-draft
                       :expenses :available-columns [:notes]])

    (let [cfg (get-in @rf-db/app-db [:admin :user-settings :draft :table-columns :expenses])]
      (is (= ["notes"] (:available-columns cfg)))
      (is (= ["notes"] (:default-visible-columns cfg)))
      (is (= ["notes"] (:filterable-columns cfg)))
      (is (= [] (:sortable-columns cfg)))
      (is (= [] (:always-visible cfg)))
      (is (= {:notes {:type "noop"}} (:computed-fields cfg)))
      (is (= {:notes {:width "80px"}} (:column-config cfg)))
      (is (= {:notes {:label "Notes"}} (:column-metadata cfg))))))

(deftest table-column-label-draft-set-and-clear
  (testing "::set-table-column-label-draft updates labels without dropping existing metadata"
    (setup/reset-db!)
    (setup/install-http-stub!)

    (rf/dispatch-sync [::user-settings/init])
    (setup/respond-success!
      (example-response
        {:table-columns {:expenses {:available-columns [:purchased_at :notes]
                                    :default-visible-columns [:purchased_at]
                                    :column-metadata {:purchased_at {:label-key :common/purchased-at}}}}}))

    (rf/dispatch-sync [::user-settings/set-table-column-label-draft
                       :expenses
                       :purchased_at
                       "Purchased"])
    (is (= {:label-key :common/purchased-at
            :label "Purchased"}
          (get-in @rf-db/app-db [:admin :user-settings :draft :table-columns :expenses :column-metadata "purchased_at"])))

    (rf/dispatch-sync [::user-settings/set-table-column-label-draft
                       :expenses
                       :purchased_at
                       "   "])
    (is (= {:label-key :common/purchased-at}
          (get-in @rf-db/app-db [:admin :user-settings :draft :table-columns :expenses :column-metadata "purchased_at"])))

    (rf/dispatch-sync [::user-settings/set-table-column-label-draft
                       :expenses
                       :purchased_at
                       "Purchased"])
    (rf/dispatch-sync [::user-settings/set-table-column-label-draft
                       :expenses
                       :purchased_at
                       nil])
    (is (= {:label-key :common/purchased-at}
          (get-in @rf-db/app-db [:admin :user-settings :draft :table-columns :expenses :column-metadata "purchased_at"])))))

(deftest save-sends-put-and-updates-state-on-success
  (testing "::save sends PUT and syncs state on success"
    (setup/reset-db!)
    (setup/install-http-stub!)

    (rf/dispatch-sync [::user-settings/init])
    (setup/respond-success! (example-response))

    (rf/dispatch-sync [::user-settings/set-display-setting-draft
                       :expenses
                       :show-edit?
                       {:kind :default :value false}])
    (rf/dispatch-sync [::user-settings/save])

    (let [req (setup/last-http-request)
          payload (:params req)]
      (is (= :put (:method req)))
      (is (= "/admin/api/settings/user-ui-config" (:uri req)))
      (is (= #{:entities :view-options :form-fields :table-columns :navigation}
            (set (keys payload))))
      (is (= false (get-in payload [:view-options :expenses :display-defaults :show-edit?]))))

    ;; Simulate backend returning the updated config
    (setup/respond-success!
      (example-response
        {:view-options {:expenses {:display-defaults {:show-edit? false}
                                   :display-locks {:show-delete? false}
                                   :column-defaults {:notes false}
                                   :column-locks {:purchased_at true}}}}))

    (let [db @rf-db/app-db]
      (is (= false (get-in db [:domain :config :view-options :expenses :display-defaults :show-edit?])))
      (is (false? (get-in db [:admin :user-settings :saving?] false)))
      (is (number? (get-in db [:admin :user-settings :last-saved])))
      (is (= (get-in db [:admin :user-settings :draft])
            (get-in db [:admin :user-settings :saved]))))))
