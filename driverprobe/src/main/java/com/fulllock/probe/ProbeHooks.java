package com.fulllock.probe;

import android.util.Log;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Keeps the probe's callbacks independent of the module entry API. */
abstract class ProbeHooks {
    abstract void hookMethod(Method method, Callback callback);

    int hookAllMethods(Class<?> type, String name, Callback callback) {
        int installed = 0;
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().equals(name) && !Modifier.isAbstract(method.getModifiers())) {
                hookMethod(method, callback);
                installed++;
            }
        }
        return installed;
    }

    static class Callback {
        static final int PRIORITY_HIGHEST = 10000;
        final int priority;
        Callback() { this(50); }
        Callback(int priority) { this.priority = priority; }
        protected void beforeHookedMethod(Call call) {}
        protected void afterHookedMethod(Call call) {}
    }

    static final class Call {
        final Object thisObject;
        final Object[] args;
        final boolean failed;
        boolean skipOriginal;
        Object result;

        Call(Object receiver, Object[] args, boolean failed) {
            this.thisObject = receiver;
            this.args = args;
            this.failed = failed;
        }
        boolean hasThrowable() { return failed; }
        void setResult(Object result) {
            this.result = result;
            skipOriginal = true;
        }
    }

    static Class<?> findClass(String name, ClassLoader loader) throws ClassNotFoundException {
        return Class.forName(name, false, loader);
    }

    static Object callMethod(Object receiver, String name, Object... args) throws ReflectiveOperationException {
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = args[i] instanceof Integer ? int.class : args[i].getClass();
        }
        try {
            Method method = receiver.getClass().getMethod(name, types);
            method.setAccessible(true);
            return method.invoke(receiver, args);
        } catch (NoSuchMethodException ignored) {
            for (Class<?> type = receiver.getClass(); type != null; type = type.getSuperclass()) {
                try {
                    Method method = type.getDeclaredMethod(name, types);
                    method.setAccessible(true);
                    return method.invoke(receiver, args);
                } catch (NoSuchMethodException missing) {
                    // The target can inherit obfuscated methods from a superclass.
                }
            }
        }
        // RecyclerView declares some arguments as View, but callers pass concrete subclasses.
        for (Method method : receiver.getClass().getMethods()) {
            if (method.getName().equals(name) && matches(method.getParameterTypes(), args)) {
                method.setAccessible(true);
                return method.invoke(receiver, args);
            }
        }
        for (Class<?> type = receiver.getClass(); type != null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (method.getName().equals(name) && matches(method.getParameterTypes(), args)) {
                    method.setAccessible(true);
                    return method.invoke(receiver, args);
                }
            }
        }
        throw new NoSuchMethodException(name);
    }

    private static boolean matches(Class<?>[] types, Object[] args) {
        if (types.length != args.length) return false;
        for (int i = 0; i < args.length; i++) {
            if (types[i] == int.class && args[i] instanceof Integer) continue;
            if (!types[i].isInstance(args[i])) return false;
        }
        return true;
    }

    static Object getObjectField(Object receiver, String name) throws ReflectiveOperationException {
        for (Class<?> type = receiver.getClass(); type != null; type = type.getSuperclass()) {
            try {
                var field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(receiver);
            } catch (NoSuchFieldException missing) {
                // Look in the next superclass.
            }
        }
        throw new NoSuchFieldException(name);
    }

    static int getIntField(Object receiver, String name) throws ReflectiveOperationException {
        return (Integer) getObjectField(receiver, name);
    }

    static void log(String message) { Log.i("FullLockProbe", message); }
}
