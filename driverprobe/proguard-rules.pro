# LSPosed loads this entry point by the name in assets/xposed_init.
-keep public class com.fulllock.probe.DriverProbe implements de.robv.android.xposed.IXposedHookLoadPackage {
    public <init>();
    public void handleLoadPackage(de.robv.android.xposed.callbacks.XC_LoadPackage$LoadPackageParam);
}
# Xposed invokes callback overrides across the external framework boundary.
-keepclassmembers class * extends de.robv.android.xposed.XC_MethodHook {
    protected void beforeHookedMethod(de.robv.android.xposed.XC_MethodHook$MethodHookParam);
    protected void afterHookedMethod(de.robv.android.xposed.XC_MethodHook$MethodHookParam);
}
-repackageclasses com.fulllock.probe.internal
-renamesourcefileattribute SourceFile
-keepattributes LineNumberTable
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
-assumenosideeffects class de.robv.android.xposed.XposedBridge {
    public static void log(java.lang.String);
}
