package io.github.phantom.gps.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import io.github.phantom.gps.BuildConfig
import io.github.phantom.gps.R

object UpdateNotifier {
    private const val ID = 410
    private const val CHANNEL = "phantom_updates"
    fun announce(context: Context, update: UpdateChecker.Update?) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (update == null) { manager.cancel(ID); return }
        if (!manager.areNotificationsEnabled()) return
        val prefs = context.getSharedPreferences("public_updates", Context.MODE_PRIVATE)
        if (prefs.getString("announced", "") == update.version) return
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "อัปเดต Gozza Lalamove", NotificationManager.IMPORTANCE_DEFAULT))
        if (manager.getNotificationChannel(CHANNEL)?.importance == NotificationManager.IMPORTANCE_NONE) return
        val intent = PendingIntent.getActivity(context, ID,
            Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PUBLIC_SITE_BASE_URL + "download.html")),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        runCatching {
            manager.notify(ID, NotificationCompat.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_update)
                .setContentTitle("Gozza Lalamove มีอัปเดตใหม่").setContentText("${update.version} · แตะเพื่อดาวน์โหลด")
                .setContentIntent(intent).setAutoCancel(true).addAction(R.drawable.ic_update, "ดาวน์โหลด", intent).build())
            prefs.edit().putString("announced", update.version).apply()
        }
    }
}
