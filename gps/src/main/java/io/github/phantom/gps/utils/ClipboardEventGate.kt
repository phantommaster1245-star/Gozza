package io.github.phantom.gps.utils

/** Suppresses duplicate deliveries, never sleeps and permits an intentional repeat shortly after. */
class ClipboardEventGate(private val windowMs: Long = 300) {
    private var lastKey: String? = null
    private var lastAt = Long.MIN_VALUE
    @Synchronized fun accept(key: String, now: Long): Boolean {
        if (key == lastKey && now >= lastAt && now - lastAt <= windowMs) return false
        lastKey = key
        lastAt = now
        return true
    }
}
