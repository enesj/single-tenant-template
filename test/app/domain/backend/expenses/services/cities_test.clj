(ns app.domain.backend.expenses.services.cities-test
  "ZIP normalization/extraction/resolution tests for city services."
  (:require
    [app.domain.backend.expenses.services.cities :as cities]
    [app.domain.backend.expenses.services.cities-normalize :as cities-normalize]
    [app.domain.backend.expenses.services.cities-places :as cities-places]
    [app.domain.backend.expenses.services.cities-repository :as cities-repository]
    [app.domain.backend.expenses.services.places-api :as places-api]
    [clojure.test :refer [deftest is testing]])
  (:import
    [java.util UUID]))

(deftest normalize-zip-edge-cases-test
  (testing "standard and spaced ZIP formats"
    (is (= "71000" (cities/normalize-zip "71000")))
    (is (= "71000" (cities/normalize-zip "71 000"))))

  (testing "missing or unsupported ZIP formats"
    (is (nil? (cities/normalize-zip nil)))
    (is (nil? (cities/normalize-zip "")))
    (is (nil? (cities/normalize-zip "1000")))
    (is (nil? (cities/normalize-zip "710001")))))

(deftest extract-zip-from-text-edge-cases-test
  (testing "extracts standard 5-digit ZIP"
    (is (= "71000"
          (cities/extract-zip-from-text "ul. Kolodvorska br.12, 71000 Sarajevo"))))

  (testing "extracts spaced ZIP"
    (is (= "71000"
          (cities/extract-zip-from-text "Vrbanja 1, 71 000 Sarajevo"))))

  (testing "missing ZIP returns nil"
    (is (nil? (cities/extract-zip-from-text "UL. MERHEMIČA TRG BR. 3"))))

  (testing "4-digit ZIP is unsupported"
    (is (nil? (cities/extract-zip-from-text "Ul. Brače Begića broj 1, 1000 Sarajevo"))))

  (testing "malformed multi-digit noise does not produce unsafe ZIP"
    (is (nil? (cities/extract-zip-from-text "Gralicacka 1, 71 100 001 13vo"))))

  (testing "multiple ZIPs are deterministic and prefer the last valid ZIP"
    (is (= "88000"
          (cities/extract-zip-from-text "71000 Sarajevo office, moved to 88000 Mostar")))))

(deftest resolve-city-id-from-text-test
  (testing "ZIP exact match resolves city id and skips fallback lookups"
    (let [zip-id (UUID/randomUUID)
          candidate-called? (atom false)
          prefix-called? (atom false)]
      (with-redefs [cities-repository/find-city-by-country-and-zip (fn [_db country zip]
                                                                     (is (= "Bosnia and Herzegovina" country))
                                                                     (is (= "71000" zip))
                                                                     {:id zip-id})
                    cities-repository/find-city-id-by-candidate (fn [& _]
                                                                  (reset! candidate-called? true)
                                                                  nil)
                    cities-repository/find-city-by-zip-prefix (fn [& _]
                                                                (reset! prefix-called? true)
                                                                nil)]
        (is (= zip-id
              (cities/resolve-city-id-from-text :db "Vrbanja 1, 71 000 Sarajevo Centar")))
        (is (false? @candidate-called?))
        (is (false? @prefix-called?)))))

  (testing "ZIP miss falls back to candidate lookup and then ZIP prefix lookup"
    (let [prefix-id (UUID/randomUUID)
          candidate-calls (atom [])
          prefix-calls (atom [])]
      (with-redefs [cities-repository/find-city-by-country-and-zip (fn [_db country zip]
                                                                     (is (= "Bosnia and Herzegovina" country))
                                                                     (is (= "71120" zip))
                                                                     nil)
                    cities-repository/find-city-id-by-candidate (fn [_db candidate]
                                                                  (swap! candidate-calls conj candidate)
                                                                  nil)
                    cities-repository/find-city-by-zip-prefix (fn [_db zip]
                                                                (swap! prefix-calls conj zip)
                                                                {:id prefix-id})]
        (is (= prefix-id
              (cities/resolve-city-id-from-text
                :db
                "P.J.3 HORECA SHOP I MARKET, MARSALA TITA 7, 71120 Sarajevo Centar")))
        (is (= ["Sarajevo Centar"] @candidate-calls))
        (is (= ["71120"] @prefix-calls)))))

  (testing "when ZIP is absent fallback can resolve existing city by candidate"
    (let [expected-id (UUID/randomUUID)
          fallback-calls (atom [])
          zip-called? (atom false)]
      (with-redefs [cities-repository/find-city-by-country-and-zip (fn [& _]
                                                                     (reset! zip-called? true)
                                                                     nil)
                    cities-repository/find-city-id-by-candidate (fn [_db candidate]
                                                                  (swap! fallback-calls conj candidate)
                                                                  (when (= "Sarajevo Centar" candidate)
                                                                    expected-id))]
        (is (= expected-id
              (cities/resolve-city-id-from-text
                :db
                "P.J.3 HORECA SHOP I MARKET, MARSALA TITA 7, Sarajevo Centar")))
        (is (false? @zip-called?))
        (is (= ["Sarajevo Centar"] @fallback-calls))))))

