package io.github.phantom.gps.utils
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import io.github.phantom.gps.BuildConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
/** Anonymous HTTPS configuration only: never downloads executable code or embeds a GitHub token. */
object TesterAccess {
 const val URL_BASE="https://raw.githubusercontent.com/phantommaster1245-star/Gozza/main/control/access.json"
 private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
 private val busy=AtomicBoolean(false)
 private var started=false
 private var current=java.lang.ref.WeakReference<Activity>(null)
 private val mutable=MutableStateFlow("กำลังตรวจสิทธิ์เครื่องทดสอบ…")
 val status=mutable.asStateFlow()
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
  PrefManager.writeTesterAccess(false,SystemClock.elapsedRealtime(),MembershipLease.bootCount(c))
  scope.launch { while(isActive){check(c.applicationContext);delay(60000)} }
 }
 @JvmStatic fun retry(c:Context){scope.launch{check(c.applicationContext)}}
 @JvmStatic fun ensure(activity:Activity):Boolean {
  if(available(activity))return true
  activity.startActivity(Intent(activity,io.github.phantom.gps.ui.TesterAccessActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP))
  return false
 }
 private suspend fun check(c:Context) {
  if(!busy.compareAndSet(false,true))return
  try{
   val allowed=withContext(Dispatchers.IO){
    val connection=URL(URL_BASE+"?check="+System.currentTimeMillis()).openConnection() as HttpURLConnection
    connection.connectTimeout=5000;connection.readTimeout=5000;connection.instanceFollowRedirects=false;connection.useCaches=false
    connection.setRequestProperty("Cache-Control","no-cache, no-store")
    try{
     check(connection.responseCode==200)
     val bytes=connection.inputStream.use { input ->
      val output=java.io.ByteArrayOutputStream();val buffer=ByteArray(2048)
      while(true){val n=input.read(buffer);if(n<0)break;require(output.size()+n<=16384);output.write(buffer,0,n)}
      output.toByteArray()
     }
     AccessPolicy.permits(bytes.toString(Charsets.UTF_8),deviceId(c))
    }finally{connection.disconnect()}
   }
   publish(c,allowed,if(allowed)"เครื่องนี้ได้รับอนุญาตแล้ว" else "เครื่องนี้ยังไม่ได้รับอนุญาต หรือผู้พัฒนาปิดการทดสอบ")
  }catch(e:CancellationException){throw e}catch(e:Exception){publish(c,false,"ตรวจสิทธิ์ไม่ได้ · ตรวจอินเทอร์เน็ตแล้วลองใหม่")}
  finally{busy.set(false)}
 }
 private fun publish(c:Context,allowed:Boolean,message:String){
  PrefManager.writeTesterAccess(allowed,SystemClock.elapsedRealtime(),MembershipLease.bootCount(c))
  mutable.value=message
  if(!allowed)current.get()?.takeIf { it !is io.github.phantom.gps.ui.TesterAccessActivity && !it.isFinishing }?.let { ensure(it) }
  if(!allowed && PrefManager.isStarted)runCatching { LocationController.stop(c) }
  if(!allowed)runCatching { LicenseGuard.stopAllMaintenance(c); c.stopService(Intent(c,JoystickService::class.java)) }
  runCatching { LocationRefresh.send(c) }
 }
}
