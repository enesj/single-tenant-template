(ns articles.research.perplexity
  "Perplexity API interaction: querying sonar-pro, parsing responses,
  building research prompts, and running batch research."
  (:require
    [articles.db :as db]
    [articles.research.heuristics :as heuristics]
    [babashka.http-client :as http]
    [clojure.data.json :as json]
    [clojure.java.io :as io]
    [clojure.string :as str]))

;; ============================================================
;; Perplexity API
;; ============================================================

(def ^:private perplexity-url "https://api.perplexity.ai/chat/completions")

(defn load-env-file [path]
  (when (.exists (io/file path))
    (->> (slurp path)
      str/split-lines
      (keep (fn [line]
              (when-let [[_ k v] (re-matches #"^([A-Za-z_][A-Za-z0-9_]*)=(.*)$" line)]
                [(str/trim k) (str/trim (str/replace v #"^['\"]|['\"]$" ""))])))
      (into {}))))

(defn get-perplexity-key []
  (or (some-> (load-env-file ".env") (get "PERPLEXITY_API_KEY"))
    (System/getenv "PERPLEXITY_API_KEY")))

(defn query-perplexity
  "Call Perplexity sonar-pro with system + user prompts. Returns parsed response map."
  [system-prompt user-prompt]
  (let [api-key (get-perplexity-key)]
    (when-not api-key
      (throw (ex-info "PERPLEXITY_API_KEY not found"
               {:hint "Add PERPLEXITY_API_KEY=<key> to .env or set env var"})))
    (let [payload {:model "sonar-pro"
                   :messages [{:role "system" :content system-prompt}
                              {:role "user" :content user-prompt}]
                   :max_tokens 4096
                   :temperature 0.1}
          resp (http/post perplexity-url
                 {:headers {"Authorization" (str "Bearer " api-key)
                            "Content-Type" "application/json"}
                  :body (json/write-str payload)
                  :throw false})]
      (when-not (= 200 (:status resp))
        (throw (ex-info "Perplexity API error"
                 {:status (:status resp)
                  :body (some-> (:body resp) (subs 0 (min 300 (count (:body resp)))))})))
      (json/read-str (:body resp) :key-fn keyword))))

(defn extract-content [response]
  (get-in response [:choices 0 :message :content]))

(defn parse-json-response
  "Parse a JSON array from Perplexity response, stripping markdown fences if present."
  [text]
  (when text
    (let [cleaned (-> text
                    str/trim
                    (str/replace #"^```(?:json)?\s*" "")
                    (str/replace #"\s*```\s*$" "")
                    str/trim)]
      (or (try
            (let [parsed (json/read-str cleaned :key-fn keyword)]
              (when (sequential? parsed) parsed))
            (catch Exception _ nil))
          ;; Fallback: extract first JSON array from text
        (when-let [match (re-find #"\[[\s\S]*\]" cleaned)]
          (try
            (let [parsed (json/read-str match :key-fn keyword)]
              (when (sequential? parsed) parsed))
            (catch Exception _ nil)))))))

;; ============================================================
;; Prompt building & batch research
;; ============================================================

(defn load-system-prompt-template []
  (let [f (io/file "scripts/bb/articles/perplexity-system-prompt.txt")]
    (if (.exists f)
      (slurp f)
      (throw (ex-info "Perplexity system prompt template not found"
               {:path (.getAbsolutePath f)
                :hint "Expected at scripts/bb/articles/perplexity-system-prompt.txt"})))))

(defn build-research-prompt
  "Build Perplexity system + user prompts for a batch of alias groups.
  Loads the system prompt template from perplexity-system-prompt.txt and
  injects the live category list, taxonomy, and brand mapping tables."
  [groups db-category-names subcategory-map]
  (let [cat-list (str/join ", " db-category-names)
        taxonomy-block (when (seq subcategory-map)
                         (str "\nExisting category \u2192 subcategory taxonomy. You MUST pick a subcategory from this list when one fits. Only create a new Bosnian subcategory if absolutely none of the existing ones are appropriate:\n"
                           (->> subcategory-map
                             (map (fn [[cat subcats]]
                                    (str "  " cat ": " (str/join ", " subcats))))
                             (str/join "\n"))))
        ;; Brand mapping tables injected from taxonomy EDN files
        brand-parent-map (or (heuristics/load-taxonomy "brand-parent-mappings.edn") {})
        self-named       (or (heuristics/load-taxonomy "self-named-brands.edn") [])
        brand-block (str "- Key brand\u2192parent-company mappings (use the right-hand value as mfr): "
                      (->> brand-parent-map
                        (group-by val)
                        (sort-by key)
                        (map (fn [[parent brands]]
                               (str (str/join "/" (sort (map key brands)))
                                 " \u2192 \"" parent "\"")))
                        (str/join ", "))
                      "\n- Self-named brands (the brand IS the manufacturer \u2014 use brand name as mfr): "
                      (str/join ", " (sort self-named)))
        system-prompt (-> (load-system-prompt-template)
                        (str/replace "{{CATEGORIES}}" cat-list)
                        (str/replace "{{TAXONOMY}}" (or taxonomy-block ""))
                        (str/replace "{{BRAND_MAPPINGS}}" brand-block))
        items-text (->> groups
                     (map-indexed
                       (fn [idx {:keys [raw-label suppliers]}]
                         (format "%d. \"%s\" (store: %s)"
                           (inc idx)
                           raw-label
                           (str/join " / " (take 2 suppliers)))))
                     (str/join "\n"))]
    {:system system-prompt
     :user   (str "Identify these products from Bosnian store receipts:\n" items-text)}))

(defn research-batch!
  "Send a batch of alias groups to Perplexity for identification.
  Returns a vector of resolved group maps."
  [groups db-category-names subcategory-map]
  (let [{:keys [system user]} (build-research-prompt groups db-category-names subcategory-map)
        response (query-perplexity system user)
        content (extract-content response)
        parsed (parse-json-response content)]
    (if parsed
      (->> parsed
        (keep (fn [item]
                (let [idx (dec (or (:i item) 0))
                      group (get (vec groups) idx)]
                  (when (and group item)
                    (merge group
                      {:canonical-name   (or (:name item) (heuristics/build-canonical-name (:raw-label group)))
                       :manufacturer-name (:mfr item)
                       :category-name    (or (:cat item) "Ostalo")
                       :subcategory-name (or (:subcat item) "Opste")
                       :confidence       :medium
                       :resolution       :perplexity})))))
        vec)
      (do
        (binding [*out* *err*]
          (println "WARNING: Failed to parse Perplexity response")
          (when content
            (println "  Content preview:" (subs (str content) 0 (min 200 (count (str content)))))))
        (mapv #(assoc % :resolution :failed :confidence :low) groups)))))
