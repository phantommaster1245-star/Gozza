package io.github.phantom.gps.update

import android.content.Context
import android.os.Parcelable
import com.google.gson.Gson
import io.github.phantom.gps.BuildConfig
import io.github.phantom.gps.utils.PrefManager
import kotlinx.parcelize.Parcelize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

class UpdateChecker @Inject constructor() {
    fun getLatestRelease() = flow {
        while (true) {
            if (PrefManager.isUpdateDisabled) emit(null)
            else runCatching { fetchLatest() }.onSuccess { emit(it) }
            delay(15 * 60 * 1000L)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun fetchLatest(): Update? = withContext(Dispatchers.IO) {
        val connection = URL(BuildConfig.PUBLIC_SITE_BASE_URL + "downloads/version.json").openConnection() as HttpURLConnection
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.instanceFollowRedirects = false
        connection.useCaches = false
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Cache-Control", "no-cache")
        try {
            check(connection.responseCode == 200) { "Release metadata unavailable" }
            val bytes = connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    check(output.size() + count <= 32 * 1024) { "Release metadata too large" }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            val release = Gson().fromJson(bytes.toString(Charsets.UTF_8), PublicRelease::class.java)
            require(release.isValid()) { "Invalid release metadata" }
            if (!release.isNewerThan(BuildConfig.VERSION_CODE, BuildConfig.TAG_NAME)) return@withContext null
            Update("Gozza Lalamove ${release.version}", release.changes.joinToString("\n") { "• $it" },
                release.publishedAt, BuildConfig.PUBLIC_SITE_BASE_URL + "downloads/" + release.filename,
                release.filename, BuildConfig.PUBLIC_SITE_BASE_URL + "download.html", release.version)
        } finally { connection.disconnect() }
    }

    fun clearCachedDownloads(context: Context) {
        context.externalCacheDir?.let { File(it, "updates").deleteRecursively() }
    }

    @Parcelize
    data class Update(val name: String, val changelog: String, val timestamp: String,
        val assetUrl: String, val assetName: String, val releaseUrl: String, val version: String) : Parcelable
}
