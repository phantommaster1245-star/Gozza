package io.github.phantom.gps.utils

import java.util.Locale

/** Trust only the current signer; historical certificates are not current APK signers. */
internal object SigningPolicy {
    private fun normalized(value: String) = value.replace(":", "").replace(Regex("\\s+"), "").uppercase(Locale.US)
    fun matches(expected: String, currentSigners: Set<String>): Boolean {
        val pin = normalized(expected)
        if (!pin.matches(Regex("[0-9A-F]{64}")) || currentSigners.size != 1) return false
        return normalized(currentSigners.single()) == pin
    }
}
