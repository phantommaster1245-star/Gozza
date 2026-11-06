# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# The Xposed entry looks up precisely this class/method by name.
-keep class io.github.phantom.gps.ui.viewmodel.MainViewModel {
    public void updateXposedState();
}

-keep class io.github.phantom.gps.xposed.HookEntry{*;}
-keep class de.robv.android.xposed.**{*;}
-keepnames class de.robv.android.xposed.**

-repackageclasses
-allowaccessmodification
-overloadaggressively


# Replace original source file names while retaining line mappings for private crash retracing.
-renamesourcefileattribute SourceFile
