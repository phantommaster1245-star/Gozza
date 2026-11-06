package io.github.phantom.gps.utils

import android.content.Context
import android.content.Intent
import io.github.phantom.gps.BuildConfig

object LocationRefresh {
    const val TRACE_ID_PREF = "clipboard_trace_id"
    const val CAPTURED_AT_PREF = "clipboard_captured_at_ms"
    val DIAGNOSTIC_ACTION = BuildConfig.APPLICATION_ID + ".action.DIAGNOSE_HOOK"
    val CONFIRM_ACTION = BuildConfig.APPLICATION_ID + ".action.CONFIRM_LOCATION"
    val ACTION = BuildConfig.APPLICATION_ID + ".action.REFRESH_LOCATION"
    val SENDER_PERMISSION = BuildConfig.APPLICATION_ID + ".permission.REFRESH_LOCATION"

    fun confirm(context: Context, targetPackage: String, requestId: String, reply: android.os.ResultReceiver) {
        // The Binder callback is delivered only to the specified target package, not broadcast globally.
        // Marshal as the platform class, not an anonymous app subclass unavailable in the other process.
        val parcel = android.os.Parcel.obtain()
        val remoteReply = try {
            reply.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            android.os.ResultReceiver.CREATOR.createFromParcel(parcel)
        } finally { parcel.recycle() }
        runCatching { context.sendBroadcast(Intent(CONFIRM_ACTION).setPackage(targetPackage)
            .addFlags(Intent.FLAG_RECEIVER_FOREGROUND).putExtra("request_id", requestId).putExtra("reply", remoteReply)) }
    }

    fun diagnose(context:Context,targetPackage:String,queryId:String,reply:android.os.ResultReceiver) {
        val parcel=android.os.Parcel.obtain()
        val remote=try { reply.writeToParcel(parcel,0);parcel.setDataPosition(0);android.os.ResultReceiver.CREATOR.createFromParcel(parcel) } finally { parcel.recycle() }
        context.sendBroadcast(Intent(DIAGNOSTIC_ACTION).setPackage(targetPackage).addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            .putExtra("query_id",queryId).putExtra("reply",remote))
    }

    fun send(context: Context, trace: ClipboardTrace? = null) {
        // No coordinates are transmitted. Hook receivers require our signature permission
        // from the sender and read the committed module preferences themselves.
        runCatching {
            context.sendBroadcast(Intent(ACTION).addFlags(Intent.FLAG_RECEIVER_FOREGROUND))
            trace?.mark("refresh_sent")
        }.onFailure {
            // Existing preference polling remains the fallback on restricted ROMs.
            trace?.mark("refresh_unavailable")
        }
    }
}
