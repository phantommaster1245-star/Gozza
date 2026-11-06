package com.fulllock.app;
import android.app.*;import android.content.*;import android.os.*;import java.io.*;import java.util.concurrent.*;
/** Root is requested interactively. Uses a framework CLI or a schema-checked, module-scoped root helper. */
final class RootSetup {
 static final int REQUIRED_PROBE_CODE=44;
 static boolean moduleOutdated(long code,boolean matchingSignature){return code<REQUIRED_PROBE_CODE||!matchingSignature;}
 static volatile boolean updatePending;private static boolean upgradeAttempted;
 static boolean needsUpgrade(Context c){return Store.prefs(c).getInt("probeUpgradeCheck",0)<BuildConfig.VERSION_CODE||Store.prefs(c).getBoolean("probeUpgradeIncomplete",false)||installedOutdated(c);}
 static boolean installedOutdated(Context c){try{var info=c.getPackageManager().getPackageInfo("com.gozza.lalamove",0);return moduleOutdated(android.os.Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode,c.getPackageManager().checkSignatures(c.getPackageName(),"com.gozza.lalamove")==android.content.pm.PackageManager.SIGNATURE_MATCH);}catch(Exception ignored){return true;}}
 static void afterUpgrade(MainActivity activity,Runnable repaint){
  if(BuildConfig.DEBUG||running||upgradeAttempted||activity.isFinishing()||activity.isDestroyed()||!RootMode.enabled(activity)||Updates.blocked(activity))return;
  updatePending=needsUpgrade(activity);if(!updatePending)return;upgradeAttempted=true;begin(activity,repaint,true);
 }
 private static volatile boolean running;static volatile String stage="IDLE",frameworkStatus="NOT_CHECKED";static volatile String lastMessage="";static volatile boolean lastInstalled,lastReused;private static boolean mountMaster;
 private static boolean framework(String output){frameworkStatus="CHECK_FAILED";for(String line:output.split("\n")){String value=line.trim();if(java.util.Set.of("CONFIGURED","MODULE_MISSING","APK_STALE","MODULE_DISABLED","SCOPE_MISSING","SCOPE_MISMATCH","CONFIG_NOT_READY","UNSUPPORTED","READ_FAILED","WRITE_FAILED").contains(value))frameworkStatus=value;}return frameworkStatus.equals("CONFIGURED");}
 private static String frameworkHint(){return switch(frameworkStatus){case "MODULE_MISSING"->"ไม่พบ Probe ในรายการโมดูลของ LSPosed";case "APK_STALE"->"LSPosed ยังอ้างอิงส่วนเสริมตัวเก่า กรุณาปิดแล้วเปิดโมดูล Probe ใน LSPosed";case "MODULE_DISABLED"->"กรุณาเปิดโมดูล Gozza Lalamove ใน LSPosed";case "SCOPE_MISSING"->"กรุณาเลือก Lalamove Driver และ System Framework ในขอบเขตของ Gozza Lalamove";case "SCOPE_MISMATCH"->"กรุณาเลือก Lalamove Driver และ System Framework ในขอบเขตของ Gozza Lalamove";case "WRITE_FAILED"->"บันทึกการตั้งค่า LSPosed ไม่สำเร็จ กรุณาตรวจสิทธิ์รูทแล้วลองใหม่";case "CONFIG_NOT_READY"->"เปิดโมดูล Gozza Lalamove และเลือก System Framework, Lalamove Driver และ Gozza Lalamove ใน LSPosed";case "UNSUPPORTED"->"ยังตรวจการตั้งค่าของ LSPosed รุ่นนี้ไม่ได้";default->"ยังยืนยันการตั้งค่า LSPosed ไม่ได้";};}
 private static byte[] hexBytes(String value){byte[] bytes=new byte[value.length()/2];for(int i=0;i<bytes.length;i++)bytes[i]=(byte)Integer.parseInt(value.substring(i*2,i*2+2),16);return bytes;}
 private static String shell(String script,int seconds)throws Exception{
  java.lang.Process p=new ProcessBuilder(mountMaster?new String[]{"su","-M","-c",script}:new String[]{"su","-c",script}).redirectErrorStream(true).start();
  ByteArrayOutputStream data=new ByteArrayOutputStream();Thread reader=new Thread(()->{try(InputStream in=p.getInputStream()){byte[] b=new byte[2048];int n;while((n=in.read(b))!=-1)if(data.size()<16384)data.write(b,0,Math.min(n,16384-data.size()));}catch(IOException ignored){}});reader.start();
  if(!p.waitFor(seconds,TimeUnit.SECONDS)){p.destroyForcibly();throw new IOException("หมดเวลาขอสิทธิ์รูท");}reader.join(1000);return data.toString("UTF-8");
 }
 static void begin(MainActivity activity,Runnable repaint){begin(activity,repaint,false);}
 private static void begin(MainActivity activity,Runnable repaint,boolean upgrade){if(running)return;running=true;frameworkStatus="NOT_CHECKED";lastMessage="";lastInstalled=false;lastReused=false;Context c=activity.getApplicationContext();updatePending=true;if(upgrade)Store.prefs(c).edit().putBoolean("probeUpgradeIncomplete",true).apply();if(upgrade){ScreenActions.stopAutomatic();LocalRelayClient.stop();}else RootMode.setEnabled(c,false);repaint.run();
  ProgressDialog progress=new ProgressDialog(activity);progress.setMessage(upgrade?"กำลังอัปเดตส่วนเสริม…":"กำลังตรวจโหมดเครื่องรูท…");progress.setCancelable(false);progress.show();AppTheme.styleDialog(activity,progress);
  new Thread(()->{String message;boolean success=false,manual=false,reused=false;try{
   if(android.os.Process.myUid()/100000!=0)throw new IOException("เปิดโหมดรูทจากผู้ใช้หลักของเครื่องเท่านั้น");
   stage="ROOT_PERMISSION";mountMaster=false;String id=shell("id",45);if(!id.contains("uid=0")){mountMaster=true;id=shell("id",15);}if(!id.contains("uid=0"))throw new IOException("ไม่ได้รับสิทธิ์รูท");
   stage="FRAMEWORK";if(!shell("test -d /data/adb/lspd && echo FRAMEWORK",8).contains("FRAMEWORK"))throw new IOException("ไม่พบ LSPosed กรุณาติดตั้งและเปิดใช้งานก่อน");
   if(!DriverBridge.probeConnected()&&!shell("pidof lspd || pidof org.lsposed.lspd",8).trim().matches("[0-9 ]+"))throw new IOException("LSPosed ยังไม่ทำงาน กรุณาเปิดใช้งานแล้วลองใหม่");
   String app="'"+c.getApplicationInfo().sourceDir.replace("'","'\\''")+"'";
   stage="CONFIGURE_MODULE";
   reused=framework(shell("CLASSPATH="+app+" app_process /system/bin com.fulllock.app.RootFrameworkConfig "+app+" --check",12));
   success=reused || framework(shell("CLASSPATH="+app+" app_process /system/bin com.fulllock.app.RootFrameworkConfig "+app,15));
   manual=!success;if(!success)AppIssues.record(c,"ROOT_SETUP",null,false);message=reused?"เปิดโหมดเครื่องรูทแล้ว":success?"เตรียมโหมดเครื่องรูทแล้ว ต้องการรีสตาร์ตเครื่องหรือไม่?":frameworkHint();
   stage="VERIFY_PROBE";if(success&&installedOutdated(c))throw new IOException("ตรวจสอบส่วนเสริมไม่ผ่าน กรุณาลองอีกครั้ง");
   if(success){stage="READY";updatePending=false;Store.prefs(c).edit().putInt("probeUpgradeCheck",BuildConfig.VERSION_CODE).putBoolean("probeUpgradeIncomplete",false).apply();c.getSharedPreferences("root_research",0).edit().putBoolean("screenViews",true).apply();RootMode.setEnabled(c,true);ScreenActions.wake(c);}
  }catch(Exception|LinkageError e){AppIssues.record(c,"ROOT_SETUP",e,false);message=e instanceof IOException?e.getMessage():"ขอสิทธิ์รูทไม่สำเร็จ กรุณาตรวจแอปจัดการรูท";if(!upgrade)RootMode.setEnabled(c,false);}
   if(upgrade&&success)message=reused?"ตรวจสอบส่วนเสริมแล้ว ใช้รุ่นล่าสุดอยู่":"อัปเดตส่วนเสริมแล้ว ต้องการรีสตาร์ตเครื่องเพื่อเริ่มใช้ส่วนเสริมใหม่หรือไม่?";
   if(!success&&!upgrade)updatePending=false;
   lastReused=reused;lastMessage=message;final String text=message;final boolean ok=success,open=manual,quick=reused;running=false;
   activity.runOnUiThread(()->{if(activity.isFinishing()||activity.isDestroyed())return;progress.dismiss();repaint.run();if(ok&&quick&&!upgrade){android.widget.Toast.makeText(activity,"เปิดโหมดเครื่องรูทแล้ว",android.widget.Toast.LENGTH_SHORT).show();return;}AlertDialog.Builder d=new AlertDialog.Builder(activity).setTitle(upgrade?"อัปเดตส่วนเสริม":ok?"โหมดเครื่องรูท":"เตรียมโหมดเครื่องรูท").setMessage(text);
    if(ok&&quick)d.setPositiveButton("ปิด",null);
    else if(ok)d.setPositiveButton("รีสตาร์ตเครื่อง",(v,w)->new Thread(()->{try{shell("reboot",8);}catch(Exception ignored){}},"root-reboot").start()).setNegativeButton("ภายหลัง",null);
    else if(upgrade)d.setPositiveButton("ลองอีกครั้ง",(v,w)->begin(activity,repaint,true)).setNegativeButton("ภายหลัง",null);
    else if(open)d.setPositiveButton("เปิด LSPosed",(v,w)->{try{activity.startActivity(new Intent("android.intent.action.MAIN").setClassName("com.android.shell","org.lsposed.manager.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(Exception ex){android.widget.Toast.makeText(activity,"เปิด LSPosed จากไอคอนบนเครื่อง",android.widget.Toast.LENGTH_SHORT).show();}}).setNegativeButton("ปิด",null);
    else d.setPositiveButton("ปิด",null);if(!ok)d.setNeutralButton("คัดลอก log",(v,w)->AppIssues.copy(activity));AppTheme.styleDialog(activity,d.show());});
  },"root-setup").start();
 }
}
