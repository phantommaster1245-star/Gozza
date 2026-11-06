package com.fulllock.app;
import android.content.Context;import android.os.Bundle;
/** Compatibility endpoint: Gozza has no job-accept capability. */
final class AutoAcceptActions {
 static void cancel(){} static void left(){}
 static boolean enabled(Context c){return false;}
 static void observe(Context c,ScreenCardParser.Card card,boolean match,long at,Bundle out){}
 static Bundle request(Context c,Bundle data){return new Bundle();}
}
