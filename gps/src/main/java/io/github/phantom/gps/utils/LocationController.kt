package io.github.phantom.gps.utils

import android.app.Notification
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import io.github.phantom.gps.R

object LocationController {

    fun start(context: Context, lat: Double, lon: Double, label: String?, trace: ClipboardTrace? = null):Boolean =
        applyImmediately(context, lat, lon, label, trace)

    fun applyImmediately(context: Context, lat: Double, lon: Double, label: String?,
        trace: ClipboardTrace? = null, requestId: String = "", onApplied: () -> Unit = {}): Boolean {
        if (!LicenseGuard.isLocallyAllowedForCriticalAction(context) || !PrefManager.isHookSharingAvailable) return false
        if (!PrefManager.updateNow(start = true, la = lat, ln = lon, trace = trace, requestId = requestId)) return false
        LocationRefresh.send(context, trace)
        // The caller gets its fast acknowledgement before optional UI and notification work.
        onApplied()
        context.sendBroadcast(
            Intent(LocationActions.ACTION_LOCATION_STARTED).apply {
                setPackage(context.packageName)
                addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                putExtra(LocationActions.EXTRA_LAT, lat)
                putExtra(LocationActions.EXTRA_LON, lon)
                if (!label.isNullOrBlank()) {
                    putExtra(LocationActions.EXTRA_LABEL, label)
                }
            }
        )
        showStartedNotification(context, label)
        LicenseGuard.startHeartbeat(context)
        trace?.mark("notification_posted")
        return true
    }

    fun stop(context: Context) {
        PrefManager.updateNow(start = false, la = PrefManager.getLat, ln = PrefManager.getLng)
        LocationRefresh.send(context)
        context.stopService(Intent(context, JoystickService::class.java))
        LicenseGuard.stopActiveHeartbeat(context)
        LocationNotification.stopped(context)
        context.sendBroadcast(
            Intent(LocationActions.ACTION_LOCATION_STOPPED).apply {
                setPackage(context.packageName)
                addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            }
        )
    }

    fun updateStartedNotification(context: Context, label: String?) {
        if(PrefManager.isStarted)LocationNotification.updateLabel(context,label)
    }

    private fun showStartedNotification(context: Context, label: String?) {
        if(PrefManager.isStarted)LocationNotification.show(context,label)
    }
}
