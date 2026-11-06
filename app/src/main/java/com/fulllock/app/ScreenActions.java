package com.fulllock.app;
import android.content.*;import android.os.*;import org.json.*;import java.util.*;
final class ScreenActions {
 private static final LinkedHashMap<String,JobParser.Job> visible=new LinkedHashMap<>();
 private static long observed;
 static int clickedCount(){return 0;}
 static synchronized void paused(){visible.clear();}
 static void wake(Context c){c.sendBroadcast(new Intent("com.gozza.lalamove.PROBE_REFRESH").setPackage(JobListener.DRIVER));}
 static void stopAutomatic(){AutoAcceptActions.cancel();}
 static Bundle controls(Context c){
  Bundle b=new Bundle();b.putBoolean("enabled",DriverBridge.viewsEnabled(c));
  b.putBoolean("readOnly",true);b.putBoolean("auto",false);b.putBoolean("autoAccept",false);b.putBoolean("swipePreview",false);
  b.putBoolean("pickupActionEnabled",true);b.putBoolean("pickupDirectSend",true);
  b.putInt("pickupTouchPercent",PickupTouchArea.percent(Store.prefs(c).getInt("pickupTouchPercent",PickupTouchArea.DEFAULT)));
  b.putBoolean("drawCards",Store.prefs(c).getBoolean("cardOverlayEnabled",true));
  for(String key:new String[]{"cardShowNet","cardShowDistance","cardShowRate","cardShowPickup"})b.putBoolean(key,Store.prefs(c).getBoolean(key,true));
  b.putInt("rootOverlayBorder",com.fulllock.app.GozzaDesign.primary);b.putInt("rootOverlayBackground",com.fulllock.app.GozzaDesign.surface);b.putInt("rootOverlayText",com.fulllock.app.GozzaDesign.textPrimary);
  b.putBoolean("rootOverlayRgb",false);b.putBoolean("rootOverlayPulse",false);
  JSONObject display=new JSONObject();try{display.put("fee",Store.prefs(c).getInt("fee",20));}catch(JSONException ignored){}
  b.putString("rules",display.toString());b.putStringArrayList("matched",new ArrayList<>());return b;
 }
 static synchronized Bundle snapshot(Context c,JSONObject data,long at)throws JSONException{
  visible.clear();observed=at;
  for(ScreenCardParser.Card card:ScreenCardParser.parse(data.getJSONArray("nodes"))){
   visible.put(card.token(),card.job());
   JobListener.screenJob(card.token(),System.currentTimeMillis(),card.job(),true,"อ่านข้อมูล",card.commissionReduction());
  }
  return controls(c);
 }
 static boolean request(Context c,String token){return false;}
 static boolean request(Context c,JobParser.Job job){return false;}
 static Bundle permit(Context c,Bundle b){return new Bundle();}
 static void result(Context c,Bundle b){}
 static synchronized boolean uniqueForMerge(JobParser.Job job){return visible.values().stream().filter(j->j.equals(job)).count()==1;}
 static synchronized boolean hasFreshJob(JobParser.Job job){return SystemClock.elapsedRealtime()-observed<500&&visible.containsValue(job);}
 static synchronized boolean hasLiveList(){return !visible.isEmpty()&&SystemClock.elapsedRealtime()-observed<1000;}
 static String firstMatching(){return "";}
 static synchronized String firstVisible(){return visible.isEmpty()?"":visible.keySet().iterator().next();}
 static String metrics(){return "อ่านข้อมูลและวาดทับ · ไม่มีการเปิดหรือรับงาน";}
}
