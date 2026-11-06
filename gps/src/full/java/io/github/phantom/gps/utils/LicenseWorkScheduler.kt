package io.github.phantom.gps.utils
import android.content.Context
import androidx.work.WorkManager
object LicenseWorkScheduler {
 fun schedule(context:Context,nextCheckSeconds:Long=LicensePrefs.DEFAULT_NEXT_CHECK_SECONDS)=cancel(context)
 fun cancel(context:Context) { WorkManager.getInstance(context).cancelUniqueWork("license_check") }
}
