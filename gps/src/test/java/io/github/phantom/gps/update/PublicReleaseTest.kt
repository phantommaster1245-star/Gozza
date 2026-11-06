package io.github.phantom.gps.update

import org.junit.Assert.*
import org.junit.Test

class PublicReleaseTest {
    private val valid = PublicRelease("io.github.phantom.gps", "v0.0.10", 10,
        "PHANToM-GPS-v0.0.10.apk", "a".repeat(64), 4000000,
        "2026-11-03T12:00:00Z", listOf("Public update page"))

    @Test fun readsWebsiteJsonContract() {
        val json = """{"packageName":"io.github.phantom.gps","version":"v0.0.10","versionCode":10,"filename":"PHANToM-GPS-v0.0.10.apk","sha256":"${"a".repeat(64)}","bytes":4000000,"publishedAt":"2026-11-03T12:00:00Z","changes":["Update"]}"""
        val release = com.google.gson.Gson().fromJson(json, PublicRelease::class.java)
        assertTrue(release.isValid())
        assertTrue(release.isNewerThan(9, "0.0.9"))
    }

    @Test fun acceptsPublishedAppOnly() {
        assertTrue(valid.isValid())
        assertFalse(valid.copy(packageName = "io.github.phantom.gps.installer").isValid())
        assertFalse(valid.copy(filename = "installer-release.apk").isValid())
        assertFalse(valid.copy(filename = "../PHANToM-GPS-v0.0.10.apk").isValid())
    }
    @Test fun comparesNumericVersionsAndInstallCodes() {
        assertTrue(valid.isNewerThan(9, "0.0.9"))
        assertFalse(valid.isNewerThan(10, "0.0.10"))
        assertFalse(valid.isNewerThan(11, "0.0.11"))
        assertFalse(valid.copy(version = "v0.0.8", versionCode = 12).isNewerThan(10, "0.0.10"))
        assertTrue(valid.copy(versionCode = 11).isNewerThan(10, "0.0.10"))
        assertFalse(valid.copy(version = "v0.0.10-beta").isValid())
    }
    @Test fun rejectsCorruptOrUnboundedMetadata() {
        assertFalse(valid.copy(sha256 = "bad").isValid())
        assertFalse(valid.copy(bytes = 0).isValid())
        assertFalse(valid.copy(bytes = 101L * 1024 * 1024).isValid())
        assertFalse(valid.copy(publishedAt = "bad").isValid())
        assertFalse(valid.copy(changes = List(21) { "extra" }).isValid())
    }
}
