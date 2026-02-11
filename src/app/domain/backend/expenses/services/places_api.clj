(ns app.domain.backend.expenses.services.places-api
  "Google Places API v1 helpers for supplier canonicalization."
  (:require
    [cheshire.core :as json]
    [clj-http.client :as http]
    [clojure.string :as str]
    [taoensso.timbre :as log]))

(def ^:private default-base-url "https://places.googleapis.com/v1/places:searchText")
(def ^:private default-timeout-ms 3000)
(def ^:private default-max-results 5)

(defn build-config
  "Build Places config from the app config map."
  [app-config]
  (let [cfg (or (:places app-config) {})]
    {:api-key (:api-key cfg)
     :base-url (or (:base-url cfg) default-base-url)
     :region-code (:region-code cfg)
     :language-code (:language-code cfg)
     :timeout-ms (or (:timeout-ms cfg) default-timeout-ms)
     :max-results (or (:max-results cfg) default-max-results)
     :location-bias (:location-bias cfg)
     :currency-region-map (:currency-region-map cfg)}))

(defn- safe-body-snippet [s]
  (when (string? s)
    (let [s (str/replace s #"\s+" " ")]
      (if (> (count s) 1000)
        (str (subs s 0 1000) "…")
        s))))

(defn- parse-json-body [body]
  (when (and (string? body) (not (str/blank? body)))
    (try
      (json/parse-string body true)
      (catch Exception _
        nil))))

(defn- parsed-error-info [parsed-body]
  (let [err (:error parsed-body)
        first-detail (first (:details err))]
    (when (map? err)
      {:error-status (:status err)
       :error-message (:message err)
       :error-reason (:reason first-detail)})))

(defn- location-bias->request [location-bias]
  (when-let [circle (:circle location-bias)]
    (let [{:keys [lat lng radius-m]} circle]
      (when (and (number? lat) (number? lng) (number? radius-m))
        {:circle {:center {:latitude lat :longitude lng}
                  :radius radius-m}}))))

(defn http-post!
  "Wrapper around clj-http POST. Kept as a var for tests."
  [url opts]
  (http/post url opts))

(defn- response->error [message {:keys [status body] :as resp} parsed-body]
  (merge
    {:type :places/http-error
     :message message
     :status status
     :body-snippet (safe-body-snippet body)
     :response (select-keys resp [:status :headers :reason-phrase])}
    (parsed-error-info parsed-body)))

(defn- redact-api-key
  "Return a redacted version of the API key for logging (first 10 + last 10 chars).
	 WARNING: Set timbre level to :info or higher in production to avoid leaking full key."
  [api-key]
  (when (seq api-key)
    (let [s (str api-key)
          len (count s)]
      (if (> len 20)
        (str (subs s 0 10) "..." (subs s (- len 10)))
        (str (repeat len "*"))))))

(defn search-text!
  "Call Places API v1 (places:searchText).

  opts:
  - :region-code (string, optional)
  - :language-code (string, optional)
  - :location-bias (map, optional)
  - :max-results (int, optional)
  - :field-mask (string, optional). Example:
    \"places.displayName,places.id,places.formattedAddress\"

  Returns {:places [{:name \"Bingo\" :raw <place>} ...] :error nil} on success.
  Returns {:places [] :error {:type ...}} on failure."
  [cfg text {:keys [region-code language-code location-bias max-results field-mask] :as _opts}]
  (let [api-key (:api-key cfg)
        text* (some-> text str str/trim not-empty)
        field-mask* (or (some-> field-mask str str/trim not-empty)
                      "places.displayName,places.id")]
    (cond
      (str/blank? text*)
      {:places [] :error {:type :places/blank-query}}

      (not (seq api-key))
      (do
        (log/warn "Places API: no api-key configured")
        {:places [] :error {:type :places/missing-api-key}})

      :else
      (let [bias (location-bias->request location-bias)
            _ (log/debug "Places API request"
                {:api-key-redacted (redact-api-key api-key)
                 :query text*
                 :region-code region-code
                 :language-code language-code
                 :field-mask field-mask*})
            body (cond-> {:textQuery text*}
                   (seq region-code) (assoc :regionCode region-code)
                   (seq language-code) (assoc :languageCode language-code)
                   (some? max-results) (assoc :maxResultCount (long max-results))
                   bias (assoc :locationBias bias))
            req-opts {:headers {"X-Goog-Api-Key" api-key
                                "X-Goog-FieldMask" field-mask*
                                "Content-Type" "application/json"}
                      :body (json/generate-string body)
                      :as :text
                      :throw-exceptions false
                      :socket-timeout (:timeout-ms cfg)
                      :conn-timeout (:timeout-ms cfg)}]
        (try
          (let [resp (http-post! (:base-url cfg) req-opts)
                status (:status resp)
                data (parse-json-body (:body resp))]
            (if (= 200 status)
              (let [places (->> (:places data)
                             (keep (fn [place]
                                     (let [display-name (:displayName place)
                                           name (cond
                                                  (string? display-name) display-name
                                                  (string? (:text display-name)) (:text display-name)
                                                  :else nil)]
                                       (when (seq (some-> name str str/trim))
                                         {:name (str/trim name)
                                          :raw place}))))
                             vec)
                    place-names (mapv :name places)]
                (log/info "Places API response"
                  {:status status
                   :query text*
                   :region-code region-code
                   :language-code language-code
                   :field-mask field-mask*
                   :places place-names
                   :raw-body-snippet (safe-body-snippet (:body resp))})
                {:places places :error nil})
              (do
                (log/info "Places API non-200 response"
                  (merge
                    {:status status
                     :query text*
                     :region-code region-code
                     :language-code language-code
                     :field-mask field-mask*
                     :raw-body-snippet (safe-body-snippet (:body resp))}
                    (parsed-error-info data)))
                {:places []
                 :error (response->error "Places API error" resp data)})))
          (catch Exception e
            (log/warn e "Places API request failed"
              {:query text*
               :region-code region-code
               :language-code language-code
               :field-mask field-mask*})
            {:places []
             :error {:type :places/exception
                     :message (or (.getMessage e) (str (class e)))}}))))))

(defn get-place-details!
  "Fetch detailed place information for a given place-id.
  
  Returns map with :id, :display-name, :formatted-address, :address-components on success.
  Returns nil on errors (API error, HTTP error, or exception).
  
  Example:
    (get-place-details! cfg \"ChIJQR...place-id\")
    ;; => {:id \"ChIJQR...\", 
    ;;     :display-name \"Bingo Sarajevo\",
    ;;     :formatted-address \"Ul. Kolodvorska 12, 71000 Sarajevo\",
    ;;     :address-components [{:longText \"Sarajevo\" :types [\"locality\" \"political\"]} ...]}"
  [cfg place-id]
  (let [api-key (:api-key cfg)
        place-id* (some-> place-id str str/trim not-empty)
        field-mask "id,displayName,formattedAddress,addressComponents"]
    (cond
      (str/blank? place-id*)
      (do
        (log/debug "get-place-details: blank place-id")
        nil)

      (not (seq api-key))
      (do
        (log/warn "get-place-details: no api-key configured")
        nil)

      :else
      (let [url (str "https://places.googleapis.com/v1/places/" place-id*)
            req-opts {:headers {"X-Goog-Api-Key" api-key
                                "X-Goog-FieldMask" field-mask}
                      :as :text
                      :throw-exceptions false
                      :socket-timeout (:timeout-ms cfg)
                      :conn-timeout (:timeout-ms cfg)}]
        (try
          (let [resp (http/get url req-opts)
                status (:status resp)
                data (parse-json-body (:body resp))]
            (if (= 200 status)
              (let [display-name (or (get-in data [:displayName :text])
                                   (get data :displayName))
                    formatted-address (get data :formattedAddress)
                    address-components (get data :addressComponents)
                    id (get data :id)]
                (log/debug "get-place-details success"
                  {:place-id place-id*
                   :display-name display-name
                   :address-components-count (count address-components)})
                {:id id
                 :display-name display-name
                 :formatted-address formatted-address
                 :address-components address-components})
              (do
                (log/warn "get-place-details non-200 response"
                  {:status status
                   :place-id place-id*
                   :raw-body-snippet (safe-body-snippet (:body resp))})
                nil)))
          (catch Exception e
            (log/warn e "get-place-details request failed"
              {:place-id place-id*})
            nil))))))

(defn extract-city-from-address-components
  "Extract city name from Places API addressComponents array.
  
  Tries in order:
  1. Component with type \"locality\" (city)
  2. Component with type \"postal_town\"
  3. Component with type \"administrative_area_level_2\"
  
  Returns the longText field from first matching component, or nil if not found.
  
  Example addressComponents structure:
    [{:longText \"Sarajevo\"
      :shortText \"Sarajevo\"
      :types [\"locality\" \"political\"]
      :languageCode \"bs\"}
     {:longText \"71000\"
      :types [\"postal_code\"]}]
  
  Example:
    (extract-city-from-address-components
      [{:longText \"Sarajevo\" :types [\"locality\" \"political\"]}
       {:longText \"71000\" :types [\"postal_code\"]}])
    ;; => \"Sarajevo\""
  [address-components]
  (when (sequential? address-components)
    (let [;; Try locality first (most common for cities)
          locality (some (fn [component]
                           (when (and (map? component)
                                   (some #{"locality"} (:types component)))
                             (:longText component)))
                     address-components)]
      (or locality
        ;; Fallback to postal_town
        (some (fn [component]
                (when (and (map? component)
                        (some #{"postal_town"} (:types component)))
                  (:longText component)))
          address-components)
        ;; Last fallback: administrative_area_level_2
        (some (fn [component]
                (when (and (map? component)
                        (some #{"administrative_area_level_2"} (:types component)))
                  (:longText component)))
          address-components)))))