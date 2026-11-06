# Gozza Lalamove 0.1.50 (51) — signed Release

The Release APK is built with the existing external production signing configuration, R8 minification and resource shrinking. No private key/properties are checked in. The local deliverable is `artifacts/private/Gozza-Lalamove-v0.1.50-release.apk` with a SHA-256 sidecar and `release-verification.json`.

## Executed validation

- `:app:assembleRelease :app:lintRelease :gps:testFullReleaseUnitTest`: passed.
- 47 Release GPS/policy tests, 0 failures.
- APK signature verification passed using v2/v3, one signer; manifest is non-debuggable.
- Inspecting actual DEX confirmed both assets/xposed_init classes remain: `io.github.phantom.gps.xposed.HookEntry`, `com.fulllock.probe.DriverProbe`.
- Reflected `MainViewModel.updateXposedState` remains named/available after shrinking.
- Host and GPS generated certificate hashes both match the signed APK.

The integrated app needed explicit keep rules for both Xposed entries and their externally invoked callbacks. These rules are now applied in the host Release shrinker, together with the existing root app_process entry rules.

## Installation distinction

The Release signer is different from the installed Debug signer. It cannot be installed as an ordinary update over that Debug APK. No existing app/data was removed to build this release. Preserve user data before choosing a signing migration strategy.

Android's app-scoped device ID depends on signing identity. The Release installation can show a different tester ID; authorize that code in control/access.json after installation. No login or GitHub token is embedded.

Release runtime hook/touch/theme behavior has not yet been tested on a device; DEX/signature/build validation is not a substitute for that test. The local APK has not been automatically published as a public binary release.
