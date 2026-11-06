package com.fulllock.app;
import android.app.*;import android.content.*;import android.graphics.Typeface;import android.os.Bundle;import android.view.*;import android.widget.*;
/** Read and overlay controls. Job filtering and job-opening controls are intentionally absent. */
public class MainActivity extends Activity {
 static volatile boolean foreground,historyForeground;
 private LinearLayout body;private FrameLayout frame;private View drawer;private boolean permissions,theme;
 private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
 @Override public void onCreate(Bundle state){super.onCreate(state);if(!AppIntegrity.trusted(this)){finish();return;}permissions=state!=null&&state.getBoolean("permissions");theme=state!=null?state.getBoolean("theme"):"theme".equals(getIntent().getStringExtra("gozzaPage"));}
 @Override protected void onResume(){super.onResume();foreground=true;if(!io.github.phantom.gps.utils.TesterAccess.ensure(this)){finish();return;}render();JobListener.refreshFloating();}
 @Override protected void onPause(){foreground=false;super.onPause();}
 @Override protected void onSaveInstanceState(Bundle out){out.putBoolean("permissions",permissions);out.putBoolean("theme",theme);super.onSaveInstanceState(out);}
 @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);theme="theme".equals(intent.getStringExtra("gozzaPage"));permissions=false;}
 static boolean showUpdate(String version){return false;}
 static void cancelPendingStop(){}
 private TextView text(String value,int size,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(AppTheme.ink(this));if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
 private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
 private void gap(LinearLayout l,int n){View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,dp(n)));}
 private void action(LinearLayout parent,String label,Runnable run){TextView t=text(label,16,true);t.setTextColor(AppTheme.ink(this));t.setMinHeight(dp(56));t.setPadding(dp(12),dp(16),dp(12),dp(16));t.setBackground(AppTheme.actionBackground(this,dp(14)));t.setFocusable(true);t.setOnClickListener(v->run.run());parent.addView(t,new LinearLayout.LayoutParams(-1,-2));gap(parent,8);}
 private void toggle(String title,String detail,String key,boolean fallback){Switch s=new Switch(this);s.setText(title);s.setTextColor(AppTheme.ink(this));s.setTextSize(16);s.setPadding(0,dp(12),0,dp(12));s.setChecked(Store.prefs(this).getBoolean(key,fallback));s.setMinHeight(dp(56));s.setTrackTintList(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{GozzaDesign.primary,GozzaDesign.switchOff}));s.setThumbTintList(android.content.res.ColorStateList.valueOf(GozzaDesign.textPrimary));body.addView(s);if(detail!=null){TextView d=text(detail,12,false);d.setTextColor(AppTheme.muted(this));body.addView(d);}s.setOnCheckedChangeListener((button,on)->{Store.prefs(this).edit().putBoolean(key,on).apply();ScreenActions.wake(this);JobListener.refreshFloating();});}
 private void heading(String title){gap(body,18);TextView h=text(title,GozzaDesign.text_section,true);h.setTextColor(AppTheme.accent(this));body.addView(h);gap(body,8);}
 private void render(){
  drawer=null;getWindow().setStatusBarColor(AppTheme.page(this));getWindow().setNavigationBarColor(AppTheme.page(this));getWindow().getDecorView().setSystemUiVisibility(0);
  frame=new FrameLayout(this);frame.setBackgroundColor(AppTheme.page(this));LinearLayout page=column();frame.addView(page);page.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
  LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(18),dp(8),dp(18),dp(8));bar.setBackgroundColor(AppTheme.surface(this));TextView menu=text("☰",26,true);menu.setContentDescription("เปิดเมนู");menu.setPadding(0,0,dp(20),0);menu.setOnClickListener(v->openDrawer());bar.addView(menu);TextView brand=text("Gozza Lalamove",GozzaDesign.text_title,true);brand.setTextColor(AppTheme.accent(this));bar.addView(brand);page.addView(bar,new LinearLayout.LayoutParams(-1,dp(64)));
  ScrollView scroll=new ScrollView(this);page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));body=column();body.setPadding(dp(20),dp(8),dp(20),dp(24));scroll.addView(body);
  if(permissions){heading("สิทธิ์การใช้งาน");for(int i:new int[]{0,3,4}){final int step=i;action(body,(PermissionSetup.ready(this,i)?"✓ ":"○ ")+PermissionSetup.TITLES[i],()->PermissionSetup.open(this,step));}}
  else if(theme){themeBody();}
  else {
   heading("อ่านข้อมูลและวาดทับ");body.addView(text("แสดงข้อมูลบนหน้า Driver · แตะป้ายตำแหน่งเพื่อตั้ง GPS",14,false));gap(body,12);
   action(body,RootSetup.updatePending?"เตรียมโมดูล LSPosed รุ่นใหม่":RootMode.enabled(this)?"● เปิดการอ่านข้อมูลแล้ว · แตะเพื่อปิด":"เปิดการอ่านข้อมูลผ่าน LSPosed",()->{if(RootMode.enabled(this)&&!RootSetup.updatePending){RootMode.setEnabled(this,false);render();}else RootSetup.begin(this,this::render);});
   toggle("แสดงข้อมูลบนการ์ดงาน",null,"cardOverlayEnabled",true);
   toggle("รายได้สุทธิ",null,"cardShowNet",true);toggle("ระยะทางงาน",null,"cardShowDistance",true);toggle("ราคาเฉลี่ยต่อกิโลเมตร",null,"cardShowRate",true);
   toggle("ป้ายตำแหน่งจุดรับ","แตะป้ายเพื่อส่งพิกัดเข้า GPS hook ทันที","cardShowPickup",true);
   touchArea();gap(body,12);action(body,"ธีมการวาดทับ",()->{theme=true;render();});
   heading("สถานะการอ่าน");body.addView(text(JobListener.connected?"เชื่อมต่อบริการอ่านแจ้งเตือนแล้ว":"ยังไม่เชื่อมต่อบริการอ่านแจ้งเตือน",14,false));gap(body,8);body.addView(text(DriverBridge.probeConnected()?"ได้รับข้อมูลจาก Driver แล้ว":"รอเปิดหน้า Driver เพื่อรับข้อมูล",14,false));
   gap(body,16);action(body,"สิทธิ์การใช้งาน",()->{permissions=true;theme=false;render();});action(body,"ตรวจสอบระบบ",()->SystemDiagnostics.show(this));
  }
  setContentView(frame);
 }

 private void touchArea(){
  heading("พื้นที่แตะป้ายตำแหน่ง");int selected=PickupTouchArea.percent(Store.prefs(this).getInt("pickupTouchPercent",PickupTouchArea.DEFAULT));
  TextView value=text(selected+"%",16,true);value.setTag("pickup-touch-value");value.setTextColor(AppTheme.accent(this));body.addView(value);
  PickupTouchPreview preview=new PickupTouchPreview(this,AppTheme.accent(this),AppTheme.ink(this));preview.setTag("pickup-touch-preview");body.addView(preview,new LinearLayout.LayoutParams(-1,dp(100)));preview.percent(selected);
  SeekBar slider=new SeekBar(this);slider.setTag("pickup-touch-percent");slider.setContentDescription("พื้นที่แตะป้ายตำแหน่ง 100 ถึง 300 เปอร์เซ็นต์");slider.setMax(200);slider.setProgress(selected-100);slider.setMinimumHeight(dp(48));slider.setProgressTintList(android.content.res.ColorStateList.valueOf(AppTheme.accent(this)));slider.setThumbTintList(android.content.res.ColorStateList.valueOf(AppTheme.accent(this)));body.addView(slider,new LinearLayout.LayoutParams(-1,dp(48)));
  slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar bar,int n,boolean user){int percent=PickupTouchArea.percent(n+100);value.setText(percent+"%");preview.percent(percent);if(user)Store.prefs(MainActivity.this).edit().putInt("pickupTouchPercent",percent).apply();}public void onStartTrackingTouch(SeekBar bar){}public void onStopTrackingTouch(SeekBar bar){ScreenActions.wake(MainActivity.this);}});
  TextView hint=text("100% = เท่าป้าย · 300% = กว้างและสูง 3 เท่า\nขยายเฉพาะพื้นที่แตะ ไม่ขยายป้าย และยังเลื่อนหน้าได้ตามเดิม",12,false);hint.setTextColor(AppTheme.muted(this));body.addView(hint);
 }
 private void themeBody(){
  heading("ธีมการวาดทับ");body.addView(text("ปรับสีข้อมูลบนการ์ด รายละเอียดงาน และป้ายตำแหน่ง",14,false));
  OverlayThemePreview preview=new OverlayThemePreview(this);preview.setTag("overlay-theme-preview");body.addView(preview,new LinearLayout.LayoutParams(-1,dp(148)));
  String[] names={"พื้นหลังป้ายข้อมูล","ตัวเลขและข้อความข้อมูล","หัวข้อป้ายข้อมูล","ขอบป้ายและแผงข้อมูล","พื้นหลังป้ายตำแหน่ง","ข้อความพิกัด","ขอบป้ายตำแหน่ง"};
  for(int i=0;i<OverlayPalette.KEYS.length;i++){final String key=OverlayPalette.KEYS[i],name=names[i];int color=Store.prefs(this).getInt(key,OverlayPalette.fallback(key));action(body,name+"  "+String.format(java.util.Locale.ROOT,"#%08X",color),()->colorDialog(key,name));}
  action(body,"คืนค่าสีเริ่มต้น",()->{AlertDialog dialog=new AlertDialog.Builder(this).setTitle("คืนค่าสีการวาดทับ?").setMessage("คืนค่าสีป้ายและแผงข้อมูลทั้งหมด โดยไม่เปลี่ยนขนาดพื้นที่แตะ").setNegativeButton("ยกเลิก",null).setPositiveButton("คืนค่า",(d,w)->{var edit=Store.prefs(this).edit();for(String key:OverlayPalette.KEYS)edit.remove(key);edit.apply();ScreenActions.wake(this);render();}).show();AppTheme.styleDialog(this,dialog);});
 }
 private void colorDialog(String key,String name){
  LinearLayout content=column();content.setPadding(dp(20),dp(8),dp(20),dp(8));EditText hex=new EditText(this);hex.setSingleLine(true);hex.setTextColor(AppTheme.ink(this));hex.setTextSize(16);hex.setHint("#RRGGBB หรือ #AARRGGBB");hex.setText(String.format(java.util.Locale.ROOT,"#%08X",Store.prefs(this).getInt(key,OverlayPalette.fallback(key))));hex.setSelectAllOnFocus(true);content.addView(hex,new LinearLayout.LayoutParams(-1,dp(56)));
  GridLayout colors=new GridLayout(this);colors.setColumnCount(4);content.addView(colors,new LinearLayout.LayoutParams(-1,-2));int[] swatches={GozzaDesign.primary,GozzaDesign.primaryHighlight,GozzaDesign.textPrimary,0xFF000000,GozzaDesign.success,GozzaDesign.info,GozzaDesign.error,GozzaDesign.background,GozzaDesign.surface,GozzaDesign.elevated,0xFF8B5CF6,0x00000000};
  for(int color:swatches){TextView chip=text(color==0?"∅":"",18,true);chip.setGravity(Gravity.CENTER);chip.setContentDescription(String.format(java.util.Locale.ROOT,"เลือกสี #%08X",color));var bg=AppTheme.background(color,dp(8));bg.setStroke(dp(1),GozzaDesign.textSecondary);chip.setBackground(bg);GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);lp.width=0;lp.height=dp(48);lp.setMargins(dp(3),dp(3),dp(3),dp(3));colors.addView(chip,lp);chip.setOnClickListener(v->hex.setText(String.format(java.util.Locale.ROOT,"#%08X",color)));}
  ScrollView fields=new ScrollView(this);fields.addView(content);
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle(name).setView(fields).setNegativeButton("ยกเลิก",null).setPositiveButton("บันทึก",null).create();dialog.show();AppTheme.styleDialog(this,dialog);dialog.getButton(-1).setOnClickListener(v->{try{String input=hex.getText().toString().trim();if(!input.startsWith("#"))input="#"+input;if(!input.matches("#[a-fA-F0-9]{6}([a-fA-F0-9]{2})?"))throw new IllegalArgumentException();int color=android.graphics.Color.parseColor(input);Store.prefs(this).edit().putInt(key,color).apply();ScreenActions.wake(this);dialog.dismiss();render();}catch(IllegalArgumentException invalid){hex.setError("ใช้รหัสสี 6 หรือ 8 หลัก เช่น #FF7A18");}});
 }

 private void map(){startActivity(new Intent(this,io.github.phantom.gps.ui.MapActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));finish();}
 private void openDrawer(){if(drawer!=null)return;FrameLayout shade=new FrameLayout(this);shade.setBackgroundColor(0x99000000);frame.addView(shade,new FrameLayout.LayoutParams(-1,-1));drawer=shade;shade.setOnClickListener(v->closeDrawer());LinearLayout nav=column();nav.setPadding(dp(20),dp(48),dp(20),dp(24));nav.setBackgroundColor(AppTheme.surface(this));shade.addView(nav,new FrameLayout.LayoutParams(Math.min(dp(320),getResources().getDisplayMetrics().widthPixels-dp(36)),-1,Gravity.START));nav.setOnClickListener(v->{});TextView title=text("Gozza Lalamove",22,true);title.setTextColor(AppTheme.accent(this));nav.addView(title);gap(nav,24);action(nav,"แผนที่และ GPS",this::map);action(nav,"อ่านข้อมูลและวาดทับ",()->{permissions=false;theme=false;render();});action(nav,"ธีมการวาดทับ",()->{permissions=false;theme=true;render();});action(nav,"ตั้งค่า GPS",()->{closeDrawer();startActivity(new Intent(this,io.github.phantom.gps.ui.ActivitySettings.class));});action(nav,"สิทธิ์การใช้งาน",()->{permissions=true;theme=false;render();});}
 private void closeDrawer(){if(drawer!=null){frame.removeView(drawer);drawer=null;}}
 @Override public void onBackPressed(){if(drawer!=null){closeDrawer();return;}if(permissions||theme){permissions=false;theme=false;render();return;}map();}
}