(deftest resolve-city-id-from-text-bang-test
  (testing "ZIP hit resolves existing city and skips Places confirmation"
    (let [zip-id (UUID/randomUUID)
          places-called? (atom false)]
      (with-redefs [cities-repository/find-city-by-country-and-zip (fn [_db _country _zip]
                                                                     {:id zip-id})
                    cities-places/confirm-city-via-places (fn [& _]
                                                            (reset! places-called? true)
                                                            "Sarajevo")]
        (is (= zip-id
              (cities/resolve-city-id-from-text! :db "Vrbanja 1, 71000 Sarajevo" {:places-cfg {:api-key "x"}})))
        (is (false? @places-called?)))))

  (testing "ZIP miss with city candidate can confirm via Places and create/resolve city row"
    (let [created-id (UUID/randomUUID)
          ensured (atom nil)]
      (with-redefs [cities-repository/find-city-by-country-and-zip (fn [& _] nil)
                    cities-repository/find-city-id-by-candidate (fn [& _] nil)
                    cities-repository/find-city-by-zip-prefix (fn [& _] nil)
                    cities-normalize/extract-city-fallback-candidate (fn [_] "Sarajevo Centar")
                    cities-places/confirm-city-via-places (fn [zip candidate opts]
                                                            (is (= "71000" zip))
                                                            (is (= "Sarajevo Centar" candidate))
                                                            (is (= {:api-key "x"} (:places-cfg opts)))
                                                            "Sarajevo")
                    cities-repository/ensure-city-by-country-and-zip! (fn [_db country zip city-name]
                                                                        (reset! ensured [country zip city-name])
                                                                        created-id)]
        (is (= created-id
              (cities/resolve-city-id-from-text!
                :db
                "Bosnia and Herzegovina"
                "Marsala Tita 7, 71000 Sarajevo Centar"
                {:places-cfg {:api-key "x"}})))
        (is (= ["Bosnia and Herzegovina" "71000" "Sarajevo"] @ensured)))))

  (testing "ZIP miss without Places confirmation does not create city"
    (let [ensure-called? (atom false)]
      (with-redefs [cities-repository/find-city-by-country-and-zip (fn [& _] nil)
                    cities-repository/find-city-id-by-candidate (fn [& _] nil)
                    cities-repository/find-city-by-zip-prefix (fn [& _] nil)
                    cities-normalize/extract-city-fallback-candidate (fn [_] "Sarajevo Centar")
                    cities-places/confirm-city-via-places (fn [& _] nil)
                    cities-places/infer-city-and-zip-via-places (fn [& _] nil)
                    cities-repository/ensure-city-by-country-and-zip! (fn [& _]
                                                                        (reset! ensure-called? true)
                                                                        (UUID/randomUUID))]
        (is (nil?
              (cities/resolve-city-id-from-text!
                :db
                "Marsala Tita 7, 71000 Sarajevo Centar"
                {:places-cfg {:api-key "x"}})))
        (is (false? @ensure-called?)))))

  (testing "ZIP missing keeps candidate lookup behavior before Places inference"
    (let [expected-id (UUID/randomUUID)
          places-called? (atom false)]
      (with-redefs [cities-repository/find-city-id-by-candidate (fn [_db candidate]
                                                                  (is (= "SARAJEVO CENTAR" candidate))
                                                                  expected-id)
                    cities-places/infer-city-and-zip-via-places (fn [& _]
                                                                  (reset! places-called? true)
                                                                  nil)]
        (is (= expected-id
              (cities/resolve-city-id-from-text! :db "MARSALA TITA 7, SARAJEVO CENTAR" {:places-cfg {:api-key "x"}})))
        (is (false? @places-called?)))))

  (testing "ZIP missing + candidate not found can infer via Places and create city"
    (let [created-id (UUID/randomUUID)
          ensured (atom nil)]
      (with-redefs [cities-normalize/extract-city-fallback-candidate (fn [_] "Istočna lildža")
                    cities-repository/find-city-id-by-candidate (fn [& _] nil)
                    cities-places/infer-city-and-zip-via-places (fn [text opts & {:keys [candidate]}]
                                                                  (is (= "Ulica vojvode Radomira Putnika do broja 8, Istočna lildža" text))
                                                                  (is (= "Istočna lildža" candidate))
                                                                  (is (= {:api-key "x"} (:places-cfg opts)))
                                                                  {:zip "71210"
                                                                   :city-name "Istočna Ilidža"})
                    cities-repository/ensure-city-by-country-and-zip! (fn [_db country zip city-name]
                                                                        (reset! ensured [country zip city-name])
                                                                        created-id)]
        (is (= created-id
              (cities/resolve-city-id-from-text!
                :db
                "Ulica vojvode Radomira Putnika do broja 8, Istočna lildža"
                {:places-cfg {:api-key "x"}})))
        (is (= ["Bosnia and Herzegovina" "71210" "Istočna Ilidža"] @ensured)))))

  (testing "ZIP missing + no candidate can infer via Places (city must appear in text)"
    (let [created-id (UUID/randomUUID)
          ensured (atom nil)]
      (with-redefs [cities-normalize/extract-city-fallback-candidate (fn [_] nil)
                    cities-places/infer-city-and-zip-via-places (fn [text opts & _]
                                                                  (is (= "PODRUŽNICA SARAJEVO BROJ 126\nUL. MERHEMIČA TRG BR. 3" text))
                                                                  (is (= {:api-key "x"} (:places-cfg opts)))
                                                                  {:zip "71000"
                                                                   :city-name "Sarajevo"})
                    cities-repository/ensure-city-by-country-and-zip! (fn [_db country zip city-name]
                                                                        (reset! ensured [country zip city-name])
                                                                        created-id)]
        (is (= created-id
              (cities/resolve-city-id-from-text!
                :db
                "PODRUŽNICA SARAJEVO BROJ 126\nUL. MERHEMIČA TRG BR. 3"
                {:places-cfg {:api-key "x"}})))
        (is (= ["Bosnia and Herzegovina" "71000" "Sarajevo"] @ensured))))))

