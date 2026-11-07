# Android entry points are preserved by AGP's generated manifest rules.
# No application-wide keep rule: internal filters, models, and helpers can be renamed/optimized.
-renamesourcefileattribute SourceFile
-keepattributes LineNumberTable
-repackageclasses 'com.fulllock.internal'

# Invoked by app_process during root framework setup.
-keep public class com.fulllock.app.RootFrameworkConfig { public static void main(java.lang.String[]); }

-keep public class com.fulllock.app.LocalRelayMain { public static void main(java.lang.String[]); }

# Unified APK: entry names in assets/xposed_init are consumed outside Android manifests.
-keep public class io.github.phantom.gps.xposed.HookEntry {
    public <init>();
    public void handleLoadPackage(de.robv.android.xposed.callbacks.XC_LoadPackage$LoadPackageParam);
}
-keep public class com.fulllock.probe.DriverProbe {
    public <init>();
    public void handleLoadPackage(de.robv.android.xposed.callbacks.XC_LoadPackage$LoadPackageParam);
}
-keepclassmembers class * extends de.robv.android.xposed.XC_MethodHook {
    protected void beforeHookedMethod(de.robv.android.xposed.XC_MethodHook$MethodHookParam);
    protected void afterHookedMethod(de.robv.android.xposed.XC_MethodHook$MethodHookParam);
}
# GPS self-process hook looks up this class/method using literal names.
-keep class io.github.phantom.gps.ui.viewmodel.MainViewModel {
    public void updateXposedState();
}
# API 102 entry classes are instantiated by the framework from META-INF/xposed/java_init.list.
-keep public class io.github.phantom.gps.xposed.ModernHookEntry { *; }
-keep public class com.fulllock.probe.ModernDriverProbe { *; }
