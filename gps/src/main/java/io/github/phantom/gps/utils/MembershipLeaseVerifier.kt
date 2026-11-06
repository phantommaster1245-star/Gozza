package io.github.phantom.gps.utils

import androidx.annotation.Keep
import com.google.gson.Gson
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

@Keep data class MembershipLeasePayload(val v: Int = 0, val kid: String = "", val uid: String = "", val session: String = "",
    val device: String = "", val issuedAt: Long = 0, val expiresAt: Long = 0, val membershipExpiresAt: Long = 0,
    val boot: Int = -1, val anchor: Long = -1, val audience: String = "")

/** A signed capability, not an auth token. No network and no secrets in the Hook. */
class MembershipLeaseVerifier(publicKey: String) {
    private val key = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(publicKey)))
    private var lastToken = ""
    private var lastPayload: MembershipLeasePayload? = null
    @Synchronized fun inspect(token: String): MembershipLeasePayload? {
        if (token == lastToken) return lastPayload
        val parsed = runCatching {
            require(token.length in 100..8192)
            val parts = token.split('.'); require(parts.size == 2)
            val signature = Signature.getInstance("SHA256withRSA").apply { initVerify(key); update(parts[0].toByteArray(Charsets.US_ASCII)) }
            require(signature.verify(Base64.getUrlDecoder().decode(parts[1])))
            Gson().fromJson(String(Base64.getUrlDecoder().decode(parts[0]), Charsets.UTF_8), MembershipLeasePayload::class.java).also {
                require(it.v == 1 && it.kid == "membership-v1" && it.audience == "com.gozza.lalamove")
                require(it.uid.isNotBlank() && it.session.isNotBlank() && it.device.matches(Regex("[a-f0-9]{64}")))
                require(it.anchor >= 0 && it.boot >= 0 && it.expiresAt > it.issuedAt && it.expiresAt - it.issuedAt <= 600000)
                require(it.expiresAt <= it.membershipExpiresAt)
            }
        }.getOrNull()
        lastToken = token; lastPayload = parsed; return parsed
    }
    fun isValid(token: String, wallMs: Long, elapsedMs: Long, bootCount: Int, deviceId: String? = null): Boolean {
        val p = inspect(token) ?: return false
        return p.boot == bootCount && (deviceId == null || p.device == deviceId) && wallMs >= p.issuedAt - 120000 && wallMs < p.expiresAt &&
            elapsedMs >= p.anchor && elapsedMs - p.anchor < p.expiresAt - p.issuedAt
    }
}
