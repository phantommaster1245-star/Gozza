package io.github.phantom.gps.utils

import org.junit.Assert.*
import org.junit.Test

class ClipboardFormatsTest {
    private val numbersOnly = ClipboardFormats(true, false, false, false, false)
    @Test fun parsesEnabledCoordinatePairsAndChecksBounds() {
        assertEquals(13.7563 to 100.5018, numbersOnly.coordinatesFrom("จุดรับ 13.7563, 100.5018 หมายเหตุ"))
        assertEquals(13.7563 to 100.5018, numbersOnly.coordinatesFrom("13.7563; 100.5018"))
        assertNull(numbersOnly.coordinatesFrom("91, 100"))
        assertNull(numbersOnly.coordinatesFrom("13, 181"))
        assertNull(numbersOnly.coordinatesFrom("https://maps.google.com/?q=13.7563,100.5018"))
        assertNull(numbersOnly.copy(coordinates=false).coordinatesFrom("13.7563, 100.5018"))
    }
    @Test fun coordinatesOnlyPreservesEmbeddedPairsButExcludesLinks() {
        assertEquals("จุดรับ 13.7563, 100.5018", numbersOnly.coordinateText("จุดรับ 13.7563, 100.5018"))
        assertEquals("13.7563; 100.5018", numbersOnly.coordinateText("13.7563; 100.5018"))
        assertEquals("", numbersOnly.coordinateText("https://maps.google.com/?q=13.7563,100.5018"))
        assertEquals("", numbersOnly.coordinateText("geo:13.7563,100.5018"))
        assertTrue(numbersOnly.urls("https://maps.google.com/?q=13.7563,100.5018").isEmpty())
        assertNull(numbersOnly.addressText("เทศบาลนครปากเกร็ด นนทบุรี"))
        assertFalse(numbersOnly.plusCodes)
    }
    @Test fun disablingNumbersCannotFallThroughToGeocoding() {
        val addresses = ClipboardFormats(false, false, false, true, false)
        assertNull(addresses.addressText("พิกัด 13.7563, 100.5018"))
        assertNull(addresses.addressText("พิกัด 13.7563; 100.5018"))
        assertNull(addresses.addressText("VGX5+R5C เทศบาลนครปากเกร็ด นนทบุรี"))
        assertNull(addresses.addressText("https://maps.google.com/?q=นนทบุรี"))
        assertEquals("เทศบาลนครปากเกร็ด นนทบุรี", addresses.addressText("เทศบาลนครปากเกร็ด นนทบุรี"))
    }
    @Test fun mixedTextDoesNotExposeUrlCoordinatesToNumberRoute() {
        assertEquals("จุดรับ 13.75, 100.50", numbersOnly.coordinateText("จุดรับ 13.75, 100.50 https://maps.google.com/@18.78,98.98"))
    }
    @Test fun defaultsKeepSupportAndEachSwitchChangesCacheNamespace() {
        val defaults = ClipboardFormats()
        assertTrue(defaults.anyEnabled)
        assertEquals(5, ClipboardFormats.KEYS.size)
        for (selection in listOf(defaults.copy(coordinates=false), defaults.copy(mapCoordinates=false), defaults.copy(plusCodes=false), defaults.copy(placeText=false), defaults.copy(mapPlaces=false))) assertNotEquals(defaults.signature, selection.signature)
        val none = ClipboardFormats(false,false,false,false,false)
        assertFalse(none.anyEnabled)
        assertEquals("", none.coordinateText("13.7563,100.5018"))
        assertNull(none.addressText("นนทบุรี"))
        assertTrue(none.urls("geo:13.7,100.5").isEmpty())
    }
}
