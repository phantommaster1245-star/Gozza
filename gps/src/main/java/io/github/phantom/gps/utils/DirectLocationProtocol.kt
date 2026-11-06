package io.github.phantom.gps.utils

object DirectLocationProtocol {
    const val VERSION = 1
    const val PACKAGE = "com.gozza.lalamove"
    const val CALLER_PACKAGE = "com.gozza.lalamove"
    const val ACTION = "com.gozza.lalamove.action.SET_LOCATION_DIRECT"
    const val COMPONENT = "io.github.phantom.gps.utils.DirectLocationReceiver"
    const val APPLIED = 100
    const val HOOK_LOADED = 101
    const val INVALID_REQUEST = 400
    const val UNAUTHORIZED = 401
    const val LICENSE_REQUIRED = 402
    const val LICENSE_LOCKED = 403
    const val SUPERSEDED = 409
    const val HOOK_NOT_READY = 412
    const val APPLY_FAILED = 500
    const val HOOK_TIMEOUT = 504
}

data class DirectLocationRequest(
    val id: String, val latitude: Double, val longitude: Double,
    val label: String = "", val targetPackage: String = ""
) {
    fun isValid(): Boolean = id.matches(Regex("[A-Za-z0-9_.-]{1,96}")) &&
        latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
        label.length <= 160 && (targetPackage.isEmpty() ||
        (targetPackage.length <= 200 && (targetPackage == "android" || targetPackage.matches(Regex("[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)+"))) && targetPackage != DirectLocationProtocol.PACKAGE))
}

/** A duplicate ID cannot silently refer to different coordinates. Keeps only recent successful writes. */
class DirectLocationHistory {
    private val applied = LinkedHashMap<String, DirectLocationRequest>()
    @Synchronized fun find(id: String): DirectLocationRequest? = applied[id]
    @Synchronized fun record(request: DirectLocationRequest) {
        applied[request.id] = request
        while (applied.size > 64) applied.remove(applied.keys.first())
    }
}
