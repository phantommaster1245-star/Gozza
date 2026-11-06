package io.github.phantom.gps.utils

import org.junit.Assert.*
import org.junit.Test

class SigningPolicyTest {
    private val pin = "AB".repeat(32)
    @Test fun acceptsOnlyPinnedCurrentSigner() {
        assertTrue(SigningPolicy.matches(pin, setOf(pin)))
        assertTrue(SigningPolicy.matches(pin.lowercase(), setOf(pin.chunked(2).joinToString(":"))))
        assertFalse(SigningPolicy.matches(pin, setOf("CD".repeat(32))))
    }
    @Test fun rejectsUnknownMissingAndMalformedPins() {
        assertFalse(SigningPolicy.matches("", setOf(pin)))
        assertFalse(SigningPolicy.matches("AB", setOf(pin)))
        assertFalse(SigningPolicy.matches("ZZ".repeat(32), setOf(pin)))
        assertFalse(SigningPolicy.matches(pin, emptySet()))
    }
    @Test fun rejectsAdditionalSignerEvenWhenPinPresent() {
        assertFalse(SigningPolicy.matches(pin, setOf(pin, "CD".repeat(32))))
    }
}
