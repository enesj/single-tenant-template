(ns app.template.backend.services.onboarding.config
  "Per-role onboarding step definitions.

   Each role has an ordered list of steps. Steps are created from these
   definitions when onboarding begins. The step kind values match the
   onboarding_step_kind PostgreSQL enum.")

(def owner-steps
  [{:kind "name_workspace"       :order 1}
   {:kind "setup_profile"        :order 2}
  {:kind "rename_default_category" :order 3}
  {:kind "configure_categories" :order 4}
  {:kind "review_payer_types"   :order 5}
  {:kind "invite_members"       :order 6}
  {:kind "management_intro"     :order 7}
  {:kind "upload_first_receipt" :order 8}])

(def admin-steps
  [{:kind "setup_profile"        :order 1}
   {:kind "management_intro"     :order 2}
   {:kind "upload_first_receipt" :order 3}])

(def member-steps
  [{:kind "setup_profile"        :order 1}
   {:kind "upload_first_receipt" :order 2}])

(def viewer-steps
  [{:kind "setup_profile" :order 1}
   {:kind "browse_intro"  :order 2}])

(defn steps-for-role
  "Return the step definitions for a given role string."
  [role]
  (case (if (keyword? role) (name role) (str role))
    "owner"  owner-steps
    "admin"  admin-steps
    "member" member-steps
    "viewer" viewer-steps
    member-steps))
