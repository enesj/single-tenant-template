#!/usr/bin/env bb

(ns scripts.bb.articles.create-article)

(defn -main
  [& args]
  (binding [*out* *err*]
    (println "create_article.clj is deprecated; use create_articles.clj instead."))
  (let [main-fn (requiring-resolve 'scripts.bb.articles.create-articles/-main)]
    (apply main-fn args)))

(apply -main *command-line-args*)
