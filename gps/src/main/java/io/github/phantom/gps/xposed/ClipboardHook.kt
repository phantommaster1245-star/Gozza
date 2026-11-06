package io.github.phantom.gps.xposed

import android.app.AndroidAppHelper
import android.content.ClipData
import android.content.Context
import android.content.Intent
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.phantom.gps.BuildConfig
import io.github.phantom.gps.utils.LocationActions
import io.github.phantom.gps.utils.ClipboardTrace
import io.github.phantom.gps.utils.ClipboardEventGate
import android.os.SystemClock
import java.util.concurrent.Executors

object ClipboardHook {

    private val settings = Xshare()
    private var initialized = false
    private val eventGate = ClipboardEventGate()
    private val dispatcher = Executors.newSingleThreadExecutor { task -> Thread(task, "Phantom-clipboard-dispatch").apply { isDaemon = true } }

    fun initHooks(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "android" || initialized) return
        initialized = true

        val targets = listOf(
            "com.android.server.clipboard.ClipboardService",
            "com.android.server.clipboard.ClipboardService\$ClipboardImpl"
        )

        var hookedCount = 0
        targets.forEach { className ->
            val targetClass = runCatching {
                XposedHelpers.findClass(className, lpparam.classLoader)
            }.getOrNull() ?: return@forEach

            hookedCount += hookClipboardMethods(targetClass)
        }

        if (hookedCount == 0) {
            XposedBridge.log("ClipboardHook: no compatible clipboard method found")
        } else {
            XposedBridge.log("ClipboardHook: hooked methods = $hookedCount")
        }
    }

    private fun hookClipboardMethods(targetClass: Class<*>): Int {
        var count = 0

        for (method in targetClass.declaredMethods) {
            if (method.name != "setPrimaryClip" && method.name != "setPrimaryClipAsPackage") continue
            if (method.parameterTypes.isEmpty()) continue

            val clipArgIndex = method.parameterTypes.indexOfFirst {
                ClipData::class.java.isAssignableFrom(it)
            }
            if (clipArgIndex < 0) continue

            runCatching {
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (param.hasThrowable()) return
                        val capturedAtMs = SystemClock.elapsedRealtime()
                        if (!settings.isClipboardMonitorEnabled) return
                        val clip = param.args.getOrNull(clipArgIndex) as? ClipData ?: return
                        if (clip.itemCount <= 0) return
                        val item = clip.getItemAt(0)
                        val context = resolveContext(param)
                        val raw = item.text?.toString()
                            ?: context?.let { item.coerceToText(it)?.toString() }
                        val text = raw?.trim()
                        if (text.isNullOrEmpty()) return
                        if (!eventGate.accept(text, SystemClock.elapsedRealtime())) return
                        dispatcher.execute {
                            if (context != null) LocationHook.ensureRefreshReceiver(context)
                            dispatchClipboardText(context, text, capturedAtMs)
                        }
                    }
                })
                count += 1
            }.onFailure {
                XposedBridge.log("ClipboardHook: failed ${targetClass.name}#${method.name}: $it")
            }
        }

        return count
    }

    private fun dispatchClipboardText(context: Context?, text: String, capturedAtMs: Long) {
        val ctx = context ?: return
        ctx.sendBroadcast(Intent().apply {
            action = LocationActions.ACTION_CLIPBOARD_TEXT
            setClassName(
                BuildConfig.APPLICATION_ID,
                "io.github.phantom.gps.utils.ClipboardLocationReceiver"
            )
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            putExtra(LocationActions.EXTRA_CLIPBOARD_TEXT, text)
            putExtra(ClipboardTrace.EXTRA_CAPTURED_AT, capturedAtMs)
        })
    }

    private fun resolveContext(param: XC_MethodHook.MethodHookParam): Context? {
        (AndroidAppHelper.currentApplication() as? Context)?.let { return it }
        val thisObj = param.thisObject ?: return null

        runCatching {
            XposedHelpers.getObjectField(thisObj, "mContext") as? Context
        }.getOrNull()?.let { return it }

        runCatching {
            val outer = XposedHelpers.getObjectField(thisObj, "this$0")
            XposedHelpers.getObjectField(outer, "mContext") as? Context
        }.getOrNull()?.let { return it }

        return null
    }

}
