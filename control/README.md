# Gozza tester control

Edit `control/access.json` on the **main** branch of this repository.

- `enabled: false`: disable all testers.
- Remove a device hash from `devices`: revoke that device.
- Delete `access.json`: deny everyone (HTTP 404).
- Add the 64-character device code shown by the app to `devices`: authorize a tester. No login or GitHub token in the APK.

The app fetches the HTTPS manifest at launch and approximately every 60 seconds. Successful checks grant a 90-second monotonic lease tied to the current boot. Failed network/HTTP/JSON checks revoke access; offline operation is deliberately unavailable. Existing GPS hook snapshots also expire when the app process disappears. GitHub caching/availability can delay a repository edit; revocation is not instantaneous.

Only this configuration is consumed. The app does not download or execute source code from the repository. Public device codes are SHA-256 hashes of app-scoped Android IDs; they are identifiers, not secrets. Avoid publishing raw IDs, names, addresses or credentials. Signing identity/reinstall/device changes can require re-enrollment.

This controls the unchanged tester APK; it cannot stop a determined attacker from patching/rebuilding it or modifying preferences on a compromised rooted device. Source is public by the owner's explicit choice. Never put a GitHub PAT/private signing key in this repository or APK.

The connected V2408A tester is enrolled initially. The ordinary access panel provides device-code copy and retry while GPS and Driver overlays are blocked. Existing user settings and saved locations are retained; revocation stops active location override rather than deleting data.
