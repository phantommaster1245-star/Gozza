# Android entry points are preserved by AGP's generated manifest rules.
# No application-wide keep rule: internal filters, models, and helpers can be renamed/optimized.
-renamesourcefileattribute SourceFile
-keepattributes LineNumberTable
-repackageclasses 'com.fulllock.internal'

# Invoked by app_process during root framework setup.
-keep public class com.fulllock.app.RootFrameworkConfig { public static void main(java.lang.String[]); }

-keep public class com.fulllock.app.LocalRelayMain { public static void main(java.lang.String[]); }
