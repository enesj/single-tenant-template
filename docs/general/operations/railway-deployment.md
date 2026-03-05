<!-- ai: {:tags [:operations :deployment :railway] :kind :runbook} -->

# Deploying to Railway

This guide walks through deploying the app to [Railway](https://railway.app) using the included `Dockerfile`.

Railway detects the `Dockerfile` automatically and handles container builds, HTTPS, and domain assignment without extra configuration.

## Prerequisites

- A Railway account and the [Railway CLI](https://docs.railway.app/develop/cli) (optional — the dashboard works too)
- Your app's GitHub repository connected to Railway
- A Google Cloud project for OAuth login and Gmail email sending (see [Gmail API setup](#gmail-api-setup-for-email))

---

## Step 1: Create a Railway Project

1. Go to [railway.app/dashboard](https://railway.app/dashboard) and click **New Project**.
2. Choose **Deploy from GitHub repo** and select your repository.
3. Railway will detect the `Dockerfile` and start the first build. It will fail until you add environment variables — that is expected.

---

## Step 2: Add a PostgreSQL Database

1. In your Railway project, click **+ New Service → Database → PostgreSQL**.
2. Railway will provision a Postgres instance and automatically inject `DATABASE_URL` into all services in the project.

No manual DB connection string is needed — `config/base.edn` reads `DATABASE_URL` in the `:prod` profile.

---

## Step 3: Set Environment Variables

In your Railway service → **Variables** tab, add the following.

### Required

| Variable | Description |
|---|---|
| `DATABASE_URL` | Auto-injected by Railway when Postgres is linked. Do not set manually. |
| `PORT` | Auto-injected by Railway. Do not set manually. |
| `BASE_URL` | Your public app URL, e.g. `https://your-app.railway.app`. No trailing slash. Used in email links and OAuth callbacks. |
| `GOOGLE_OAUTH_CLIENT_ID` | Google OAuth client ID (also used for Gmail API email sending). |
| `GOOGLE_OAUTH_CLIENT_SECRET` | Google OAuth client secret (also used for Gmail API email sending). |
| `GMAIL_REFRESH_TOKEN` | OAuth2 refresh token for sending email via Gmail REST API. See [Gmail API setup](#gmail-api-setup-for-email). |
| `SMTP_FROM_EMAIL` | The Gmail address to send from, e.g. `noreply@yourdomain.com`. |
| `GOOGLE_OAUTH_REDIRECT_URI` | Google OAuth callback URL: `https://your-app.railway.app/oauth/google/callback` |

> **Why Gmail REST API for email?** Railway blocks outbound SMTP ports 465 and 587. The app uses the Gmail REST API over HTTPS (port 443) in production to work around this. The same Google OAuth credentials used for login are reused for email sending.

### Optional — GitHub OAuth

| Variable | Description |
|---|---|
| `GITHUB_OAUTH_CLIENT_ID` | GitHub OAuth App client ID. |
| `GITHUB_OAUTH_CLIENT_SECRET` | GitHub OAuth App client secret. |
| `GITHUB_OAUTH_REDIRECT_URI` | GitHub OAuth callback URL: `https://your-app.railway.app/oauth2/github/callback` |

### Optional — Stripe

| Variable | Description |
|---|---|
| `STRIPE_LIVE_API_KEY` | Stripe live secret key (`sk_live_...`). |
| `STRIPE_LIVE_WEBHOOK_SECRET` | Stripe live webhook endpoint secret (`whsec_...`). |

### Optional — Postmark (alternative email provider)

| Variable | Description |
|---|---|
| `POSTMARK_API_KEY` | Postmark server API token. |
| `POSTMARK_FROM_EMAIL` | Verified Postmark sender address. |

---

## Step 4: Run Database Migrations

After the first successful deploy, run migrations once from your local machine against the production database.

**Option A — Railway CLI (recommended)**

```bash
# Install Railway CLI if you haven't
npm install -g @railway/cli

# Log in and link the project
railway login
railway link

# Run migrations via the app's REPL against the production DB
railway run clj -M:nrepl
# Then in the REPL:
# (require 'app.template.backend.migrations.simple-repl :reload)
# (app.template.backend.migrations.simple-repl/migrate!)
```

**Option B — One-shot migration service**

Create a temporary Railway service that runs migrations on start, then remove it after it succeeds. See the [migration overview](../../general/migrations/migration-overview.md) for the full workflow.

---

## Step 5: Configure OAuth Redirect URIs

In [Google Cloud Console](https://console.cloud.google.com/apis/credentials):

1. Open your OAuth 2.0 Client ID.
2. Under **Authorized redirect URIs**, add:
   - `https://your-app.railway.app/oauth/google/callback`
3. Under **Authorized JavaScript origins**, add:
   - `https://your-app.railway.app`

For GitHub OAuth: in your [GitHub OAuth App settings](https://github.com/settings/developers), set the **Authorization callback URL** to `https://your-app.railway.app/oauth2/github/callback`.

---

## How the Dockerfile Works

The build uses two stages to keep the runtime image small:

1. **Builder** (`eclipse-temurin:21-jdk-jammy` + Node 22): installs all tooling, runs `npm ci`, downloads Clojure deps, compiles ClojureScript with shadow-cljs, and produces an AOT-compiled uberjar via `clj -T:build uber`.
2. **Runtime** (`eclipse-temurin:21-jre-jammy`): copies only the uberjar — no build tools, no source code.

Railway injects `PORT` at runtime; the app reads it from the environment and binds there. The healthcheck polls `GET /health` every 30 seconds with a 30-second startup grace period (sufficient for JVM cold start).

---

## Gmail API Setup for Email

Railway blocks outbound SMTP. The app sends email in production via the Gmail REST API using an OAuth2 refresh token.

### One-time setup

1. In [Google Cloud Console](https://console.cloud.google.com/apis/dashboard), enable the **Gmail API** for your project.
2. Your existing OAuth 2.0 client (the one used for Google login) already has the right credentials. Add `https://mail.google.com/` to its **Authorized scopes** if not already present.
3. Use the [OAuth 2.0 Playground](https://developers.google.com/oauthplayground/) to generate a refresh token:
   - Click the gear icon → check **Use your own OAuth credentials** → enter your `GOOGLE_OAUTH_CLIENT_ID` and `GOOGLE_OAUTH_CLIENT_SECRET`.
   - In step 1, find **Gmail API v1** → select `https://mail.google.com/` → **Authorize APIs**.
   - Complete the consent flow, then in step 2 click **Exchange authorization code for tokens**.
   - Copy the **Refresh token** value.
4. Set `GMAIL_REFRESH_TOKEN` in Railway to this value.
5. Set `SMTP_FROM_EMAIL` to the Gmail address you authorized.

> The refresh token does not expire as long as the app uses it at least once every 6 months and the Google account remains in good standing.

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| Build fails at `npm ci` | `package-lock.json` missing | Commit `package-lock.json` and push |
| App starts but DB errors on boot | Migrations not run | Follow [Step 4](#step-4-run-database-migrations) |
| OAuth login fails with redirect mismatch | Redirect URI not added to Google Console | Follow [Step 5](#step-5-configure-oauth-redirect-uris) |
| Email not sent, no error | `GMAIL_REFRESH_TOKEN` or `SMTP_FROM_EMAIL` missing | Add both Railway variables |
| Health check fails, container restarts | JVM startup > 30s or missing env vars | Check deploy logs; increase `--start-period` in Dockerfile if needed |
