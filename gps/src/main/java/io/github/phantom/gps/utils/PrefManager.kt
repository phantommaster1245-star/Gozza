package io.github.phantom.gps.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import io.github.phantom.gps.BuildConfig
import io.github.phantom.gps.gsApp
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


@SuppressLint("WorldReadableFiles")
object PrefManager   {

    var membershipLease: String
        get() = pref.getString("membership_lease", "").orEmpty()
        set(value) { pref.edit().putString("membership_lease", value).commit() }

    private const val START = "start"
    private const val LATITUDE = "latitude"
    private const val LONGITUDE = "longitude"
    private const val HOOKED_SYSTEM = "system_hooked"
    private const val RANDOM_POSITION = "random_position"
    private const val RANDOM_RADIUS_METERS = "random_radius_meters"
    private const val ACCURACY_SETTING = "accuracy_level"
    private const val MAP_TYPE = "map_type"
    private const val DARK_THEME = "dark_theme"
    private const val DISABLE_UPDATE = "update_disabled"
    private const val ENABLE_JOYSTICK = "joystick_enabled"
    private const val ENABLE_CLIPBOARD_MONITOR = "clipboard_monitor_enabled"
    private const val CLIPBOARD_AUTO_START = "clipboard_monitor_auto_start"


    @Volatile private var sharedHookPreferences = false
    val isHookSharingAvailable: Boolean
        get() {
            if (BuildConfig.MODERN_XPOSED) return HookPreferenceChannel.ready
            pref
            return sharedHookPreferences
        }

    @Synchronized
    fun publishHookState(): Boolean = HookPreferenceChannel.publish(pref.all)

    fun modernScopeStatus(): String = HookPreferenceChannel.scopeStatus()

    @Synchronized
    private fun saveHookSetting(change: SharedPreferences.Editor.() -> Unit) {
        pref.edit().apply(change).apply()
        if (BuildConfig.MODERN_XPOSED) publishHookState()
    }

    private val pref: SharedPreferences by lazy {
        if (BuildConfig.MODERN_XPOSED) {
            gsApp.getSharedPreferences("${BuildConfig.APPLICATION_ID}_prefs", Context.MODE_PRIVATE)
        } else try {
            val prefsFile = "${BuildConfig.APPLICATION_ID}_prefs"
            gsApp.getSharedPreferences(
                prefsFile,
                Context.MODE_WORLD_READABLE
            ).also { sharedHookPreferences = true }
        }catch (e:SecurityException){
            val prefsFile = "${BuildConfig.APPLICATION_ID}_prefs"
            gsApp.getSharedPreferences(
                prefsFile,
                Context.MODE_PRIVATE
            )
        }

    }


    @Synchronized
    fun writeTesterAccess(allowed:Boolean,issued:Long,boot:Int) {
        pref.edit().putBoolean("tester_access_allowed",allowed).putLong("tester_access_issued",issued)
            .putLong("tester_access_until",if(allowed)issued+AccessPolicy.LEASE_MS else issued).putInt("tester_access_boot",boot).commit()
        if (BuildConfig.MODERN_XPOSED) publishHookState()
    }
    fun testerAccessValid(boot:Int)=AccessPolicy.fresh(pref.all,android.os.SystemClock.elapsedRealtime(),boot)

    val isStarted : Boolean
        get() = pref.getBoolean(START, false)

    val getLat : Double
        get() = if (pref.contains("latitude_bits")) Double.fromBits(pref.getLong("latitude_bits", 0)) else pref.getFloat(LATITUDE, 40.7128F).toDouble()

    val getLng : Double
        get() = if (pref.contains("longitude_bits")) Double.fromBits(pref.getLong("longitude_bits", 0)) else pref.getFloat(LONGITUDE, -74.0060F).toDouble()

    val directRequestId: String get() = pref.getString("direct_request_id", "").orEmpty()

    fun hasSavedLocation(): Boolean {
        return pref.contains(LATITUDE) && pref.contains(LONGITUDE)
    }

    var isSystemHooked : Boolean
        get() = pref.getBoolean(HOOKED_SYSTEM, false)
        set(value) { saveHookSetting { putBoolean(HOOKED_SYSTEM, value) } }

    var isRandomPosition :Boolean
        get() = pref.getBoolean(RANDOM_POSITION, false)
        set(value) { saveHookSetting { putBoolean(RANDOM_POSITION, value) } }

    var randomRadiusMeters: String?
        get() = pref.getString(RANDOM_RADIUS_METERS, "10")
        set(value) { saveHookSetting { putString(RANDOM_RADIUS_METERS, value) } }

    var accuracy : String?
        get() = pref.getString(ACCURACY_SETTING,"10")
        set(value) { saveHookSetting { putString(ACCURACY_SETTING, value) } }

    var mapType : Int
        get() = pref.getInt(MAP_TYPE,1)
        set(value) { pref.edit().putInt(MAP_TYPE,value).apply()}

