(ns app.domain.shared.routes.contract
  "Shared route descriptor contract helpers used by frontend/backend route builders."
  (:require
    [clojure.string :as str]))

(def required-descriptor-keys
  "Required keys for each canonical route descriptor."
  #{:id :path})

(defn valid-descriptor?
  "Return true when descriptor has required keys and a non-blank path string."
  [descriptor]
  (and (map? descriptor)
    (every? #(contains? descriptor %) required-descriptor-keys)
    (keyword? (:id descriptor))
    (string? (:path descriptor))
    (not (str/blank? (:path descriptor)))))

(defn descriptor-paths
  "Extract descriptor paths as a vector, skipping nil values."
  [descriptors]
  (->> descriptors
    (keep :path)
    (vec)))

(defn duplicate-paths
  "Return duplicate descriptor paths in stable first-seen order."
  [descriptors]
  (let [paths (descriptor-paths descriptors)]
    (->> paths
      (frequencies)
      (keep (fn [[path n]]
              (when (> n 1)
                path)))
      (vec))))

(defn unique-paths?
  "Return true when descriptor paths are unique."
  [descriptors]
  (empty? (duplicate-paths descriptors)))

(defn spa-fallback-paths
  "Return distinct SPA fallback paths from descriptors where `:spa-fallback?` is truthy."
  [descriptors]
  (->> descriptors
    (filter :spa-fallback?)
    (keep :path)
    (distinct)
    (vec)))
