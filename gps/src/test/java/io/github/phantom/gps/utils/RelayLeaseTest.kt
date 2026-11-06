package io.github.phantom.gps.utils

import org.junit.Assert.*
import org.junit.Test

class RelayLeaseTest {
    @Test fun expiresAtBoundaryAndCannotBeRevived() {
        var time = 100L
        val lease = RelayLease({ time })
        time = 15099L; assertTrue(lease.valid())
        time = 15100L; assertFalse(lease.valid()); assertFalse(lease.renew())
    }
    @Test fun heartbeatExtendsOnlyLiveLease() {
        var time = 0L
        val lease = RelayLease({ time })
        time = 10000L; assertTrue(lease.renew())
        time = 20000L; assertTrue(lease.valid())
        time = 25000L; assertFalse(lease.valid())
    }
    @Test fun revokeIsPermanent() {
        val lease = RelayLease({ 0L })
        lease.revoke(); assertFalse(lease.valid()); assertFalse(lease.renew())
    }
    @Test fun clockRegressionFailsClosed() {
        var time = 100L
        val lease = RelayLease({ time })
        time = 99L; assertFalse(lease.valid()); assertFalse(lease.renew())
    }
}
