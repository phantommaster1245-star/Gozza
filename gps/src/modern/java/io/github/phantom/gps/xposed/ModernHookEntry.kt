package io.github.phantom.gps.xposed

import android.app.Application
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.location.GnssStatus
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.ResultReceiver
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import io.github.phantom.gps.BuildConfig
import io.github.phantom.gps.utils.AccessPolicy
import io.github.phantom.gps.utils.AppIntegrity
import io.github.phantom.gps.utils.ClipboardEventGate
import io.github.phantom.gps.utils.ClipboardTrace
import io.github.phantom.gps.utils.DirectLocationProtocol
import io.github.phantom.gps.utils.HookLocationSnapshot
import io.github.phantom.gps.utils.HookPreferenceChannel
import io.github.phantom.gps.utils.LocationActions
import io.github.phantom.gps.utils.LocationRefresh
import io.github.phantom.gps.utils.MembershipLease
import io.github.phantom.gps.utils.RemoteHookSnapshot
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class ModernHookEntry : XposedModule() {
    private data class Fix(val snapshot: HookLocationSnapshot, val lat: Double, val lon: Double)
    private data class State(val fix: Fix?, val systemMode: Boolean, val clipboard: Boolean, val reason: String)

    @Volatile private var context: Context? = null
    @Volatile private var receiverRegistered = false
    @Volatile private var platformHooks = 0
    @Volatile private var fusedHooks = 0
    @Volatile private var gnssHooks = 0
    @Volatile private var nmeaHooks = 0
    @Volatile private var integrityValid = false
    private var preferences: SharedPreferences? = null
    private var encodedSnapshot: String? = null
    private var snapshotValues: Map<String, Any> = emptyMap()
    private var sampledAt = -1000L
    private var state = State(null, false, false, "SNAPSHOT_MISSING")
    private var promotedAt = -30000L
    private val clipboardGate = ClipboardEventGate()
    private val clipboardWorker by lazy { HandlerThread("Gozza-clipboard").apply { start() } }
    private var consumedTrace = 0L

    override fun onPackageReady(param: PackageReadyParam) {
        if (!param.isFirstPackage || param.packageName == BuildConfig.APPLICATION_ID) return
        val loader = param.classLoader
        methods(Application::class.java, "attach") { chain ->
            val result = chain.proceed()
            (chain.args.firstOrNull() as? Context)?.let(::attachContext)
            result
        }
        runCatching {
            val thread = Class.forName("android.app.ActivityThread")
            (thread.getDeclaredMethod("currentApplication").invoke(null) as? Context)?.let(::attachContext)
        }
        if (param.packageName == "com.google.android.gms") {
            // Change serialized locations in system mode, leaving Play Services' internal calculations alone.
            fusedHooks += methods(Location::class.java, "writeToParcel") { chain ->
                val fix = activeFix(systemOnly = true)
                val original = chain.thisObject as? Location
                if (fix == null || original == null) chain.proceed()
                else chain.proceedWith(replace(original, fix))
            }
            return
        }
        installClientLocation(loader)
        installGnss(loader, system = false)
    }

    override fun onSystemServerStarting(param: SystemServerStartingParam) {
        val loader = param.classLoader
        methods(loader, "com.android.server.SystemServer", "startOtherServices") { chain ->
            val result = chain.proceed()
            field(chain.thisObject, "mSystemContext")?.let { if (it is Context) attachContext(it) }
            result
        }
        for (name in listOf("com.android.server.location.LocationManagerService", "com.android.server.LocationManagerService")) {
            platformHooks += transformResults(loader, name, true, "getLastLocation")
        }
        platformHooks += transformArguments(loader, "android.location.ILocationListener\$Stub\$Proxy", true, "onLocationChanged")
        platformHooks += transformArguments(loader, "android.location.ILocationCallback\$Stub\$Proxy", true, "onLocation")
        // LocationResult is also the source of the last location used for PendingIntent delivery.
        platformHooks += transformResults(loader, "android.location.LocationResult", true, "getLastLocation", "asList")
        installGnss(loader, system = true)
        installClipboard(loader)
    }

    private fun installClientLocation(loader: ClassLoader) {
        for (getter in listOf("getLatitude", "getLongitude", "getAccuracy")) {
            platformHooks += methods(Location::class.java, getter) { chain ->
                val fix = activeFix() ?: return@methods chain.proceed()
                markConsumed(fix)
                when (getter) {
                    "getLatitude" -> fix.lat
                    "getLongitude" -> fix.lon
                    else -> fix.snapshot.accuracy
                }
            }
        }
        platformHooks += transformResults(loader, LocationManager::class.java.name, false, "getLastKnownLocation")
        // Incoming Android Location parcels include the fixes returned in GMS last/current-location Tasks.
        platformHooks += methods(Location.CREATOR.javaClass, "createFromParcel") { chain ->
            transform(chain.proceed(), systemOnly = false)
        }
        fusedHooks += transformResults(loader, "com.google.android.gms.location.LocationResult",
            false, "getLastLocation", "getLocations")
    }

    private fun installGnss(loader: ClassLoader, system: Boolean) {
        val nmeaClasses = if (system) listOf("android.location.IGnssNmeaListener\$Stub\$Proxy",
            "android.location.IGnssStatusListener\$Stub\$Proxy")
        else listOf("android.location.LocationManager\$GnssNmeaTransport",
            "android.location.LocationManager\$GnssStatusListenerTransport")
        for (name in nmeaClasses) {
            nmeaHooks += methods(loader, name, "onNmeaReceived") { chain ->
                val fix = activeFix(systemOnly = system) ?: return@methods chain.proceed()
                val args = chain.args.toTypedArray()
                val index = args.indexOfFirst { it is String }
                if (index < 0) return@methods chain.proceed()
                val timestamp = args.firstOrNull { it is Long } as? Long ?: System.currentTimeMillis()
                args[index] = GnssSimulation.sentence(args[index] as String, timestamp, fix.lat, fix.lon)
                    ?: return@methods null
                chain.proceed(args)
            }
        }
        if (Build.VERSION.SDK_INT >= 30) {
            val status = if (system) "android.location.IGnssStatusListener\$Stub\$Proxy"
                else "android.location.LocationManager\$GnssStatusTransport"
            gnssHooks += methods(loader, status, "onSvStatusChanged") { chain ->
                val args = chain.args.toTypedArray()
                val index = args.indexOfFirst { it is GnssStatus }
                if (index < 0 || activeFix(systemOnly = system) == null) return@methods chain.proceed()
                args[index] = syntheticStatus
                chain.proceed(args)
            }
        }
    }

    private val syntheticStatus: GnssStatus by lazy { buildSatelliteStatus() }

    @RequiresApi(30)
    private fun buildSatelliteStatus(): GnssStatus {
        val builder = GnssStatus.Builder()
        for ((index, svid) in GnssSimulation.satellites.withIndex()) {
            builder.addSatellite(GnssStatus.CONSTELLATION_GPS, svid, 29f + index, 20f + 6 * index,
                42f * index, true, true, true, true, 1575.42e6f, false, 0f)
        }
        return builder.build()
    }

    private fun installClipboard(loader: ClassLoader) {
        for (name in listOf("com.android.server.clipboard.ClipboardService",
            "com.android.server.clipboard.ClipboardService\$ClipboardImpl")) {
            methods(loader, name, "setPrimaryClip", "setPrimaryClipAsPackage") { chain ->
                val result = chain.proceed()
                if (!readState().clipboard) return@methods result
                val clip = chain.args.firstOrNull { it is ClipData } as? ClipData ?: return@methods result
                val source = context ?: (field(chain.thisObject, "mContext") as? Context)
                    ?: (field(field(chain.thisObject, "this\$0"), "mContext") as? Context)
                    ?: return@methods result
                if (clip.itemCount == 0) return@methods result
                val item = clip.getItemAt(0)
                val text = (item.text?.toString() ?: item.uri?.toString())?.trim().orEmpty()
                val capturedAt = SystemClock.elapsedRealtime()
                if (text.isEmpty() || !clipboardGate.accept(text, capturedAt)) return@methods result
                Handler(clipboardWorker.looper).post {
                    attachContext(source)
                    source.sendBroadcast(Intent(LocationActions.ACTION_CLIPBOARD_TEXT)
                        .setClassName(BuildConfig.APPLICATION_ID, "io.github.phantom.gps.utils.ClipboardLocationReceiver")
                        .addFlags(Intent.FLAG_RECEIVER_FOREGROUND or Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                        .putExtra(LocationActions.EXTRA_CLIPBOARD_TEXT, text)
                        .putExtra(ClipboardTrace.EXTRA_CAPTURED_AT, capturedAt))
                }
                result
            }
        }
    }

    @Synchronized
    private fun attachContext(source: Context) {
        if (receiverRegistered) return
        val ctx = source.applicationContext ?: source
        if (ctx.packageName == BuildConfig.APPLICATION_ID) return
        integrityValid = AppIntegrity.isModuleApkValid(ctx)
        if (!integrityValid) return
        context = ctx
        MembershipLease.initializeHookClock(ctx)
        val worker = HandlerThread("Gozza-modern-refresh").apply { start() }
        val filter = IntentFilter(LocationRefresh.ACTION).apply {
            addAction(LocationRefresh.CONFIRM_ACTION)
            addAction(LocationRefresh.DIAGNOSTIC_ACTION)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val current = readState(force = true)
                when (intent.action) {
                    LocationRefresh.ACTION -> promoteIfActive(ctx, current)
                    LocationRefresh.CONFIRM_ACTION -> {
                        val id = intent.getStringExtra("request_id")?.takeIf { it.isNotBlank() } ?: return
                        val reply = intent.getParcelableExtra<ResultReceiver>("reply") ?: return
                        confirm(ctx, id, reply, Handler(worker.looper), attempts = 4)
                    }
                    LocationRefresh.DIAGNOSTIC_ACTION -> {
                        val id = intent.getStringExtra("query_id")?.takeIf { it.length in 16..96 } ?: return
                        val reply = intent.getParcelableExtra<ResultReceiver>("reply") ?: return
                        reply.send(0, Bundle().apply {
                            putString("query_id", id)
                            putString("package", ctx.packageName)
                            putString("version", BuildConfig.VERSION_NAME)
                            putString("reason", current.reason)
                            putBoolean("system_mode_enabled", current.systemMode)
                            putBoolean("system_hooks_ready", platformHooks > 0)
                            putInt("platform_hooks", platformHooks)
                            putInt("fused_hooks", fusedHooks)
                            putInt("gnss_hooks", gnssHooks)
                            putInt("nmea_hooks", nmeaHooks)
                        })
                    }
                }
            }
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= 33) ctx.registerReceiver(receiver, filter, LocationRefresh.SENDER_PERMISSION,
                Handler(worker.looper), Context.RECEIVER_EXPORTED)
            else ctx.registerReceiver(receiver, filter, LocationRefresh.SENDER_PERMISSION, Handler(worker.looper))
            receiverRegistered = true
        }.onFailure { worker.quitSafely() }
    }

    private fun confirm(ctx: Context, id: String, reply: ResultReceiver, handler: Handler, attempts: Int) {
        val current = readState(force = true)
        val ready = when (ctx.packageName) {
            "android" -> current.systemMode && platformHooks > 0
            "com.google.android.gms" -> current.systemMode && fusedHooks > 0
            else -> platformHooks + fusedHooks > 0
        }
        val requestId = current.fix?.snapshot?.requestId
        if (ready && requestId != id && attempts > 0) {
            // The preference notification can arrive after the app's confirmation broadcast.
            handler.postDelayed({ confirm(ctx, id, reply, handler, attempts - 1) }, 35)
            return
        }
        val code = when {
            !ready || requestId == null -> DirectLocationProtocol.HOOK_NOT_READY
            requestId != id -> DirectLocationProtocol.SUPERSEDED
            else -> DirectLocationProtocol.HOOK_LOADED
        }
        reply.send(code, Bundle().apply { putString("request_id", id) })
    }

    private fun promoteIfActive(ctx: Context, current: State) {
        if (ctx.packageName != "android" || current.fix == null) return
        val now = SystemClock.elapsedRealtime()
        if (now - promotedAt < 30000) return
        runCatching {
            ctx.startForegroundService(Intent().setClassName(BuildConfig.APPLICATION_ID,
                "io.github.phantom.gps.utils.MembershipHeartbeatService"))
            promotedAt = now
        }
    }

    @Synchronized
    private fun readState(force: Boolean = false): State {
        val now = SystemClock.elapsedRealtime()
        if (!force && now - sampledAt < 100) return state
        sampledAt = now
        val values = runCatching {
            val prefs = preferences ?: getRemotePreferences(HookPreferenceChannel.GROUP).also { preferences = it }
            val encoded = prefs.getString(RemoteHookSnapshot.KEY, null)
            if (encoded != encodedSnapshot) {
                snapshotValues = encoded?.let(RemoteHookSnapshot::decode) ?: emptyMap()
                encodedSnapshot = encoded
            }
            snapshotValues
        }.getOrElse {
            preferences = null
            return State(null, false, false, "SNAPSHOT_MISSING").also { state = it }
        }
        val snapshot = HookLocationSnapshot.from(values)
        val accessAllowed = integrityValid && AccessPolicy.fresh(values, now, MembershipLease.hookBootCount)
        val reason = when {
            !values.containsKey("start") -> "SNAPSHOT_MISSING"
            !accessAllowed -> "LEASE_TIME_INVALID"
            !snapshot.started -> "STOPPED"
            snapshot.latitude !in -90.0..90.0 || snapshot.longitude !in -180.0..180.0 ||
                !snapshot.accuracy.isFinite() || snapshot.accuracy < 0f -> "SNAPSHOT_INVALID"
            else -> "READY"
        }
        val fix = if (reason == "READY") state.fix?.takeIf { it.snapshot == snapshot } ?: buildFix(snapshot) else null
        state = State(fix, values["system_hooked"] == true,
            accessAllowed && values["clipboard_monitor_enabled"] == true, reason)
        return state
    }

    private fun buildFix(snapshot: HookLocationSnapshot): Fix {
        if (!snapshot.allowRandom || !snapshot.radiusMeters.isFinite()) return Fix(snapshot, snapshot.latitude, snapshot.longitude)
        val radians = PI / 180.0
        // Every hooked process derives the same offset for a given committed snapshot.
        val random = Random(snapshot.latitude.toBits() xor snapshot.longitude.toBits() xor snapshot.traceId)
        val angle = random.nextDouble(2 * PI)
        val distance = sqrt(random.nextDouble()) * snapshot.radiusMeters / 6378137.0 / radians
        val lat = (snapshot.latitude + distance * cos(angle)).coerceIn(-90.0, 90.0)
        val cosLat = abs(cos(snapshot.latitude * radians)).coerceAtLeast(1e-6)
        val lon = snapshot.longitude + distance * sin(angle) / cosLat
        return Fix(snapshot, lat, ((lon + 180.0) % 360.0 + 360.0) % 360.0 - 180.0)
    }

    private fun activeFix(systemOnly: Boolean = false): Fix? =
        readState().let { if (!systemOnly || it.systemMode) it.fix else null }

    @Synchronized
    private fun markConsumed(fix: Fix) {
        val snapshot = fix.snapshot
        if (snapshot.traceId <= 0 || snapshot.capturedAtMs <= 0 || consumedTrace == snapshot.traceId) return
        consumedTrace = snapshot.traceId
        ClipboardTrace(snapshot.traceId, snapshot.capturedAtMs).mark("hook_first_read")
    }

    private fun replace(original: Location, fix: Fix): Location {
        markConsumed(fix)
        return Location(original).apply {
            latitude = fix.lat
            longitude = fix.lon
            accuracy = fix.snapshot.accuracy
            altitude = 0.0
            speed = 0f
            bearing = 0f
            extras = null
        }
    }

    private fun transform(value: Any?, systemOnly: Boolean): Any? {
        val fix = activeFix(systemOnly) ?: return value
        return transform(value, fix)
    }

    private fun transform(value: Any?, fix: Fix): Any? = when (value) {
        is Location -> replace(value, fix)
        is List<*> -> if (value.isNotEmpty() && value.all { it is Location }) value.map { replace(it as Location, fix) } else value
        else -> value
    }

    private fun transformResults(loader: ClassLoader, type: String, system: Boolean, vararg names: String): Int =
        methods(loader, type, *names) { chain -> transform(chain.proceed(), system) }

    private fun transformArguments(loader: ClassLoader, type: String, system: Boolean, vararg names: String): Int =
        methods(loader, type, *names) { chain ->
            val fix = activeFix(systemOnly = system) ?: return@methods chain.proceed()
            val args = chain.args.map { transform(it, fix) }.toTypedArray()
            chain.proceed(args)
        }

    private fun methods(loader: ClassLoader, name: String, vararg names: String,
                        interceptor: (XposedInterface.Chain) -> Any?): Int {
        val type = runCatching { Class.forName(name, false, loader) }.getOrNull() ?: return 0
        return methods(type, *names, interceptor = interceptor)
    }

    private fun methods(type: Class<*>, vararg names: String,
                        interceptor: (XposedInterface.Chain) -> Any?): Int {
        val candidates = runCatching { type.declaredMethods }.getOrNull() ?: return 0
        return candidates.count { method: Method ->
            method.name in names && !method.isBridge && !Modifier.isAbstract(method.modifiers) && runCatching {
                hook(method).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(interceptor)
            }.onFailure {
                log(Log.WARN, "GeoX", "Could not hook ${type.name}.${method.name}", it)
            }.isSuccess
        }
    }

    private fun field(value: Any?, name: String): Any? {
        if (value == null) return null
        return runCatching {
            value.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(value)
        }.getOrNull()
    }
}
