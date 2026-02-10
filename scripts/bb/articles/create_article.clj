#!/usr/bin/env bb

(ns scripts.bb.articles.create-article
  (:require
    [clojure.java.io :as io]))

(load-file (.getPath (io/file "scripts/bb/articles/create_articles.clj")))

(defn -main
  [& args]
  (binding [*out* *err*]
    (println "create_article.clj is deprecated; use create_articles.clj instead."))
  (apply scripts.bb.articles.create-articles/-main args))

(apply -main *command-line-args*)
