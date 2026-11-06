package io.github.phantom.gps.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.PrivateKey
import java.util.Base64
import java.security.MessageDigest
import java.security.spec.ECGenParameterSpec
import java.util.Locale

object DeviceIdProvider {
    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "phantom_gps_session_device_key_v1"

    @Volatile private var cachedId: String? = null
    @Synchronized fun getDeviceId(): String {
        cachedId?.let { return it }
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        val publicKeyBytes = if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.getCertificate(KEY_ALIAS).publicKey.encoded
        } else {
            val generator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC,
                KEYSTORE
            )
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setUserAuthenticationRequired(false)
                .build()
            generator.initialize(spec)
            generator.generateKeyPair().public.encoded
        }

        return sha256Hex(publicKeyBytes).also { cachedId = it }
    }

    @Synchronized fun publicKeyBase64():String {
        getDeviceId()
        val store=KeyStore.getInstance(KEYSTORE).apply { load(null) }
        return Base64.getEncoder().encodeToString(store.getCertificate(KEY_ALIAS).publicKey.encoded)
    }

    @Synchronized fun signChallenge(message:String):String {
        getDeviceId()
        val store=KeyStore.getInstance(KEYSTORE).apply { load(null) }
        val key=store.getKey(KEY_ALIAS,null) as PrivateKey
        val signer=Signature.getInstance("SHA256withECDSA").apply { initSign(key);update(message.toByteArray(Charsets.UTF_8)) }
        return Base64.getEncoder().encodeToString(signer.sign())
    }

    private fun sha256Hex(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(data)
        val builder = StringBuilder(digest.size * 2)
        digest.forEach { b ->
            builder.append(String.format(Locale.US, "%02x", b))
        }
        return builder.toString()
    }
}
