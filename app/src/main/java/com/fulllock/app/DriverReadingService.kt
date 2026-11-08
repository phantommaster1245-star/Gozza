package com.fulllock.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import android.util.Log
import io.github.phantom.gps.utils.TesterAccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Keeps the user-enabled Driver reading session independent of the Gozza task. */
class DriverReadingService : Service() {
    companion object {
        private const val CHANNEL = "gozza.driver_reading"
        private const val NOTIFICATION_ID = 127
        private const val STOP = "com.gozza.lalamove.action.STOP_DRIVER_READING"
        @Volatile private var foreground = false

        @JvmStatic
        fun startIfEnabled(context: Context) {
            if (foreground || !DriverBridge.viewsEnabled(context) || !AppIntegrity.trusted(context)) return
            runCatching { context.startForegroundService(Intent(context, DriverReadingService::class.java)) }
                .onFailure { Log.w("GozzaReading", "Unable to start Driver reading service", it) }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val rootSettings by lazy { getSharedPreferences("root_research", MODE_PRIVATE) }
    private val rootListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if ((key == "enabled" || key == "screenViews") && foreground) syncSession()
    }

    override fun onCreate() {
        super.onCreate()
        rootSettings.registerOnSharedPreferenceChangeListener(rootListener)
        scope.launch {
            TesterAccess.state.collect { if (foreground) syncSession() }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // A queued stop must still satisfy Android's foreground-start deadline.
        if (!foreground) {
            startForeground(NOTIFICATION_ID, readingNotification())
            foreground = true
        }
        if (intent?.action == STOP) RootMode.setEnabled(this, false)
        return if (syncSession()) START_STICKY else START_NOT_STICKY
    }

    private fun syncSession(): Boolean {
        val enabled = RootMode.enabled(this) && rootSettings.getBoolean("screenViews", false) &&
            !RootSetup.updatePending && AppIntegrity.trusted(this)
        if (!enabled || (!TesterAccess.available(this) && TesterAccess.state.value != TesterAccess.State.Loading)) {
            LocalRelayClient.stop()
            ScreenActions.wake(this)
            stopSelf()
            return false
        }
        if (TesterAccess.available(this)) LocalRelayClient.start(this)
        return true
    }

    private fun readingNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "อ่านข้อมูล Driver", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, NOTIFICATION_ID, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stop = PendingIntent.getService(this, NOTIFICATION_ID,
            Intent(this, DriverReadingService::class.java).setAction(STOP), PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("Gozza Lalamove")
            .setContentText("กำลังอ่านข้อมูลและแสดงป้ายบน Driver")
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(Notification.Action.Builder(null, "หยุดอ่านข้อมูล", stop).build())
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        foreground = false
        rootSettings.unregisterOnSharedPreferenceChangeListener(rootListener)
        scope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}
