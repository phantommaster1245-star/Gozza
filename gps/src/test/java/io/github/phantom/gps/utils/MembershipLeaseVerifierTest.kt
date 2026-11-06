package io.github.phantom.gps.utils

import com.google.gson.Gson
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64
import org.junit.Assert.*
import org.junit.Test

class MembershipLeaseVerifierTest {
    private val keys=KeyPairGenerator.getInstance("RSA").apply{initialize(2048)}.generateKeyPair()
    private val verifier=MembershipLeaseVerifier(Base64.getEncoder().encodeToString(keys.public.encoded))
    private val payload=MembershipLeasePayload(1,"membership-v1","user","session","ab".repeat(32),1000000,1600000,10000000,2,10000,"com.gozza.lalamove")
    private fun token(p:MembershipLeasePayload=payload):String {
        val body=Base64.getUrlEncoder().withoutPadding().encodeToString(Gson().toJson(p).toByteArray())
        val sig=Signature.getInstance("SHA256withRSA").apply{initSign(keys.private);update(body.toByteArray(Charsets.US_ASCII))}.sign()
        return body+"."+Base64.getUrlEncoder().withoutPadding().encodeToString(sig)
    }
    @Test fun acceptsSignedBoundLeaseAndExpiresAtBoundary() {
        val t=token();assertTrue(verifier.isValid(t,1000000,10000,2,payload.device))
        assertFalse(verifier.isValid(t,1600000,610000,2,payload.device))
        assertFalse(verifier.isValid(t,1000000,610000,2,payload.device))
    }
    @Test fun rejectsWrongDeviceRebootAndReversedMonotonicClock() {
        val t=token();assertFalse(verifier.isValid(t,1000000,10000,3,payload.device))
        assertFalse(verifier.isValid(t,1000000,10000,2,"cd".repeat(32)))
        assertFalse(verifier.isValid(t,1000000,9999,2,payload.device))
        assertFalse(verifier.isValid(t,800000,10000,2,payload.device))
    }
    @Test fun rejectsTamperedPayloadSignatureAndDifferentKey() {
        val t=token();val altered="e30."+t.substringAfter('.');assertNull(verifier.inspect(altered))
        assertNull(verifier.inspect(t.dropLast(5)+"abcde"))
        val other=KeyPairGenerator.getInstance("RSA").apply{initialize(2048)}.generateKeyPair()
        assertNull(MembershipLeaseVerifier(Base64.getEncoder().encodeToString(other.public.encoded)).inspect(t))
    }
    @Test fun rejectsSignedButUnsupportedOrUnboundedLease() {
        for(p in listOf(payload.copy(v=2),payload.copy(audience="other"),payload.copy(expiresAt=1600001),payload.copy(membershipExpiresAt=1200000),payload.copy(anchor=-1)))assertNull(verifier.inspect(token(p)))
    }
    @Test fun clockRollbackCannotExtendMonotonicWindowAndCacheDoesNotFreezeExpiry() {
        val t=token();assertTrue(verifier.isValid(t,1000000,10000,2))
        assertFalse(verifier.isValid(t,1000000,610000,2))
        assertFalse(verifier.isValid(t,1600000,20000,2))
    }
}
