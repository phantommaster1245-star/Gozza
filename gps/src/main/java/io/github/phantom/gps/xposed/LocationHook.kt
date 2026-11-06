package io.github.phantom.gps.xposed

// https://github.com/rovo89/XposedBridge/wiki/Helpers

import android.annotation.SuppressLint
import android.app.AndroidAppHelper
import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.HandlerThread
import io.github.phantom.gps.utils.LocationRefresh
import io.github.phantom.gps.utils.ClipboardTrace
import android.location.Location
import android.location.LocationManager
import android.location.LocationRequest
import android.os.Build
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.phantom.gps.BuildConfig
import org.lsposed.hiddenapibypass.HiddenApiBypass
import timber.log.Timber
import java.util.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object LocationHook {

    var newlat: Double = 45.0000
    var newlng: Double = 0.0000
    private const val pi = 3.14159265359
    private var accuracy: Float = 0.0f
    private val rand: Random = Random()
    private const val earth = 6378137.0
    private val settings = Xshare()
    @Volatile private var mLastUpdated: Long = 0
    @Volatile private var refreshRegistered = false
    @Volatile private var currentTrace: ClipboardTrace? = null
    @Volatile private var loadedDirectRequestId: String = ""
    @Volatile private var loadedStarted: Boolean = false
    @Volatile private var systemHooksReady: Boolean = false
    private var loadedTraceId: Long = 0
    private var consumedTraceId: Long = 0
    private var lastActiveServicePromotion=0L

    @Synchronized
    fun ensureRefreshReceiver(source: Context) {
        io.github.phantom.gps.utils.MembershipLease.initializeHookClock(source)
        if (refreshRegistered) return
        val receiverContext = source.applicationContext ?: source
        if (receiverContext.packageName == BuildConfig.APPLICATION_ID) return
        val worker = HandlerThread("Phantom-location-refresh").apply { start() }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent?) {
                if (intent?.action == LocationRefresh.DIAGNOSTIC_ACTION) {
                    val id=intent.getStringExtra("query_id").orEmpty()
                    if(id.length !in 16..96)return
                    val reply=intent.getParcelableExtra<android.os.ResultReceiver>("reply") ?: return
                    val state=runCatching { settings.diagnosticState() }.getOrElse { android.os.Bundle().apply { putString("reason","DIAGNOSTIC_FAILED") } }
                    state.putString("query_id",id)
                    state.putString("package",receiverContext.packageName)
                    state.putString("version",BuildConfig.VERSION_NAME)
                    state.putBoolean("system_hooks_ready",systemHooksReady)
                    state.putBoolean("system_mode_enabled",settings.isHookedSystem)
                    reply.send(0,state)
                    XposedBridge.log("Gozza Lalamove Hook diagnostic: package=${receiverContext.packageName} version=${BuildConfig.VERSION_NAME} reason=${state.getString("reason")}")
                } else if (intent?.action == LocationRefresh.CONFIRM_ACTION) {
                    val id = intent.getStringExtra("request_id").orEmpty()
                    val reply = intent.getParcelableExtra<android.os.ResultReceiver>("reply") ?: return
                    if (id.isBlank()) return
                    if (loadedDirectRequestId != id) updateLocation(forceReload = true)
                    val matching = loadedDirectRequestId == id
                    val modeReady = receiverContext.packageName != "android" || (systemHooksReady && settings.isHookedSystem)
                    val code = if (!matching) 409 else if (loadedStarted && settings.isStarted && modeReady) 101 else 412
                    reply.send(code, android.os.Bundle().apply { putString("request_id", id) })
                } else if (intent?.action == LocationRefresh.ACTION) {
                    val wasStarted=loadedStarted
                    updateLocation(forceReload = true)
                    val now=android.os.SystemClock.elapsedRealtime()
                    if(receiverContext.packageName=="android"&&loadedStarted&&(!wasStarted||now-lastActiveServicePromotion>=30000)) {
                        // An authorized module refresh only. System scope can promote the already-validated active engine
                        // when Android restricts a background app's own FGS launch. Never changes coordinates or grants a lease.
                        runCatching {
                            receiverContext.startForegroundService(Intent().setClassName(BuildConfig.APPLICATION_ID,
                                "io.github.phantom.gps.utils.MembershipHeartbeatService"))
                            lastActiveServicePromotion=now
                        }.onFailure { XposedBridge.log("Gozza Lalamove: active service promotion unavailable: "+it.javaClass.simpleName) }
                    }
                }
            }
        }
        val filter = IntentFilter(LocationRefresh.ACTION).apply { addAction(LocationRefresh.CONFIRM_ACTION); addAction(LocationRefresh.DIAGNOSTIC_ACTION) }
        runCatching {
            if (Build.VERSION.SDK_INT >= 33) {
                receiverContext.registerReceiver(receiver, filter,
                    LocationRefresh.SENDER_PERMISSION, Handler(worker.looper), Context.RECEIVER_EXPORTED)
            } else {
                receiverContext.registerReceiver(receiver, filter,
                    LocationRefresh.SENDER_PERMISSION, Handler(worker.looper))
            }
            refreshRegistered = true
            // The first clipboard event can beat receiver registration. Load the
            // latest committed snapshot once so that an early refresh is not lost.
            updateLocation(forceReload = true)
        }.onFailure { worker.quitSafely() }
    }

    @Synchronized private fun markLocationRead() {
        val trace = currentTrace ?: return
        if (trace.id != consumedTraceId) {
            consumedTraceId = trace.id
            trace.mark("hook_first_read")
        }
    }
    private val ignorePkg = arrayListOf("com.android.location.fused", BuildConfig.APPLICATION_ID)

    private val context by lazy { AndroidAppHelper.currentApplication() as Context }

    @Synchronized
    private fun updateLocation(forceReload: Boolean = false) {
        try {
            val snapshot = settings.locationSnapshot(forceReload)
            mLastUpdated = System.currentTimeMillis()
            val baseLat = snapshot.latitude
            val baseLng = snapshot.longitude

            if (snapshot.allowRandom) {
                val radiusMeters = snapshot.radiusMeters
                val angle = rand.nextDouble() * 2.0 * pi
                val distance = sqrt(rand.nextDouble()) * radiusMeters
                val dlat = (distance * cos(angle)) / earth
                val latCos = cos(pi * baseLat / 180.0)
                val safeLatCos = if (abs(latCos) < 1e-6) 1e-6 else latCos
                val dlng = (distance * sin(angle)) / (earth * safeLatCos)

                newlat = baseLat + (dlat * 180.0 / pi)
                newlng = baseLng + (dlng * 180.0 / pi)
            } else {
                newlat = baseLat
                newlng = baseLng
            }
            accuracy = snapshot.accuracy
            loadedStarted = snapshot.started && settings.isStarted
            loadedDirectRequestId = snapshot.requestId
            currentTrace = if (snapshot.traceId > 0 && snapshot.capturedAtMs > 0) ClipboardTrace(snapshot.traceId, snapshot.capturedAtMs) else null
            currentTrace?.let { trace ->
                if (trace.id != loadedTraceId) {
                    loadedTraceId = trace.id
                    trace.mark("hook_loaded")
                }
            }

        } catch (e: Exception) {
            Timber.tag("GPS Setter")
                .e(e, "Failed to read Gozza Lalamove hook snapshot")
        }
    }

    @SuppressLint("NewApi")
    fun initHooks(lpparam: XC_LoadPackage.LoadPackageParam) {
        (AndroidAppHelper.currentApplication() as? Context)?.let { ensureRefreshReceiver(it) }
        if (lpparam.packageName != "android" && lpparam.packageName != BuildConfig.APPLICATION_ID) {
            XposedHelpers.findAndHookMethod(android.app.Application::class.java, "attach", Context::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        (param.args.firstOrNull() as? Context)?.let { ensureRefreshReceiver(it) }
                    }
                })
        }


        if (lpparam.packageName == "android") {
            // Register after ActivityManager is ready, including when Clipboard monitor is off.
            runCatching {
                val server = XposedHelpers.findClass("com.android.server.SystemServer", lpparam.classLoader)
                XposedBridge.hookAllMethods(server, "startOtherServices", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        runCatching { XposedHelpers.getObjectField(param.thisObject, "mSystemContext") as? Context }
                            .getOrNull()?.let { ensureRefreshReceiver(it) }
                    }
                })
            }
        if (!ignorePkg.contains(lpparam.packageName)) {
            if (System.currentTimeMillis() - mLastUpdated > 200) {
                updateLocation()
            }

            if (Build.VERSION.SDK_INT < 34) {

                val LocationManagerServiceClass = XposedHelpers.findClass(
                    "com.android.server.LocationManagerService",
                    lpparam.classLoader
                )

                XposedHelpers.findAndHookMethod(
                    LocationManagerServiceClass, "getLastLocation",
                    LocationRequest::class.java, String::class.java,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            if (!settings.isStarted || !settings.isHookedSystem) return
                            val location = Location(LocationManager.GPS_PROVIDER)
                            location.time = System.currentTimeMillis() - 300
                            markLocationRead()
                            location.latitude = newlat
                            location.longitude = newlng
                            location.altitude = 0.0
                            location.speed = 0F
                            location.accuracy = accuracy
                            location.speedAccuracyMetersPerSecond = 0F
                            param.result = location
                        }
                    }
                )

                for (method in LocationManagerServiceClass.declaredMethods) {
                    if (method.returnType == Boolean::class.java) {
                        if (method.name == "addGnssBatchingCallback" ||
                            method.name == "addGnssMeasurementsListener" ||
                            method.name == "addGnssNavigationMessageListener"
                        ) {
                            XposedBridge.hookMethod(
                                method,
                                object : XC_MethodHook() {
                                    override fun beforeHookedMethod(param: MethodHookParam) {
                            if (!settings.isStarted || !settings.isHookedSystem) return
                                        param.result = false
                                    }
                                }
                            )
                        }
                    }
                }

                XposedHelpers.findAndHookMethod(
                    "com.android.server.LocationManagerService.Receiver", // com.android.server.LocationManagerService\$Receiver
                    lpparam.classLoader,
                    "callLocationChangedLocked",
                    Location::class.java,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            if (!settings.isStarted || !settings.isHookedSystem) return
                            lateinit var location: Location
                            lateinit var originLocation: Location
                            if (param.args[0] == null) {
                                location = Location(LocationManager.GPS_PROVIDER)
                                location.time = System.currentTimeMillis() - 300
                            } else {
                                originLocation = param.args[0] as Location
                                location = Location(originLocation.provider)
                                location.time = originLocation.time
                                location.accuracy = accuracy
                                location.bearing = originLocation.bearing
                                location.bearingAccuracyDegrees = originLocation.bearingAccuracyDegrees
                                location.elapsedRealtimeNanos = originLocation.elapsedRealtimeNanos
                                location.verticalAccuracyMeters = originLocation.verticalAccuracyMeters
                            }

                            markLocationRead()
                            location.latitude = newlat
                            location.longitude = newlng
                            location.altitude = 0.0
                            location.speed = 0F
                            location.speedAccuracyMetersPerSecond = 0F
                            try {
                                HiddenApiBypass.invoke(
                                    location.javaClass, location, "setIsFromMockProvider", false
                                )
                            } catch (e: Exception) {
                                XposedBridge.log("LocationHook: unable to set mock $e")
                            }
                            param.args[0] = location
                        }
                    }
                )
            } else {

                val LocationManagerServiceClass = XposedHelpers.findClass(
                    "com.android.server.location.LocationManagerService",
                    lpparam.classLoader
                )
                for (method in LocationManagerServiceClass.declaredMethods) {
                    if (method.name == "getLastLocation" && method.returnType == Location::class.java) {
                        // params: String::class.java, LastLocationRequest::class.java, String::class.java, String::class.java
                        XposedBridge.hookMethod(
                            method,
                            object : XC_MethodHook() {
                                override fun beforeHookedMethod(param: MethodHookParam) {
                            if (!settings.isStarted || !settings.isHookedSystem) return
                                    val location = Location(LocationManager.GPS_PROVIDER)
                                    location.time = System.currentTimeMillis() - 300
                                    markLocationRead()
                            location.latitude = newlat
                                    location.longitude = newlng
                                    location.altitude = 0.0
                                    location.speed = 0F
                                    location.accuracy = accuracy
                                    location.speedAccuracyMetersPerSecond = 0F
                                    param.result = location
                                }
                            }
                        )
                    } else if (method.returnType == Void::class.java) {
                        if (method.name == "startGnssBatch" ||
                            method.name == "addGnssAntennaInfoListener" ||
                            method.name == "addGnssMeasurementsListener" ||
                            method.name == "addGnssNavigationMessageListener"
                        ) {
                            XposedBridge.hookMethod(
                                method,
                                object : XC_MethodHook() {
                                    override fun beforeHookedMethod(param: MethodHookParam) {
                            if (!settings.isStarted || !settings.isHookedSystem) return
                                        param.result = null
                                    }
                                }
                            )
                        }
                    }
                }
                XposedHelpers.findAndHookMethod(
                    LocationManagerServiceClass,
                    "injectLocation",
                    Location::class.java,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            if (!settings.isStarted || !settings.isHookedSystem) return
                            lateinit var location: Location
                            lateinit var originLocation: Location
                            if (param.args[0] == null) {
                                location = Location(LocationManager.GPS_PROVIDER)
                                location.time = System.currentTimeMillis() - 300
                            } else {
                                originLocation = param.args[0] as Location
                                location = Location(originLocation.provider)
                                location.time = originLocation.time
                                location.accuracy = accuracy
                                location.bearing = originLocation.bearing
                                location.bearingAccuracyDegrees = originLocation.bearingAccuracyDegrees
                                location.elapsedRealtimeNanos = originLocation.elapsedRealtimeNanos
                                location.verticalAccuracyMeters = originLocation.verticalAccuracyMeters
                            }

                            markLocationRead()
                            location.latitude = newlat
                            location.longitude = newlng
                            location.altitude = 0.0
                            location.speed = 0F
                            location.speedAccuracyMetersPerSecond = 0F
                            try {
                                HiddenApiBypass.invoke(
                                    location.javaClass, location, "setIsFromMockProvider", false
                                )
                            } catch (e: Exception) {
                                XposedBridge.log("LocationHook: unable to set mock $e")
                            }
                            param.args[0] = location
                        }
                    }
                )
                systemHooksReady = true
            }
        }
        } else { // application hook

            val LocationClass = XposedHelpers.findClass(
                "android.location.Location",
                lpparam.classLoader
            )
            val interval = 80 // 200

            for (method in LocationClass.declaredMethods) {
                if (method.name == "getLatitude") {
                    XposedBridge.hookMethod(
                        method,
                        object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                if (System.currentTimeMillis() - mLastUpdated > interval) {
                                    updateLocation()
                                }
                                if (settings.isStarted && !ignorePkg.contains(lpparam.packageName)) {
                                    markLocationRead()
                                    param.result = newlat
                                }
                            }
                        }
                    )
                } else if (method.name == "getLongitude") {
                    XposedBridge.hookMethod(
                        method,
                        object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                if (System.currentTimeMillis() - mLastUpdated > interval) {
                                    updateLocation()
                                }
                                if (settings.isStarted && !ignorePkg.contains(lpparam.packageName)) {
                                    markLocationRead()
                                    param.result = newlng
                                }
                            }
                        }
                    )
                } else if (method.name == "getAccuracy") {
                    XposedBridge.hookMethod(
                        method,
                        object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                if (System.currentTimeMillis() - mLastUpdated > interval) {
                                    updateLocation()
                                }
                                if (settings.isStarted && !ignorePkg.contains(lpparam.packageName)) {
                                    param.result = accuracy
                                }
                            }
                        }
                    )
                }
            }

            XposedHelpers.findAndHookMethod(
                LocationClass,
                "set",
                Location::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {

                        if (System.currentTimeMillis() - mLastUpdated > interval) {
                            updateLocation()
                        }
                        if (settings.isStarted && !ignorePkg.contains(lpparam.packageName)) {
                            lateinit var location: Location
                            lateinit var originLocation: Location
                            if (param.args[0] == null) {
                                location = Location(LocationManager.GPS_PROVIDER)
                                location.time = System.currentTimeMillis() - 300
                            } else {
                                originLocation = param.args[0] as Location
                                location = Location(originLocation.provider)
                                location.time = originLocation.time
                                location.accuracy = accuracy
                                location.bearing = originLocation.bearing
                                location.bearingAccuracyDegrees = originLocation.bearingAccuracyDegrees
                                location.elapsedRealtimeNanos = originLocation.elapsedRealtimeNanos
                                location.verticalAccuracyMeters = originLocation.verticalAccuracyMeters
                            }

                            markLocationRead()
                            location.latitude = newlat
                            location.longitude = newlng
                            location.altitude = 0.0
                            location.speed = 0F
                            location.speedAccuracyMetersPerSecond = 0F
                            try {
                                HiddenApiBypass.invoke(
                                    location.javaClass, location, "setIsFromMockProvider", false
                                )
                            } catch (e: Exception) {
                                XposedBridge.log("LocationHook: unable to set mock $e")
                            }
                            param.args[0] = location
                        }
                    }
                }
            )

            XposedHelpers.findAndHookMethod(
                "android.location.LocationManager",
                lpparam.classLoader,
                "getLastKnownLocation",
                String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (System.currentTimeMillis() - mLastUpdated > interval) {
                            updateLocation()
                        }
                        if (settings.isStarted && !ignorePkg.contains(lpparam.packageName)) {
                            val provider = param.args[0] as String
                            val location = Location(provider)
                            location.time = System.currentTimeMillis() - 300
                            markLocationRead()
                            location.latitude = newlat
                            location.longitude = newlng
                            location.altitude = 0.0
                            location.speed = 0F
                            location.speedAccuracyMetersPerSecond = 0F
                            try {
                                HiddenApiBypass.invoke(
                                    location.javaClass, location, "setIsFromMockProvider", false
                                )
                            } catch (e: Exception) {
                                XposedBridge.log("LocationHook: unable to set mock $e")
                            }
                            param.result = location
                        }
                    }
                }
            )
        }
    }
}

