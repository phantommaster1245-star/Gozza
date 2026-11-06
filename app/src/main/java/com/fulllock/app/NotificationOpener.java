package com.fulllock.app;
import android.app.PendingIntent;import android.content.Context;
/** Compatibility endpoint: notifications supply data only and cannot open jobs. */
final class NotificationOpener {
 static String burstToken(Context c){return "";}
 static void detailEvaluated(Context c,JobParser.Job j,String ref,boolean settled,String attempt){}
 static void detailLeft(){}
 static boolean protects(String key,long time){return false;}
 static String protectedKey(){return "";} static long protectedTime(){return 0;}
 static void send(Context c,PendingIntent intent)throws PendingIntent.CanceledException{throw new SecurityException("Read-only jobs");}
 static String pendingUuid(){return "";}static boolean waiting(){return false;}
 static void open(Context c,PendingIntent p,JobParser.Job j,String key,long time,String title,String text,String uuid){}
 static void dispatch(Context c,String token){}
 static void detailObserved(Context c,JobParser.Job j,String ref,long at){}
 static void cancel(Context c){}
}
