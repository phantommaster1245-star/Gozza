package com.fulllock.probe;
import android.content.Context;import android.net.Uri;import android.os.Bundle;import com.fulllock.transport.RelayEndpoint;
/** Experimental Binder-only transport: disconnecting control fails closed. */
final class BridgeTransport {
 private static volatile RelayEndpoint endpoint;private static long checked;private static boolean visible;
 static boolean active(){RelayEndpoint peer=endpoint;return peer!=null&&peer.connected();}
 static void sendGps(String id,double lat,double lng,String label,android.os.ResultReceiver callback)throws Exception{RelayEndpoint peer=endpoint;if(peer==null||!peer.connected())throw new android.os.RemoteException("No GPS relay");peer.sendGps(id,lat,lng,label,callback);}
 static boolean appVisible(Context c){long now=android.os.SystemClock.elapsedRealtime();if(now-checked>5000){checked=now;try{c.getPackageManager().getApplicationInfo("com.gozza.lalamove",0);visible=true;}catch(Exception missing){visible=false;}}return visible;}
 static Bundle call(Context c,String method,Bundle data)throws Exception{RelayEndpoint peer=endpoint;if(peer==null)synchronized(BridgeTransport.class){if(endpoint==null)endpoint=new RelayEndpoint(c,null);peer=endpoint;}
  if(peer.connected())try{return peer.call(method,data);}catch(Exception ignored){}
  throw new android.os.RemoteException("Local control session unavailable");
 }
}
