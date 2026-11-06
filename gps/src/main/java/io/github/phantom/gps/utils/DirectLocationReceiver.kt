package io.github.phantom.gps.utils

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.ResultReceiver

/** Explicit app-to-app numeric path. No clipboard, shell, geocoder, or remote license request. */
class DirectLocationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != DirectLocationProtocol.ACTION) return
        val reply = runCatching { intent.getParcelableExtra<ResultReceiver>("reply") }.getOrNull() ?: return
        val id = intent.getStringExtra("request_id").orEmpty()
        val result = reply
        val identity = runCatching { intent.getParcelableExtra<PendingIntent>("caller_identity") }.getOrNull()
        val authorized = identity != null && identity.creatorPackage == DirectLocationProtocol.CALLER_PACKAGE &&
            runCatching { context.packageManager.getPackageUid(DirectLocationProtocol.CALLER_PACKAGE, 0) == identity.creatorUid }.getOrDefault(false)
        if (!authorized) { result.send(DirectLocationProtocol.UNAUTHORIZED, Bundle().apply { putString("request_id", id); putString("status", "UNAUTHORIZED") }); return }
        val pending = goAsync()
        DirectLocationEngine.submit(context, intent, reply, finished = { pending.finish() })
    }
}
