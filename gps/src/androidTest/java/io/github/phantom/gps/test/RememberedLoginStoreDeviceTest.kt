package io.github.phantom.gps.test

import android.content.Context
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.phantom.gps.utils.RememberedLoginStore
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore

@RunWith(AndroidJUnit4::class)
class RememberedLoginStoreDeviceTest {
 private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
 private val namespace="phantom_remembered_login_fixture"
 private val username="remember_fixture_user"
 private val password="FixtureOnlyPassword123!"
 private val prefs get()=context.getSharedPreferences(namespace,Context.MODE_PRIVATE)
 private fun cleanup(){prefs.edit().clear().commit();KeyStore.getInstance("AndroidKeyStore").apply{load(null);deleteEntry("$namespace.key.v1")}}
 @Test fun encryptedPersistenceOptOutAndTamper() {
  cleanup()
  try {
   val store=RememberedLoginStore(context,namespace);store.save(username,password,true)
   assertEquals(username,RememberedLoginStore(context,namespace).read()!!.username)
   assertEquals(password,RememberedLoginStore(context,namespace).read()!!.password)
   val saved=prefs.getString("encrypted",null)!!
   assertFalse(prefs.all.values.any{it.toString().contains(username)||it.toString().contains(password)})
   assertFalse(String(Base64.decode(saved,Base64.NO_WRAP)).contains(password))
   store.save(username,password,true);assertNotEquals(saved,prefs.getString("encrypted",null))
   val corrupted=Base64.decode(prefs.getString("encrypted",null),Base64.NO_WRAP);corrupted[corrupted.lastIndex]=(corrupted.last().toInt() xor 1).toByte()
   prefs.edit().putString("encrypted",Base64.encodeToString(corrupted,Base64.NO_WRAP)).commit()
   assertNull(store.read());assertFalse(prefs.contains("encrypted"))
   store.save(username,password,true);store.setEnabled(false)
   assertFalse(store.isEnabled());assertNull(store.read());assertFalse(prefs.contains("encrypted"))
  }finally{cleanup()}
 }
 @Test fun missingKeyDoesNotFallBackToPlaintextAndCanSaveAgain() {
  cleanup()
  try {
   val store=RememberedLoginStore(context,namespace);store.save(username,password,true)
   KeyStore.getInstance("AndroidKeyStore").apply{load(null);deleteEntry("$namespace.key.v1")}
   assertNull(store.read());assertFalse(prefs.contains("encrypted"))
   store.save(username,password,true);assertEquals(password,store.read()!!.password)
   store.forget();assertNull(store.read());assertFalse(store.isEnabled())
  }finally{cleanup()}
 }
 @Test fun persistenceAcrossProcessRestart() {
  when(InstrumentationRegistry.getArguments().getString("rememberPhase")){
   "write"->{cleanup();RememberedLoginStore(context,namespace).save(username,password,true);assertTrue(prefs.contains("encrypted"))}
   "read"->try{val saved=RememberedLoginStore(context,namespace).read();assertNotNull(saved);assertEquals(username,saved!!.username);assertEquals(password,saved.password)}finally{cleanup()}
   else->org.junit.Assume.assumeTrue("Host runs this test in separate write/read processes",false)
  }
 }
}
