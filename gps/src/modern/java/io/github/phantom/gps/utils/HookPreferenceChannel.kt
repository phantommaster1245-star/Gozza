package io.github.phantom.gps.utils

import android.content.SharedPreferences
import android.util.Log
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import io.github.phantom.gps.gsApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal object HookPreferenceChannel : XposedServiceHelper.OnServiceListener {
    const val GROUP = "gozza_hook_state"
    @Volatile private var connection: Connection? = null
    @Volatile private var published: Connection? = null
    val ready: Boolean get() = connection?.let { it === published } == true
    private data class Connection(val service: XposedService, val prefs: SharedPreferences)

    fun scopeStatus(): String {
        val bound = connection ?: return "CONFIG_NOT_READY"
        if (published !== bound) return "CONFIG_NOT_READY"
        return runCatching {
            val required = setOf("android", "com.lalamove.global.driver.sea", "com.google.android.apps.maps")
            if (bound.service.scope.containsAll(required)) "CONFIGURED" else "SCOPE_MISSING"
        }.getOrDefault("CONFIG_NOT_READY")
    }

    fun initialize() {
        XposedServiceHelper.registerListener(this)
    }

    override fun onServiceBind(service: XposedService) {
        val bound = runCatching {
            if (service.apiVersion < 102) return
            Connection(service, service.getRemotePreferences(GROUP))
        }.getOrElse {
            Log.w("Gozza", "Remote preferences unavailable", it)
            return
        }
        connection = bound
        gsApp.globalScope.launch(Dispatchers.IO) {
            if (PrefManager.publishHookState()) LocationRefresh.send(gsApp)
        }
    }

    override fun onServiceDied(service: XposedService) {
        if (connection?.service === service) connection = null
    }

    fun publish(values: Map<String, *>): Boolean {
        val bound = connection ?: return false
        val committed = runCatching {
            bound.prefs.edit()
                .putString(RemoteHookSnapshot.KEY, RemoteHookSnapshot.encode(values))
                .commit()
        }.getOrDefault(false)
        if (connection !== bound) return false
        published = bound.takeIf { committed }
        return committed
    }
}
