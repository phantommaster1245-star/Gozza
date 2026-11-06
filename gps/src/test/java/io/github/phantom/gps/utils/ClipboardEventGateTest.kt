package io.github.phantom.gps.utils

import org.junit.Assert.*
import org.junit.Test

class ClipboardEventGateTest {
    @Test fun twoSourcesWithSameTextProduceOneAcceptedJob() {
        val gate = ClipboardEventGate()
        assertTrue(gate.accept("11000:13.7,100.5", 1000))
        assertFalse(gate.accept("11000:13.7,100.5", 1010))
        assertFalse(gate.accept("11000:13.7,100.5", 1299))
    }
    @Test fun aDifferentCoordinateNeverWaitsForDuplicateWindow() {
        val gate = ClipboardEventGate()
        assertTrue(gate.accept("13.7,100.5", 1000))
        assertTrue(gate.accept("13.8,100.6", 1001))
    }
    @Test fun sameTextCanBeRetriedAndFormatChangesAreAccepted() {
        val gate = ClipboardEventGate()
        assertTrue(gate.accept("11000:VGX5+R5C", 1000))
        assertTrue(gate.accept("11000:VGX5+R5C", 1301))
        assertTrue(gate.accept("11100:VGX5+R5C", 1302))
    }
    @Test fun clockResetDoesNotSuppressInput() {
        val gate = ClipboardEventGate()
        assertTrue(gate.accept("input", 1000))
        assertTrue(gate.accept("input", 10))
    }
}
