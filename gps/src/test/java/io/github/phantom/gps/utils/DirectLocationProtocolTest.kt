package io.github.phantom.gps.utils

import org.junit.Assert.*
import org.junit.Test

class DirectLocationProtocolTest {
    private val request = DirectLocationRequest("job-123", 13.756300123456, 100.501800123456, "Pickup", "com.example.driver")
    @Test fun acceptsNativeDoubleCoordinatesWithoutRounding() {
        assertTrue(request.isValid())
        assertEquals(request.latitude, Double.fromBits(request.latitude.toBits()), 0.0)
        assertEquals(request.longitude, Double.fromBits(request.longitude.toBits()), 0.0)
        assertNotEquals(request.latitude, request.latitude.toFloat().toDouble())
        assertTrue(request.copy(latitude = -90.0, longitude = 180.0).isValid())
    }
    @Test fun rejectsInvalidAndNonFiniteNumbers() {
        for (value in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 90.0001, -90.0001)) assertFalse(request.copy(latitude = value).isValid())
        for (value in listOf(Double.NaN, Double.POSITIVE_INFINITY, 180.0001, -180.0001)) assertFalse(request.copy(longitude = value).isValid())
    }
    @Test fun checksCorrelationAndTargetMetadata() {
        assertFalse(request.copy(id = "").isValid())
        assertFalse(request.copy(id = "x".repeat(97)).isValid())
        assertFalse(request.copy(targetPackage = "https://example.com").isValid())
        assertFalse(request.copy(targetPackage = DirectLocationProtocol.PACKAGE).isValid())
        assertFalse(request.copy(label = "x".repeat(161)).isValid())
        assertTrue(request.copy(targetPackage = "").isValid())
        assertTrue(request.copy(targetPackage = "android").isValid())
    }
    @Test fun historyRetainsOriginalPayloadForDuplicateValidation() {
        val history = DirectLocationHistory()
        history.record(request)
        assertEquals(request, history.find(request.id))
        assertNotEquals(request.copy(latitude = 14.0), history.find(request.id))
        for (i in 0..64) history.record(request.copy(id = "job-$i"))
        assertNull(history.find(request.id))
        assertNotNull(history.find("job-64"))
    }
}
