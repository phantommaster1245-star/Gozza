package io.github.phantom.gps.utils

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import io.github.phantom.gps.R

/** One location card; no membership or clipboard-service status in this slot. */
object LocationNotification {
    private data class Candidate(val label:String?,val lat:Double,val lon:Double)
    @Volatile private var candidate:Candidate?=null
    fun build(c:Context,label:String?=null,alert:Boolean=false):Notification {
        val active=PrefManager.isStarted
        val policy=LocationNoticePolicy.state(active,candidate!=null,PrefManager.isClipboardAutoStart)
        val pending=candidate.takeIf { policy.start }
        return NotificationsChannel().buildNotification(c,NotificationsChannel.CHANNEL_ID_LOCATION) { b->
            b.setSmallIcon(if(active)R.drawable.ic_stop else R.drawable.ic_play)
                .setContentTitle(if(pending!=null)"Gozza · พบพิกัด" else "Gozza · Active")
                .setContentText(pending?.label ?: label ?: String.format(java.util.Locale.US,"%.6f, %.6f",PrefManager.getLat,PrefManager.getLng))
                .setContentIntent(PendingIntent.getActivity(c,142,Intent(c,io.github.phantom.gps.ui.MapActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
                .setCategory(Notification.CATEGORY_EVENT).setOngoing(policy.ongoing).setAutoCancel(!policy.ongoing)
                .setOnlyAlertOnce(!alert).setSilent(!alert).setPriority(NotificationCompat.PRIORITY_DEFAULT)
            if(alert)b.setDefaults(NotificationCompat.DEFAULT_SOUND)
            if(pending!=null)b.addAction(R.drawable.ic_play,"START",NotificationIntents.startLocationPendingIntent(c,pending.lat,pending.lon,pending.label))
            if(policy.stop)b.addAction(R.drawable.ic_stop,"STOP",NotificationIntents.stopLocationPendingIntent(c))
        }
    }
    @Synchronized fun show(c:Context,label:String?=null,lat:Double?=null,lon:Double?=null,alert:Boolean=true) {
        if(lat!=null&&lon!=null){
            if(PrefManager.isClipboardAutoStart)return
            candidate=Candidate(label,lat,lon)
        }else candidate=null
        if(!PrefManager.isStarted&&candidate==null){cancel(c);return}
        if(NotificationsChannel.canPostNotifications(c))c.getSystemService(android.app.NotificationManager::class.java)
            .notify(NotificationsChannel.NOTIFICATION_ID_LOCATION,build(c,label,alert))
    }
    @Synchronized fun updateLabel(c:Context,label:String?) {
        if(PrefManager.isStarted&&NotificationsChannel.canPostNotifications(c))c.getSystemService(android.app.NotificationManager::class.java)
            .notify(NotificationsChannel.NOTIFICATION_ID_LOCATION,build(c,label,alert=false))
    }
    @Synchronized fun stopped(c:Context) {
        if(candidate!=null&&!PrefManager.isClipboardAutoStart) {
            if(NotificationsChannel.canPostNotifications(c))c.getSystemService(android.app.NotificationManager::class.java)
                .notify(NotificationsChannel.NOTIFICATION_ID_LOCATION,build(c,alert=false))
        }else cancel(c)
    }
    private fun cancel(c:Context)=NotificationsChannel().cancelNotification(c,NotificationsChannel.NOTIFICATION_ID_LOCATION)
}
