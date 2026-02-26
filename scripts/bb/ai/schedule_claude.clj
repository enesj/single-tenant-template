#!/usr/bin/env bb
(ns schedule-claude
  "Schedule Claude CLI commands to run at a specific time using the 'at' command."
  (:require [babashka.process :refer [shell]]
    [clojure.string :as str]))

(defn print-usage
  "Print usage information."
  []
  (println "Schedule Claude CLI commands to run at a specific time.")
  (println)
  (println "Usage:")
  (println "  bb schedule-claude --prompt \"YOUR_PROMPT\" --time \"TIME_SPEC\"")
  (println "  bb schedule-claude -p \"YOUR_PROMPT\" -t \"TIME_SPEC\" [-l LOG_PATH]")
  (println)
  (println "Options:")
  (println "  -p, --prompt   The prompt to send to Claude CLI")
  (println "  -t, --time     Time specification for the 'at' command")
  (println "  -l, --log      Log file path (default: tmp/claude-scheduled.log)")
  (println "  -h, --help     Show this help message")
  (println)
  (println "Time Examples:")
  (println "  \"2AM tomorrow\"       - Run at 2 AM tomorrow")
  (println "  \"3PM today\"          - Run at 3 PM today")
  (println "  \"10:30 AM\"           - Run at 10:30 AM today")
  (println "  \"now + 1 hour\"       - Run in 1 hour")
  (println "  \"now + 30 minutes\"   - Run in 30 minutes")
  (println "  \"midnight\"           - Run at midnight")
  (println "  \"teatime\"            - Run at 4 PM")
  (println)
  (println "Examples:")
  (println "  bb schedule-claude -p \"Run the remove-unused-vars prompt\" -t \"2AM tomorrow\"")
  (println "  bb schedule-claude --prompt \"Review the codebase\" --time \"now + 1 hour\"")
  (println "  bb schedule-claude -p \"Clean up code\" -t \"3PM today\" -l tmp/cleanup.log")
  (println)
  (println "Logging:")
  (println "  Output is logged to the specified log file.")
  (println "  Check execution status with: tail -n 50 tmp/claude-scheduled.log")
  (println)
  (println "macOS note:")
  (println "  Jobs are queued by `at`, but execution requires the atrun daemon.")
  (println "  Enable it:")
  (println "    sudo launchctl load -w /System/Library/LaunchDaemons/com.apple.atrun.plist")
  (println "  Check status:")
  (println "    launchctl list com.apple.atrun")
  (println)
  (println "Note: The `at` command must be installed."))

(defn parse-args
  "Parse command line arguments."
  [args]
  (loop [remaining args
         opts {:prompt nil :time nil :log "tmp/claude-scheduled.log"}]
    (if (empty? remaining)
      opts
      (let [arg (first remaining)]
        (cond
          (or (= arg "-h") (= arg "--help"))
          {:help true}

          (or (= arg "-p") (= arg "--prompt"))
          (if (empty? (rest remaining))
            (do (println "Error: --prompt requires a value") {:error true})
            (recur (drop 2 remaining) (assoc opts :prompt (second remaining))))

          (or (= arg "-t") (= arg "--time"))
          (if (empty? (rest remaining))
            (do (println "Error: --time requires a value") {:error true})
            (recur (drop 2 remaining) (assoc opts :time (second remaining))))

          (or (= arg "-l") (= arg "--log"))
          (if (empty? (rest remaining))
            (do (println "Error: --log requires a value") {:error true})
            (recur (drop 2 remaining) (assoc opts :log (second remaining))))

          :else
          (do (println (str "Unknown argument: " arg)) {:error true}))))))

(defn validate-opts
  "Validate that required options are present."
  [{:keys [prompt time] :as opts}]
  (cond
    (or (:help opts) (:error opts))
    opts

    (str/blank? prompt)
    (do (println "Error: --prompt is required") (assoc opts :error true))

    (str/blank? time)
    (do (println "Error: --time is required") (assoc opts :error true))

    :else
    opts))

(defn check-at-daemon
  "Check if the at daemon is running. Returns true if running or on Linux."
  []
  (let [os-name (System/getProperty "os.name")]
    (if (str/includes? os-name "Mac")
      ;; macOS: check if atrun is loaded
      (try
        (let [result (shell {:out :string :err :string} "launchctl" "list" "com.apple.atrun")]
          (= 0 (:exit result)))
        (catch Exception _ false))
      ;; Linux/other: assume atd is running
      true)))

(defn sh-single-quote
  "Shell-quote a string using single quotes, escaping internal single quotes.\n\nThis prevents shell expansions like $(...) inside the prompt from being executed\nwhen the `at` job runs."
  [s]
  (str "'" (str/replace s "'" "'\\''") "'"))

(defn schedule-command
  "Schedule the Claude CLI command using 'at'."
  [prompt time-spec log-path]
  (let [os-name (System/getProperty "os.name")
        macos? (str/includes? os-name "Mac")
        daemon-running? (check-at-daemon)
        prompt-one-line (-> prompt
                          (str/replace #"\R" " ")
                          (str/trim))
        prompt-q (sh-single-quote prompt-one-line)
        log-path-q (sh-single-quote log-path)
        log-dir (or (.getParent (java.io.File. log-path)) ".")
        log-dir-q (sh-single-quote log-dir)
        ;; Ensure directory exists, then log execution
        claude-cmd (str "mkdir -p " log-dir-q " && "
                     "echo \"=== Claude execution started at $(date) ===\" >> " log-path-q " && "
                     "claude --dangerously-skip-permissions " prompt-q " >> " log-path-q " 2>&1 && "
                     "echo \"=== Claude execution completed at $(date) ===\" >> " log-path-q)]
    (println "📅 Scheduling Claude command...")
    (println (str "   Prompt: " prompt-one-line))
    (println (str "   Time: " time-spec))
    (println (str "   Log: " log-path))
    (println)
    (when (and macos? (not daemon-running?))
      (println "⚠️  WARNING: The 'at' daemon (atrun) does not appear to be running!")
      (println "   This job will be queued, but it may not execute until atrun is enabled.")
      (println "   On macOS, enable it with:")
      (println "     sudo launchctl load -w /System/Library/LaunchDaemons/com.apple.atrun.plist")
      (println))
    (try
      ;; Feed the shell command to `at` via stdin.
      (let [result (shell {:in claude-cmd :out :string :err :string} "at" time-spec)]
        (println "✅ Command queued via at.")
        (when (not (str/blank? (:out result)))
          (println (:out result)))
        (when (not (str/blank? (:err result)))
          (println (:err result)))
        (println)
        (println "To view scheduled jobs, run: atq")
        (println "To remove a job, run: atrm <job-number>")
        (println)
        (println "To confirm it ran:")
        (println "  1) It disappears from `atq` after its scheduled time")
        (println (str "  2) The log contains start/completed markers: tail -n 50 " log-path)))
      (catch Exception e
        (println "❌ Failed to schedule command:")
        (println (str "   " (.getMessage e)))
        (println)
        (println "Make sure the 'at' command is installed and the at daemon is running.")
        (System/exit 1)))))

(defn main
  "Main entry point."
  [args]
  (let [opts (-> args parse-args validate-opts)]
    (cond
      (:help opts)
      (print-usage)

      (:error opts)
      (do (print-usage) (System/exit 1))

      :else
      (schedule-command (:prompt opts) (:time opts) (:log opts)))))

(when (= *file* (System/getProperty "babashka.file"))
  (main *command-line-args*))