    var darkTheme: Int
        get() = AppCompatDelegate.MODE_NIGHT_YES
        set(value) = pref.edit().putInt(DARK_THEME, value).apply()

    var isUpdateDisabled: Boolean
        get() = pref.getBoolean(DISABLE_UPDATE, false)
        set(value) = pref.edit().putBoolean(DISABLE_UPDATE, value).apply()

    var isJoystickEnabled: Boolean
        get() = pref.getBoolean(ENABLE_JOYSTICK, false)
        set(value) = pref.edit().putBoolean(ENABLE_JOYSTICK, value).apply()

    var isClipboardMonitorEnabled: Boolean
        get() = pref.getBoolean(ENABLE_CLIPBOARD_MONITOR, false)
        set(value) { saveHookSetting { putBoolean(ENABLE_CLIPBOARD_MONITOR, value) } }

    var isClipboardAutoStart: Boolean
        get() = pref.getBoolean(CLIPBOARD_AUTO_START, true)
        set(value) = pref.edit().putBoolean(CLIPBOARD_AUTO_START, value).apply()

    fun isClipboardFormatEnabled(key: String): Boolean {
        require(key in ClipboardFormats.KEYS)
        return pref.getBoolean(key, true)
    }

    fun setClipboardFormatEnabled(key: String, enabled: Boolean) {
        require(key in ClipboardFormats.KEYS)
        pref.edit().putBoolean(key, enabled).apply()
    }

    fun clipboardFormats(): ClipboardFormats = ClipboardFormats(
        isClipboardFormatEnabled(ClipboardFormats.KEYS[0]),
        isClipboardFormatEnabled(ClipboardFormats.KEYS[1]),
        isClipboardFormatEnabled(ClipboardFormats.KEYS[2]),
        isClipboardFormatEnabled(ClipboardFormats.KEYS[3]),
        isClipboardFormatEnabled(ClipboardFormats.KEYS[4])
    )

    fun update(start:Boolean, la: Double, ln: Double) {
        runInBackground {
            if (BuildConfig.MODERN_XPOSED) {
                updateNow(start, la, ln)
                return@runInBackground
            }
            if(start&&!isHookSharingAvailable)return@runInBackground
            val prefEditor = pref.edit()
            prefEditor.putLong("latitude_bits", la.toBits())
            prefEditor.putLong("longitude_bits", ln.toBits())
            prefEditor.remove("direct_request_id")
            prefEditor.putFloat(LATITUDE, la.toFloat())
            prefEditor.putFloat(LONGITUDE, ln.toFloat())
            prefEditor.putBoolean(START, start)
            prefEditor.apply()
        }

    }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        pref.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        pref.unregisterOnSharedPreferenceChangeListener(listener)
    }

    /**
     * Synchronous write for cross-process consumers (XSharedPreferences in Xposed).
     * Prefer this for "Start/Stop" actions where we want the hook to react immediately.
     */
    @Synchronized
    fun updateNow(start: Boolean, la: Double, ln: Double, trace: ClipboardTrace? = null, requestId: String = ""): Boolean {
        if(start&&!isHookSharingAvailable)return false
        val previous = if (BuildConfig.MODERN_XPOSED && start) pref.all else null
        val prefEditor = pref.edit()
        prefEditor.putLong("latitude_bits", la.toBits())
        prefEditor.putLong("longitude_bits", ln.toBits())
        prefEditor.putString("direct_request_id", requestId)
        prefEditor.putFloat(LATITUDE, la.toFloat())
        prefEditor.putFloat(LONGITUDE, ln.toFloat())
        prefEditor.putBoolean(START, start)
        prefEditor.putLong(LocationRefresh.TRACE_ID_PREF, trace?.id ?: 0)
        prefEditor.putLong(LocationRefresh.CAPTURED_AT_PREF, trace?.capturedAtMs ?: 0)
        val localCommitted = prefEditor.commit()
        val committed = localCommitted && (!BuildConfig.MODERN_XPOSED || publishHookState())
        if (!committed && previous != null) {
            val restore = pref.edit()
            for (key in listOf(START, LATITUDE, LONGITUDE, "latitude_bits", "longitude_bits",
                "direct_request_id", LocationRefresh.TRACE_ID_PREF, LocationRefresh.CAPTURED_AT_PREF)) {
                when (val value = previous[key]) {
                    is Boolean -> restore.putBoolean(key, value)
                    is Float -> restore.putFloat(key, value)
                    is Long -> restore.putLong(key, value)
                    is String -> restore.putString(key, value)
                    else -> restore.remove(key)
                }
            }
            restore.commit()
        }
        if (committed) trace?.mark("committed")
        return committed
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun runInBackground(method: suspend () -> Unit){
        GlobalScope.launch(Dispatchers.IO) {
            method.invoke()
        }
    }

}

