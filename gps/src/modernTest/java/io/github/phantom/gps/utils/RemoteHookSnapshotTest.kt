package io.github.phantom.gps.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteHookSnapshotTest {
    @Test
    fun snapshotPreservesDoubleCoordinatesRequestAndAccessExpiry() {
        val lat = 13.736123456789
        val lon = 100.523123456789
        val issued = 1000L
        val expiry = issued + AccessPolicy.LEASE_MS
        val values = mapOf(
            "start" to true, "latitude_bits" to lat.toBits(), "longitude_bits" to lon.toBits(),
            "direct_request_id" to "pickup-1", "tester_access_allowed" to true,
            "tester_access_issued" to issued, "tester_access_until" to expiry, "tester_access_boot" to 7,
            "membership_lease" to "unused credential"
        )

        val decoded = RemoteHookSnapshot.decode(RemoteHookSnapshot.encode(values))
        val location = HookLocationSnapshot.from(decoded)
        assertEquals(lat, location.latitude, 0.0)
        assertEquals(lon, location.longitude, 0.0)
        assertEquals("pickup-1", location.requestId)
        assertTrue(location.started)
        assertTrue(AccessPolicy.fresh(decoded, expiry - 1, 7))
        assertFalse(AccessPolicy.fresh(decoded, expiry, 7))
        assertFalse(AccessPolicy.fresh(decoded, expiry - 1, 8))
        assertFalse(decoded.containsKey("membership_lease"))

        val revoked = values + ("tester_access_allowed" to false)
        assertFalse(AccessPolicy.fresh(RemoteHookSnapshot.decode(RemoteHookSnapshot.encode(revoked)), expiry - 1, 7))
    }
}
