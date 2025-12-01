#!/usr/bin/env bb

(ns pretty-tasks
  (:require
   [clojure.java.shell :as shell]
   [clojure.string :as str]))

(def colors
  {:reset "\033[0m"
   :bold "\033[1m"
   :blue "\033[34m"
   :cyan "\033[36m"
   :green "\033[32m"
   :gray "\033[39m"
   :yellow "\033[33m"})

(defn colorize [color text]
  (str (color colors) text (:reset colors)))

(defn format-task-line [line]
  (let [trimmed-line (str/trim line)]
    (cond
      (str/blank? trimmed-line) ""
      (str/starts-with? trimmed-line "The following") nil ; Skip header
      (re-matches #"^[a-zA-Z][a-zA-Z0-9\-_]*\s+.*" trimmed-line)
      (let [parts (str/split trimmed-line #"\s+" 2)
            task-name (first parts)
            description (second parts)]
        (str (colorize :cyan (format "%-20s" task-name))
          " "
          (colorize :gray description)))
      :else trimmed-line)))

(defn main []
  (let [result (shell/sh "bb" "tasks")]
    (if (zero? (:exit result))
      (do
        (println)
        (println (colorize :bold "📋 Available Babashka Tasks"))
        (println (colorize :gray (str (apply str (repeat 60 "─")))))
        (println)
        (doseq [line (str/split-lines (:out result))
                :let [formatted (format-task-line line)]
                :when formatted]
          (println formatted))
        (println)
        (println (colorize :gray "💡 Run any task with: bb [task-name] [args...]"))
        (println))
      (do
        (println (colorize :red "❌ Error running 'bb tasks'"))
        (println (:err result))
        (System/exit (:exit result))))))

(when (= *file* (System/getProperty "babashka.file"))
  (main))
