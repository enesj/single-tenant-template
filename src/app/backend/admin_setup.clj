(ns app.backend.admin-setup
  "Helper tasks for single-tenant dev setup (seed admin, optional cleanup)."
  (:require
    [app.backend.services.admin :as admin]
    [app.shared.data :as shared-data]
    [honey.sql :as hsql]
    [next.jdbc :as jdbc]
    [system.state :as state]))

(defn get-db
  "Get database connection from running system"
  []
  (if-let [current-state @state/state]
    (:database current-state)
    (throw (ex-info "System not running. Start with (start) in REPL" {}))))

(defn setup-test-admin!
  "Create a test admin user for development."
  ([] (setup-test-admin! (get-db)))
  ([db]
   (println "Setting up test admin user...")
   (try
     (let [existing (admin/find-admin-by-email db "admin@example.com")]
       (if existing
         (println "Admin user already exists: admin@example.com")
         (do
           (admin/create-admin! db {:email "admin@example.com"
                                    :password "admin123"
                                    :full_name "System Administrator"
                                    :role "owner"})
           (println "Created admin user:")
           (println "  Email: admin@example.com")
           (println "  Password: admin123")
           (println "  Role: owner"))))
     (catch Exception e
       (println "Error creating admin:" (.getMessage e))))))

(defn create-test-data!
  "No-op placeholder for multi-tenant seed data. Left here to keep API compatibility."
  ([] (create-test-data! (get-db)))
  ([_db]
   (println "Skipping multi-tenant test data generation (single-tenant template).")))

(defn setup-all!
  "Setup admin user; kept for compatibility with multi-tenant workflow."
  []
  (let [db (get-db)]
    (setup-test-admin! db)
    (println "\n✅ Admin setup complete!")
    (if-let [current-state @state/state]
      (let [port (shared-data/get-server-port (:service-container current-state))
            host (get-in current-state [:config :webserver :host] "localhost")]
        (println "You can now login at http://" host ":" port "/admin/login"))
      (println "You can now login at http://localhost:8085/admin/login"))
    (println "Email: admin@example.com")
    (println "Password: admin123")))

(defn dangerously-delete-all-data!
  "Delete all data from admin-related tables."
  ([] (dangerously-delete-all-data! (get-db)))
  ([db]
   (println "DANGER: Deleting all data from admin-related tables...")
   (try
     (jdbc/execute! db (hsql/format {:delete-from :email_verification_tokens}))
     (jdbc/execute! db (hsql/format {:delete-from :users}))
     (jdbc/execute! db (hsql/format {:delete-from :admins}))
     (println "All data deleted successfully!")
     (catch Exception e
       (println "Error deleting data:" (.getMessage e))))))
