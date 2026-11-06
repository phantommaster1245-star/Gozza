package com.fulllock.app;
import android.content.Context;import android.os.Bundle;
final class OverlaySettingsChecks {
 static String run(Context c){var prefs=Store.prefs(c);var saved=prefs.getAll();int checks=0;
  try{
   String previous=ScreenActions.controls(c).getString("overlayStyleKey");
   for(int i=0;i<OverlayPalette.KEYS.length;i++){
    String key=OverlayPalette.KEYS[i];int chosen=0x80112233+i;prefs.edit().putInt(key,chosen).commit();Bundle controls=ScreenActions.controls(c);
    if(controls.getInt(key)!=chosen)throw new AssertionError("Style not sent: "+key);checks++;
    if(previous.equals(controls.getString("overlayStyleKey")))throw new AssertionError("Drawing cache not invalidated: "+key);checks++;
    previous=controls.getString("overlayStyleKey");
   }
   for(int[] pair:new int[][]{{0,100},{100,100},{200,200},{300,300},{999,300}}){prefs.edit().putInt("pickupTouchPercent",pair[0]).commit();if(ScreenActions.controls(c).getInt("pickupTouchPercent")!=pair[1])throw new AssertionError("Touch clamp/transport");checks++;}
   if(PickupTouchArea.padding(24,200)!=12f||PickupTouchArea.padding(24,300)!=24f)throw new AssertionError("Hit expansion geometry");checks++;
   Bundle controls=ScreenActions.controls(c);if(!controls.getBoolean("pickupDirectSend")||controls.getBoolean("auto")||controls.getBoolean("autoAccept"))throw new AssertionError("Read-only/direct GPS behavior changed");checks++;
   return checks+" overlay style, cache revision, touch-area and read-only checks passed";
  }finally{var restore=prefs.edit();for(String key:OverlayPalette.KEYS){if(saved.containsKey(key))restore.putInt(key,(Integer)saved.get(key));else restore.remove(key);}if(saved.containsKey("pickupTouchPercent"))restore.putInt("pickupTouchPercent",(Integer)saved.get("pickupTouchPercent"));else restore.remove("pickupTouchPercent");restore.commit();ScreenActions.wake(c);}
 }
}
