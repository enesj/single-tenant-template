#!/usr/bin/env clj

(ns scripts.bb.database.seed-categories
  (:require
    [aero.core :as aero]
    [clojure.string :as str]
    [next.jdbc :as jdbc])
  (:import
    [java.util UUID]))

(def categories
  "General-purpose categories for article categorization.

  This list is intentionally small at the top level:
  - Max 5 food categories.
  - 20 non-food categories.

  Notes:
  - Uses `categories.name` as the stable identifier (unique index).
  - This script is idempotent: it will insert missing rows and refresh descriptions.
  - It also prunes categories not in this list.
  - Before pruning, it remaps `subcategories.category_id` from legacy category names to the
    new consolidated names (so subcategories aren’t lost)."
  [;; Food (5)
   {:name "Produce" :description "Fresh fruits, vegetables, herbs."}
   {:name "Dairy & Eggs" :description "Milk, cheese, yogurt, butter, eggs."}
   {:name "Meat, Seafood & Deli" :description "Meat, seafood, deli meats and related items."}
   {:name "Bakery & Desserts" :description "Bread, pastries, cakes, desserts."}
   {:name "Packaged Foods & Drinks" :description "Pantry, snacks, frozen foods, beverages, coffee/tea, alcohol."}

   ;; Non-food (20)
   {:name "Cleaning Supplies" :description "Cleaning chemicals, detergents, sanitizers."}
   {:name "Paper Goods" :description "Paper towels, napkins, tissues, toilet paper."}
   {:name "Disposables & Packaging" :description "To-go containers, bags, wraps, labels."}
   {:name "Kitchen Supplies" :description "Foil, film, parchment, storage, prep items."}
   {:name "Smallwares & Utensils" :description "Cutlery, tongs, ladles, tools, smallwares."}
   {:name "Equipment & Appliances" :description "Appliances and equipment."}
   {:name "Maintenance & Repair" :description "Repairs, parts, maintenance supplies."}
   {:name "Office Supplies" :description "Stationery, printer supplies, admin items."}
   {:name "Household Essentials" :description "Batteries, light bulbs, general household essentials."}
   {:name "Storage & Organization" :description "Bins, shelves, organizers, containers."}
   {:name "Personal Care" :description "Toiletries, hygiene and grooming items."}
   {:name "Health & Pharmacy" :description "Over-the-counter health products and pharmacy items."}
   {:name "Baby Products" :description "Diapers, wipes and baby care items."}
   {:name "Pet Supplies" :description "Pet food, litter, grooming and accessories."}
   {:name "Electronics & Accessories" :description "Electronics, cables, batteries, accessories."}
   {:name "Clothing & Accessories" :description "Apparel, shoes, accessories."}
   {:name "Home & Garden" :description "Home improvement, garden and outdoor items."}
   {:name "Hardware & Tools" :description "Tools, hardware, fasteners and related supplies."}
   {:name "Toys & Games" :description "Toys, games and hobby items."}
   {:name "Other" :description "Miscellaneous items that don’t fit elsewhere."}])

(def subcategory-remaps
  "Legacy category names to consolidate into the new top-level categories.

  This is best-effort; missing legacy categories are ignored."
  [["Meat & Poultry" "Meat, Seafood & Deli"]
   ["Seafood" "Meat, Seafood & Deli"]
   ["Bakery" "Bakery & Desserts"]

   ["Pantry Staples" "Packaged Foods & Drinks"]
   ["Grains & Pasta" "Packaged Foods & Drinks"]
   ["Canned & Jarred" "Packaged Foods & Drinks"]
   ["Condiments & Sauces" "Packaged Foods & Drinks"]
   ["Spices & Seasonings" "Packaged Foods & Drinks"]
   ["Baking Supplies" "Packaged Foods & Drinks"]
   ["Snacks" "Packaged Foods & Drinks"]
   ["Frozen Foods" "Packaged Foods & Drinks"]
   ["Beverages" "Packaged Foods & Drinks"]
   ["Coffee & Tea" "Packaged Foods & Drinks"]
   ["Alcohol" "Packaged Foods & Drinks"]])

(defn get-db-config [profile]
  (let [config (aero/read-config "config/base.edn" {:profile profile})]
    (:database config)))

(defn datasource [{:keys [host port dbname user password]}]
  (jdbc/get-datasource {:dbtype "postgresql"
                        :host host
                        :port port
                        :dbname dbname
                        :user user
                        :password password}))

(defn- update-count [result]
  (or (:next.jdbc/update-count result)
    (:update-count result)
    0))

(defn- upsert-categories! [tx]
  (let [sql (str "insert into categories (id, name, description, created_at, updated_at) "
              "values (?, ?, ?, now(), now()) "
              "on conflict (name) do update "
              "  set description = excluded.description, "
              "      updated_at = now()")]
    (doseq [{:keys [name description]} categories]
      (jdbc/execute-one! tx [sql (UUID/randomUUID) name description]))))

(defn- remap-subcategories! [tx]
  (let [sql (str "update subcategories s "
              "   set category_id = c_to.id "
              "  from categories c_from, categories c_to "
              " where c_from.name = ? "
              "   and c_to.name = ? "
              "   and s.category_id = c_from.id")]
    (reduce
      (fn [acc [from-name to-name]]
        (+ acc (update-count (jdbc/execute-one! tx (into [sql] [from-name to-name])))))
      0
      subcategory-remaps)))

(defn- prune-categories! [tx seed-names]
  (let [placeholders (str/join ", " (repeat (count seed-names) "?"))
        sql (str "delete from categories where name not in (" placeholders ")")]
    (update-count (jdbc/execute-one! tx (into [sql] seed-names)))))

(defn seed-categories! [ds]
  (let [seed-names (mapv :name categories)]
    (jdbc/with-transaction [tx ds]
      (upsert-categories! tx)
      (let [remapped (remap-subcategories! tx)
            pruned (prune-categories! tx seed-names)]
        {:remapped-subcategories remapped
         :pruned-categories pruned}))))

(defn total-count [ds]
  (:total
   (jdbc/execute-one!
     ds
     ["select count(*)::int as total from categories"])))

(defn -main [& args]
  (let [env (or (first args) "dev")
        profile (keyword env)]
    (when-not (#{:dev :test} profile)
      (println "❌ Invalid environment. Use: dev or test")
      (System/exit 1))
    (try
      (let [db (get-db-config profile)
            ds (datasource db)
            before (total-count ds)
            {:keys [remapped-subcategories pruned-categories]} (seed-categories! ds)
            after (total-count ds)]
        (when-not (= after (count categories))
          (throw (ex-info "Categories seed did not converge to the expected total" {:expected (count categories)
                                                                                    :actual after})))
        (println "✅ Categories ensured:")
        (println "   Seeded set:          " (count categories))
        (println "   Remapped subcategories" remapped-subcategories)
        (println "   Pruned categories:    " pruned-categories)
        (println "   Total rows:           " after "(was" before ")")
        (println "   DB:                  " (str (:host db) ":" (:port db) "/" (:dbname db))))
      (catch Exception e
        (println "❌ Failed to seed categories:" (.getMessage e))
        (System/exit 1)))))

(apply -main *command-line-args*)
