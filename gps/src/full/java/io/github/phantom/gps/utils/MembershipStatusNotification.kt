package io.github.phantom.gps.utils

import android.app.*
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import io.github.phantom.gps.R

object MembershipStatusNotification {
    private const val ID=142
    private const val CHANNEL="geox.membership.status"
    fun show(c:Context,message:String) {
        val manager=c.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL,"สิทธิ์สมาชิก Gozza Lalamove",NotificationManager.IMPORTANCE_LOW))
        if(!NotificationsChannel.canPostNotifications(c))return
        val open=PendingIntent.getActivity(c,145,Intent(c,io.github.phantom.gps.ui.MapActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        manager.notify(ID,NotificationCompat.Builder(c,CHANNEL).setSmallIcon(R.drawable.ic_stop).setContentTitle("Gozza · สิทธิ์สมาชิก")
            .setContentText(message).setStyle(NotificationCompat.BigTextStyle().bigText(message)).setContentIntent(open)
            .setOngoing(false).setAutoCancel(true).setSilent(true).build())
    }
    fun clear(c:Context){c.getSystemService(NotificationManager::class.java).cancel(ID)}
}
