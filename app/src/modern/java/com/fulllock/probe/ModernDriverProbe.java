package com.fulllock.probe;

import android.app.Application;
import android.content.Context;
import io.github.libxposed.api.XposedModule;
import java.lang.reflect.Method;

/** Loads the same read and overlay probe through the API 102 hook chain. */
public final class ModernDriverProbe extends XposedModule {
    private String processName;

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        processName = param.getProcessName();
    }

    @Override
    public void onPackageReady(PackageReadyParam param) {
        if (!param.isFirstPackage() || !DriverProbeSession.DRIVER.equals(param.getPackageName()) ||
            !DriverProbeSession.DRIVER.equals(processName)) return;

        ProbeHooks hooks = new ProbeHooks() {
            @Override
            void hookMethod(Method method, Callback callback) {
                hook(method).setPriority(callback.priority).setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Call call = new Call(chain.getThisObject(), chain.getArgs().toArray(), false);
                        callback.beforeHookedMethod(call);
                        Object result = call.skipOriginal ? call.result : chain.proceed();
                        callback.afterHookedMethod(call);
                        return result;
                    });
            }
        };
        DriverProbeSession session = new DriverProbeSession(hooks);
        hooks.hookAllMethods(Application.class, "attach", new ProbeHooks.Callback() {
            @Override
            protected void afterHookedMethod(ProbeHooks.Call call) {
                if (call.args.length == 1 && call.args[0] instanceof Context context) {
                    session.attach(context);
                }
            }
        });
    }
}
