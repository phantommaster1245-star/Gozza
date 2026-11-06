package io.github.phantom.gps.utils

/** Monotonic, non-resurrectable session lease. Callers must authenticate before renew/revoke. */
internal class RelayLease(private val now: () -> Long, private val duration: Long = 15000L) {
    private var renewedAt = now()
    private var revoked = false
    @Synchronized fun valid(): Boolean = !revoked && (now() - renewedAt).let { it >= 0 && it < duration }
    @Synchronized fun renew(): Boolean {
        if (!valid()) return false
        renewedAt = now()
        return true
    }
    @Synchronized fun revoke() { revoked = true }
}
