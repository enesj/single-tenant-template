(ns build
  "Production build script using clojure.tools.build.api.

  Usage:
    clj -T:build clean             ; remove target/
    clj -T:build release-frontend  ; shadow-cljs release :app :admin + postcss
    clj -T:build uber              ; full build → target/app-<version>-standalone.jar

  Pass :version to override APP_VERSION env var:
    clj -T:build uber :version '\"1.2.3\"'"
  (:require [clojure.tools.build.api :as b]))

(def ^:private class-dir "target/classes")

(defn- app-version []
  (or (System/getenv "APP_VERSION") "0.1.0-SNAPSHOT"))

(defn- uber-file [version]
  (format "target/app-%s-standalone.jar" version))

;; Production basis: base deps only (no :dev / :test / :nrepl aliases)
(def ^:private basis
  (delay (b/create-basis {:project "deps.edn"})))

;; ─── Tasks ────────────────────────────────────────────────────────────────────

(defn clean
  "Delete the target/ directory."
  [_]
  (println "🧹 Cleaning target/...")
  (b/delete {:path "target"}))

(defn release-frontend
  "Build the ClojureScript frontend and CSS for production."
  [_]
  (println "🎨 Building CSS (postcss)...")
  (b/process {:command-args ["npm" "run" "postcss"]})
  (println "⚡ Building ClojureScript (shadow-cljs release app admin)...")
  (b/process {:command-args ["npx" "shadow-cljs" "release" "app" "admin"]}))

(defn uber
  "Full production build: clean → frontend → AOT backend → uberjar.

  Options:
    :version  Jar version string (default: APP_VERSION env or '0.1.0-SNAPSHOT')"
  [{:keys [version] :as _opts}]
  (let [ver  (or version (app-version))
        out  (uber-file ver)]
    (clean nil)
    (release-frontend nil)

    (println "📦 Copying sources and resources into" class-dir "...")
    (b/copy-dir {:src-dirs   ["src" "config" "resources" "vendor"]
                 :target-dir class-dir})

    (println "🔨 AOT compiling entry point (app.main and transitive deps)...")
    (b/compile-clj {:basis      @basis
                    :src-dirs   ["src"]
                    :class-dir  class-dir
                    :ns-compile '[app.main app.migrate]})

    (println "🗜️  Assembling uberjar →" out "...")
    (b/uber {:class-dir class-dir
             :uber-file out
             :basis     @basis
             :main      'app.main})

    (println (str "✅ Production build complete: " out))))
