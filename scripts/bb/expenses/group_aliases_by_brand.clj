#!/usr/bin/env bb

(ns scripts.bb.expenses.group-aliases-by-brand
  "Group unmapped article aliases by detected brand/product family.

  Extracts size tokens (g, kg, ml, L, etc.) and supplier type (restaurant vs retail)
  so the agent can spot size variants BEFORE creating articles -- preventing the
  classic mistake of mapping different sizes to the same article.

  Usage:
    bb group-aliases-by-brand [dev|test] [--mapped] [--min-group N] [--json]

  Flags:
    --mapped      Include already-mapped aliases (default: unmapped only)
    --min-group N Only show groups with >= N aliases (default: 1)
    --json        Output as JSON instead of human-readable text"
  (:require
    [aero.core :as aero]
    [clojure.data.json :as json]
    [clojure.java.shell :as sh]
    [clojure.string :as str]))

;; ---------------------------------------------------------------------------
;; DB helpers (psql-based, bb-compatible)
;; ---------------------------------------------------------------------------

(defn- db-config [profile]
  (:database (aero/read-config "config/base.edn" {:profile profile})))

(defn- psql-query-maps
  "Run SQL via psql, returning vector of maps. Uses header row for keys."
  [{:keys [host port dbname user password]} sql]
  (let [{:keys [out err exit]}
        (sh/sh "psql" "-h" host "-p" (str port) "-U" user "-d" dbname
               "-F" "\t" "-A" "-c" sql
               :env (merge (into {} (System/getenv)) {"PGPASSWORD" password}))]
    (when (not= 0 exit)
      (binding [*out* *err*]
        (println "psql error:" err))
      (System/exit 1))
    (let [lines (->> (str/split-lines out)
                     (remove str/blank?)
                     (remove #(re-matches #"\(\d+ rows?\)" %))
                     vec)]
      (when (>= (count lines) 2)
        (let [headers (mapv keyword (str/split (first lines) #"\t"))]
          (mapv (fn [line]
                  (zipmap headers (str/split line #"\t" -1)))
                (rest lines)))))))

;; ---------------------------------------------------------------------------
;; Size / brand extraction
;; ---------------------------------------------------------------------------

(def ^:private pack-size-pattern
  ;; 3x200g, 2x1.5l, 4x500ml
  #"(?i)\b(\d+)\s*x\s*(\d+(?:[.,]\d+)?)\s*(l|ml|g|kg|cl|dl|kom)\b")

(def ^:private decimal-space-size-pattern
  ;; 1 25L (1.25L), 0 25l (0.25l)
  #"(?i)\b(\d)\s+(\d{2})\s*(l|ml|g|kg|cl|dl)\b")

(def ^:private decimal-size-pattern
  ;; 1.25L, 0,5l
  #"(?i)\b(\d+(?:[.,]\d+))\s*(l|ml|g|kg|cl|dl)\b")

(def ^:private simple-size-pattern
  ;; 2L, 500ml, 200g
  #"(?i)\b(\d{1,5})\s*(l|ml|g|kg|cl|dl|kom|pcs|tab)\b")

(def ^:private restaurant-keywords
  ["cevabdzinica" "fast food" "restoran" "kafic" "caffe"
   "cafe" "bistro" "pizzeria" "buregdzinica"
   "grill" "pekara" "slasticarna" "pub" "bar " "konoba"])

(defn- normalize-number
  "Normalize a number string to a dot-decimal representation."
  [s]
  (some-> s str/trim (str/replace "," ".")))

(defn- size-token
  [num unit]
  (str (normalize-number num) (str/lower-case unit)))

(defn- pack-token
  [n num unit]
  (str (str/trim n) "x" (normalize-number num) (str/lower-case unit)))

(defn- prune-overlapping-fractions
  "If we detected a decimal size like 1.25l from a spaced form (e.g. 1 25l), we may
  also accidentally detect the fractional token (e.g. 25l). Remove those."
  [sizes]
  (let [decimal-tokens (filter #(re-find #"\d+\.\d+" %) sizes)
        fraction-tokens
        (->> decimal-tokens
             (keep (fn [t]
                     (when-let [[_ _whole frac unit]
                                (re-matches #"^(\d+)\.(\d+)([a-z]+)$" t)]
                       (str frac unit))))
             set)]
    (reduce disj (set sizes) fraction-tokens)))

(defn- extract-sizes
  "Return a set of normalized size tokens from a label."
  [text]
  (when (some? text)
    (let [t (-> text str/lower-case)
          packs (->> (re-seq pack-size-pattern t)
                     (map (fn [[_ n num unit]] (pack-token n num unit)))
                     set)
          decimals-space (->> (re-seq decimal-space-size-pattern t)
                              (map (fn [[_ a b unit]] (size-token (str a "." b) unit)))
                              set)
          decimals (->> (re-seq decimal-size-pattern t)
                        (map (fn [[_ num unit]] (size-token num unit)))
                        set)
          simples (->> (re-seq simple-size-pattern t)
                       (map (fn [[_ num unit]] (size-token num unit)))
                       set)]
      (-> (into #{} (concat packs decimals-space decimals simples))
          prune-overlapping-fractions))))

(defn- extract-sizes-from-normalized
  "Extract sizes from a hyphenated normalized key.

  Handles patterns like:
  - sok-1-25l-coca-cola     => 1.25l
  - coca-cola-2l-pet        => 2l
  - keks-200g               => 200g
  - pecivo-9x33g            => 9x33g"
  [nk]
  (when (some? nk)
    (let [tokens (->> (str/split (str/lower-case nk) #"-+")
                      (remove str/blank?)
                      vec)
          n (count tokens)]
      (loop [i 0
             sizes #{}]
        (if (>= i n)
          (prune-overlapping-fractions sizes)
          (let [t (nth tokens i)
                next (when (< (inc i) n) (nth tokens (inc i)))
                ;; token like "25l" => ["25" "l"]
                num+unit (when-let [[_ num unit]
                                   (re-matches #"^(\d{1,5})(l|ml|g|kg|cl|dl|kom)$" t)]
                           [num unit])
                pack (when-let [[_ cnt num unit]
                                (re-matches #"^(\d+)x(\d+(?:\.\d+)?)(l|ml|g|kg|cl|dl|kom)$" t)]
                       [cnt num unit])
                decimal-space (when (and (re-matches #"^\d$" t)
                                         (some? next)
                                         (re-matches #"^\d{2}(l|ml|g|kg|cl|dl)$" next))
                                (let [[_ frac unit] (re-matches #"^(\d{2})([a-z]+)$" next)]
                                  [(str t "." frac) unit]))]
            (cond
              (some? decimal-space)
              (recur (+ i 2) (conj sizes (apply size-token decimal-space)))

              (some? pack)
              (recur (inc i) (conj sizes (apply pack-token pack)))

              (some? num+unit)
              (recur (inc i) (conj sizes (apply size-token num+unit)))

              :else
              (recur (inc i) sizes))))))))

(defn- restaurant-supplier?
  "Heuristic: does the supplier name suggest a restaurant/cafe/fast-food?"
  [supplier-name]
  (when supplier-name
    (let [lower (str/lower-case supplier-name)]
      (boolean (some #(str/includes? lower %) restaurant-keywords)))))

(def ^:private common-non-brand-words
  #"(?i)\b(sok|pivo|voda|kafa|keks|mlijeko|jogurt|sir|pasteta|bombone?|cokolad\w*|brasno|secer|ulje|det\w+|salvete|vrecice?|flasteri|tablete|sirup|prasak|folija|pet)\b")

(defn- extract-brand-tokens
  "Extract a simple brand-ish string from a label.

  Note: this is heuristic and intentionally conservative."
  [raw-label]
  (when raw-label
    (-> raw-label
        str/lower-case
        ;; Remove size patterns (including spaced decimals like "1 25l")
        (str/replace pack-size-pattern " ")
        (str/replace decimal-space-size-pattern " ")
        (str/replace decimal-size-pattern " ")
        (str/replace simple-size-pattern " ")
        ;; Remove common non-brand words
        (str/replace common-non-brand-words " ")
        ;; Remove punctuation
        (str/replace #"[^a-z0-9\s]" " ")
        str/trim
        (str/replace #"\s+" " "))))

(defn- strip-trailing-size-tokens
  "Remove obvious trailing size tokens from a normalized key."
  [normalized]
  (let [tokens (->> (str/split (or normalized "") #"-+")
                    (remove str/blank?)
                    vec)
        size-token? (fn [t]
                      (or (re-matches #"^\d{1,5}(l|ml|g|kg|cl|dl|kom|pcs|tab)$" (str/lower-case t))
                          (re-matches #"^\d$" (str/lower-case t))
                          (re-matches #"^\d{2}(l|ml|g|kg|cl|dl)$" (str/lower-case t))
                          (re-matches #"^\d+x\d+(?:[.,]\d+)?(l|ml|g|kg|cl|dl|kom)$" (str/lower-case t))
                          (= "pet" (str/lower-case t))))]
    (loop [ts tokens]
      (if (and (seq ts) (size-token? (last ts)))
        (recur (pop ts))
        (-> (str/join "-" ts)
            (str/replace #"-+" "-")
            (str/replace #"^-|-$" ""))))))

(defn- derive-brand-key
  "Derive a grouping key from the raw label and its normalized form."
  [raw-label raw-label-normalized]
  (let [tokens (extract-brand-tokens raw-label)
        nk-clean (some-> raw-label-normalized strip-trailing-size-tokens)
        candidates (remove str/blank? [tokens nk-clean])]
    (if (seq candidates)
      (first (sort-by count candidates))
      (or raw-label-normalized raw-label "unknown"))))

;; ---------------------------------------------------------------------------
;; Arg parsing
;; ---------------------------------------------------------------------------

(defn- parse-args
  [args]
  (loop [args (vec args)
         opts {:profile :dev
               :mapped? false
               :min-group 1
               :json? false}]
    (if (empty? args)
      opts
      (let [[a & rest-args] args]
        (cond
          (#{"dev" "test"} a)
          (recur (vec rest-args) (assoc opts :profile (keyword a)))

          (= a "--dev")
          (recur (vec rest-args) (assoc opts :profile :dev))

          (= a "--test")
          (recur (vec rest-args) (assoc opts :profile :test))

          (= a "--mapped")
          (recur (vec rest-args) (assoc opts :mapped? true))

          (= a "--json")
          (recur (vec rest-args) (assoc opts :json? true))

          (= a "--min-group")
          (let [n (try (Integer/parseInt (first rest-args)) (catch Exception _ 1))]
            (recur (vec (rest rest-args)) (assoc opts :min-group n)))

          (str/starts-with? a "--min-group=")
          (let [n (try (Integer/parseInt (subs a (count "--min-group="))) (catch Exception _ 1))]
            (recur (vec rest-args) (assoc opts :min-group n)))

          (or (= a "--help") (= a "-h"))
          (do
            (println "Usage: bb group-aliases-by-brand [dev|test] [--mapped] [--min-group N] [--json]")
            (println "")
            (println "Groups article aliases by detected brand/product family.")
            (println "Shows size tokens and supplier type to help spot variant conflicts.")
            (System/exit 0))

          :else
          (recur (vec rest-args) opts))))))

;; ---------------------------------------------------------------------------
;; Main
;; ---------------------------------------------------------------------------

(defn -main
  [& args]
  (let [{:keys [profile mapped? min-group json?]} (parse-args args)
        db-cfg (db-config profile)
        sql (str "SELECT aa.id::text, aa.raw_label, aa.raw_label_normalized, "
                 "COALESCE(s.display_name, '') AS supplier, "
                 "COALESCE(a.canonical_name, '') AS mapped_article, "
                 "COALESCE(a.normalized_key, '') AS mapped_article_key "
                 "FROM article_aliases aa "
                 "LEFT JOIN suppliers s ON s.id = aa.supplier_id "
                 "LEFT JOIN articles a ON a.id = aa.article_id "
                 (if mapped? "" "WHERE aa.article_id IS NULL ")
                 "ORDER BY aa.raw_label")
        aliases (or (psql-query-maps db-cfg sql) [])
         grouped (->> aliases
            (map (fn [{:keys [raw_label raw_label_normalized supplier] :as alias}]
                            (assoc alias
                                   :brand-key (derive-brand-key raw_label raw_label_normalized)
                                   :sizes (into (extract-sizes raw_label)
                                                (extract-sizes-from-normalized raw_label_normalized))
                                   :restaurant? (restaurant-supplier? supplier))))
                     (group-by :brand-key)
                     (map (fn [[brand-key items]]
                            (let [all-sizes (into #{} (mapcat :sizes) items)
                                  has-restaurant? (boolean (some :restaurant? items))
                                  has-retail? (boolean (some (complement :restaurant?) items))
                                  variant-risk? (or (> (count all-sizes) 1)
                                                    (and has-restaurant? has-retail?))]
                              {:brand-key brand-key
                               :alias-count (count items)
                               :sizes (vec (sort all-sizes))
                               :has-restaurant? has-restaurant?
                               :has-retail? has-retail?
                               :variant-risk? variant-risk?
                               :aliases (mapv (fn [{:keys [raw_label raw_label_normalized supplier
                                                           sizes restaurant? mapped_article]}]
                                                (cond-> {:raw_label raw_label
                                                         :normalized raw_label_normalized
                                                         :supplier supplier
                                                         :sizes (vec (sort sizes))
                                                         :restaurant? restaurant?}
                                                  (and mapped_article (not= mapped_article ""))
                                                  (assoc :mapped_to mapped_article)))
                                              items)})))
                     (filter #(>= (:alias-count %) min-group))
                     (sort-by (juxt (comp not :variant-risk?) :brand-key))
                     vec)]

    (if json?
      (println (json/write-str grouped :indent true))

      (do
        (println (str "Found " (count aliases) (if mapped? " total" " unmapped")
                      " aliases in " (count grouped) " brand groups"
                      (when (> min-group 1) (str " (min " min-group " aliases)"))
                      "\n"))

        (let [risky (filter :variant-risk? grouped)
              safe (remove :variant-risk? grouped)]
          (when (seq risky)
            (println "VARIANT RISK -- these groups have multiple sizes or mixed supplier types:")
            (println (apply str (repeat 72 "-")))
            (doseq [{:keys [brand-key alias-count sizes aliases]} risky]
              (println (str "\n  [!] " brand-key " (" alias-count " aliases, sizes: "
                            (if (seq sizes) (str/join ", " sizes) "none detected")
                            ")"))
              (doseq [{:keys [raw_label normalized supplier sizes restaurant? mapped_to]} aliases]
                (println (str "      " raw_label
                              " [" normalized "]"
                              " -- " supplier
                              (when restaurant? " [restaurant]")
                              (when (seq sizes) (str " (" (str/join ", " sizes) ")"))
                              (when mapped_to (str " -> " mapped_to))))))
            (println ""))

          (when (seq safe)
            (println "Single-variant groups:")
            (println (apply str (repeat 72 "-")))
            (doseq [{:keys [brand-key alias-count sizes aliases]} safe]
              (println (str "\n  " brand-key " (" alias-count " aliases"
                            (when (seq sizes) (str ", " (str/join ", " sizes)))
                            ")"))
              (doseq [{:keys [raw_label supplier restaurant? mapped_to]} aliases]
                (println (str "      " raw_label " -- " supplier
                              (when restaurant? " [restaurant]")
                              (when mapped_to (str " -> " mapped_to))))))))))))

(apply -main *command-line-args*)
