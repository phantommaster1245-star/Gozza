package io.github.phantom.gps.utils

import android.os.SystemClock
import android.util.Log

/** Duration-only diagnostics: never includes clipboard text, coordinates, codes or labels. */
data class ClipboardTrace(val id: Long, val capturedAtMs: Long) {
    fun mark(stage: String) {
        Log.i(TAG, "trace=$id stage=$stage elapsed_ms=${(SystemClock.elapsedRealtime() - capturedAtMs).coerceAtLeast(0)}")
    }
    companion object {
        const val TAG = "PhantomClipboardPerf"
        const val EXTRA_CAPTURED_AT = "clipboard_captured_at_ms"
        const val EXTRA_TRACE_ID = "clipboard_trace_id"
        fun begin(capturedAtMs: Long): ClipboardTrace {
            val now = SystemClock.elapsedRealtime()
            return ClipboardTrace(SystemClock.elapsedRealtimeNanos(), capturedAtMs.coerceIn(0, now))
        }
    }
}
