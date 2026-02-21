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

(defn run-command!
  "Run a command and stream output. Returns true when exit code is 0."
  [label & cmd]
  (println (str "🧪 " label "..."))
  (let [result (apply p/shell (concat [{:out :inherit :err :inherit :continue true}] cmd))]
    (if (= 0 (:exit result))
      (do
        (println (str "✅ " label " passed"))
        true)
      (do
        (println (str "❌ " label " failed"))
        false))))

(defn run-required-tests!
  "Run required backend/frontend tests before AI analysis."
  []
  (println "\n=== Required Test Gate ===")
  (println "AI commit message generation is blocked until tests are clean.")
  (let [backend-ok?  (run-command! "Backend tests (bb be-test)" "bb" "be-test")
        frontend-ok? (when backend-ok?
                       (run-command! "Frontend tests (bb fe-test-parallel)" "bb" "fe-test-parallel"))]
    (if (and backend-ok? frontend-ok?)
      (do
        (println "✅ All required tests passed. Proceeding with AI analysis.\n")
        true)
      (do
        (println "❌ Tests are not clean. Commit aborted before AI analysis.")
        (System/exit 1)))))

(defn commit-with-message!
  "Commit staged changes with generated commit message. Returns true when commit succeeds."
  [commit-msg]
  (let [full-msg (str commit-msg
                   "\n\n🤖 Generated with [Claude Code](https://claude.ai/code)"
                   "\n\nCo-Authored-By: Claude <noreply@anthropic.com>")]
    (run-command! "Commit changes" "git" "commit" "-m" full-msg)))

(defn push-origin!
  "Push current HEAD to origin. Returns true when push succeeds."
  []
  (run-command! "Push to origin" "git" "push" "origin" "HEAD"))

(defn parse-status-line
  "Parse a `git status --porcelain` line safely."
  [line]
  (when (and (not (str/blank? line))
          (>= (count line) 3))
    {:index-status    (subs line 0 1)
     :worktree-status (subs line 1 2)
     :path            (str/trim (subs line 3))}))

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
(let [arg-set    (set *command-line-args*)
      push-mode? (or (contains? arg-set "push")
                  (contains? arg-set "--push"))
      initial-status-result (p/shell {:out :string} "git status --porcelain")
      initial-status-lines (->> (str/split-lines (:out initial-status-result))
                             (map parse-status-line)
                             (remove nil?))
      initial-unstaged-files (->> initial-status-lines
                               (filter (fn [{:keys [index-status worktree-status]}]
                                         (or (= index-status "?") ; untracked
                                           (not= worktree-status " "))))
                               (map :path)
                               (remove str/blank?))
      _          (when (or push-mode? (seq initial-unstaged-files))
                   (if push-mode?
                     (println "🚀 Push mode enabled: staging all changes first.")
                     (do
                       (println "⚠️  Unstaged changes detected. Auto-staging them before commit flow.")
                       (doseq [file initial-unstaged-files]
                         (println "  " file))))
                   (when-not (run-command! "Stage all changes (git add .)" "git" "add" ".")
                     (println "❌ Could not stage changes.")
                     (System/exit 1)))
      status-result (p/shell {:out :string} "git status --porcelain")
      status-lines (->> (str/split-lines (:out status-result))
                     (map parse-status-line)
                     (remove nil?))

      ;; Files that are staged for commit (first status char not space)
      staged-files (->> status-lines
                     (filter #(re-matches #"[MARCD]" (:index-status %)))
                     (map :path)
                     (remove str/blank?))

      ;; Files with unstaged modifications or untracked files
      unstaged-files (->> status-lines
                       (filter (fn [{:keys [index-status worktree-status]}]
                                 (or (= index-status "?") ; untracked
                                   (not= worktree-status " "))))
                       (map :path)
                       (remove str/blank?))

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
    (empty? staged-files)
    (do (println "No staged changes to commit.")
      (System/exit 0))

    (str/blank? diff-content)
    (do (println "No diff content found. Make sure files are properly staged.")
      (System/exit 1))

    :else
    (do
      (when (seq unstaged-files)
        (println "⚠️  Some files are still unstaged after auto-staging; proceeding with staged changes only.")
        (doseq [file unstaged-files]
          (println "  " file))
        (println))

      (println "=== Staged Changes ===")
      (doseq [file staged-files]
        (println "  " file))
      (println)

      ;; Block AI analysis until tests are clean
      (run-required-tests!)

      ;; Use Cerebras API
      (let [commit-msg (call-cerebras-api final-diff-content staged-files)]
        (println "\n=== Generated Commit Message ===")
        (println commit-msg)
        (println)

        (if push-mode?
          (do
            (println "🚀 Push mode: committing and pushing to origin.")
            (if (commit-with-message! commit-msg)
              (if (push-origin!)
                (println "✅ Changes committed and pushed to origin successfully!")
                (do
                  (println "❌ Push failed.")
                  (System/exit 1)))
              (do
                (println "❌ Commit failed.")
                (System/exit 1))))
          (do
            (print "Proceed with this commit? (y/N): ")
            (flush)
            (let [input (read-line)
                  user-input (if input (str/lower-case (str/trim input)) "n")]
              (if (= user-input "y")
                (if (commit-with-message! commit-msg)
                  (println "✅ Changes committed successfully!")
                  (do
                    (println "❌ Commit failed.")
                    (System/exit 1)))
                (println "❌ Commit cancelled.")))))))))
