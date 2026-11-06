package com.fulllock.probe;
import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.os.*;
import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import org.json.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Scope-limited after-hook: does not change parameters, results or exceptions. */
public final class DriverProbe implements IXposedHookLoadPackage {
 private final AtomicBoolean installed=new AtomicBoolean();
 private Context context;
 private Handler worker;private DriverScreenProbe screenProbe;
 private static final String DRIVER="com.lalamove.global.driver.sea";
 public void handleLoadPackage(XC_LoadPackage.LoadPackageParam pkg)throws Throwable{
  if(!DRIVER.equals(pkg.packageName)||!DRIVER.equals(pkg.processName))return;
  XposedBridge.log("FullLockProbe: Driver process loaded");
  XposedHelpers.findAndHookMethod(Application.class,"attach",Context.class,new XC_MethodHook(){
   protected void afterHookedMethod(MethodHookParam p){
    context=((Context)p.args[0]).getApplicationContext();if(context==null)context=(Context)p.args[0];
    HandlerThread t=new HandlerThread("FullLockReadOnlyProbe");t.start();worker=new Handler(t.getLooper());
    screenProbe=new DriverScreenProbe(context);screenProbe.install();
    final ClassLoader loader=((Context)p.args[0]).getClassLoader();
    worker.post(new Runnable(){int tries;public void run(){
     if(installed.get())return;
     try{
      Class<?> repo=XposedHelpers.findClass("com.deliverysdk.global.driver.data.order.OrderRepositoryImpl",loader);
      if(installed.compareAndSet(false,true)){
       XposedBridge.hookAllMethods(repo,"oooo",new XC_MethodHook(){protected void afterHookedMethod(MethodHookParam p){
        if(p.hasThrowable()||p.args.length!=2||!(p.args[0] instanceof List))return;
        String src=String.valueOf(p.args[1]);if(src.equals("request_list"))screenProbe.dataChanged();if(!src.equals("request_list")&&!src.equals("order_detail"))return;
        try{
         List<?> original=(List<?>)p.args[0];if(src.equals("order_detail")&&original.size()==1)screenProbe.detailModel(original.get(0));int total=original.size();List<?> list=new ArrayList<>(original.subList(0,Math.min(60,total)));
         worker.post(()->{try{
         int captured=list.size();
         JSONArray jobs=new JSONArray();
         for(int i=0;i<captured;i++){
          Object job=list.get(i);JSONObject obj=readJob(job);remember(job,obj);jobs.put(obj);
          if(jobs.length()==2||i==captured-1){String payload=new JSONObject().put("source",src).put("totalJobs",total).put("capturedJobs",captured).put("observedElapsed",SystemClock.elapsedRealtime()).put("jobs",jobs).toString();worker.post(()->send("jobs",payload));jobs=new JSONArray();}
         }
         if(captured==0){String payload=new JSONObject().put("source",src).put("totalJobs",0).put("capturedJobs",0).put("observedElapsed",SystemClock.elapsedRealtime()).put("jobs",jobs).toString();worker.post(()->send("jobs",payload));}
         }catch(Throwable ignored){send("error","field_read_failed");}});

        }catch(Throwable ignored){worker.post(()->send("error","field_read_failed"));}
       }});
       XposedBridge.log("FullLockProbe: repository hook attached");
       screenProbe.installServices(loader);
       installSignals(loader);
       send("ready","hook_attached");
      }
     }catch(Throwable ignored){if(++tries<30)worker.postDelayed(this,1000);else send("error","hook_target_not_found");}
    }});
   }
  });
 }
 private static final class ObservedJob {
  final java.lang.ref.WeakReference<Object> object;String json;long at;
  ObservedJob(Object o,String j,long t){object=new java.lang.ref.WeakReference<>(o);json=j;at=t;}
 }
 private final java.util.LinkedHashMap<String,Long> lastSignalRead=new java.util.LinkedHashMap<>();
 private final java.util.LinkedHashMap<String,ObservedJob> loaded=new java.util.LinkedHashMap<>();
 private synchronized void remember(Object job,JSONObject data){String ref=data.optString("reference","");if(!ref.matches("[a-f0-9]{12}"))return;loaded.remove(ref);loaded.put(ref,new ObservedJob(job,data.toString(),SystemClock.elapsedRealtime()));while(loaded.size()>60)loaded.remove(loaded.keySet().iterator().next());}
 private void reread(String ref){
  try{
   ObservedJob entry;synchronized(this){entry=loaded.get(ref);}
   if(entry==null||SystemClock.elapsedRealtime()-entry.at>120000)return;
   Object job=entry.object.get();if(job==null)return;
   JSONObject fresh=readJob(job);String json=fresh.toString();long observed;
   synchronized(this){if(!json.equals(entry.json)){entry.json=json;entry.at=SystemClock.elapsedRealtime();}observed=entry.at;}
   send("jobs",new JSONObject().put("source","cache_recheck").put("observedElapsed",observed).put("jobs",new JSONArray().put(fresh)).toString());
  }catch(Throwable ignored){}
 }
 private JSONObject readJob(Object job)throws Exception{
  JSONObject obj=new JSONObject();
          Object uuid=safeCall(job,"getUuid");
          if(uuid!=null){byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest(uuid.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));StringBuilder id=new StringBuilder();for(int n=0;n<6;n++)id.append(String.format(java.util.Locale.ROOT,"%02x",digest[n]&255));obj.put("reference",id.toString());}
          String[][] fields={{"getTotalDistance","total_distance"},{"getToPickupDistance","to_pickup_distance"},{"getEstimatedTravelTime","estimated_travel_time"},{"getTotalPriceFen","total_price_fen"},{"getDriverTotalEarningFenV2","driver_total_earnings_fen_v2"},{"getOrderSubset","order_subset"},{"getOrderTime","order_time"}};
          for(String[] f:fields){Object value=safeCall(job,f[0]);if(value instanceof Number)obj.put(f[1],value);}
          for(String name:new String[]{"getSubset","getPlanType","getPickupType","getOrderFlowType","getPayType","getOrderPricingModel","getPricingModel"}){
           Object value=safeCall(job,name);if(value!=null)obj.put(name,value.toString().substring(0,Math.min(48,value.toString().length())));
          }
          String vehicle=clean(safeCall(job,"getVehicleTypeName"));if(!vehicle.isEmpty())obj.put("vehicle_name",vehicle);
          String display=clean(safeCall(job,"getDriverPriceDisplay"));if(display.matches("[0-9฿., +\\-]{1,80}"))obj.put("driver_price_display",display);
          JSONArray charges=new JSONArray();Object services=safeCall(job,"getSpecialServicePriceItemList");
          if(services instanceof List){List<?> list=(List<?>)services;for(int j=0;j<Math.min(8,list.size());j++){
           Object service=list.get(j),amount=safeCall(service,"getValueFen");if(!(amount instanceof Number))continue;
           JSONObject charge=new JSONObject().put("value_fen",amount);
           for(String[] f:new String[][]{{"getType","type"},{"getKeyName","key"}}){String v=clean(safeCall(service,f[0]));if(v.matches("[A-Za-z0-9_-]{1,80}"))charge.put(f[1],v);}
           charges.put(charge);
          }}obj.put("service_charges",charges);
          Object masked=safeCall(job,"getMaskedAddress");obj.put("address_masked",Boolean.TRUE.equals(masked));
          Object addresses=safeCall(job,"getAddrInfo");JSONArray stops=new JSONArray();
          if(!Boolean.TRUE.equals(masked)&&addresses instanceof List){List<?> aa=(List<?>)addresses;
           obj.put("total_stops",aa.size());
           for(int n=0;n<Math.min(32,aa.size());n++){
            int actual=n==31&&aa.size()>32?aa.size()-1:n;
            Object address=aa.get(actual);String area=clean(safeCall(address,"getDistrictName"));
            if(area.isEmpty())area="ไม่ระบุพื้นที่";
            String type=clean(safeCall(address,"getAddressType"));
            JSONObject stop=new JSONObject().put("area",area).put("type",type).put("index",actual);Object node=safeCall(address,"getNode");if(node instanceof Number)stop.put("node",node);
            if(n==0||type.equals("PICK_UP")){
             Object coordinates=safeCall(address,"getLatLng");Object lat=safeCall(coordinates,"getLatitude"),lon=safeCall(coordinates,"getLongitude");
             if(lat instanceof Number&&lon instanceof Number){double la=((Number)lat).doubleValue(),lo=((Number)lon).doubleValue();
              if(Double.isFinite(la)&&Double.isFinite(lo)&&Math.abs(la)<=90&&Math.abs(lo)<=180&&(la!=0||lo!=0)){
               stop.put("latitude",la).put("longitude",lo).put("coordinate_role",type.equals("PICK_UP")?"pickup":"first_stop");
              }
             }
            }
            stops.put(stop);
           }
          }
          obj.put("stops",stops);
          Object item=safeCall(job,"getItemInfo");
          if(item!=null){JSONObject cargo=new JSONObject();
           cargo.put("categories",texts(safeCall(item,"getCategories")));
           cargo.put("handling",texts(safeCall(item,"getHandling")));
           for(String[] f:new String[][]{{"getWeight","weight"},{"getQuantity","quantity"},{"getTotalSize","size"}}){String value=clean(safeCall(item,f[0]));if(!value.isEmpty())cargo.put(f[1],value);}
           obj.put("cargo",cargo);
          }
  return obj;
 }
 private void installSignals(ClassLoader loader){
  try{
   Class<?> handler=XposedHelpers.findClass("com.deliverysdk.common_android.push.handler.OrderNewHandler",loader);
   XposedBridge.hookAllMethods(handler,"OOOO",new XC_MethodHook(){protected void afterHookedMethod(MethodHookParam p){
    if(p.hasThrowable()||p.args.length!=3)return;
    Object data=safeCall(p.args[0],"getData");
    if(data==null||!data.getClass().getName().equals("com.deliverysdk.common_android.push.data.OrderNew"))return;
    signal(safeCall(data,"getUuid"),"driver_push");
   }});
   Class<?> bus=XposedHelpers.findClass("org.simple.eventbus.EventBus",loader);
   XposedBridge.hookAllMethods(bus,"post",new XC_MethodHook(){protected void afterHookedMethod(MethodHookParam p){
    if(p.hasThrowable()||p.args.length!=2||!"push_order_new".equals(p.args[1]))return;
    Object event=p.args[0];if(event==null||!event.getClass().getName().equals("O000oo.OOOO0"))return;
    signal(safeCall(event,"OOO0"),"driver_foreground_event");
   }});
   send("signal_ready","order_new_hooks_attached");
  }catch(Throwable e){send("signal_error","signal_hook_unavailable");}
 }
 private void signal(Object uuid,String source){
  screenProbe.dataChanged();
  if(!(uuid instanceof String)||((String)uuid).isEmpty()||((String)uuid).length()>128)return;
  try{
   byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest(((String)uuid).getBytes(java.nio.charset.StandardCharsets.UTF_8));
   StringBuilder ref=new StringBuilder();for(int n=0;n<6;n++)ref.append(String.format(java.util.Locale.ROOT,"%02x",digest[n]&255));
   String payload=new JSONObject().put("reference",ref.toString()).put("source",source).toString();
   String reference=ref.toString();
   worker.post(()->{
    send("signal",payload);
    long now=SystemClock.elapsedRealtime();Long previous=lastSignalRead.get(reference);
    if(previous!=null&&now-previous<2000)return;
    lastSignalRead.remove(reference);lastSignalRead.put(reference,now);while(lastSignalRead.size()>60)lastSignalRead.remove(lastSignalRead.keySet().iterator().next());
    reread(reference);worker.postDelayed(()->reread(reference),500);worker.postDelayed(()->reread(reference),1500);
   });
  }catch(Throwable ignored){}
 }
 private static Object safeCall(Object target,String name){if(target==null)return null;try{return XposedHelpers.callMethod(target,name);}catch(Throwable ignored){return null;}}
 private static String clean(Object value){if(value==null)return "";String s=value.toString().replaceAll("[\\p{Cntrl}]"," ").trim();return s.substring(0,Math.min(80,s.length()));}
 private static JSONArray texts(Object value){JSONArray a=new JSONArray();if(value instanceof List){List<?> list=(List<?>)value;for(int i=0;i<Math.min(5,list.size());i++){Object item=list.get(i);if(item instanceof String){String s=clean(item);if(!s.isEmpty())a.put(s);}}}return a;}
 private void send(String state,String payload){try{
  Bundle b=new Bundle();b.putString("state",state);b.putString("payload",payload);
  BridgeTransport.call(context,"update",b);
 }catch(Throwable e){XposedBridge.log("FullLockProbe: bridge failed "+e.getClass().getSimpleName());}}
}
