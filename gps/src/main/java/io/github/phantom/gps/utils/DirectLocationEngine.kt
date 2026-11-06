package io.github.phantom.gps.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.os.SystemClock
import java.util.concurrent.atomic.AtomicBoolean

/** Shared serialized pipeline for Intent and authenticated Binder sessions. */
internal object DirectLocationEngine {
    fun submit(context: Context, intent: Intent, reply: ResultReceiver, allowed: () -> Boolean = { true }, finished: () -> Unit = {}) {
        val receivedAt = SystemClock.elapsedRealtime()
        val id = intent.getStringExtra("request_id").orEmpty()
        val result = Responder(reply, id, receivedAt)
        val extras = intent.extras
        val lat = extras?.get("latitude") as? Double
        val lon = extras?.get("longitude") as? Double
        if (intent.getIntExtra("api_version", 0) != DirectLocationProtocol.VERSION || lat == null || lon == null) {
            result.send(DirectLocationProtocol.INVALID_REQUEST, "INVALID_REQUEST"); finished(); return
        }
        val request = DirectLocationRequest(id, lat, lon, intent.getStringExtra("label").orEmpty(), intent.getStringExtra("target_package").orEmpty())
        if (!request.isValid()) { result.send(DirectLocationProtocol.INVALID_REQUEST, "INVALID_REQUEST"); finished(); return }
        val app = context.applicationContext
        try { queue.execute { isLatest ->
            var written = false
            try {
                if (!allowed() || !isLatest()) { result.send(DirectLocationProtocol.SUPERSEDED, "SUPERSEDED"); return@execute }
                if (!AppIntegrity.isSelfValid(app)) { result.send(DirectLocationProtocol.APPLY_FAILED, "INTEGRITY_FAILED"); return@execute }
                if (LicenseStore.isLocked(app)) { result.send(DirectLocationProtocol.LICENSE_LOCKED, "LICENSE_LOCKED"); return@execute }
                if (!LicenseGuard.isLocallyAllowedForCriticalAction(app)) { result.send(DirectLocationProtocol.LICENSE_REQUIRED, "LICENSE_REQUIRED"); return@execute }
                val old = history.find(id)
                if (old != null && old != request) { result.send(DirectLocationProtocol.INVALID_REQUEST, "REQUEST_ID_REUSED"); return@execute }
                if (old == request) {
                    if (!LicenseGuard.isLocallyAllowedForCriticalAction(app)) { result.send(DirectLocationProtocol.LICENSE_REQUIRED, "LICENSE_REQUIRED"); return@execute }
                    if (PrefManager.directRequestId != id || !PrefManager.isStarted) { result.send(DirectLocationProtocol.SUPERSEDED, "SUPERSEDED", applied = true); return@execute }
                    result.send(DirectLocationProtocol.APPLIED, "APPLIED", applied = true)
                    confirmHook(app, request, result)
                    return@execute
                }
                if (!allowed() || !isLatest()) { result.send(DirectLocationProtocol.SUPERSEDED, "SUPERSEDED"); return@execute }
                var reported = false
                val applied = LocationController.applyImmediately(app, lat, lon, request.label, requestId = id) {
                    history.record(request)
                    written = true
                    reported = true
                    result.send(DirectLocationProtocol.APPLIED, "APPLIED", applied = true)
                    confirmHook(app, request, result)
                }
                if (!applied && !reported) {
                    if (!LicenseGuard.isLocallyAllowedForCriticalAction(app)) result.send(DirectLocationProtocol.LICENSE_REQUIRED, "LICENSE_REQUIRED")
                    else result.send(DirectLocationProtocol.APPLY_FAILED, "APPLY_FAILED")
                }
            } catch (_: Exception) {
                if (!written) result.send(DirectLocationProtocol.APPLY_FAILED, "APPLY_FAILED")
            } finally { finished() }
        } } catch (_: java.util.concurrent.RejectedExecutionException) {
            result.send(503, "BUSY")
            finished()
        }
    }

    private class Responder(private val receiver: ResultReceiver, private val id: String, private val receivedAt: Long) {
        fun send(code: Int, status: String, applied: Boolean = false, hookPackage: String = "") {
            runCatching { receiver.send(code, Bundle().apply {
                putInt("api_version", DirectLocationProtocol.VERSION)
                putString("request_id", id)
                putString("status", status)
                putBoolean("applied", applied)
                putBoolean("hook_loaded", code == DirectLocationProtocol.HOOK_LOADED)
                putLong("elapsed_ms", (SystemClock.elapsedRealtime() - receivedAt).coerceAtLeast(0))
                if (hookPackage.isNotEmpty()) putString("hook_package", hookPackage)
            }) }
        }
    }


        private val worker = java.util.concurrent.ThreadPoolExecutor(1, 1, 0L, java.util.concurrent.TimeUnit.MILLISECONDS, java.util.concurrent.ArrayBlockingQueue<Runnable>(32), { task -> Thread(task, "Phantom-direct-location").apply { isDaemon = true } })
        private val queue = DirectRequestQueue(worker)
        private val history = DirectLocationHistory()
        private val main = Handler(Looper.getMainLooper())

        private fun confirmHook(context: Context, request: DirectLocationRequest, result: Responder) {
            if (request.targetPackage.isEmpty()) return
            val finished = AtomicBoolean(false)
            val timeout = Runnable {
                if (finished.compareAndSet(false, true)) result.send(DirectLocationProtocol.HOOK_TIMEOUT, "HOOK_TIMEOUT", applied = true)
            }
            val callback = object : ResultReceiver(main) {
                override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                    if (resultData?.getString("request_id") != request.id || !finished.compareAndSet(false, true)) return
                    main.removeCallbacks(timeout)
                    if (PrefManager.directRequestId != request.id || resultCode == DirectLocationProtocol.SUPERSEDED) {
                        result.send(DirectLocationProtocol.SUPERSEDED, "SUPERSEDED", applied = true)
                    } else if (resultCode == DirectLocationProtocol.HOOK_LOADED) {
                        result.send(DirectLocationProtocol.HOOK_LOADED, "HOOK_LOADED", applied = true, hookPackage = request.targetPackage)
                    } else {
                        result.send(DirectLocationProtocol.HOOK_NOT_READY, "HOOK_NOT_READY", applied = true)
                    }
                }
            }
            main.postDelayed(timeout, 1500)
            LocationRefresh.confirm(context, request.targetPackage, request.id, callback)
        }
}
