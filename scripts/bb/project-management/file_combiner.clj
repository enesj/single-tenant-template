(ns file-combiner
  (:require
   [babashka.fs :as fs]
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(defn read-gitignore []
  (let [gitignore-file ".gitignore"]
    (if (fs/exists? gitignore-file)
      (str/split-lines (slurp gitignore-file))
      [])))

(defn glob->regex [pattern]
  (-> pattern
    (str/replace "." "\\.")
    (str/replace "*" ".*")
    (str/replace "?" ".")
    (as-> p (str "^" p "$"))))

(defn matches-gitignore? [path gitignore-patterns]
  (let [path-str (str path)]
    (some (fn [pattern]
            (let [pattern (str/trim pattern)]
              (cond
                (str/blank? pattern) false
                (str/starts-with? pattern "#") false
                :else
                (let [regex (re-pattern (glob->regex pattern))]
                  (or (re-find regex path-str)
                    (re-find regex (fs/file-name path)))))))
      gitignore-patterns)))

(defn ignore-patterns [include-folders]
  (let [standard-ignore-patterns ["node_modules" "package-lock" "resources/public/js"
                                  "resources/public/assets/js" "out/" "combiner" "iml" "idea" ".css" ".jpg" ".png" ".jpeg"]
        project-ignore-patterns ["airbnb" "prompts"]
        all-folders-to-ignore ["backend"
                               "frontend"
                               "test"
                               "dev"
                               "vendor"
                               "scripts"
                               "config"
                               "none"]
        folders-to-ignore (if (seq include-folders)
                            (remove (set include-folders) all-folders-to-ignore)
                            [])
        files-to-ignore ["none"]]
    (concat standard-ignore-patterns project-ignore-patterns folders-to-ignore files-to-ignore)))

(defn remove-patterns [ignore-patterns coll]
  (loop [patterns ignore-patterns
         coll coll]
    (if (seq patterns)
      (recur (rest patterns) (remove #(str/includes? (.toString %) (first patterns)) coll))
      coll)))

(defn get-project-files [ignore-patterns]
  (let [gitignore-patterns (read-gitignore)]
    (->> (fs/glob "." "**")
      (remove-patterns ignore-patterns)
      (filter fs/regular-file?)
      (remove #(matches-gitignore? % gitignore-patterns))
      (map str))))

(defn combine-files [files output-file]
  ;;(println "Combine files:" files)
  (with-open [writer (io/writer output-file)]
    (doseq [file files]
      (println "file: " file)
      (.write writer (str "\n=== " file " ===\n\n"))
      (.write writer (slurp file))
      (.write writer "\n"))))

(def gpt2-tokenizer
  (delay
    (let [url "https://huggingface.co/gpt2/resolve/main/vocab.json"
          encoder (json/parse-string (slurp url))]
      encoder)))

(defn estimate-tokens [text]
  (let [tokenizer @gpt2-tokenizer
        ;; bpe-ranks (into {} (map-indexed #(vector %2 %1) (keys tokenizer)))
        pattern #"'s|'t|'re|'ve|'m|'ll|'d| ?\p{L}+| ?\p{N}+| ?[^\s\p{L}\p{N}]+|\s+(?!\S)|\s+"]
    (->> (re-seq pattern text)
      (remove str/blank?)
      (mapcat #(if-let [token (get tokenizer %)]
                 [token]
                 (map (fn [c] (get tokenizer (str c))) %)))
      count)))

(defn count-tokens [file-path]
  (let [content (slurp file-path)]
    (estimate-tokens content)))

(defn process-project [& include-folders]
  (println "Processing project...")
  (let [ignore-patterns (ignore-patterns include-folders)
        files (sort (get-project-files ignore-patterns))
        folder-names (if (seq include-folders)
                       (str/join "-" include-folders)
                       "all")
        output-file (str "scripts/" folder-names "-files.txt")]
    (combine-files files output-file)
    (let [token-count (count-tokens output-file)
          claude-token-estimate (int (* token-count 0.75))]
      (println "Created" output-file "with" (count files) "files combined.")
      (println "Estimated GPT-2 token count:" token-count)
      (println "Estimated Claude/Sonnet 3.5 token count:" claude-token-estimate))))

(let [args *command-line-args*]
  (apply process-project args))
