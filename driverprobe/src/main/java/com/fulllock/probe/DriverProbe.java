package com.fulllock.probe;

import android.app.Application;
import android.content.Context;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.lang.reflect.Method;

/** Legacy entry retained for standalone DriverProbe and Gozza's legacy build. */
public final class DriverProbe implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam pkg) {
        if (!DriverProbeSession.DRIVER.equals(pkg.packageName) ||
            !DriverProbeSession.DRIVER.equals(pkg.processName)) return;

        DriverProbeSession session = new DriverProbeSession(new ProbeHooks() {
            @Override
            void hookMethod(Method method, Callback callback) {
                XposedBridge.hookMethod(method, new XC_MethodHook(callback.priority) {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam p) {
                        Call call = new Call(p.thisObject, p.args, false);
                        callback.beforeHookedMethod(call);
                        if (call.skipOriginal) p.setResult(call.result);
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam p) {
                        callback.afterHookedMethod(new Call(p.thisObject, p.args, p.hasThrowable()));
                    }
                });
            }
        });
        XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam p) {
                if (!p.hasThrowable()) session.attach((Context) p.args[0]);
            }
        });
    }
}
