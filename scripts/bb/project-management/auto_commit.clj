#!/usr/bin/env bb

(require '[babashka.process :as p])
(require '[clojure.string :as str])
(require '[cheshire.core :as json])

(defn get-openai-api-key
  "Get OpenAI API key from environment or .api_credentials.sh"
  []
  (or (System/getenv "OPENAI_API_KEY")
    (try
      (let [result (p/shell {:out :string} "bash" "-c" "source .api_credentials.sh && echo $OPENAI_API_KEY")]
        (str/trim (:out result)))
      (catch Exception e
        (throw (Exception. "Could not load OpenAI API key from .api_credentials.sh"))))))

(defn call-openai-api
  "Use OpenAI API to generate AI-powered commit message"
  [diff-content files-list]
  (try
    (println "🤖 Calling OpenAI API to analyze diff...")

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
                   "Return ONLY the commit message text with no additional commentary, markdown formatting, or code blocks.")

          ;; Debug output
          _ (do
              (println "\n=== DEBUG: FILES LIST ===")
              (doseq [file files-list]
                (println "  " file))
              (println "\n=== DEBUG: DIFF INFO ===")
              (println (str "Total diff length: " (count diff-content) " chars"))
              (println (str "Sending to API: " (min 8000 (count diff-content)) " chars"))
              (println "\n=== DEBUG: DIFF CONTENT (first 15000 chars) ===")
              (println (subs diff-content 0 (min 8000 (count diff-content))))
              (println "...\n"))

          api-key (get-openai-api-key)

          ;; Prepare request body
          request-body (json/generate-string
                         {:model "gpt-4.1"
                          :messages [{:role "user" :content prompt}]
                          :max_tokens 1000
                          :temperature 0.3})

          ;; Call OpenAI API
          result (p/shell {:out :string :err :string :continue true :in request-body}
                   "curl" "-s" "-X" "POST"
                   "https://api.openai.com/v1/chat/completions"
                   "-H" "Content-Type: application/json"
                   "-H" (str "Authorization: Bearer " api-key)
                   "-d" "@-")]

      (if (= 0 (:exit result))
        (let [response (json/parse-string (:out result) true)
              ai-response (str/trim (get-in response [:choices 0 :message :content]))]
          (if (str/blank? ai-response)
            (throw (Exception. "OpenAI returned empty response"))
            (do
              (println "✅ AI analysis completed successfully!")
              ai-response)))
        (throw (Exception. (str "OpenAI API failed: " (:err result))))))

    (catch Exception e
      (let [error-msg (str "⚠️  OpenAI API failed: " (.getMessage e))]
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

      ;; Use OpenAI API
      (let [commit-msg (call-openai-api final-diff-content staged-files)]
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