(deftest confirm-city-via-places-prefers-search-components-test
  (testing "search result addressComponents are used directly without details call"
    (let [details-calls (atom 0)
          captured-search-opts (atom nil)
          result (with-redefs [places-api/search-text! (fn [_cfg query opts]
                                                         (reset! captured-search-opts opts)
                                                         (is (= "Sarajevo Centar 71000" query))
                                                         {:places [{:name "Bingo"
                                                                    :raw {:id "place-1"
                                                                          :formattedAddress "Zmaja od Bosne 1, 71000 Sarajevo, Bosnia and Herzegovina"
                                                                          :addressComponents [{:longText "Sarajevo"
                                                                                               :types ["locality" "political"]}
                                                                                              {:longText "71000"
                                                                                               :types ["postal_code"]}]}}]})
                               places-api/get-place-details! (fn [& _]
                                                               (swap! details-calls inc)
                                                               nil)]
                   (#'cities-places/confirm-city-via-places
                    "71000"
                    "Sarajevo Centar"
                    {:places-cfg {:api-key "x" :region-code "BA"}}))]
      (is (= "Sarajevo" result))
      (is (zero? @details-calls))
      (is (= "places.displayName,places.id,places.formattedAddress,places.addressComponents"
            (:field-mask @captured-search-opts)))))

  (testing "missing search result addressComponents falls back to get-place-details!"
    (let [details-calls (atom [])
          result (with-redefs [places-api/search-text! (fn [& _]
                                                         {:places [{:name "Bingo"
                                                                    :raw {:id "place-2"
                                                                          :formattedAddress "Zmaja od Bosne 1, 71000 Sarajevo, Bosnia and Herzegovina"}}]})
                               places-api/get-place-details! (fn [_cfg place-id]
                                                               (swap! details-calls conj place-id)
                                                               {:id place-id
                                                                :display-name "Bingo Sarajevo"
                                                                :formatted-address "Zmaja od Bosne 1, 71000 Sarajevo, Bosnia and Herzegovina"
                                                                :address-components [{:longText "Sarajevo"
                                                                                      :types ["locality" "political"]}
                                                                                     {:longText "71000"
                                                                                      :types ["postal_code"]}]})]
                   (#'cities-places/confirm-city-via-places
                    "71000"
                    "Sarajevo Centar"
                    {:places-cfg {:api-key "x" :region-code "BA"}}))]
      (is (= "Sarajevo" result))
      (is (= ["place-2"] @details-calls)))))

