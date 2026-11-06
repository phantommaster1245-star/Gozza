package io.github.phantom.gps.test;
import android.app.Application;
import androidx.test.runner.AndroidJUnitRunner;
import com.google.firebase.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.analytics.FirebaseAnalytics;
/** Local Emulator Suite only. Never compiled into a public APK. */
public final class MembershipTestRunner extends AndroidJUnitRunner {
 @Override public void callApplicationOnCreate(Application app){
  if("true".equals(androidx.test.platform.app.InstrumentationRegistry.getArguments().getString("credentialsOnly"))){
   String target=androidx.test.platform.app.InstrumentationRegistry.getArguments().getString("class","");
   if(!target.equals("io.github.phantom.gps.test.RememberedLoginStoreDeviceTest")&&!target.startsWith("io.github.phantom.gps.test.RememberedLoginStoreDeviceTest#"))throw new IllegalArgumentException("Credentials-only tests required");
   super.callApplicationOnCreate(app);return;
  }
  try { FirebaseApp.getInstance().delete(); } catch(IllegalStateException ignored) { }
  FirebaseApp.initializeApp(app,new FirebaseOptions.Builder().setApiKey("AIza00000000000000000000000000000000000").setApplicationId("1:123456789:android:0123456789abcdef012345").setProjectId("demo-phantom-gps").build());
  FirebaseAuth.getInstance().useEmulator("127.0.0.1",9099);
  FirebaseFunctions.getInstance("asia-southeast1").useEmulator("127.0.0.1",5001);
  FirebaseAnalytics.getInstance(app).setAnalyticsCollectionEnabled(false);
  super.callApplicationOnCreate(app);
 }
}
