package io.github.phantom.gps.utils
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import com.google.gson.JsonParser
import org.json.JSONObject
import android.provider.Settings
import io.github.phantom.gps.BuildConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
/** Registers this installation and refreshes its lease using the Sheets backend. */
object TesterAccess {
 enum class State { Loading, Available, Unavailable }

 private const val REGISTRATION_URL="https://script.google.com/macros/s/AKfycbwUjllPl3HqguJUDR58LCalEqF41OarjK0S_3wSjxkp66o3Hr5nEBwmKBsweroFYJVh0g/exec"
 private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
 private val busy=AtomicBoolean(false)
 private var started=false
 private var current=java.lang.ref.WeakReference<Activity>(null)
 private val mutable = MutableStateFlow(State.Loading)
 val state = mutable.asStateFlow()
 @JvmStatic fun deviceId(c:Context):String {
  val id=Settings.Secure.getString(c.contentResolver,Settings.Secure.ANDROID_ID) ?: return ""
  return MessageDigest.getInstance("SHA-256").digest((BuildConfig.APPLICATION_ID+"|"+id).toByteArray()).joinToString("") { "%02x".format(it.toInt() and 255) }
 }
 @JvmStatic fun available(c:Context)=PrefManager.testerAccessValid(MembershipLease.bootCount(c))
 @JvmStatic fun start(c:Context) {
  if(started)return;started=true
  (c.applicationContext as android.app.Application).registerActivityLifecycleCallbacks(object:android.app.Application.ActivityLifecycleCallbacks {
   override fun onActivityResumed(a:Activity){current=java.lang.ref.WeakReference(a)}
   override fun onActivityPaused(a:Activity){if(current.get()===a)current.clear()}
   override fun onActivityCreated(a:Activity,b:android.os.Bundle?){}
   override fun onActivityStarted(a:Activity){}
   override fun onActivityStopped(a:Activity){}
   override fun onActivitySaveInstanceState(a:Activity,b:android.os.Bundle){}
   override fun onActivityDestroyed(a:Activity){}
  })
  scope.launch { while(isActive){check(c.applicationContext);delay(120000)} }
 }
 @JvmStatic fun retry(c:Context){scope.launch{check(c.applicationContext)}}
 @JvmStatic fun ensure(activity:Activity):Boolean {
  if(available(activity))return true
  if (mutable.value == State.Available) retry(activity)
  activity.startActivity(Intent(activity,io.github.phantom.gps.ui.TesterAccessActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP))
  return false
 }
 private suspend fun check(context: Context) {
  if (!busy.compareAndSet(false, true)) return
  try {
   if (!available(context)) mutable.value = State.Loading
   val allowed = try {
    withContext(Dispatchers.IO) { registerAndCheck(context) }
   } catch (error: CancellationException) {
    throw error
   } catch (_: Exception) {
    // Network and server errors cannot grant access or extend a previously verified lease.
    if (available(context)) mutable.value = State.Available
    else publish(context, false)
    return
   }
   publish(context, allowed)
  } finally {
   busy.set(false)
  }
 }
 private fun registerAndCheck(context: Context): Boolean {
  val id = deviceId(context)
  require(id.isNotEmpty())
  val deviceName = Settings.Global.getString(context.contentResolver, "device_name")
      ?.takeIf { it.isNotBlank() } ?: Build.DEVICE
  val payload = JSONObject()
      .put("deviceId", id)
      .put("model", Build.MODEL.take(100))
      .put("deviceName", deviceName.take(100))
      .toString().toByteArray(Charsets.UTF_8)

  val connection = URL(REGISTRATION_URL).openConnection() as HttpURLConnection
  connection.connectTimeout = 8000
  connection.readTimeout = 12000
  connection.requestMethod = "POST"
  connection.doOutput = true
  connection.useCaches = false
  connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
  connection.setRequestProperty("Accept", "application/json")
  try {
   connection.outputStream.use { it.write(payload) }
   check(connection.responseCode == 200)
   val response = connection.inputStream.use { input ->
    val data = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(1024)
    while (true) {
     val count = input.read(buffer)
     if (count < 0) break
     require(data.size() + count <= 16384)
     data.write(buffer, 0, count)
    }
    data.toString("UTF-8")
   }
   val result = JsonParser().parse(response).asJsonObject
   val ok = result.getAsJsonPrimitive("ok")
   val allowed = result.getAsJsonPrimitive("allowed")
   check(ok.isBoolean && ok.asBoolean && allowed.isBoolean)
   return allowed.asBoolean
  } finally {
   connection.disconnect()
  }
 }
 private fun publish(c:Context,allowed:Boolean){
  PrefManager.writeTesterAccess(allowed,SystemClock.elapsedRealtime(),MembershipLease.bootCount(c))
  mutable.value = if (available(c)) State.Available else State.Unavailable
  if(!allowed)current.get()?.takeIf { it !is io.github.phantom.gps.ui.TesterAccessActivity && !it.isFinishing }?.let { ensure(it) }
  if(!allowed && PrefManager.isStarted)runCatching { LocationController.stop(c) }
  if(!allowed)runCatching { LicenseGuard.stopAllMaintenance(c); c.stopService(Intent(c,JoystickService::class.java)) }
  runCatching { LocationRefresh.send(c) }
 }
}
