package io.github.phantom.gps.utils

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Private, device-bound credentials. Never use Hook-readable preferences for passwords. */
class RememberedLoginStore(context:Context,namespace:String="phantom_remembered_login") {
    init { require(namespace.matches(Regex("phantom_[a-zA-Z0-9_]{1,70}"))) }
    private val prefs=context.applicationContext.getSharedPreferences(namespace,Context.MODE_PRIVATE)
    private val alias="$namespace.key.v1"
    private val aad="${context.packageName}:$namespace:v1".toByteArray(Charsets.UTF_8)
    // Avoid a generated data-class toString that might accidentally print the password.
    class Credentials(val username:String,val password:String)
    fun isEnabled():Boolean=prefs.getBoolean("enabled",true)
    @Synchronized fun setEnabled(enabled:Boolean) {
        val edit=prefs.edit().putBoolean("enabled",enabled)
        if(!enabled)edit.remove("encrypted")
        check(edit.commit()) { "CREDENTIAL_STORAGE_FAILED" }
    }
    @Synchronized fun save(username:String,password:String,remember:Boolean) {
        if(!remember){setEnabled(false);return}
        require(username.isNotBlank()&&username.length<=128&&password.isNotEmpty()&&password.length<=256)
        val plaintext=JSONObject().put("username",username.trim()).put("password",password).toString().toByteArray(Charsets.UTF_8)
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE,key(create=true));cipher.updateAAD(aad)
        val ciphertext=try{cipher.doFinal(plaintext)}finally{plaintext.fill(0)}
        check(cipher.iv.size==12)
        val payload=byteArrayOf(1)+cipher.iv+ciphertext
        check(prefs.edit().putBoolean("enabled",true).putString("encrypted",Base64.encodeToString(payload,Base64.NO_WRAP)).commit()) { "CREDENTIAL_STORAGE_FAILED" }
    }
    @Synchronized fun read():Credentials? {
        if(!isEnabled())return null
        val encoded=prefs.getString("encrypted",null)?:return null
        return try {
            require(encoded.length<=8192)
            val payload=Base64.decode(encoded,Base64.NO_WRAP)
            require(payload.size>=30&&payload[0]==1.toByte())
            val cipher=Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE,key(create=false),GCMParameterSpec(128,payload.copyOfRange(1,13)))
            cipher.updateAAD(aad)
            val plaintext=cipher.doFinal(payload.copyOfRange(13,payload.size))
            val json=try{JSONObject(String(plaintext,Charsets.UTF_8))}finally{plaintext.fill(0)}
            val username=json.getString("username");val password=json.getString("password")
            require(username.isNotBlank()&&username.length<=128&&password.isNotEmpty()&&password.length<=256)
            Credentials(username,password)
        }catch(_:Exception){prefs.edit().remove("encrypted").commit();null}
    }
    fun forget()=setEnabled(false)
    companion object {private val keyLock=Any()}
    private fun key(create:Boolean):SecretKey = synchronized(keyLock) {
        val store=KeyStore.getInstance("AndroidKeyStore").apply{load(null)}
        (store.getKey(alias,null) as? SecretKey)?.let{return it}
        check(create) { "CREDENTIAL_KEY_UNAVAILABLE" }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).setRandomizedEncryptionRequired(true).setUserAuthenticationRequired(false).build())
            generateKey()
        }
    }
}
