package com.fulllock.app;
import android.app.*;import android.content.*;import android.os.Bundle;import org.json.JSONObject;
/** Migration checks: stale Full Lock settings cannot restore opening or copying behavior. */
public class ReadOnlyChecks extends Instrumentation {
 @Override public void onCreate(Bundle args){super.onCreate(args);start();}
 private void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 @Override public void onStart(){Bundle result=new Bundle();Context c=getTargetContext();var prefs=Store.prefs(c);var saved=prefs.getAll();String[] keys={"enabled","autoAccept","pickupDirectSend"};PendingIntent intent=null;
  try{
   prefs.edit().putBoolean("enabled",true).putBoolean("autoAccept",true).putBoolean("pickupDirectSend",false).commit();
   Bundle config=ScreenActions.controls(c);
   check(!config.getBoolean("auto")&&!config.getBoolean("autoAccept"),"legacy automatic flags must not enable job actions");
   check(config.getBoolean("pickupDirectSend"),"pickup taps always use GPS");
   check(config.getStringArrayList("matched").isEmpty(),"no filter-based marks");
   JSONObject display=new JSONObject(config.getString("rules"));check(display.length()==1&&display.has("fee"),"display data must not contain job filters");
   check(!ScreenActions.request(c,"stale-card"),"manual card opening denied");
   check(!ScreenActions.permit(c,new Bundle()).getBoolean("allow"),"click permit denied");
   check(!AutoAcceptActions.enabled(c)&&!AutoAcceptActions.request(c,new Bundle()).getBoolean("queued"),"accept commands denied");
   intent=PendingIntent.getActivity(c,919,new Intent().setClassName(c.getPackageName(),"com.fulllock.app.MainActivity"),PendingIntent.FLAG_IMMUTABLE);
   boolean denied=false;try{NotificationOpener.send(c,intent);}catch(SecurityException expected){denied=true;}check(denied,"notification launch denied");
   check(c.getPackageManager().getLaunchIntentForPackage(c.getPackageName()).getComponent().getClassName().equals("io.github.phantom.gps.ui.MapActivity"),"map remains home");
   check(AppTheme.accent(c)==0xFFF16623&&AppTheme.page(c)==0xFF101416,"unified orange and black palette");
   result.putString("result","10 read-only migration checks passed");finish(Activity.RESULT_OK,result);
  }catch(Throwable error){result.putString("error",error.toString());finish(Activity.RESULT_CANCELED,result);}
  finally{if(intent!=null)intent.cancel();var restore=prefs.edit();for(String key:keys){if(saved.containsKey(key))restore.putBoolean(key,(Boolean)saved.get(key));else restore.remove(key);}restore.commit();}
 }
}
