(ns ade
  (:require
    [cheshire.core :as json]
    [clj-http.client :as http]
    [clojure.java.io :as io])
  (:import
    (java.io File)
    (java.nio.file Files)
    (java.nio.file.attribute FileAttribute)))

;; -----------------------------------------------------------------------------
;; Config
;; -----------------------------------------------------------------------------
;; Auth is via HTTP header: Authorization: Bearer YOUR_API_KEY :contentReference[oaicite:3]{index=3}
;; US base: https://api.va.landing.ai
;; EU base: https://api.va.eu-west-1.landing.ai :contentReference[oaicite:4]{index=4}

(defn env
  ([k] (System/getenv k))
  ([k default] (or (System/getenv k) default)))

(defn ade-base-url []
  (case (env "ADE_ENV" "us")
    "eu" "https://api.va.eu-west-1.landing.ai"
    "us" "https://api.va.landing.ai"
    ;; allow overriding directly if you want:
    (env "ADE_BASE_URL" "https://api.va.landing.ai")))

(defn ade-api-key []
  (or (env "VISION_AGENT_API_KEY")
      (throw (ex-info "Missing env var VISION_AGENT_API_KEY" {}))))

(defn auth-headers []
  {"Authorization" (str "Bearer " (ade-api-key))})

(def default-http-opts
  {:throw-exceptions false
   :as :json
   :socket-timeout 120000
   :conn-timeout 10000})

(defn- retry?
  "Retry on transient errors commonly seen with hosted APIs."
  [status]
  (or (= status 429)
      (<= 500 status 599)))

(defn- request-with-retry
  "Simple exponential backoff retry for transient statuses."
  [req-fn {:keys [max-attempts] :or {max-attempts 4}}]
  (loop [attempt 1
         delay-ms 300]
    (let [resp (req-fn)
          status (:status resp)]
      (cond
        (<= 200 status 299) resp
        (and (< attempt max-attempts) (retry? status))
        (do (Thread/sleep delay-ms)
            (recur (inc attempt) (min 5000 (* 2 delay-ms))))
        :else
        (throw (ex-info "ADE request failed"
                        {:status status
                         :headers (:headers resp)
                         :body (:body resp)}))))))

;; -----------------------------------------------------------------------------
;; ADE Schemas (Totals + Line Items) - as a Clojure map; encoded to JSON string.
;; -----------------------------------------------------------------------------

(def pos-receipt-schema
  {:type "object"
   :title "POS Receipt - Totals + Line Items"
   :description "Extract supplier, purchase datetime, total, optional payment hints, and line items for price comparison."
   :properties
   {:supplier {:type "object"
               :title "Supplier / Merchant"
               :properties {:name {:type "string" :description "Merchant/store name."}
                            :address {:type "string" :nullable true :description "Merchant address, if present."}
                            :tax_id {:type "string" :nullable true :description "Merchant tax ID, if present."}}
               :required ["name"]}

    :purchased_at {:title "Purchase Date/Time"
                   :description "Purchase date and time if available. Prefer ISO 8601. If only date is present, return YYYY-MM-DD."
                   :anyOf [{:type "string" :format "date-time"}
                           {:type "string" :format "date"}
                           {:type "string"}]
                   :nullable true}

    :total {:type "object"
            :title "Total Amount"
            :properties {:amount {:type "number" :minimum 0 :description "Total amount paid (gross)."}
                         :currency {:type "string" :nullable true :description "Currency code if present."}}
            :required ["amount"]}

    :payment_hints {:type "object"
                    :title "Payment Hints"
                    :nullable true
                    :description "Optional hints used to auto-suggest payer; user can override."
                    :properties {:method {:type "string" :nullable true :enum ["cash" "card" "unknown"]}
                                 :card_last4 {:type "string" :nullable true :description "Last 4 digits if printed."}}}

    :line_items {:type "array"
                 :title "Line Items"
                 :description "Items purchased as printed on the receipt. Use this for article normalization + price comparisons."
                 :minItems 0
                 :maxItems 100
                 :items {:type "object"
                         :properties {:raw_label {:type "string" :description "Item/Article label exactly as printed."}
                                      :qty {:type "number" :nullable true :minimum 0 :description "Quantity, if present."}
                                      :unit_price {:type "number" :nullable true :minimum 0 :description "Unit price, if present."}
                                      :line_total {:type "number" :minimum 0 :description "Total price for this line item."}}
                         :required ["raw_label" "line_total"]
                         :propertyOrdering ["raw_label" "qty" "unit_price" "line_total"]}}}
   :required ["supplier" "total"]
   :propertyOrdering ["supplier" "purchased_at" "total" "payment_hints" "line_items"]})

