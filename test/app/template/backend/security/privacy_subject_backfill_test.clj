(ns app.template.backend.security.privacy-subject-backfill-test
  (:require
    [app.template.backend.security.privacy-subject :as subject]
    [app.template.backend.security.privacy-subject-backfill :as sut]
    [clojure.test :refer [deftest is testing]])
  (:import
    [java.util UUID]))

(def explicit-key-b64
  "BQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQU=")

(defn- with-env
  [env f]
  (with-redefs-fn {#'subject/getenv* (fn [k] (get env k))}
    f))

(deftest row-updates-backfill-missing-subject-refs
  (with-env {"AERO_PROFILE" "prod"
             "PRIVACY_SUBJECT_KEY_B64" explicit-key-b64}
    #(let [user-id (UUID/randomUUID)
           creator-id (UUID/randomUUID)
           updates (#'sut/row-updates
                    [{:legacy :user_id :subject :subject_ref}
                     {:legacy :created_by :subject :created_by_subject_ref}]
                    {:user_id user-id
                     :subject_ref nil
                     :created_by creator-id
                     :created_by_subject_ref nil}
                    {:cutover? false})]
       (is (re-matches #"[0-9a-f]{64}" (:subject_ref updates)))
       (is (re-matches #"[0-9a-f]{64}" (:created_by_subject_ref updates)))
       (is (not (contains? updates :user_id)))
       (is (not (contains? updates :created_by))))))

(deftest row-updates-cutover-nulls-direct-links-after-subject-exists
  (with-env {"AERO_PROFILE" "prod"
             "PRIVACY_SUBJECT_KEY_B64" explicit-key-b64}
    #(let [user-id (UUID/randomUUID)
           creator-id (UUID/randomUUID)
           existing-subject (subject/user-subject-ref user-id)
           updates (#'sut/row-updates
                    [{:legacy :user_id :subject :subject_ref}
                     {:legacy :created_by :subject :created_by_subject_ref}]
                    {:user_id user-id
                     :subject_ref existing-subject
                     :created_by creator-id
                     :created_by_subject_ref nil}
                    {:cutover? true})]
       (is (not (contains? updates :subject_ref)) "existing subject ref is preserved by not re-setting it")
       (is (re-matches #"[0-9a-f]{64}" (:created_by_subject_ref updates)))
       (is (contains? updates :user_id))
       (is (nil? (:user_id updates)))
       (is (contains? updates :created_by))
       (is (nil? (:created_by updates))))))

(deftest merged-settings-fields-prefers-existing-subject-row
  (testing "target row values win, with legacy values filling only blanks"
    (let [legacy-payer (UUID/randomUUID)
          existing-payer (UUID/randomUUID)]
      (is (= {:default_payer_id existing-payer
              :receipt_ocr_provider "mistral"
              :updated_at [:now]}
            (#'sut/merged-settings-fields
             {:default_payer_id existing-payer
              :receipt_ocr_provider "mistral"}
             {:default_payer_id legacy-payer
              :receipt_ocr_provider "other"})))
      (is (= {:default_payer_id legacy-payer
              :receipt_ocr_provider "other"
              :updated_at [:now]}
            (#'sut/merged-settings-fields
             {:default_payer_id nil
              :receipt_ocr_provider nil}
             {:default_payer_id legacy-payer
              :receipt_ocr_provider "other"}))))))
