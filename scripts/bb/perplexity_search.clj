(ns perplexity-search)
(ns perplexity-search
  "Query Perplexity API with sonar-pro for web search results."
  (:require [clojure.java.shell :refer [sh]]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [cheshire.core :as json]))

(def api-url "https://api.perplexity.ai/chat/completions")

(defn parse-env-line [line]
  (when-let [[_ key value] (re-matches #"^([A-Za-z_][A-Za-z0-9_]*)=(.*)$" line)]
    [(str/trim key) (str/trim (str/replace value #"^['\"]|['\"]$" ""))]))

(defn load-env-file [path]
  (when (.exists (io/file path))
    (->> (slurp path)
         str/split-lines
         (keep parse-env-line)
         (into {}))))

(defn get-api-key []
  (or (-> (load-env-file ".env")
          (get "PERPLEXITY_API_KEY"))
      (System/getenv "PERPLEXITY_API_KEY")
      (throw (ex-info "PERPLEXITY_API_KEY not found"
                      {:hint "Add PERPLEXITY_API_KEY=your-key to .env file or set environment variable"}))))

(defn query-perplexity [question & {:keys [model max-tokens]
                                    :or {model "sonar-pro"
                                         max-tokens 1024}}]
  (let [api-key (get-api-key)
        payload {:model model
                 :messages [{:role "system"
                             :content "Be concise and accurate. Always cite your sources."}
                            {:role "user"
                             :content question}]
                 :max_tokens max-tokens
                 :temperature 0.2}
        response (sh "curl" "-s" "-X" "POST" api-url
                     "-H" (str "Authorization: Bearer " api-key)
                     "-H" "Content-Type: application/json"
                     "-d" (json/generate-string payload))]
    (if (= 0 (:exit response))
      (json/parse-string (:out response) true)
      (throw (ex-info "API request failed" response)))))

(defn format-citations [citations]
  (when (seq citations)
    (str "\n\n📚 Sources:\n"
         (str/join "\n"
                   (map-indexed (fn [idx url]
                                  (format "  [%d] %s" (inc idx) url))
                                citations)))))

(defn print-result [response]
  (let [choice (first (get-in response [:choices]))
        message (:message choice)
        content (:content message)
        citations (:citations response)]
    (println "\n" (str/join (repeat 60 "═")))
    (println "📝 Answer:")
    (println (str/join (repeat 60 "─")))
    (println content)
    (when-let [cites (format-citations citations)]
      (println cites))
    (println (str/join (repeat 60 "═")))
    (println (format "\n🔍 Model: %s | Tokens: %d"
                     (:model response "unknown")
                     (get-in response [:usage :total_tokens] 0)))))

(defn usage []
  (println "Usage: bb perplexity_search.clj <question>")
  (println "")
  (println "Configuration:")
  (println "  Reads PERPLEXITY_API_KEY from .env file (project root)")
  (println "  Falls back to PERPLEXITY_API_KEY environment variable")
  (println "")
  (println "Examples:")
  (println "  bb perplexity_search.clj \"What were the results of the 2025 French Open Finals?\"")
  (println "  bb perplexity_search.clj \"Latest news about Clojure 1.12\"")
  (System/exit 1))

(defn -main [& args]
  (if (or (empty? args)
          (= "-h" (first args))
          (= "--help" (first args)))
    (usage)
    (let [question (str/join " " args)]
      (println (format "🔍 Querying Perplexity (sonar-pro): %s..." (str/join " " (take 10 (str/split question #" ")))))
      (try
        (-> (query-perplexity question)
            (print-result))
        (catch Exception e
          (let [data (ex-data e)]
            (println "❌ Error:" (ex-message e))
            (when (:hint data)
              (println "💡" (:hint data)))
            (when (:err data)
              (println "📋 Details:" (:err data)))
            (System/exit 1)))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
