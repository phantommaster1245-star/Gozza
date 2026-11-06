package io.github.phantom.gps.utils

import org.junit.Assert.*
import org.junit.Test

class HookLocationSnapshotTest {
    @Test fun preservesExactDoubleCoordinatesAndCorrelatedRequest() {
        val lat = 13.756300123456; val lon = 100.501800123456
        val s = HookLocationSnapshot.from(mapOf("latitude_bits" to lat.toBits(), "longitude_bits" to lon.toBits(), "direct_request_id" to "job-1", "start" to true))
        assertEquals(lat, s.latitude, 0.0); assertEquals(lon, s.longitude, 0.0)
        assertEquals("job-1", s.requestId); assertTrue(s.started)
    }
    @Test fun legacyFloatSettingsStillWork() {
        val s = HookLocationSnapshot.from(mapOf("latitude" to 13.75F, "longitude" to 100.5F))
        assertEquals(13.75, s.latitude, 0.0); assertEquals(100.5, s.longitude, 0.0)
    }
    @Test fun directRequestsStayExactWhileOtherChannelsKeepRandomSetting() {
        assertFalse(HookLocationSnapshot.from(mapOf("random_position" to true, "direct_request_id" to "job-1")).allowRandom)
        assertTrue(HookLocationSnapshot.from(mapOf("random_position" to true)).allowRandom)
    }
    @Test fun laterPreferenceChangesDoNotMutateLoadedPair() {
        val data = mutableMapOf<String, Any>("latitude_bits" to 13.0.toBits(), "longitude_bits" to 100.0.toBits(), "direct_request_id" to "A")
        val snapshot = HookLocationSnapshot.from(data)
        data["latitude_bits"] = 14.0.toBits(); data["direct_request_id"] = "B"
        assertEquals(13.0, snapshot.latitude, 0.0); assertEquals("A", snapshot.requestId)
    }
}
