# Gozza Dark Performance — implementation and validation

## Delivered UI and preserved behavior

The existing Java Views + Kotlin/XML/Material + Xposed Drawable architecture is retained. No Compose, remote fonts or new UI dependencies were introduced. Map, search, selected coordinates, START/STOP, left navigation, read/overlay settings, permission actions, GPS preferences, favorites, dialogs and joystick share the Dark Performance palette. Circle/rounded Gozza artwork remains. No removed job filters, profiles or automatic job commands were restored.

`design/tokens.json` is the color/type source. `tools/generate-design.py` generates `gps/src/main/res/values/gozza_tokens.xml` and `shared/src/main/java/com/fulllock/app/GozzaDesign.java`. System sans-serif supplies Thai fallback; numeric labels use tabular numerals where possible. Buttons use 48dp or larger targets, 12dp corners, tonal secondary actions and pressed/focus states. Surfaces are #191E25 on #0D1014; dialogs use #252B34/18dp; primary is #FF7A18 with dark on-primary text. Dialog rendering was checked on the device and corrected to avoid Material's brown elevation tint.

The map dock reflows vertically without changing search or start/stop handlers. Preference keys, favorites storage, GPS validation, Binder field order, coordinate hit testing and scroll/drag cancellation are preserved. Overlay typography/colors were updated within the existing drawing rectangles; live Driver touch/scroll gestures still require separate field verification.

## Main file groups

| Area | Files |
|---|---|
| Tokens | design/tokens.json, tools/generate-design.py, gozza_tokens.xml, GozzaDesign.java |
| Native UI | AppTheme.java, MainActivity.java, ScreenActions.java |
| Map/navigation | activity_map.xml, include_search.xml, drawer_header.xml, BaseMapActivity.kt |
| Settings/dialogs | themes.xml, GozzaDialogs.kt, SettingsActivity.kt, dialog.xml, fav_items.xml, joystick.xml |
| Driver drawing | CardInfoDrawable.java, PickupTags.java |
| Repo access | AccessPolicy.kt, TesterAccess.kt, TesterAccessActivity.kt, PrefManager.kt, Xshare.kt, GpsAccess.kt, DriverBridge.java, control/access.json |
| Root setup fix | RootFrameworkConfig.java, RootSetup.java, RootConfigurationChecks.java |

## Executed checks

- `:app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug :gps:testFullDebugUnitTest` passed. Lint: 0 errors, 38 warnings (legacy/dependency warnings remain).
- GPS/policy JVM tests: **47 tests, 0 failures**, including explicit device allowlist, global disable, malformed data, lease expiry, boot mismatch, elapsed-time rollback and oversized leases.
- V2408A Android 15: **10 read-only migration checks passed** with old automatic-open/accept flags deliberately enabled. Command endpoints still refuse actions; map remains launcher; direct GPS and palette contracts hold.
- V2408A: **10 root configuration checks passed** against in-memory SQLite. Required scopes, idempotence, preservation of another module/another user and rollback on an injected insert failure were verified. These fixture checks do not modify LSPosed.
- Physical UI: map, Favorites dialog, left drawer → read/overlay page, GPS settings and Back navigation were opened and captured. At font scale 1.3 the primary map action remained visible; original 1.0 scale was restored.
- Live repo disable test after switching from stale raw.githubusercontent.com responses to the GitHub Contents API: access screen appeared in **108.5 seconds**. After restoring enabled=true and pressing Retry, map access returned in **3.2 seconds**. The control file is restored to enabled=true and the V2408A hash remains enrolled.
- Installed final APK is **0.1.48 (49)**. SHA-256 of the installed base APK matches the locally delivered APK. Signing configuration and application ID remain unchanged.

## ROOT_SETUP issue baeeb18b

The previous helper inspected configuration but returned CONFIG_NOT_READY without configuring the unified Gozza module. Live inspection showed SCOPE_MISSING. The fix supports the known modern schema, creates a consistent SQLite backup with VACUUM INTO, then updates only com.gozza.lalamove and adds missing System Framework, Driver and Gozza scopes in one transaction. Existing extra GPS targets, other users and other modules are retained. Unknown schemas and legacy layouts remain read-only.

On V2408A: SCOPE_MISSING → CONFIGURED; the subsequent check returned CONFIGURED. The app's actual Enable reading action completed and its UI showed reading enabled. No reboot was performed. A restart is still needed to confirm loading in an already running System Framework/Driver process; runtime hook readiness must not be inferred solely from database configuration.

## Evidence and limits

Local screenshots and machine-readable results are in `artifacts/private/`: final-map.png, final-settings.png, final-favorites.png, large-font.png, access-disabled.png, root-result.png, access-live-results.json, root-setup-live-results.json and verification.json. They are local test artifacts, not user/device logs published to the public source repository.

The repo gate requires internet and GitHub availability. It checks about every 120 seconds, uses 150-second boot-bound leases, and fails closed on HTTP/network/schema errors. Anonymous GitHub limits apply, especially to testers sharing a public IP. This controls unchanged APKs; public source or rooted-device tampering can bypass a rebuilt client. It is not DRM. See control/README.md.

No end-to-end Driver coordinate tap, active GPS override, joystick drag or System Framework restart test is claimed for this change. Existing permissions were retained; no system reboot or unrelated module changes were performed.
