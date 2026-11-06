package io.github.phantom.gps.utils

/** Separate clipboard input routes; URLs never fall through to the plain-text route. */
data class ClipboardFormats(
    val coordinates: Boolean = true,
    val mapCoordinates: Boolean = true,
    val plusCodes: Boolean = true,
    val placeText: Boolean = true,
    val mapPlaces: Boolean = true
) {
    val signature: String get() = listOf(coordinates, mapCoordinates, plusCodes, placeText, mapPlaces).joinToString("") { if (it) "1" else "0" }
    val anyEnabled: Boolean get() = coordinates || mapCoordinates || plusCodes || placeText || mapPlaces
    fun plainText(text: String): String = URL.replace(text, " ").trim()
    fun coordinateText(text: String): String = if (coordinates) plainText(text) else ""
    fun coordinatesFrom(text: String): Pair<Double, Double>? = if (coordinates) parsePair(plainText(text)) else null
    fun addressText(text: String): String? {
        if (!placeText) return null
        val plain = plainText(text)
        if (plain.isBlank() || COORDINATES.containsMatchIn(plain.replace(';', ',')) || OpenLocationCode.extractFromText(plain) != null) return null
        return plain
    }
    fun urls(text: String): List<String> = if (mapCoordinates || mapPlaces) URL.findAll(text).map { it.value }.distinct().toList() else emptyList()
    companion object {
        val KEYS = listOf("clipboard_format_coordinates", "clipboard_format_map_coordinates", "clipboard_format_plus_codes", "clipboard_format_place_text", "clipboard_format_map_places")
        private val URL = Regex("(?i)\\b(?:https?://|geo:)[^\\s]+")
        private val COORDINATES = Regex("([-+]?\\d{1,3}(?:[.]\\d+)?)\\s*[,;]\\s*([-+]?\\d{1,3}(?:[.]\\d+)?)")
        fun parsePair(text: String): Pair<Double, Double>? {
            val match = COORDINATES.find(text) ?: return null
            val lat = match.groupValues[1].toDoubleOrNull() ?: return null
            val lon = match.groupValues[2].toDoubleOrNull() ?: return null
            return if (lat in -90.0..90.0 && lon in -180.0..180.0) lat to lon else null
        }
    }
}
