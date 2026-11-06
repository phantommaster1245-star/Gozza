package com.fulllock.app
@dagger.hilt.android.HiltAndroidApp
class GozzaApp : io.github.phantom.gps.App() {
 override fun onCreate() { super.onCreate(); FullLockApp.initialize(this) }
}
