package io.github.phantom.gps.utils

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import io.github.phantom.gps.BuildConfig

object MembershipLease {
    val verifier by lazy { MembershipLeaseVerifier(BuildConfig.MEMBERSHIP_PUBLIC_KEY) }
    @Volatile var hookBootCount = -1
    @Volatile private var cachedBootCount = -1
    @Volatile private var hookContext: Context? = null
    @Volatile private var nextHookBootRead = 0L
    fun initializeHookClock(context:Context) {
        hookContext=context.applicationContext ?: context
        hookBootCount=bootCount(context)
    }
    private fun retryHookClock() {
        if(hookBootCount>=0)return
        val elapsed=SystemClock.elapsedRealtime()
        if(elapsed<nextHookBootRead)return
        nextHookBootRead=elapsed+1000
        hookContext?.let { hookBootCount=bootCount(it) }
    }
    fun bootCount(context: Context): Int {
        if(cachedBootCount >= 0) return cachedBootCount
        return runCatching { Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT) }.getOrDefault(-1).also { if(it >= 0) cachedBootCount = it }
    }
    fun valid(context: Context): Boolean = verifier.isValid(PrefManager.membershipLease, System.currentTimeMillis(), SystemClock.elapsedRealtime(), bootCount(context), DeviceIdProvider.getDeviceId())
    fun hookValid(token: String): Boolean {
        retryHookClock()
        return verifier.isValid(token, System.currentTimeMillis(), SystemClock.elapsedRealtime(), hookBootCount)
    }
}