(defn schema->json ^String [schema-map]
  ;; don't pretty print to keep payload small
  (json/generate-string schema-map))

;; -----------------------------------------------------------------------------
;; ADE Parse
;; -----------------------------------------------------------------------------
;; Body is multipart/form-data with: document (file) or document_url; optional model; optional split=page :contentReference[oaicite:5]{index=5}
;; Parse returns JSON including "markdown" string with parsed content :contentReference[oaicite:6]{index=6}

(defn ade-parse-file
  "Parse a local PDF/image file.
   model example: \"dpt-2-latest\" (optional) :contentReference[oaicite:7]{index=7}
   split example: \"page\" (optional) :contentReference[oaicite:8]{index=8}"
  [{:keys [file-path model split max-attempts]
    :or {model "dpt-2-latest"}}]
  (let [url (str (ade-base-url) "/v1/ade/parse")
        f   (io/file file-path)]
    (when-not (.exists ^File f)
      (throw (ex-info "File not found" {:file-path file-path})))
    (request-with-retry
      #(http/post url
                  (merge default-http-opts
                         {:headers (auth-headers)
                          :multipart (cond-> [{:name "document" :content f}]
                                             {:name "model" :content model}
                                       split (conj {:name "split" :content split}))}))
      {:max-attempts max-attempts})))

;; -----------------------------------------------------------------------------
;; ADE Extract
;; -----------------------------------------------------------------------------
;; Extract expects multipart/form-data:
;; - schema: string (JSON schema)
;; - markdown: file (the markdown output) OR markdown content (docs show file usage) :contentReference[oaicite:9]{index=9}
;; - model: optional, e.g. extract-latest :contentReference[oaicite:10]{index=10}

(defn- write-temp-markdown! ^File [^String markdown]
  (let [p (Files/createTempFile "ade-" ".md" (make-array FileAttribute 0))
        f (.toFile p)]
    (spit f markdown)
    f))

(defn ade-extract-markdown
  "Run extraction on markdown content (string) by writing it to a temp file and uploading it.
   model example: \"extract-latest\" :contentReference[oaicite:11]{index=11}"
  [{:keys [markdown schema-json model max-attempts]
    :or {model "extract-latest"}}]
  (let [url (str (ade-base-url) "/v1/ade/extract")
        md-file (write-temp-markdown! markdown)]
    (try
      (request-with-retry
        #(http/post url
                    (merge default-http-opts
                           {:headers (auth-headers)
                            :multipart [{:name "markdown" :content md-file}
                                        {:name "schema" :content schema-json}
                                        {:name "model" :content model}]}))
        {:max-attempts max-attempts})
      (finally
        (try (.delete ^File md-file) (catch Exception _))))))

(defn ade-parse-and-extract
  "Convenience: parse a receipt file then extract fields using the POS schema.
   Returns {:parse <parse-json> :extract <extract-json>}."
  [{:keys [file-path parse-model extract-model max-attempts split]
    :or {parse-model "dpt-2-latest"
         extract-model "extract-latest"}}]
  (let [parse-resp (ade-parse-file {:file-path file-path}
                                   :model parse-model
                                   :split split
                                   :max-attempts max-attempts)
        markdown   (get-in parse-resp [:body "markdown"])
        schema     (schema->json pos-receipt-schema)
        extract-resp (ade-extract-markdown {:markdown markdown
                                            :schema-json schema
                                            :model extract-model
                                            :max-attempts max-attempts})]
    {:parse   (:body parse-resp)
     :extract (:body extract-resp)}))

;; -----------------------------------------------------------------------------
;; Example (REPL)
;; -----------------------------------------------------------------------------
(comment
  ;; export VISION_AGENT_API_KEY=...
  ;; export ADE_ENV=eu   ; optional (or leave default "us") :contentReference[oaicite:12]{index=12}

  (ade-parse-and-extract
    {:file-path "receipts/pos-bill.jpg"
     :split nil
     :max-attempts 4}))
