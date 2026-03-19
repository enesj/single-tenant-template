(ns app.domain.backend.expenses.services.exchange-rates
  "Daily exchange rate service — fetches and caches CBBH rates.

   Exchange rates are lazily cached: the first non-BAM expense of the day
   triggers a bulk fetch for all enabled currencies via Serper API → CBBH
   kursna lista search. Rates are stored per-currency per-date.

   On fetch failure, the most recent cached rates are copied as fallback
   and an alert is created for admin visibility."
  (:require
    [app.shared.adapters.database :as db-adapter]
    [cheshire.core :as json]
    [clj-http.client :as http]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [taoensso.timbre :as log])
  (:import
    [java.time LocalDate]
    [java.util UUID]))

;; ---------------------------------------------------------------------------
;; Configuration
;; ---------------------------------------------------------------------------

(def ^:private serper-endpoint "https://google.serper.dev/search")

(defn build-config
  "Extract exchange rate config from the app config map."
  [app-config]
  {:serper-api-key (get-in app-config [:serper :api-key])
   :serper-timeout-ms (or (get-in app-config [:serper :timeout-ms]) 4000)})

;; ---------------------------------------------------------------------------
;; DB operations
;; ---------------------------------------------------------------------------

(defn get-daily-rates
  "Return all cached rates for a given date."
  [db ^LocalDate rate-date]
  (->> (jdbc/execute!
         db
         (sql/format {:select [:*]
                      :from [:daily_exchange_rates]
                      :where [:= :rate_date rate-date]
                      :order-by [[:currency_code :asc]]})
         {:builder-fn rs/as-unqualified-lower-maps})
    (mapv db-adapter/to-app)))

(defn get-rate-for-currency
  "Lookup the cached rate for a specific currency on a given date.
   Returns nil if not cached."
  [db currency-code ^LocalDate rate-date]
  (-> (jdbc/execute-one!
        db
        (sql/format {:select [:*]
                     :from [:daily_exchange_rates]
                     :where [:and
                             [:= :currency_code currency-code]
                             [:= :rate_date rate-date]]})
        {:builder-fn rs/as-unqualified-lower-maps})
    db-adapter/to-app))

(defn- get-latest-rates
  "Return the most recently cached rates (any date), one per currency."
  [db]
  (->> (jdbc/execute!
         db
         [(str "SELECT DISTINCT ON (currency_code) "
            "currency_code, rate_date, rate, fetched_at, is_fallback "
            "FROM daily_exchange_rates "
            "ORDER BY currency_code, rate_date DESC")]
         {:builder-fn rs/as-unqualified-lower-maps})
    (mapv db-adapter/to-app)))

(defn- store-rates!
  "Bulk insert rates for a date. Skips duplicates via ON CONFLICT."
  [db ^LocalDate rate-date rates is-fallback]
  (when (seq rates)
    (let [rows (mapv (fn [{:keys [currency-code rate]}]
                       {:id (UUID/randomUUID)
                        :currency_code currency-code
                        :rate_date rate-date
                        :rate rate
                        :fetched_at [:now]
                        :is_fallback (boolean is-fallback)})
                 rates)]
      (jdbc/execute!
        db
        (sql/format {:insert-into :daily_exchange_rates
                     :values rows
                     :on-conflict [:currency_code :rate_date]
                     :do-nothing true})))))

;; ---------------------------------------------------------------------------
;; Alerts
;; ---------------------------------------------------------------------------

