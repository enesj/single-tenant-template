# OAuth Account Conflict Fix - Test Instructions

## What Was Fixed

Previously, when a user registered with email/password and then tried to log in with OAuth (Google/GitHub) using the same email, the OAuth login would **overwrite** the `auth_provider` field from `"password"` to `"google"` or `"github"`. This created a security vulnerability where:

1. Anyone could "takeover" a password-based account by logging in via OAuth with the same email
2. The user's authentication method would silently change without their consent

## The Fix

Added a security check in `src/app/template/backend/auth/service.clj` (process-oauth-callback function):
- Before allowing OAuth login, checks if a user exists with that email
- If user exists AND `auth_provider = "password"`, throws an `:account-conflict` error
- The error is caught in `src/app/template/backend/routes/oauth.clj` and displays a user-friendly error page

## Testing Steps

### Setup
Your user `enes.jakic@gmail.com` has been reset to:
- `auth_provider`: `password`
- `provider_user_id`: `NULL`

### Test 1: Password Login (Should Work)
1. Navigate to http://localhost:8085/login
2. Enter email: `enes.jakic@gmail.com`
3. Enter your password
4. Click "Sign In"
5. ✅ **Expected**: Login succeeds, redirects to `/entities`

### Test 2: Google OAuth Login (Should Be Blocked)
1. Log out if logged in
2. Navigate to http://localhost:8085/login
3. Click "Continue with Google"
4. Complete Google authentication
5. ✅ **Expected**: Shows error page with message:
   - Title: "Account Already Exists"
   - Message: "This email is already registered with password authentication. Please log in with your password instead."
   - Instructions to link account via settings
   - Link to login page

### Test 3: New OAuth User (Should Work)
1. Log out if logged in
2. Use a different Google account that hasn't registered with password
3. Click "Continue with Google"
4. Complete Google authentication
5. ✅ **Expected**: Creates new user with `auth_provider = "google"`, redirects to `/entities`

### Test 4: Existing OAuth User (Should Work)
1. Create a user via OAuth (or use Test 3 user)
2. Log out
3. Log in again with the same Google account
4. ✅ **Expected**: Login succeeds, updates user profile (name, avatar), redirects to `/entities`

## Database Verification

After each test, you can verify the database state:

```sql
SELECT email, auth_provider, provider_user_id, email_verified
FROM users 
WHERE email = 'enes.jakic@gmail.com';
```

## Current State

Your user is now set to `auth_provider = 'password'` so you can test that:
- Password login works
- Google OAuth login is properly blocked with a clear error message

## Future Enhancement: Account Linking

To allow users to link OAuth providers to their password accounts, you would need to:
1. Create an account settings page
2. Add a "Link Google Account" button
3. Verify the user is logged in before allowing OAuth linking
4. Update the `process-oauth-callback` to accept a "linking mode" parameter
5. Only in linking mode, allow updating a password account to OAuth

This would be implemented in a future task.
