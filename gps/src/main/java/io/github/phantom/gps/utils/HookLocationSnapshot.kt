package io.github.phantom.gps.utils

/** One immutable preference snapshot: a request ID can never be paired with half-old coordinates. */
data class HookLocationSnapshot(
    val latitude: Double, val longitude: Double, val started: Boolean, val requestId: String,
    val allowRandom: Boolean, val radiusMeters: Double, val accuracy: Float,
    val traceId: Long, val capturedAtMs: Long
) {
    companion object {
        fun from(values: Map<String, *>): HookLocationSnapshot {
            val id = values["direct_request_id"] as? String ?: ""
            return HookLocationSnapshot(
                (values["latitude_bits"] as? Long)?.let { Double.fromBits(it) } ?: (values["latitude"] as? Float ?: 45F).toDouble(),
                (values["longitude_bits"] as? Long)?.let { Double.fromBits(it) } ?: (values["longitude"] as? Float ?: 0F).toDouble(),
                values["start"] as? Boolean ?: false, id,
                (values["random_position"] as? Boolean == true) && id.isEmpty(),
                (values["random_radius_meters"] as? String)?.toDoubleOrNull()?.coerceAtLeast(1.0) ?: 10.0,
                (values["accuracy_level"] as? String)?.toFloatOrNull() ?: 10F,
                values["clipboard_trace_id"] as? Long ?: 0,
                values["clipboard_captured_at_ms"] as? Long ?: 0
            )
        }
    }
}
