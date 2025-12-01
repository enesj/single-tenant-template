#!/usr/bin/env clj

(ns scripts.bb.database.seed-admin
  (:require
    [aero.core :as aero]
    [clojure.string :as str]
    [next.jdbc :as jdbc])
  (:import
    [java.util UUID]))

(def default-admin
  {:email "admin@example.com"
   :full-name "System Administrator"
   ;; bcrypt+sha512 hash for password "admin123" with 12 iterations
   :password-hash "bcrypt+sha512$604531f02285a11cf0d3612b67bcb199$12$a8924fc3552ff2cb30d6743f13fa7d053cf86d2c2e025806"
   :role "owner"
   :status "active"})

(defn get-db-config [profile]
  (let [config (aero/read-config "config/base.edn" {:profile profile})]
    (:database config)))

(defn column-type [ds table column]
  (-> (jdbc/execute-one!
        ds
        ["select format_type(a.atttypid, a.atttymod) as type
            from pg_attribute a
            join pg_class c on a.attrelid = c.oid
            join pg_namespace n on c.relnamespace = n.oid
           where c.relname = ?
             and n.nspname = current_schema()
             and a.attname = ?
             and not a.attisdropped" table column])
      :type))

(defn enum-labels [ds enum-name]
  (let [enum-name (some-> enum-name (str/split #"\\.") last)]
    (map :enumlabel
         (jdbc/execute!
           ds
           ["select enumlabel
               from pg_enum e
               join pg_type t on e.enumtypid = t.oid
              where t.typname = ?
              order by e.enumsortorder" enum-name]))))

(defn choose-enum-value [labels preferred fallback]
  (or (some (set labels) preferred)
      (first labels)
      fallback))

(defn seed-admin!
  [{:keys [host port dbname user password] :as db} {:keys [email full-name password-hash role status]}]
  (let [ds (jdbc/get-datasource {:dbtype "postgresql"
                                 :host host
                                 :port port
                                 :dbname dbname
                                 :user user
                                 :password password})
        role-type (or (column-type ds "admins" "role")
                      (throw (ex-info "Could not determine role column type" {})))
        status-type (or (column-type ds "admins" "status")
                        (throw (ex-info "Could not determine status column type" {})))
        role-labels (enum-labels ds role-type)
        status-labels (enum-labels ds status-type)
        chosen-role (choose-enum-value role-labels [role] role)
        chosen-status (choose-enum-value status-labels [status] status)
        admin-id (UUID/randomUUID)
        sql (format (str "insert into admins (id, email, full_name, password_hash, role, status, created_at, updated_at)
                           values (?, ?, ?, ?, ?::%s, ?::%s, now(), now())
                           on conflict (email) do update
                             set full_name = excluded.full_name,
                                 password_hash = excluded.password_hash,
                                 role = excluded.role,
                                 status = excluded.status,
                                 updated_at = now()")
                    role-type status-type)]
    (jdbc/execute! ds [sql admin-id email full-name password-hash chosen-role chosen-status])
    {:id admin-id
     :role chosen-role
     :status chosen-status}))

(defn -main [& args]
  (let [env (or (first args) "dev")
        profile (keyword env)]
    (when-not (#{:dev :test} profile)
      (println "❌ Invalid environment. Use: dev or test")
      (System/exit 1))
    (try
      (let [db (get-db-config profile)
            {:keys [id role status]} (seed-admin! db default-admin)]
        (println "✅ Admin user ensured:")
        (println "   Email:    " (:email default-admin))
        (println "   Password: admin123")
        (println "   Role:     " role)
        (println "   Status:   " status)
        (println "   ID:       " id)
        (println "   DB:       " (str (:host db) ":" (:port db) "/" (:dbname db))))
      (catch Exception e
        (println "❌ Failed to seed admin:" (.getMessage e))
        (System/exit 1)))))

(apply -main *command-line-args*)
