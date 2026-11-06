package com.fulllock.app;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import org.json.*;

/** Caller-UID restricted bridge. In-memory data only, no raw IDs or addresses. */
public final class DriverBridge extends ContentProvider {
 private static volatile String state="ยังไม่ได้รับข้อมูลจากโมดูล";
 private static volatile long updated;
 private static volatile String screen="ยังไม่มีข้อมูลโครงสร้างหน้าจอ";private static volatile long screenAt;
 private static volatile String screenMetadata="";
 static String screenMetadata(){return screenMetadata;}
 private static volatile long driverActiveUntil;
 private static volatile String distanceStatus="No diagnostic packet";
 static boolean driverForeground(){return SystemClock.elapsedRealtime()<driverActiveUntil;}
 static boolean viewsEnabled(Context c){return !RootSetup.updatePending&&RootMode.enabled(c)&&c.getSharedPreferences("root_research",0).getBoolean("screenViews",false);}
 static String screenSummary(){return SystemClock.elapsedRealtime()-screenAt<6000?screen:"พักอ่านโครงสร้างหน้าจอ / รอ Driver";}
 private static volatile int packets;
 private static volatile long probeAt;
 static boolean probeConnected(){return probeAt>0&&SystemClock.elapsedRealtime()-probeAt<5000;}
 private static final JobSignalCache cache=new JobSignalCache();
 private static volatile String signalHook="ยังไม่ยืนยันจุดรับงานใหม่",signalSource="";
 private static volatile int androidSignals;
 static void notificationSignal(android.os.Bundle extras){
  androidSignals++;changed();
  if(extras==null)return;
  for(String key:new String[]{"order_uuid","uuid"}){
   Object id=extras.get(key);if(!(id instanceof String)||((String)id).isEmpty()||((String)id).length()>128)continue;
   try{byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest(((String)id).getBytes(java.nio.charset.StandardCharsets.UTF_8));StringBuilder ref=new StringBuilder();for(int n=0;n<6;n++)ref.append(String.format(java.util.Locale.ROOT,"%02x",digest[n]&255));cache.signal(ref.toString(),SystemClock.elapsedRealtime());signalSource="android_notification";changed();}catch(Exception ignored){}break;
  }
 }

