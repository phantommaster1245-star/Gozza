package io.github.phantom.gps.utils
import android.app.Service
import android.content.Intent
import android.os.IBinder
class MembershipHeartbeatService:Service() {
 companion object { @Volatile var running=false; private set }
 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
  if(!GpsAccess.available(this)||!PrefManager.isStarted){stopSelf();return START_NOT_STICKY}
  running=true
  startForeground(NotificationsChannel.NOTIFICATION_ID_LOCATION,LocationNotification.build(this))
  return START_STICKY
 }
 override fun onBind(intent:Intent?):IBinder?=null
 override fun onDestroy(){running=false;stopForeground(STOP_FOREGROUND_DETACH);LocationNotification.stopped(this);super.onDestroy()}
}
