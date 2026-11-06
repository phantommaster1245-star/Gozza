package com.fulllock.app;
import android.app.*;import android.content.*;import android.graphics.Typeface;import android.os.Bundle;import android.view.*;import android.widget.*;
/** Read and overlay controls. Job filtering and job-opening controls are intentionally absent. */
public class MainActivity extends Activity {
 static volatile boolean foreground,historyForeground;
 private LinearLayout body;private FrameLayout frame;private View drawer;private boolean permissions;
 private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
 @Override public void onCreate(Bundle state){super.onCreate(state);if(!AppIntegrity.trusted(this)){finish();return;}permissions=state!=null&&state.getBoolean("permissions");}
 @Override protected void onResume(){super.onResume();foreground=true;render();JobListener.refreshFloating();}
 @Override protected void onPause(){foreground=false;super.onPause();}
 @Override protected void onSaveInstanceState(Bundle out){out.putBoolean("permissions",permissions);super.onSaveInstanceState(out);}
 static boolean showUpdate(String version){return false;}
 static void cancelPendingStop(){}
 private TextView text(String value,int size,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(AppTheme.ink(this));if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
 private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
 private void gap(LinearLayout l,int n){View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,dp(n)));}
 private void action(LinearLayout parent,String label,Runnable run){TextView t=text(label,16,true);t.setTextColor(AppTheme.accent(this));t.setPadding(dp(12),dp(16),dp(12),dp(16));t.setBackground(AppTheme.background(AppTheme.surface(this),dp(10)));t.setOnClickListener(v->run.run());parent.addView(t,new LinearLayout.LayoutParams(-1,-2));gap(parent,8);}
 private void toggle(String title,String detail,String key,boolean fallback){Switch s=new Switch(this);s.setText(title);s.setTextColor(AppTheme.ink(this));s.setTextSize(16);s.setPadding(0,dp(12),0,dp(12));s.setChecked(Store.prefs(this).getBoolean(key,fallback));s.setThumbTintList(android.content.res.ColorStateList.valueOf(AppTheme.accent(this)));body.addView(s);if(detail!=null){TextView d=text(detail,12,false);d.setTextColor(AppTheme.muted(this));body.addView(d);}s.setOnCheckedChangeListener((button,on)->{Store.prefs(this).edit().putBoolean(key,on).apply();ScreenActions.wake(this);JobListener.refreshFloating();});}
 private void heading(String title){gap(body,18);TextView h=text(title,19,true);h.setTextColor(AppTheme.accent(this));body.addView(h);gap(body,8);}
 private void render(){
  drawer=null;getWindow().setStatusBarColor(AppTheme.page(this));getWindow().setNavigationBarColor(AppTheme.page(this));getWindow().getDecorView().setSystemUiVisibility(0);
  frame=new FrameLayout(this);frame.setBackgroundColor(AppTheme.page(this));LinearLayout page=column();frame.addView(page);page.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
  LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(18),dp(8),dp(18),dp(8));bar.setBackgroundColor(AppTheme.surface(this));TextView menu=text("☰",26,true);menu.setContentDescription("เปิดเมนู");menu.setPadding(0,0,dp(20),0);menu.setOnClickListener(v->openDrawer());bar.addView(menu);TextView brand=text("Gozza Lalamove",21,true);brand.setTextColor(AppTheme.accent(this));bar.addView(brand);page.addView(bar,new LinearLayout.LayoutParams(-1,dp(64)));
  ScrollView scroll=new ScrollView(this);page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));body=column();body.setPadding(dp(20),dp(8),dp(20),dp(24));scroll.addView(body);
  if(permissions){heading("สิทธิ์การใช้งาน");for(int i:new int[]{0,3,4}){final int step=i;action(body,(PermissionSetup.ready(this,i)?"✓ ":"○ ")+PermissionSetup.TITLES[i],()->PermissionSetup.open(this,step));}}
  else {
   heading("อ่านข้อมูลและวาดทับ");body.addView(text("แสดงข้อมูลบนหน้า Driver · แตะป้ายตำแหน่งเพื่อตั้ง GPS",14,false));gap(body,12);
   action(body,RootMode.enabled(this)?"● เปิดการอ่านข้อมูลแล้ว · แตะเพื่อปิด":"เปิดการอ่านข้อมูลผ่าน LSPosed",()->{if(RootMode.enabled(this)){RootMode.setEnabled(this,false);render();}else RootSetup.begin(this,this::render);});
   toggle("แสดงข้อมูลบนการ์ดงาน",null,"cardOverlayEnabled",true);
   toggle("รายได้สุทธิ",null,"cardShowNet",true);toggle("ระยะทางงาน",null,"cardShowDistance",true);toggle("ราคาเฉลี่ยต่อกิโลเมตร",null,"cardShowRate",true);
   toggle("ป้ายตำแหน่งจุดรับ","แตะป้ายเพื่อส่งพิกัดเข้า GPS hook ทันที","cardShowPickup",true);
   heading("สถานะการอ่าน");body.addView(text(JobListener.connected?"เชื่อมต่อบริการอ่านแจ้งเตือนแล้ว":"ยังไม่เชื่อมต่อบริการอ่านแจ้งเตือน",14,false));gap(body,8);body.addView(text(DriverBridge.probeConnected()?"ได้รับข้อมูลจาก Driver แล้ว":"รอเปิดหน้า Driver เพื่อรับข้อมูล",14,false));
   gap(body,16);action(body,"สิทธิ์การใช้งาน",()->{permissions=true;render();});action(body,"ตรวจสอบระบบ",()->SystemDiagnostics.show(this));
  }
  setContentView(frame);
 }
 private void map(){startActivity(new Intent(this,io.github.phantom.gps.ui.MapActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));finish();}
 private void openDrawer(){if(drawer!=null)return;FrameLayout shade=new FrameLayout(this);shade.setBackgroundColor(0x99000000);frame.addView(shade,new FrameLayout.LayoutParams(-1,-1));drawer=shade;shade.setOnClickListener(v->closeDrawer());LinearLayout nav=column();nav.setPadding(dp(20),dp(48),dp(20),dp(24));nav.setBackgroundColor(AppTheme.surface(this));shade.addView(nav,new FrameLayout.LayoutParams(Math.min(dp(320),getResources().getDisplayMetrics().widthPixels-dp(36)),-1,Gravity.START));nav.setOnClickListener(v->{});TextView title=text("Gozza Lalamove",22,true);title.setTextColor(AppTheme.accent(this));nav.addView(title);gap(nav,24);action(nav,"แผนที่และ GPS",this::map);action(nav,"อ่านข้อมูลและวาดทับ",()->{permissions=false;render();});action(nav,"ตั้งค่า GPS",()->{closeDrawer();startActivity(new Intent(this,io.github.phantom.gps.ui.ActivitySettings.class));});action(nav,"สิทธิ์การใช้งาน",()->{permissions=true;render();});}
 private void closeDrawer(){if(drawer!=null){frame.removeView(drawer);drawer=null;}}
 @Override public void onBackPressed(){if(drawer!=null){closeDrawer();return;}if(permissions){permissions=false;render();return;}map();}
}
