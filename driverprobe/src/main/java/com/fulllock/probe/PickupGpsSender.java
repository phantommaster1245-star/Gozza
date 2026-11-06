package com.fulllock.probe;

import android.content.Context;
import android.os.*;
import android.widget.Toast;
import com.fulllock.app.PickupCoordinates;
import com.fulllock.app.GeoXFeedback;
import com.fulllock.transport.GpsWire;
import java.util.*;
import java.util.concurrent.*;

/** Independent from screen polling: no clipboard writes, retries or gesture injection. */
final class PickupGpsSender {
 private static final Handler main=new Handler(Looper.getMainLooper());
 private static final ThreadPoolExecutor worker=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(8),r->{Thread t=new Thread(r,"FullLock-pickup-GPS");t.setDaemon(true);return t;});
 private static final Map<String,ResultReceiver> pending=new HashMap<>();
 private static String latest="";
 static void send(Context source,PickupCoordinates.Point point,String label){
  Context context=source.getApplicationContext();String id=UUID.randomUUID().toString();latest=id;
  double lat,lng;try{lat=Double.parseDouble(point.latitude());lng=Double.parseDouble(point.longitude());}catch(RuntimeException error){message(context,"ส่งพิกัดไม่ได้");return;}
  String name=label==null?"":label.substring(0,Math.min(160,label.length()));
  if(!GpsWire.valid(id,lat,lng,name)){message(context,"ส่งพิกัดไม่ได้");return;}
  ResultReceiver local=new ResultReceiver(main){protected void onReceiveResult(int code,Bundle data){if(data==null||!id.equals(data.getString("request_id"))||pending.remove(id)==null)return;if(!id.equals(latest))return;message(context,GeoXFeedback.result(code,data.getBoolean("applied")));}};
  pending.put(id,local);Parcel parcel=Parcel.obtain();ResultReceiver platform;try{local.writeToParcel(parcel,0);parcel.setDataPosition(0);platform=ResultReceiver.CREATOR.createFromParcel(parcel);}finally{parcel.recycle();}
  try{worker.execute(()->{try{BridgeTransport.sendGps(id,lat,lng,name,platform);}catch(Exception error){main.post(()->{if(pending.remove(id)!=null&&id.equals(latest))message(context,GeoXFeedback.connectionFailed());});}});}catch(RejectedExecutionException busy){pending.remove(id);message(context,"กรุณาลองอีกครั้ง");return;}
  main.postDelayed(()->{if(pending.remove(id)!=null&&id.equals(latest))message(context,"ยังยืนยันการตั้งพิกัดไม่ได้");},6000);
 }
 private static void message(Context c,String text){Toast.makeText(c,text,Toast.LENGTH_SHORT).show();}
 private PickupGpsSender(){}
}
