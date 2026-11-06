package com.fulllock.app;

import android.content.Context;
import android.os.SystemClock;

/** Optional companion in this Android user/profile; never requests root or scans other apps. */
final class PickupCopyAccess {
 static final String PACKAGE="com.gozza.lalamove";
 private static long checkedAt=-1;
 private static boolean installed;
 private PickupCopyAccess(){}
 static synchronized boolean available(Context context){
  long now=SystemClock.elapsedRealtime();
  if(checkedAt>=0&&now-checkedAt<1000)return installed;
  try{context.getPackageManager().getPackageInfo(PACKAGE,0);installed=true;}
  catch(android.content.pm.PackageManager.NameNotFoundException|SecurityException e){installed=false;}
  checkedAt=now;return installed;
 }
}
