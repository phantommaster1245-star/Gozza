package io.github.phantom.gps.utils

import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner

/** Background capture belongs to the LSPosed system hook. This is a foreground-screen fallback, not a foreground service. */
class ClipboardMonitorService:Service() {
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int { stopSelf();return START_NOT_STICKY }
    override fun onBind(intent:Intent?):IBinder?=null
    companion object {
        private var clipboard:ClipboardManager?=null
        private var listener:ClipboardManager.OnPrimaryClipChangedListener?=null
        @Synchronized fun ensureRunning(context:Context) {
            context.stopService(Intent(context,ClipboardMonitorService::class.java))
            if(!PrefManager.isClipboardMonitorEnabled || !ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)){stopForegroundCapture();return}
            if(listener!=null)return
            val app=context.applicationContext
            val manager=app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val capture=ClipboardManager.OnPrimaryClipChangedListener {
                if(PrefManager.isClipboardMonitorEnabled)runCatching {
                    val text=manager.primaryClip?.takeIf { it.itemCount>0 }?.getItemAt(0)?.coerceToText(app)?.toString()
                    if(!text.isNullOrBlank())ClipboardLocationReceiver().submit(app,text,android.os.SystemClock.elapsedRealtime())
                }
            }
            clipboard=manager;listener=capture
            manager.addPrimaryClipChangedListener(capture)
            capture.onPrimaryClipChanged()
        }
        @Synchronized fun stopForegroundCapture() {
            listener?.let { l->runCatching { clipboard?.removePrimaryClipChangedListener(l) } }
            clipboard=null;listener=null
        }
    }
}
