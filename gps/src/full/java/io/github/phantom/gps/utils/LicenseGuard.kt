package io.github.phantom.gps.utils
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
/** Local GPS lifecycle for Gozza. No authentication, lease or membership requests. */
object LicenseGuard {
 data class ActivationResult(val success:Boolean,val errorMessage:String?)
 fun activate(context:Context, activationCode:String,onResult:(ActivationResult)->Unit) { onResult(ActivationResult(GpsAccess.available(context),null)) }
 fun onAppForeground(context:Context) { if(GpsAccess.available(context))ClipboardMonitorService.ensureRunning(context) }
 fun onAppBackground() { ClipboardMonitorService.stopForegroundCapture() }
 fun startHeartbeat(context:Context):Boolean {
  if(!GpsAccess.available(context)||!PrefManager.isStarted)return false
  return runCatching { ContextCompat.startForegroundService(context,Intent(context,MembershipHeartbeatService::class.java));true }.getOrDefault(false)
 }
 fun stopActiveHeartbeat(context: Context) {
  // A pending start must reach onStartCommand and promote before stopping itself.
  if (MembershipHeartbeatService.running) context.stopService(Intent(context, MembershipHeartbeatService::class.java))
 }
 fun stopAllMaintenance(context:Context) { stopActiveHeartbeat(context);ClipboardMonitorService.stopForegroundCapture() }
 fun isLocallyAllowedForCriticalAction(context:Context)=GpsAccess.available(context)
 fun runCriticalActionIfAllowed(context:Context,action:()->Unit):Boolean { if(!GpsAccess.available(context))return false;action();return true }
 fun routeToRequiredScreen(context:Context):Boolean = false
 fun verifyNowForeground(context:Context,onResult:((Boolean)->Unit)?=null) { onResult?.invoke(GpsAccess.available(context)) }
 fun verifyNowForRetry(context:Context,onResult:((Boolean)->Unit)?=null)=verifyNowForeground(context,onResult)
 suspend fun performBackgroundCheck(context:Context)=GpsAccess.available(context)
 fun clearLicenseAndDeactivate(context:Context) { LocationController.stop(context) }
 fun unlock(context:Context) = Unit
 fun lockRetryable(context:Context,reason:String,launchUi:Boolean=true) = Unit
 fun lockPermanent(context:Context,reason:String,launchUi:Boolean=true) = Unit
 fun getUserFacingReason(context:Context,reason:String)="Gozza รุ่นทดสอบส่วนตัว"
}
