package io.github.phantom.gps.xposed

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

internal object GnssSimulation {
    val satellites = listOf(3, 7, 11, 14, 19, 22, 26, 30)
    private val utcTime = DateTimeFormatter.ofPattern("HHmmss.SSS", Locale.US).withZone(ZoneOffset.UTC)
    private val utcDate = DateTimeFormatter.ofPattern("ddMMyy", Locale.US).withZone(ZoneOffset.UTC)

    private fun coordinate(value: Double, width: Int): String {
        val unitsPerDegree = 600000L
        val units = (abs(value) * unitsPerDegree).roundToLong()
        return String.format(Locale.US, "%0${width}d%07.4f", units / unitsPerDegree,
            (units % unitsPerDegree) / 10000.0)
    }

    fun sentence(original: String, timeMs: Long, lat: Double, lon: Double): String? {
        val header = original.substringBefore(',').removePrefix("$")
        if (header.length != 5 || header.any { it !in 'A'..'Z' }) return null
        val instant = Instant.ofEpochMilli(timeMs)
        val time = utcTime.format(instant)
        val date = utcDate.format(instant)
        val latitude = coordinate(lat, 2)
        val longitude = coordinate(lon, 3)
        val northSouth = if (lat < 0) "S" else "N"
        val eastWest = if (lon < 0) "W" else "E"
        val position = "$latitude,$northSouth,$longitude,$eastWest"
        val data = when (header.takeLast(3)) {
            "GGA" -> "$time,$position,1,08,1.0,0.0,M,0.0,M,,"
            "RMC" -> "$time,A,$position,0.0,0.0,$date,,,A"
            "GLL" -> "$position,$time,A,A"
            else -> return null
        }
        val body = "${header},${data}"
        val checksum = body.fold(0) { sum, character -> sum xor character.code }
        return String.format(Locale.US, "$%s*%02X\r\n", body, checksum)
    }
}
