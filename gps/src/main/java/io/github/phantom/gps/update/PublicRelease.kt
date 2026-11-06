package io.github.phantom.gps.update

import androidx.annotation.Keep

/** Shared website metadata contract. Download paths are derived from a trusted origin. */
@Keep // Gson uses these field names in the minified release APK.
data class PublicRelease(
    val packageName: String = "", val version: String = "", val versionCode: Int = 0,
    val filename: String = "", val sha256: String = "", val bytes: Long = 0,
    val publishedAt: String = "", val changes: List<String> = emptyList()
) {
    fun isValid(): Boolean = runCatching {
        packageName == "io.github.phantom.gps" && version.startsWith("v") && parts(version) != null && versionCode > 0 &&
            filename == "PHANToM-GPS-$version.apk" && sha256.matches(Regex("[a-fA-F0-9]{64}")) &&
            bytes in 1..(100L * 1024 * 1024) && java.time.Instant.parse(publishedAt) != null &&
            changes.size <= 20 && changes.all { it.length <= 1000 }
    }.getOrDefault(false)

    fun isNewerThan(installedCode: Int, installedVersion: String): Boolean {
        val remote = parts(version) ?: return false
        val local = parts(installedVersion) ?: return false
        if (versionCode <= installedCode) return false
        for (i in 0..2) if (remote[i] != local[i]) return remote[i] > local[i]
        return true
    }

    companion object {
        private fun parts(value: String): List<Int>? {
            if (!Regex("v?[0-9]{1,6}\\.[0-9]{1,6}\\.[0-9]{1,6}").matches(value)) return null
            return value.removePrefix("v").split('.').map { it.toInt() }
        }
    }
}
