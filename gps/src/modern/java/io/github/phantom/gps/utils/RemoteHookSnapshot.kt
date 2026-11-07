package io.github.phantom.gps.utils

import org.json.JSONObject

/** A single remote preference value preserves the coordinates, lease and request ID as one update. */
internal object RemoteHookSnapshot {
    const val KEY = "snapshot_v1"
    private val flags = listOf("start", "system_hooked", "random_position", "clipboard_monitor_enabled", "tester_access_allowed")
    private val longs = listOf("latitude_bits", "longitude_bits", LocationRefresh.TRACE_ID_PREF,
        LocationRefresh.CAPTURED_AT_PREF, "tester_access_issued", "tester_access_until")
    private val ints = listOf("tester_access_boot")
    private val floats = listOf("latitude", "longitude")
    private val strings = listOf("direct_request_id", "random_radius_meters", "accuracy_level")
    private val keys = flags + longs + ints + floats + strings

    fun encode(values: Map<String, *>): String {
        val snapshot = JSONObject()
        for (key in keys) {
            val value = values[key] ?: continue
            if (value is Float && !value.isFinite()) continue
            snapshot.put(key, value)
        }
        return snapshot.toString()
    }

    fun decode(encoded: String): Map<String, Any> {
        val snapshot = JSONObject(encoded)
        return buildMap {
            for (key in flags) if (snapshot.has(key)) put(key, snapshot.getBoolean(key))
            for (key in longs) if (snapshot.has(key)) put(key, snapshot.getLong(key))
            for (key in ints) if (snapshot.has(key)) put(key, snapshot.getInt(key))
            for (key in floats) if (snapshot.has(key)) put(key, snapshot.getDouble(key).toFloat())
            for (key in strings) if (snapshot.has(key)) put(key, snapshot.getString(key))
        }
    }
}
