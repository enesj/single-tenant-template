Restructure App Folder Structure
The goal is to strictly separate the application code into admin, 
template
, domain, and shared directories, with clear frontend and backend separations within them (except for shared which is for 
.cljc
).

User Review Required
WARNING

This is a massive refactoring that changes the path of almost every file in the system. All namespace declarations and require statements associated with moved files must be updated. This plan outlines the move, but the actual execution will involve significant search-and-replace operations.

Refactoring Strategy: bb rename-ns
We will use a Babashka script to automate the refactoring. This is safer and more reliable than manual moves. The script will:

Define a mapping of old-path -> new-path.
Move the physical files.
Update the ns declaration in the moved files.
Update all require references in the codebase to point to the new namespaces.
Proposed Changes
We will restructure src/app as follows:

1. Admin (src/app/admin)
Groups all admin-specific code.

Frontend: src/app/admin/frontend (Exists, Keep).
Backend: src/app/admin/backend (New).
Move src/app/backend/services/admin to src/app/admin/backend/services/admin
Move src/app/backend/services/admin.clj to src/app/admin/backend/services/admin.clj
Move src/app/backend/admin_setup.clj to src/app/admin/backend/setup.clj (namespace: app.admin.backend.setup)
2. Template (src/app/template)
Groups the base application template/infrastructure code.

Frontend: src/app/template/frontend (New).
Move src/app/frontend/* contents (dev, preload, ui, utils) to src/app/template/frontend/.
Move src/app/shared/frontend/*.cljs contents to src/app/template/frontend/shared/ (e.g. bridges).
Move src/app/template/frontend contents (merge if conflicts, but seems distinctive).
Backend: src/app/template/backend (New).
Move src/app/backend/core.clj (Main entry)
Move src/app/backend/webserver.clj
Move src/app/backend/handlers
Move src/app/backend/middleware
Move src/app/backend/routes.clj (Main routes)
Move src/app/backend/routes (Folder)
Move src/app/backend/services/gmail_smtp.clj
Move src/app/backend/services/postmark_email.clj
Move src/app/backend/services/monitoring
Move src/app/migrations to src/app/template/backend/migrations.
Move src/app/shared/*.clj files (backend utils) to src/app/template/backend/utils/ (or similar).
Shared: src/app/template/shared (Existing, keep or merge generic shared logic here if not .cljc).
3. Domain (src/app/domain)
Groups domain-specific features (e.g., expenses).

Frontend: src/app/domain/frontend (New).
Move src/app/domain/expenses/frontend to src/app/domain/frontend/expenses.
Backend: src/app/domain/backend (New).
Move src/app/domain/expenses/routes to src/app/domain/backend/expenses/routes.
Move src/app/domain/expenses/services to src/app/domain/backend/expenses/services.
Move src/app/backend/services/user_expenses.clj to src/app/domain/backend/expenses/services/user_expenses.clj.
4. Shared (src/app/shared)
Generic, pure utility logic (.cljc only).

Keep all .cljc files here.
Ensure all .clj and .cljs are moved out to template, admin, or domain as appropriate.
