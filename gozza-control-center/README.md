# Gozza Control Center

Dark Performance web admin for Gozza Android tester access. Frontend is static and can be deployed separately to Vercel or GitHub Pages. Google Sheets stores Devices, Settings and AuditLogs. Google Apps Script publishes to the existing **Gozza/control/access.json** file, which is read by the Android app. The existing policy file is **not changed by installing this project**.

## Files
- `frontend/index.html` — responsive dashboard, Google sign-in and device controls.
- `frontend/config.js` — placeholders for public OAuth client ID and Apps Script URL; fill these in before deployment.
- `apps-script/Code.gs` and `apps-script/appsscript.json` — Google Apps Script backend.

## Configuration — backend (Google Apps Script)

1. Create a new Apps Script project at https://script.google.com and paste `Code.gs`; configure manifest `appsscript.json`.
2. Set **Script Properties** (Project Settings):
   - `OWNER_EMAIL` — comma-separated allowed Google admin email addresses.
   - `GOOGLE_CLIENT_ID` — Google Cloud OAuth Web Client ID.
   - `FRONTEND_ORIGINS` — exact frontend origins, e.g. `https://YOURNAME.github.io` or `https://your-site.vercel.app`, comma-separated; do not include path/trailing slash.
   - `GITHUB_OWNER` = `phantommaster1245-star`
   - `GITHUB_REPO` = `Gozza`
   - `GITHUB_BRANCH` = `main`
   - `GITHUB_TOKEN` — fine-grained GitHub token with **Contents: Read and write** for Gozza. Keep it in Script Properties; **never put it in Git or frontend**.
3. Run `setupDatabase()` manually from Apps Script editor and authorize. It creates the spreadsheet, stores `SPREADSHEET_ID` property and prints the Sheets URL in logs.
4. Deploy > New Deployment > Web app > Execute as **Me**, Who has access **Anyone**. Each API operation independently checks a Google ID token against the approved account and Client ID.
5. Google Cloud Console > OAuth Web Client: configure your deployment origin in **Authorized JavaScript origins**. For OAuth app in Testing, add the admin account as a test user.

## Configure static frontend
1. Edit `frontend/config.js`: set `appsScriptUrl` to the Apps Script **/exec** URL and `googleClientId` to the same Web Client ID.
2. Deploy the `frontend` folder as a static site (Vercel Root Directory = `gozza-control-center/frontend`; Framework Preset = Other; no build command). You may instead copy these static files to GitHub Pages.
3. Sign in using an email listed in `OWNER_EMAIL`.

## IMPORTANT: preserve existing device allowlist!
The new Sheets database starts with an empty Devices tab. Before clicking **Publish**, copy each current 64-character hash from `control/access.json` into the Devices sheet (or add them through the dashboard, noting that dashboard changes automatically publish). Otherwise previously allowed devices will be denied after the first sync. For a safe migration, add the existing device IDs first, then publish.

## Behavior
- Global switch `enabled=false`: denies all testers, without deleting their rows.
- Per-device switch: excludes only that hash from the published allowlist.
- AuditLogs records editor changes.
- The app polls GitHub policy ~120 seconds; tester lease lasts at most 150 seconds, depending on app lifecycle and network.
- Failed GitHub publishes leave the Sheets change in place and mark the dashboard OUT OF SYNC. Use **Publish to GitHub** to retry.
- Apps Script is required for API; GitHub Pages and Vercel host **only the frontend**.

## Safety
Do not add passwords, tokens, secrets or personal Android IDs to Git. Device hash is SHA-256 of `com.gozza.lalamove|ANDROID_ID`, calculated client-side in the dashboard. This is access control for cooperative test APKs, not tamper-proof DRM. Apps Script HTML iframe transport may be blocked by stringent third-party privacy settings; test with your production browser.
