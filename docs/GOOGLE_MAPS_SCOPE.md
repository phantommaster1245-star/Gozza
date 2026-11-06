# Google Maps GPS scope — 0.1.49 (50)

`com.google.android.apps.maps` is added to the unified Gozza module's required and suggested LSPosed scopes. The existing LocationHook application path already supports it; Google Maps is not in the location ignore list. No coordinate calculation, Binder contract, stored locations, existing scope selection or permission behavior is changed.

Required targets: System Framework (`android`), Lalamove Driver, Gozza Lalamove and Google Maps. Setup adds missing targets and preserves all existing targets/users/modules. The version bump causes existing enabled installs to show the module preparation action after update.

After installing, open Read/overlay settings and prepare the new LSPosed module. Fully close and reopen the separate Google Maps app so a new process loads the scope. If the System Framework scope was not loaded previously, a device restart is still necessary. Select a position or tap a Driver pickup tag in Gozza, start GPS, then use My Location in Google Maps. Gozza's embedded map remains excluded from location override to preserve its original selection behavior.

Validation: APK, instrumentation APK, lint and 47 GPS/policy unit tests built/passed. Root fixture assertions now explicitly check that Maps is inserted exactly once, with four required targets, preserved extra GPS/user targets and idempotent setup. V2408A was subsequently connected: version 0.1.49 (50) installed with matching APK hash; all 11 updated root configuration checks passed on-device. Google Maps is installed, the live module check returned CONFIGURED, and the app preparation action completed with reading enabled. No reboot was performed and an end-to-end Maps location override is not yet claimed.
