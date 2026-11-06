package com.fulllock.app;
import android.content.Context;
final class RootMode {
 static boolean enabled(Context c){return c.getSharedPreferences("root_research",0).getBoolean("enabled",false);}
 static void setEnabled(Context c,boolean enabled){c.getSharedPreferences("root_research",0).edit().putBoolean("enabled",enabled).apply();if(!enabled){NotificationOpener.cancel(c);ScreenActions.stopAutomatic();LocalRelayClient.stop();}else LocalRelayClient.start(c);}
 static void retireUnused(Context c){var settings=c.getSharedPreferences("root_research",0);if(settings.getBoolean("cleanedFastMode",false))return;
  c.deleteSharedPreferences("authorized_api_config");
  try{java.io.File base=new java.io.File(c.getFilesDir(),"screen-ocr");String root=c.getFilesDir().getCanonicalPath()+java.io.File.separator;for(String lang:new String[]{"tha","eng"}){java.io.File file=new java.io.File(base,"tessdata/"+lang+".traineddata");if(file.getCanonicalPath().startsWith(root))file.delete();}new java.io.File(base,"tessdata").delete();base.delete();}catch(java.io.IOException ignored){}
  settings.edit().remove("screenOcr").remove("directDriver").remove("liveOverlay").remove("overlayX").remove("overlayY").putBoolean("cleanedFastMode",true).apply();
 }
}
