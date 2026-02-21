#!/usr/bin/env bb

(require
  '[babashka.process :as p]
  '[cheshire.core :as json]
  '[clojure.string :as str])

(def default-cerebras-model "gpt-oss-120b")

(defn get-cerebras-api-key
  "Get Cerebras API key from environment or .env"
  []
  (let [env-key (System/getenv "CEREBRAS_API_KEY")]
    (if (not (str/blank? env-key))
      env-key
      (let [result (try
                     (p/shell {:out :string}
                              "bash" "-c"
                              "set -a; [ -f .env ] && source .env; set +a; echo $CEREBRAS_API_KEY")
                     (catch Exception _e
                       (throw (Exception. "Could not load CEREBRAS_API_KEY from .env"))))
            file-key (str/trim (:out result))]
        (if (str/blank? file-key)
          (throw (Exception. "CEREBRAS_API_KEY is not set in the environment or .env"))
          file-key)))))

(defn get-cerebras-model
  "Get Cerebras model from environment, defaulting to a supported production model."
  []
  (let [model (System/getenv "CEREBRAS_MODEL")]
    (if (str/blank? model)
      default-cerebras-model
      model)))

(defn call-cerebras-api
  "Use Cerebras API to generate AI-powered commit message"
  [diff-content files-list]
  (try
    (println "🤖 Calling Cerebras API to analyze diff...")

    ;; Prepare detailed prompt
    (let [prompt (str "Analyze this git diff and generate a professional commit message.\n\n"
                   "FILES CHANGED:\n"
                   (str/join "\n" (map #(str "- " %) files-list))
                   "\n\nDIFF CONTENT:\n"
                   (subs diff-content 0 (min 15000 (count diff-content)))
                   (when (> (count diff-content) 15000) "\n... (truncated)")
                   "\n\nPlease generate a commit message that:\n"
                   "1. Uses conventional commit format (feat:, fix:, refactor:, chore:, test:, docs:, style:)\n"
                   "2. Has a clear, descriptive title (50 characters or less)\n"
                   "3. Includes a detailed body explaining WHAT changed and WHY\n"
                   "4. Mentions specific functions/components that were added or modified\n"
                   "5. Focuses on the business value or technical benefit\n"
                   "6. Uses professional, technical language appropriate for a development team\n\n"
                   "Return ONLY the commit message text with no additional commentary, markdown formatting, or code blocks.")]

      ;; Debug output
      (println "\n=== DEBUG: FILES LIST ===")
      (doseq [file files-list]
        (println "  " file))
      (println "\n=== DEBUG: DIFF INFO ===")
      (println (str "Total diff length: " (count diff-content) " chars"))
      (println (str "Sending to API: " (min 16000 (count diff-content)) " chars"))
      (println "\n=== DEBUG: DIFF CONTENT (first 16000 chars) ===")
      (println (subs diff-content 0 (min 16000 (count diff-content))))
      (println "...\n")

      (let [api-key       (get-cerebras-api-key)
            model         (get-cerebras-model)

            ;; Prepare request body
            request-body  (json/generate-string
                            {:model       model
                             :messages    [{:role    "user"
                                            :content prompt}]
                             :max_tokens  1000
                             :temperature 0.3})

            ;; Call Cerebras API
            result        (p/shell {:out      :string
                                    :err      :string
                                    :continue true
                                    :in       request-body}
                            "curl" "-sS" "-X" "POST" "--fail-with-body"
                            "https://api.cerebras.ai/v1/chat/completions"
                            "-H" "Content-Type: application/json"
                            "-H" (str "Authorization: Bearer " api-key)
                            "-d" "@-")
            raw-response  (:out result)
            response      (when-not (str/blank? raw-response)
                            (try
                              (json/parse-string raw-response true)
                              (catch Exception _e
                                (throw (Exception.
                                         (str "Cerebras API returned non-JSON response: "
                                           (subs raw-response 0 (min 400 (count raw-response)))))))))
            error-message (or (get-in response [:error :message])
                            (get-in response [:error :type]))]
        (println (str "Using Cerebras model: " model))
        (cond
          (not= 0 (:exit result))
          (throw (Exception. (or error-message (str/trim (:err result)) "Unknown error")))

          error-message
          (throw (Exception. error-message))

          (nil? response)
          (throw (Exception. "Cerebras API returned empty response body"))

          :else
          (let [ai-response (some-> (get-in response [:choices 0 :message :content])
                              str/trim)]
            (if (str/blank? ai-response)
              (throw (Exception. "Cerebras returned empty response"))
              (do
                (println "✅ AI analysis completed successfully!")
                ai-response))))))

    (catch Exception e
      (let [error-msg (str "⚠️  Cerebras API failed: " (or (ex-message e) (str e)))]
        (println error-msg)
        error-msg))))

;; Main execution
(let [status-result (p/shell {:out :string} "git status --porcelain")
      ;; Files that are staged for commit (first status char not space)
      staged-files (->> (str/split-lines (:out status-result))
                     (filter #(re-matches #"^[MARCD].*" %))
                     (map #(subs % 3)))

      ;; Files with unstaged modifications or untracked files
      unstaged-files (->> (str/split-lines (:out status-result))
                       (filter (fn [l]
                                 (let [idx (subs l 0 1)
                                       wt  (subs l 1 2)]
                                   (or (= idx "?") ; untracked
                                     (not= wt " ")))))
                       (map #(subs % 3)))

      ;; Get the actual diff content for AI analysis
      diff-result (p/shell {:out :string} "git diff --cached")
      diff-content (:out diff-result)

      ;; Get diff for just code changes (non-deletions) to prioritize
      code-diff-result (p/shell {:out :string} "git diff --cached --diff-filter=MARC")
      code-diff-content (:out code-diff-result)

      ;; Filter out .md files, .clojure-mcp/scratch_pad.edn, and migration files from diff content to reduce token usage
      ;; Split diff into sections and remove unwanted file sections
      filtered-diff-content (let [base-diff (if (str/blank? code-diff-content) diff-content code-diff-content)]
                              (if (str/blank? base-diff)
                                base-diff
                                (->> (str/split base-diff #"(?=diff --git)")
                                  (filter (fn [section]
                                            (and (not (re-find #"\.md\b" section))
                                              (not (re-find #"\.clojure-mcp/scratch_pad\.edn" section))
                                              (not (re-find #"resources/db/migrations/" section)))))
                                  (str/join ""))))

      ;; Use filtered diff content (without .md files, scratch_pad.edn, and migration files)
      final-diff-content filtered-diff-content]

  (cond
    ;; Warn if there are unstaged changes
    (seq unstaged-files)
    (do (println "⚠️  Unstaged changes detected. Stage or discard them before committing.")
      (doseq [file unstaged-files]
        (println "  " file))
      (System/exit 1))

    (empty? staged-files)
    (do (println "No staged changes to commit.")
      (System/exit 0))

    (str/blank? diff-content)
    (do (println "No diff content found. Make sure files are properly staged.")
      (System/exit 1))

    :else
    (do
      (println "=== Staged Changes ===")
      (doseq [file staged-files]
        (println "  " file))
      (println)

      ;; Use Cerebras API
      (let [commit-msg (call-cerebras-api final-diff-content staged-files)]
        (println "\n=== Generated Commit Message ===")
        (println commit-msg)
        (println)

        (print "Proceed with this commit? (y/N): ")
        (flush)
        (let [input (read-line)
              user-input (if input (str/lower-case (str/trim input)) "n")]
          (if (= user-input "y")
            (let [full-msg (str commit-msg "\n\n🤖 Generated with [Claude Code](https://claude.ai/code)\n\nCo-Authored-By: Claude <noreply@anthropic.com>")]
              (p/shell ["git" "commit" "-m" full-msg])
              (println "✅ Changes committed successfully!"))
            (println "❌ Commit cancelled.")))))))
