(ns app.template.frontend.events.list.filter-serialization-test
  (:require
    [app.template.frontend.events.list.filter-serialization :as filter-serialization]
    [cljs.test :refer [deftest is testing]]))

(deftest flatten-ui-filters-normalizes-ranges-and-selects
  (testing "shared UI filters flatten into backend params"
    (let [created-at-from (js/Date. "2026-04-01T00:00:00.000Z")
          created-at-to (js/Date. "2026-04-30T12:34:56.000Z")]
      (is (= {:status "review_required"
              :supplier-guess "SAMON"
              :created-at-from (.toISOString created-at-from)
              :created-at-to (.toISOString created-at-to)
              :amount-min 10
              :amount-max 25}
            (filter-serialization/flatten-ui-filters
              {:status {:value "review_required" :label "Review required"}
               :supplier-guess "  SAMON  "
               :created-at {:from created-at-from :to created-at-to}
               :amount {:min 10 :max 25}
               :ignored "   "}))))))

(deftest serialize-server-filters-maps-scalars-and-csv-joins-multi-selects
  (testing "server filter serialization uses mapped backend params and preserves ranges"
    (let [created-at-from (js/Date. "2026-04-01T00:00:00.000Z")
          created-at-to (js/Date. "2026-04-30T12:34:56.000Z")]
      (is (= {:status "uploaded,review_required"
              :original-filename "IMG_3885"
              :created-at-from (.toISOString created-at-from)
              :created-at-to (.toISOString created-at-to)}
            (filter-serialization/serialize-server-filters
              {:status [{:value :uploaded :label "Uploaded"}
                        {:value :review_required :label "Review required"}]
               :original-filename " IMG_3885 "
               :created-at {:from created-at-from :to created-at-to}
               :created-by-name "ignored because unmapped"}
              {:status :status
               :original-filename :original-filename}))))))