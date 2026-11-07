package io.github.phantom.gps.utils

internal object HookPreferenceChannel {
    val ready = false
    fun initialize() = Unit
    fun scopeStatus(): String = "UNSUPPORTED"
    fun publish(values: Map<String, *>): Boolean = false
}