(defn create-fetch-alert!
  "Log a fetch failure alert for admin visibility."
  [db ^LocalDate rate-date error-message fallback-source-date]
  (jdbc/execute-one!
    db
    (sql/format {:insert-into :exchange_rate_fetch_alerts
                 :values [{:id (UUID/randomUUID)
                           :rate_date rate-date
                           :error_message error-message
                           :fallback_source_date fallback-source-date
                           :acknowledged false}]
                 :returning [:*]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn get-unacknowledged-alerts
  "Return all unacknowledged fetch alerts, newest first."
  [db]
  (->> (jdbc/execute!
         db
         (sql/format {:select [:*]
                      :from [:exchange_rate_fetch_alerts]
                      :where [:= :acknowledged false]
                      :order-by [[:created_at :desc]]})
         {:builder-fn rs/as-unqualified-lower-maps})
    (mapv db-adapter/to-app)))

(defn acknowledge-alert!
  "Mark an alert as acknowledged."
  [db alert-id]
  (jdbc/execute-one!
    db
    (sql/format {:update :exchange_rate_fetch_alerts
                 :set {:acknowledged true}
                 :where [:= :id alert-id]
                 :returning [:*]})
    {:builder-fn rs/as-unqualified-lower-maps}))

;; ---------------------------------------------------------------------------
;; CBBH rate fetching — direct HTML (primary) + Serper (fallback)
;; ---------------------------------------------------------------------------

(def ^:private cbbh-url "https://cbbh.ba/CurrencyExchange/?lang=en")
(def ^:private cbbh-timeout-ms 8000)

(defn- parse-rate-from-snippet
  "Extract the middle exchange rate from a CBBH PDF/table snippet.
   Handles CBBH PDF format: 'EUR. 1. 1.955830. 1.955830. 1.955830.'
   and simpler formats like 'EUR 1.955830'."
  [snippet currency-code]
  (when (and snippet currency-code)
    (let [cbbh-pattern (re-pattern
                         (str "(?i)" currency-code
                           "\\.?\\s+"
                           "\\d+\\.?\\s+"
                           "[\\d.,]+\\.?\\s+"
                           "([\\d.,]+)"))
          simple-pattern (re-pattern
                           (str "(?i)" currency-code "\\s*[:=]?\\s*(\\d+[.,]\\d+)"))
          match (or (re-find cbbh-pattern snippet)
                  (re-find simple-pattern snippet))]
      (when match
        (try
          (let [rate-str (-> (second match)
                           (str/replace "," ".")
                           (str/replace #"\.$" ""))]
            (BigDecimal. rate-str))
          (catch Exception _ nil))))))

(defn- fetch-cbbh-html-rates
  "Fetch exchange rates directly from the CBBH website.
   Parses the HTML table for middle rates. Returns a seq of
   {:currency-code \"EUR\" :rate 1.955830M} or nil on failure."
  [currency-codes]
  (try
    (let [resp (http/get cbbh-url
                 {:as :string
                  :throw-exceptions false
                  :socket-timeout cbbh-timeout-ms
                  :connection-timeout cbbh-timeout-ms})]
      (when (= 200 (:status resp))
        (let [body (:body resp)
              ;; Parse: currcircle">CODE ... middle-column">RATE
              pattern #"(?s)currcircle\">([A-Z]{3})</div>.*?middle-column\">([\d.]+)</td>"
              all-rates (re-seq pattern body)
              rate-map (into {} (map (fn [[_ code rate]]
                                       [code (BigDecimal. ^String rate)])
                                  all-rates))
              target-set (set currency-codes)]
          (->> rate-map
            (keep (fn [[code rate]]
                    (when (target-set code)
                      {:currency-code code :rate rate})))
            vec))))
    (catch Exception e
      (log/warn "CBBH HTML fetch failed:" (.getMessage e))
      nil)))

(defn- search-cbbh-rates-via-serper
  "Fallback: search Serper for CBBH exchange rates.
   Returns parsed rates or nil."
  [{:keys [serper-api-key serper-timeout-ms]} ^LocalDate rate-date currency-codes]
  (when serper-api-key
    (try
      (let [query (str "CBBH kursna lista " rate-date)
            resp (http/post serper-endpoint
                   {:headers {"Accept" "application/json"
                              "Content-Type" "application/json"
                              "X-API-KEY" serper-api-key}
                    :body (json/generate-string {:q query
                                                 :num 5
                                                 :gl "ba"
                                                 :hl "bs"})
                    :as :string
                    :throw-exceptions false
                    :socket-timeout (int serper-timeout-ms)
                    :connection-timeout (int serper-timeout-ms)})]
        (when (= 200 (:status resp))
          (let [results (json/parse-string (:body resp) true)
                snippets (->> (concat (:organic results)
                                (when-let [ab (:answerBox results)]
                                  [(select-keys ab [:snippet :answer :title])]))
                           (map (fn [r]
                                  (str (:snippet r) " " (:title r) " " (:answer r))))
                           (str/join " "))]
            (->> currency-codes
              (keep (fn [code]
                      (when-let [rate (parse-rate-from-snippet snippets code)]
                        {:currency-code code :rate rate})))
              vec))))
      (catch Exception e
        (log/warn "Serper CBBH search failed:" (.getMessage e))
        nil))))

(defn- get-enabled-non-bam-codes
  "Return currency codes from enabled_currencies excluding BAM."
  [db]
  (->> (jdbc/execute!
         db
         (sql/format {:select [:code]
                      :from [:enabled_currencies]
                      :where [:<> :code "BAM"]})
         {:builder-fn rs/as-unqualified-lower-maps})
    (mapv :code)))

;; ---------------------------------------------------------------------------
;; Public API
;; ---------------------------------------------------------------------------

(defn- apply-fallback-rates!
  "Copy the latest cached rates for today and create an admin alert."
  [db ^LocalDate rate-date error-message]
  (let [latest (get-latest-rates db)]
    (if (seq latest)
      (let [fallback-rates (mapv (fn [r] {:currency-code (:currency-code r) :rate (:rate r)})
                             latest)
            fallback-date (:rate-date (first latest))]
        (store-rates! db rate-date fallback-rates true)
        (create-fetch-alert! db rate-date
          (str error-message "; using fallback rates from " fallback-date)
          fallback-date)
        (log/warn "Using fallback rates from" (str fallback-date) "for" (str rate-date))
        {:status :fallback :rates fallback-rates :fallback-date fallback-date})
      (do
        (create-fetch-alert! db rate-date
          (str error-message "; no fallback available")
          nil)
        (log/error "No exchange rates available for" (str rate-date))
        {:status :error :rates []}))))

(defn fetch-and-cache-daily-rates!
  "Fetch CBBH rates for all enabled currencies for a given date.

   Strategy: try direct CBBH HTML first (reliable, all currencies),
   then Serper search as fallback (partial results from PDF snippets).
   On total failure, copy latest cached rates and alert the admin.

   Returns {:status :ok|:fallback|:error :rates [...]}"
  [db config ^LocalDate rate-date]
  (let [currency-codes (get-enabled-non-bam-codes db)]
    (if (empty? currency-codes)
      {:status :ok :rates []}
      (try
        (let [;; Primary: direct CBBH HTML fetch
              rates (fetch-cbbh-html-rates currency-codes)
              ;; Fallback: Serper search if HTML failed or returned nothing
              rates (if (seq rates)
                      rates
                      (do
                        (log/info "CBBH HTML fetch returned no rates, trying Serper fallback")
                        (search-cbbh-rates-via-serper config rate-date currency-codes)))]
          (if (seq rates)
            (do
              (store-rates! db rate-date rates false)
              (log/info "Cached" (count rates) "exchange rates for" (str rate-date))
              {:status :ok :rates rates})
            (apply-fallback-rates! db rate-date "Both CBBH HTML and Serper fetch returned no rates")))
        (catch Exception e
          (log/error e "Exchange rate fetch failed for" (str rate-date))
          (apply-fallback-rates! db rate-date (str "Fetch exception: " (.getMessage e))))))))

(defn ensure-daily-rates!
  "Idempotent: fetch rates for today only if not already cached.
   Returns the cached rates for today."
  [db config]
  (let [today (LocalDate/now)
        existing (get-daily-rates db today)]
    (if (seq existing)
      {:status :ok :rates existing}
      (fetch-and-cache-daily-rates! db config today))))

(defn get-conversion-rate
  "Get the rate to convert from `currency-code` to BAM for a given date.
   Returns the rate as BigDecimal, or nil if not available.
   BAM→BAM returns nil (no conversion needed)."
  [db currency-code ^LocalDate rate-date]
  (when (and currency-code (not= currency-code "BAM"))
    (let [cached (get-rate-for-currency db currency-code rate-date)]
      (:rate cached))))

(comment
  ;; (require '[app.domain.backend.expenses.services.exchange-rates :as er] :reload)
  ;; (er/get-daily-rates db (LocalDate/now))
  ;; (er/ensure-daily-rates! db config)
  ;; (er/get-conversion-rate db "EUR" (LocalDate/now))
  :rcf)
