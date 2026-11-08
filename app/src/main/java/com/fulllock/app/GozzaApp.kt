package com.fulllock.app

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import io.github.phantom.gps.utils.TesterAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@dagger.hilt.android.HiltAndroidApp
class GozzaApp : io.github.phantom.gps.App() {
    override fun onCreate() {
        super.onCreate()
        FullLockApp.initialize(this)
        globalScope.launch(Dispatchers.Main.immediate) {
            ProcessLifecycleOwner.get().lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                TesterAccess.state.collect { DriverReadingService.startIfEnabled(this@GozzaApp) }
            }
        }
    }
}