 public boolean onCreate(){return true;}
 public static String metrics(){return ScreenActions.metrics();}
 public static String freshness(){return packets==0?"ยังไม่มีแพ็กเก็ตงาน":"รับแล้ว "+packets+" ครั้ง • ล่าสุด "+new java.text.SimpleDateFormat("HH:mm:ss",java.util.Locale.ROOT).format(new java.util.Date(updated));}
 public static String summary(){
  long now=SystemClock.elapsedRealtime();
  StringBuilder view=new StringBuilder("เฝ้าดู Driver ตลอด • "+signalHook+"\nเหตุการณ์งานใหม่ "+cache.signals()+" • แจ้งเตือนระบบ "+androidSignals+"\n");
  String ref=cache.reference();
  if(!ref.isEmpty()){
   view.append("งานเข้าล่าสุด #").append(ref).append(" • ").append(signalSource).append("\n");
   view.append(cache.match(now)!=null?"จับคู่รายละเอียดแล้ว":cache.expired(now)?"ยังไม่ได้รายละเอียดของงานนี้ในเวลาที่กำหนด":"กำลังรอรายละเอียดของงานนี้").append("\n");
  }
  JSONArray visible=new JSONArray();for(String raw:cache.snapshots(now))try{visible.put(new JSONObject(raw));}catch(JSONException ignored){}
  if(visible.length()>0)try{
   view.append("\nข้อมูลจากการเฝ้าดูยังอัปเดตต่อเนื่อง\n");
   if(!ref.isEmpty()&&cache.match(now)==null)view.append("รายการด้านล่างยังไม่ใช่รายละเอียดที่จับคู่กับงานเข้าล่าสุด\n");
   view.append(formatJobs(visible,"ข้อมูลล่าสุดที่ Driver โหลดไว้"));
  }catch(JSONException ignored){}
  else view.append(packets==0?state:"ยังไม่มีข้อมูลงานสดใน 2 นาทีที่ผ่านมา • รอ Driver ส่งข้อมูล");
  return view.append("\nราคา: บาท • ระยะงาน: กม.").toString();
 }
 private static void changed(){JobListener.rootDataChanged();}
 @Override public Bundle call(String method,String arg,Bundle data){

  int uid=Binder.getCallingUid();boolean allowed=false;
  try{allowed=uid==getContext().getPackageManager().getApplicationInfo(JobListener.DRIVER,0).uid;}catch(Exception ignored){}
  if(!allowed)throw new SecurityException("Driver UID required");
  return localCall(getContext(),method,data);
 }
 static Bundle localCall(Context context,String method,Bundle data){
  if(!AppIntegrity.trusted(context))throw new SecurityException("Official Gozza Lalamove required");
  if("screen_paused".equals(method)){DetailActions.left();driverActiveUntil=0;changed();ScreenActions.paused();return Bundle.EMPTY;}
  if("screen_permit".equals(method))return ScreenActions.permit(context,data);
  if("screen_result".equals(method)){ScreenActions.result(context,data);return Bundle.EMPTY;}
  if("screen_config".equals(method)){driverActiveUntil=SystemClock.elapsedRealtime()+3000;changed();probeAt=SystemClock.elapsedRealtime();return ScreenActions.controls(context);}
  if(!"update".equals(method)||data==null)return Bundle.EMPTY;
  if("screen_nodes".equals(data.getString("state"))){
   if(!RootMode.enabled(context)||!context.getSharedPreferences("root_research",0).getBoolean("screenViews",false))return Bundle.EMPTY;
   String raw=data.getString("payload","");if(raw.length()>20000)return Bundle.EMPTY;
   try{JSONObject snapshot=new JSONObject(raw);long now=SystemClock.elapsedRealtime(),at=snapshot.getLong("observedElapsed");if(at>now||now-at>5000)return Bundle.EMPTY;
    distanceStatus="touchHook="+snapshot.optBoolean("pickupTouchHook")+" drawn="+snapshot.optInt("overlayCount")+" edges="+snapshot.optInt("edgeCards")+" nodes="+snapshot.optInt("nodeCount")+" pickup="+snapshot.optString("pickupDebug","")+" "+snapshot.optString("detailIds","")+" detail="+snapshot.optBoolean("detail")+" complete="+snapshot.optBoolean("detailComplete")+" parsed="+ScreenCardParser.parse(snapshot.getJSONArray("nodes")).size()+" probe="+snapshot.optInt("distanceProbeVersion")+" exact="+snapshot.optInt("distanceExactBindings")+" bindings="+snapshot.optInt("distanceBindings")+" values="+snapshot.optInt("distanceValues")+" visible="+snapshot.optInt("distanceVisible")+" appVisibleToDriver="+snapshot.optBoolean("appVisibleToDriver",true)+" relayActive="+snapshot.optBoolean("relayActive")+" economy="+snapshot.optInt("economyDistances")+"/"+snapshot.optInt("economyVisible");JSONArray nodes=snapshot.getJSONArray("nodes");StringBuilder meta=new StringBuilder();for(int j=0;j<nodes.length();j++){JSONObject n=nodes.getJSONObject(j);if(n.optString("kind").equals("metadata"))meta.append(n.optString("viewId")).append(" len=").append(n.optString("value")).append(" y=").append(n.optInt("top")).append("\n");}screenMetadata=meta.toString();StringBuilder display=new StringBuilder("อ่านโครงสร้างโดยตรง • ไม่ใช้ OCR\nข้อความบนจอ "+snapshot.optInt("textNodes")+" จุด • อ่านได้ "+nodes.length()+" ค่า");
    display=new StringBuilder(ScreenSnapshotFormatter.format(nodes,snapshot.optInt("textNodes")));
    driverActiveUntil=SystemClock.elapsedRealtime()+3000;screen=display.toString();screenAt=at;changed();return ScreenActions.snapshot(context,snapshot,at);
   }catch(JSONException ignored){}return Bundle.EMPTY;
  }
  if(!RootMode.enabled(context))return Bundle.EMPTY;
  String status=data.getString("state","");String raw=data.getString("payload","");if(raw.length()>20000)return Bundle.EMPTY;
  if("signal_ready".equals(status)){signalHook="จุดรับงานใหม่พร้อม";android.util.Log.i("FullLockSignals","signal_hooks_ready");changed();return Bundle.EMPTY;}
  if("signal_error".equals(status)){signalHook="จุดรับงานใหม่ยังไม่พร้อม";changed();return Bundle.EMPTY;}
  if("signal".equals(status)){
   try{JSONObject event=new JSONObject(raw);String ref=event.optString("reference",""),source=event.optString("source","");
    if(JobSignalCache.valid(ref)&&(source.equals("driver_push")||source.equals("driver_foreground_event"))){cache.signal(ref,SystemClock.elapsedRealtime());signalSource=source;signalHook="รับเหตุการณ์งานใหม่ได้แล้ว";android.util.Log.i("FullLockSignals","signal source="+source+" reference="+ref);}
   }catch(JSONException ignored){}changed();return Bundle.EMPTY;
  }
  if("ready".equals(status)){state="โมดูลเชื่อมแล้ว • รอรายการงาน";updated=System.currentTimeMillis();}
  else if("error".equals(status)){state="อ่านฟิลด์งานไม่สำเร็จ";updated=System.currentTimeMillis();}
  else if("jobs".equals(status))try{
   JSONObject root=new JSONObject(raw);JSONArray jobs=root.getJSONArray("jobs");String src=root.optString("source");
   if(!src.equals("request_list")&&!src.equals("order_detail")&&!src.equals("cache_recheck"))return Bundle.EMPTY;
   long now=SystemClock.elapsedRealtime(),observed=root.optLong("observedElapsed",now);
   if(observed<0||observed>now||now-observed>120000)return Bundle.EMPTY;
   for(int i=0;i<Math.min(10,jobs.length());i++){JSONObject job=jobs.optJSONObject(i);if(job!=null)cache.put(job.optString("reference",""),job.toString(),observed);}
   state=formatJobs(jobs,src);updated=Math.max(updated,System.currentTimeMillis()-(now-observed));packets++;android.util.Log.i("FullLockSignals","snapshot source="+src+" count="+jobs.length()+" driver_total="+root.optInt("totalJobs",-1)+" captured="+root.optInt("capturedJobs",-1)+" matching_signal="+(cache.match(SystemClock.elapsedRealtime())!=null));

  }catch(JSONException ignored){state="รูปแบบข้อมูลจากโมดูลไม่ถูกต้อง";}
  changed();return Bundle.EMPTY;
 }
 private static String formatJobs(JSONArray jobs,String src)throws JSONException{
   StringBuilder s=new StringBuilder("ได้รับงาน "+jobs.length()+" รายการ • "+src);
   for(int i=0;i<Math.min(10,jobs.length());i++){
    JSONObject job=jobs.getJSONObject(i);s.append("\n\nงาน ").append(i+1);
    String ref=job.optString("reference","");if(ref.matches("[a-f0-9]{12}"))s.append(" • #").append(ref);
    Object fare=job.opt("total_price_fen"),distance=job.opt("total_distance"),earning=job.opt("driver_total_earnings_fen_v2");
    try{if(fare instanceof Number)s.append("\nราคางานก่อนทิป: ").append(JobDisplayUnits.baht((Number)fare));}catch(IllegalArgumentException ignored){}
    try{if(distance instanceof Number)s.append("\nระยะงาน: ").append(JobDisplayUnits.kilometres((Number)distance));}catch(IllegalArgumentException ignored){}
    if(earning instanceof Number&&((Number)earning).doubleValue()>0){try{s.append("\nรายได้จาก Driver: ").append(JobDisplayUnits.baht((Number)earning));}catch(IllegalArgumentException ignored){}}
    else s.append("\nรายได้จาก Driver: ยังไม่มีข้อมูลยืนยัน");
    long orderTime=job.optLong("order_time",0);if(orderTime>1577836800L&&orderTime<4102444800L)s.append("\nเวลานัด: ").append(new java.text.SimpleDateFormat("dd/MM HH:mm",java.util.Locale.ROOT).format(new java.util.Date(orderTime*1000)));
    for(String[] pair:new String[][]{{"vehicle_name","รถ"},{"driver_price_display","ราคาที่ Driver ส่งให้แสดง"}}){String text=limited(job.optString(pair[0],""));if(!text.isEmpty())s.append("\n").append(pair[1]).append(": ").append(text);}
    JSONArray charges=job.optJSONArray("service_charges");if(charges!=null)for(int n=0;n<Math.min(8,charges.length());n++){JSONObject charge=charges.optJSONObject(n);if(charge==null)continue;Object amount=charge.opt("value_fen");if(amount instanceof Number){
      String descriptor=PriceComponentDiagnostics.describe(charge.optString("type",""),charge.optString("key",""),(Number)amount);
      if(!descriptor.isEmpty())s.append("\n").append(descriptor);
     }}
    if(job.optBoolean("address_masked",false))s.append("\nพื้นที่: Driver ปิดบังข้อมูล");
    JSONArray stops=job.optJSONArray("stops");if(stops!=null){
     for(int n=0;n<Math.min(32,stops.length());n++){JSONObject stop=stops.optJSONObject(n);if(stop==null)continue;
      String area=limited(stop.optString("area",""));if(area.isEmpty())continue;
      String type=stop.optString("type","");s.append("\n").append(type.equals("PICK_UP")?"ต้นทาง":type.equals("DROP_OFF")?"ปลายทาง":"จุดที่ "+(n+1)+" (ตามลำดับ Driver)").append(": ").append(area);
      if(!job.optBoolean("address_masked",false)){
       double lat=stop.optDouble("latitude",Double.NaN),lon=stop.optDouble("longitude",Double.NaN);
       String role=stop.optString("coordinate_role","");
       if(JobDisplayUnits.coordinates(lat,lon)&&(role.equals("pickup")||role.equals("first_stop")))
        s.append("\n").append(role.equals("pickup")?"พิกัดต้นทาง":"พิกัดจุดแรก (ต้นทางตามลำดับ)").append(": ").append(String.format(java.util.Locale.ROOT,"%.6f, %.6f",lat,lon));
      }
     }
    }
    JSONObject cargo=job.optJSONObject("cargo");if(cargo!=null){
     for(String[] pair:new String[][]{{"categories","สินค้า"},{"handling","การดูแล"}}){JSONArray a=cargo.optJSONArray(pair[0]);if(a!=null&&a.length()>0){s.append("\n").append(pair[1]).append(": ");for(int n=0;n<Math.min(5,a.length());n++){if(n>0)s.append(" / ");s.append(limited(a.optString(n,"")));}}}
     for(String[] pair:new String[][]{{"weight","น้ำหนัก"},{"quantity","จำนวน"},{"size","ขนาด"}}){String v=limited(cargo.optString(pair[0],""));if(!v.isEmpty())s.append("\n").append(pair[1]).append(": ").append(v);}
    }
    for(String key:new String[]{"to_pickup_distance","order_subset"}){
     Object v=job.opt(key);if(v instanceof Number)s.append("\n").append(key).append(": ").append(v);
    }
    for(String key:new String[]{"getSubset","getPlanType","getPickupType","getOrderFlowType","getPayType","getOrderPricingModel","getPricingModel"}){
     String v=job.optString(key,"");if(v.matches("[A-Z0-9_]{1,48}"))s.append("\n").append(key).append(": ").append(v);
    }
   }
   s.append("\n\nเป็นเหตุการณ์ล่าสุด ไม่รับรองว่าตรงกับการ์ดที่เปิดบนจอ");return s.toString();
 }
 @Override public void dump(java.io.FileDescriptor fd,java.io.PrintWriter writer,String[] args){
  if(BuildConfig.DEVICE_NOTIFICATION_CHECKS&&args!=null&&java.util.Arrays.stream(args).anyMatch(value->value.startsWith("device-notification-"))){int uid=Binder.getCallingUid();if(uid!=0&&uid!=1000&&uid!=2000&&uid!=android.os.Process.myUid()){writer.println("Denied");return;}try{Class.forName("com.fulllock.app.DeviceNotificationChecks").getMethod("run",Context.class,String[].class,java.io.PrintWriter.class).invoke(null,getContext(),args,writer);}catch(Exception error){writer.println("Check unavailable: "+error.getClass().getSimpleName());}return;}

  if(args!=null&&java.util.Arrays.asList(args).contains("distance-status")){writer.println(distanceStatus+" ageMs="+(screenAt==0?-1:SystemClock.elapsedRealtime()-screenAt)+" relay="+LocalRelayClient.status());return;}
  if(!BuildConfig.DEBUG){writer.println("Disabled");return;}
  if(args!=null&&java.util.Arrays.asList(args).contains("screen")){writer.println(screenSummary()+"\n"+metrics());return;}
  JSONArray rows=new JSONArray();long now=SystemClock.elapsedRealtime();
  for(String raw:cache.allSnapshots(now))try{
   JSONObject job=new JSONObject(raw),safe=new JSONObject();
   for(String key:new String[]{"reference","total_distance","total_price_fen","order_subset","getSubset","getPayType","getPickupType","getPlanType","getOrderFlowType","getOrderPricingModel","getPricingModel","order_time","vehicle_name","driver_price_display","total_stops","service_charges"})if(job.has(key))safe.put(key,job.get(key));
   JSONArray stops=job.optJSONArray("stops"),areas=new JSONArray();
   if(stops!=null)for(int i=0;i<Math.min(32,stops.length());i++){JSONObject stop=stops.optJSONObject(i);if(stop!=null)areas.put(new JSONObject().put("area",limited(stop.optString("area",""))).put("type",limited(stop.optString("type",""))));}
   safe.put("areas",areas);JSONObject cargo=job.optJSONObject("cargo");if(cargo!=null){JSONObject selected=new JSONObject();for(String key:new String[]{"categories","weight","quantity"})if(cargo.has(key))selected.put(key,cargo.get(key));safe.put("cargo",selected);}rows.put(safe);
  }catch(JSONException ignored){}
  writer.println(rows.toString());
 }
 private static String limited(String s){s=s.replaceAll("[\\p{Cntrl}]"," ").trim();return s.substring(0,Math.min(80,s.length()));}
 public Cursor query(Uri u,String[] p,String s,String[] a,String o){throw new UnsupportedOperationException();}
 public String getType(Uri u){return null;}
 public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
 public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
 public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
}
