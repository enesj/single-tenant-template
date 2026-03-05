-- Seed a default owner admin account for initial production login.
-- Password: AdminPass1  (change immediately after first login)
-- Hash algorithm: bcrypt+sha512, 12 iterations (buddy.hashers compatible)
--
-- Idempotent: ON CONFLICT (email) DO NOTHING — safe to re-run.
INSERT INTO public.admins (id, email, password_hash, full_name, role, status, created_at)
VALUES (
  gen_random_uuid(),
  'admin@example.com',
  'bcrypt+sha512$3220a91debd17627d43a6d84efc828d5$12$618b09d504a491d804a558957ef33e105bc839fd3c0fa8a3',
  'System Administrator',
  'owner'::admin_role,
  'active'::admin_status,
  NOW()
)
ON CONFLICT (email) DO NOTHING;
