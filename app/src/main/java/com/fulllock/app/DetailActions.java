package com.fulllock.app;
import android.content.Context;import android.os.Bundle;import org.json.JSONObject;
/** Details are read by the probe; no matching, return, open or accept actions. */
final class DetailActions {
 static void arm(JobParser.Job job){}static void arm(JobParser.Job job,String reference){}
 static void cancel(){}static void left(){}
 static Bundle snapshot(Context c,JSONObject snapshot,long at){return ScreenActions.controls(c);}
 static Bundle permit(Context c,Bundle request){return new Bundle();}
}
