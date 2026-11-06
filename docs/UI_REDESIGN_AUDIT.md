# Gozza Dark Performance — audit and change map

## Existing implementation
- Host `app`: Java native Activity with programmatic Views; AppTheme supplies dialogs and controls.
- `gps`: Kotlin/XML Android library; Material 3, AppCompat, PreferenceFragmentCompat, Hilt and Google Maps. BaseMapActivity owns drawer, search, favorites and permission prompts; full/MapActivity owns map interaction.
- `driverprobe`: source included in host APK. CardInfoDrawable and PickupTags draw in Driver; GPS tap uses Binder/root relay. Separate GPS Xposed entry is packaged in the same APK.
- Current user-defined functionality is read/data/overlay only. Filtering, automatic job open/accept and login have already been removed. They must remain absent.
- Existing circle/rounded Gozza bitmap branding remains. GPS settings, favorites, location search, joystick/clipboard settings, permissions, diagnostics and direct pickup taps remain.
- Dormant legacy floating controls remain in source, hidden by current lifecycle policy; redesign must not reactivate them.

## Inconsistencies
Repeated hard-coded palettes across XML, native Java and hook drawings; old green/translucent panels, small 8–11sp labels, uneven input/dialog/button geometry, permanent borders and an oversized primary-action row at larger font scales. Hook drawing size must remain constrained to avoid intercepting Driver content.

## Proposed changes
1. Central XML tokens and Kotlin/Java constants generated from one checked-in design token JSON; system sans-serif / Thai fallback, tabular financial numerals.
2. Map toolbar, status, search and start/stop dock; preserve binding IDs, listeners, permission handling and saved coordinates. Reflow dock for narrow screens and large fonts.
3. Native read/overlay, permissions and diagnostics; consistent touch targets, switch states and semantic status colors.
4. GPS preferences, favorite dialogs and popup/input styles; retain existing keys, defaults, serialization and confirmations.
5. Hook labels: palette/typography only, keep coordinate validation, hit geometry, drag/scroll cancellation and relay protocol.
6. Independent requested capability: repo-backed tester access, anonymous HTTPS manifest plus app-scoped hashed device IDs. No cloud service, embedded GitHub token, login or arbitrary remote code execution. Access panel remains usable for viewing device ID/retry while capability is blocked.

## Rollback and validation
Capture baseline in an independent local Gozza repository before source edits. Commit UI and tester-control changes separately. Build, lint, GPS unit tests, new policy tests, read-only instrumentation, physical-device UI/screenshots where connected. Do not report hook/device cases as passed unless executed.

## Scope boundaries
No package/signing/profile/database/IPC changes. No restored filters, job commands or login. Repo gate is a separately authorized runtime dependency and must cover active GPS and overlay paths. All other lifecycle changes are limited to revocation shutdown.
