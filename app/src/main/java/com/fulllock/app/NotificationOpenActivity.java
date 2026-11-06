package com.fulllock.app;
import android.app.Activity;import android.os.Bundle;import android.view.WindowManager;
/** A short-lived foreground sender for a single live notification action. Not exported. */
public final class NotificationOpenActivity extends Activity {
 private boolean dispatched;
 @Override protected void onCreate(Bundle state){super.onCreate(state);getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);}
 @Override protected void onResume(){super.onResume();if(dispatched){finish();return;}dispatched=true;String token=getIntent()==null?"":getIntent().getStringExtra("openToken");getWindow().getDecorView().post(()->{NotificationOpener.dispatch(this,token==null?"":token);finish();});}
}
