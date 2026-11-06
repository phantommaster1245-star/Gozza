package io.github.phantom.gps.xposed

import de.robv.android.xposed.XSharedPreferences
import io.github.phantom.gps.BuildConfig
import android.os.SystemClock

class Xshare {

    private val prefsName = "${BuildConfig.APPLICATION_ID}_prefs"
    private val xPref: XSharedPreferences by lazy {
        XSharedPreferences(BuildConfig.APPLICATION_ID, prefsName)
    }

    private var lastReloadUptimeMs: Long = 0

    @Synchronized
    private fun pref(): XSharedPreferences {
        val now = SystemClock.elapsedRealtime()
        // XSharedPreferences doesn't auto-update; reload throttled to keep hooks fast.
        if (now - lastReloadUptimeMs >= 250) {
            runCatching { xPref.reload() }
            lastReloadUptimeMs = now
        }
        return xPref
    }

    @Synchronized
    fun forceReload() {
        runCatching { xPref.reload() }
        lastReloadUptimeMs = SystemClock.elapsedRealtime()
    }

    @Synchronized
    fun locationSnapshot(force: Boolean = false): io.github.phantom.gps.utils.HookLocationSnapshot {
        if (force) forceReload()
        return io.github.phantom.gps.utils.HookLocationSnapshot.from(pref().all)
    }

    fun clipboardTrace(): io.github.phantom.gps.utils.ClipboardTrace? {
        val p = pref()
        val id = p.getLong(io.github.phantom.gps.utils.LocationRefresh.TRACE_ID_PREF, 0)
        val capturedAt = p.getLong(io.github.phantom.gps.utils.LocationRefresh.CAPTURED_AT_PREF, 0)
        return if (id > 0 && capturedAt > 0) io.github.phantom.gps.utils.ClipboardTrace(id, capturedAt) else null
    }

    fun diagnosticState():android.os.Bundle {
        forceReload()
        val p=pref()
        val valid=true
        val reason=if(!p.contains("start"))"SNAPSHOT_MISSING" else if(!p.getBoolean("start",false))"STOPPED" else "READY"
        return android.os.Bundle().apply {
            putString("reason",reason)
            putBoolean("lease_valid",valid)
            putBoolean("started",p.getBoolean("start",false))
        }
    }

    val directRequestId: String get() = pref().getString("direct_request_id", "").orEmpty()

    val isStarted : Boolean
    get() = pref().let { it.getBoolean("start", false) }

    val getLat: Double get() {
        val p = pref()
        return if (p.contains("latitude_bits")) Double.fromBits(p.getLong("latitude_bits", 0)) else p.getFloat("latitude", 45F).toDouble()
    }

    val getLng: Double get() {
        val p = pref()
        return if (p.contains("longitude_bits")) Double.fromBits(p.getLong("longitude_bits", 0)) else p.getFloat("longitude", 0F).toDouble()
    }

    val isHookedSystem : Boolean
    get() = pref().getBoolean(
        "system_hooked",
        true
    )

    val isRandomPosition :Boolean
    get() = pref().getBoolean(
        "random_position",
        false
    )

    val randomRadiusMeters: String?
    get() = pref().getString("random_radius_meters","10")

    val accuracy : String?
    get() = pref().getString("accuracy_level","10")

    val isClipboardMonitorEnabled : Boolean
    get() = pref().getBoolean(
        "clipboard_monitor_enabled",
        false
    )

}
