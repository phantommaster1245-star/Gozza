package io.github.phantom.gps.utils

import android.app.Service
import android.content.Intent
import android.os.IBinder
import io.github.phantom.gps.R

class MembershipHeartbeatService : Service() {
    companion object {
        @Volatile var running = false
            private set
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificationId = NotificationsChannel.NOTIFICATION_ID_LOCATION
        if (!running) {
            // Fulfill the foreground start even if STOP or revocation beat delivery.
            val starting = NotificationsChannel().buildNotification(this, NotificationsChannel.CHANNEL_ID_LOCATION) {
                it.setSmallIcon(R.drawable.ic_stop)
                    .setContentTitle("Gozza Lalamove")
                    .setSilent(true)
                    .setOngoing(true)
            }
            startForeground(notificationId, starting)
            running = true
        }

        if (!PrefManager.isStarted || !GpsAccess.available(this)) {
            stopSelfResult(startId)
            return START_NOT_STICKY
        }
        startForeground(notificationId, LocationNotification.build(this))
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        running = false
        stopForeground(STOP_FOREGROUND_DETACH)
        LocationNotification.stopped(this)
        super.onDestroy()
    }
}
