(ns app.domain.backend.expenses.services.cities-places
  "Places API based city inference and confirmation helpers."
  (:require
    [app.domain.backend.expenses.services.cities-normalize :as normalize]
    [app.domain.backend.expenses.services.places-api :as places-api]
    [clojure.string :as str]))

(defn confirm-city-via-places
  "Confirm a city name for ZIP via Places API."
  [zip candidate {:keys [places-cfg user-region] :as _opts}]
  (let [zip* (normalize/normalize-zip zip)
        candidate* (some-> candidate str str/trim not-empty)
        cfg (when (and (map? places-cfg)
                    (seq (:api-key places-cfg)))
              places-cfg)]
    (when (and zip* candidate* cfg)
      (let [query (str candidate* " " zip*)
            search-opts {:region-code (or user-region (:region-code cfg))
                         :language-code (:language-code cfg)
                         :max-results (or (:max-results cfg) 3)
                         :location-bias (:location-bias cfg)
                         :field-mask "places.displayName,places.id,places.formattedAddress,places.addressComponents"}
            places (:places (places-api/search-text! cfg query search-opts))]
        (some (fn [place]
                (let [place-id (some-> place :raw :id str str/trim not-empty)
                      search-components (get-in place [:raw :addressComponents])
                      search-formatted-address (get-in place [:raw :formattedAddress])]
                  (when (seq place-id)
                    (let [details (when-not (seq search-components)
                                    (places-api/get-place-details! cfg place-id))
                          components (or search-components
                                       (:address-components details))
                          formatted-address (or search-formatted-address
                                              (:formatted-address details))
                          place-display-name (or (:name place)
                                               (get-in place [:raw :displayName :text])
                                               (get-in place [:raw :displayName])
                                               (:display-name details))
                          place-city (some-> (normalize/extract-city-from-components components candidate* place-display-name)
                                       str
                                       str/trim
                                       not-empty)
                          place-zip (or
                                      (some-> (places-api/extract-postal-code-from-address-components components)
                                        normalize/normalize-zip)
                                      (some-> (normalize/extract-zip-from-text formatted-address)
                                        normalize/normalize-zip))]
                      (when (and (= zip* place-zip)
                              (normalize/place-city-matches-candidate? candidate* place-city))
                        place-city)))))
          places)))))

(defn infer-city-and-zip-via-places
  "Infer {:city-name <string> :zip <string>} from free text via Places."
  [text {:keys [places-cfg user-region] :as _opts} & {:keys [expected-zip candidate query-text]}]
  (let [text* (some-> text str str/trim not-empty)
        query* (some-> (or query-text text*) str str/trim not-empty)
        expected-zip* (some-> expected-zip normalize/normalize-zip)
        candidate* (some-> candidate str str/trim not-empty)
        cfg (when (and (map? places-cfg)
                    (seq (:api-key places-cfg)))
              places-cfg)
        normalized-text (some-> text* normalize/normalize-city-key)
        normalized-query (some-> query* normalize/normalize-city-key)]
    (when (and text* query* cfg)
      (let [search-opts {:region-code (or user-region (:region-code cfg))
                         :language-code (:language-code cfg)
                         :max-results (or (:max-results cfg) 3)
                         :location-bias (:location-bias cfg)
                         :field-mask "places.displayName,places.id,places.formattedAddress,places.addressComponents"}
            places (:places (places-api/search-text! cfg query* search-opts))]
        (some (fn [place]
                (let [place-id (some-> place :raw :id str str/trim not-empty)
                      search-components (get-in place [:raw :addressComponents])
                      search-formatted-address (get-in place [:raw :formattedAddress])
                      details (when (and (seq place-id) (not (seq search-components)))
                                (places-api/get-place-details! cfg place-id))
                      components (or search-components (:address-components details))
                      formatted-address (or search-formatted-address (:formatted-address details))
                      place-display-name (or (:name place)
                                           (get-in place [:raw :displayName :text])
                                           (get-in place [:raw :displayName])
                                           (:display-name details))
                      city (some-> (normalize/extract-city-from-components components candidate* place-display-name)
                             str
                             str/trim
                             not-empty)
                      zip (or
                            (some-> (places-api/extract-postal-code-from-address-components components)
                              normalize/normalize-zip)
                            (some-> (normalize/extract-zip-from-text formatted-address)
                              normalize/normalize-zip))
                      city-key (some-> city normalize/normalize-city-key)
                      route (when (sequential? components)
                              (some (fn [component]
                                      (when (and (map? component)
                                              (some #{"route"} (:types component)))
                                        (:longText component)))
                                components))
                      route-key (some-> route normalize/normalize-city-key)
                      formatted-key (some-> formatted-address normalize/normalize-city-key)
                      street-allowed? (and (seq normalized-query)
                                        (or
                                          (and (seq route-key)
                                            (str/includes? normalized-query route-key))
                                          (and (seq formatted-key)
                                            (str/includes? normalized-query formatted-key))))
                      city-allowed? (cond
                                      ;; Candidate matches Place city name
                                      (and (seq candidate*) (seq city))
                                      (normalize/place-city-matches-candidate? candidate* city)

                                      ;; City name appears literally in the source text
                                      (and (seq normalized-text) (seq city-key)
                                        (str/includes? normalized-text city-key))
                                      true

                                      ;; Street-confirmed: the route from Places appears in
                                      ;; our query — whether or not we derived an explicit
                                      ;; street-only query string
                                      street-allowed?
                                      true

                                      :else false)
                      zip-allowed? (cond
                                     (and (seq expected-zip*) (seq zip))
                                     (= expected-zip* zip)

                                     (seq expected-zip*)
                                     false

                                     :else
                                     (seq zip))]
                  (when (and (seq city)
                          (seq zip)
                          city-allowed?
                          zip-allowed?)
                    {:city-name city
                     :zip zip})))
          places)))))
