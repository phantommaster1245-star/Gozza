package io.github.phantom.gps
import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import io.github.phantom.gps.utils.AppIntegrity
import io.github.phantom.gps.utils.LicenseGuard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
lateinit var gsApp: App
open class App:Application() {
 val globalScope=CoroutineScope(Dispatchers.Default)
 companion object { fun commonInit() { if(BuildConfig.DEBUG)Timber.plant(Timber.DebugTree()) } }
 override fun onCreate() {
  super.onCreate();gsApp=this
  if(!AppIntegrity.isSelfValid(this))return
  commonInit();AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
  ProcessLifecycleOwner.get().lifecycle.addObserver(object:DefaultLifecycleObserver {
   override fun onStart(owner:LifecycleOwner) { LicenseGuard.onAppForeground(this@App) }
   override fun onStop(owner:LifecycleOwner) { LicenseGuard.onAppBackground() }
  })
 }
}