(deftest infer-city-and-zip-via-places-display-name-fallback-test
  (testing "uses displayName-derived city when addressComponents lacks locality"
    (let [result (with-redefs [places-api/search-text! (fn [_cfg query opts]
                                                         (is (= "Ulica vojvode Radomira Putnika 8, Istočna lildža" query))
                                                         (is (= "places.displayName,places.id,places.formattedAddress,places.addressComponents"
                                                               (:field-mask opts)))
                                                         {:places [{:name "Opština Istočna Ilidža / Општина Источна Илиџа"
                                                                    :raw {:id "ChIJKQXyUsLJWEcRhIwbe-oAVuo"
                                                                          :formattedAddress "Vojvode Radomira Putnika 2, 71123"
                                                                          :addressComponents [{:longText "2"
                                                                                               :shortText "2"
                                                                                               :types ["street_number"]}
                                                                                              {:longText "Vojvode Radomira Putnika"
                                                                                               :shortText "Vojvode Radomira Putnika"
                                                                                               :types ["route"]}
                                                                                              {:longText "Lukavica"
                                                                                               :shortText "Lukavica"
                                                                                               :types ["sublocality_level_1" "sublocality" "political"]}
                                                                                              {:longText "Republika Srpska"
                                                                                               :shortText "Republika Srpska"
                                                                                               :types ["administrative_area_level_1" "political"]}
                                                                                              {:longText "Bosna i Hercegovina"
                                                                                               :shortText "BA"
                                                                                               :types ["country" "political"]}
                                                                                              {:longText "71123"
                                                                                               :shortText "71123"
                                                                                               :types ["postal_code"]}]}}]})]
                   (#'cities-places/infer-city-and-zip-via-places
                    "Ulica vojvode Radomira Putnika do broja 8, Istočna lildža"
                    {:places-cfg {:api-key "x" :region-code "BA"}}
                    :candidate "Istočna lildža"
                    :query-text "Ulica vojvode Radomira Putnika 8, Istočna lildža"))]
      (is (= {:zip "71123"
              :city-name "Istočna Ilidža"}
            result)))))