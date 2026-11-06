package com.fulllock.transport;

import android.content.*;
import android.os.*;
import java.util.UUID;
import org.json.*;

/** Root-authenticated Binder bootstrap; no package lookup is made by a client. */
public final class RelayEndpoint {
 public static final String ACTION="com.gozza.LOCAL_RELAY_READY",TOKEN="com.gozza.LocalRelay.v1";
 public static final int HELLO=1,CALL=2,PING=3,STOP=4,GPS_CONFIG=5,GPS_SEND=6,PROVE=10,DISPATCH=11;
 public interface Dispatcher {Bundle call(String method,Bundle data)throws Exception;}
 private final Context context;private final Dispatcher dispatcher;
 private volatile IBinder remote;private volatile String state="waiting";
 private final BroadcastReceiver receiver=new BroadcastReceiver(){public void onReceive(Context c,Intent intent){if(intent.getExtras()==null)return;IBinder candidate=intent.getExtras().getBinder("relay");if(candidate==null)return;new Thread(()->handshake(candidate),"relay-handshake").start();}};
 @android.annotation.SuppressLint("UnspecifiedRegisterReceiverFlag") // API 33+ uses RECEIVER_EXPORTED; older APIs use the permission overload.
 public RelayEndpoint(Context context,Dispatcher dispatcher){this.context=context;this.dispatcher=dispatcher;IntentFilter filter=new IntentFilter(ACTION);if(Build.VERSION.SDK_INT>=33)context.registerReceiver(receiver,filter,"android.permission.DUMP",null,Context.RECEIVER_EXPORTED);else context.registerReceiver(receiver,filter,"android.permission.DUMP",null);}
 public boolean connected(){IBinder binder=remote;return binder!=null&&binder.isBinderAlive();}
 public String status(){return state;}
 private void handshake(IBinder candidate){if(remote==candidate&&connected())return;String challenge=UUID.randomUUID().toString();java.util.concurrent.atomic.AtomicBoolean proved=new java.util.concurrent.atomic.AtomicBoolean();
  Binder callback=new Binder(){protected boolean onTransact(int code,Parcel data,Parcel reply,int flags)throws RemoteException{
   if(Binder.getCallingUid()!=0)throw new SecurityException("Root relay required");data.enforceInterface(TOKEN);
   if(code==PROVE){if(!challenge.equals(data.readString()))throw new SecurityException("Challenge");proved.set(true);reply.writeNoException();return true;}
   if(code==DISPATCH&&proved.get()&&dispatcher!=null){try{String method=data.readString(),encoded=data.readString();if(encoded==null||encoded.length()>LocalFrames.MAX)throw new IllegalArgumentException("Frame size");Bundle result=dispatcher.call(method,LocalFrames.decode(new JSONArray(encoded)));String text=LocalFrames.encode(result).toString();if(text.length()>LocalFrames.MAX)throw new IllegalArgumentException("Frame size");reply.writeNoException();reply.writeString(text);return true;}catch(Exception e){throw new SecurityException("Invalid relay message");}}
   return false;
  }};
  Parcel data=Parcel.obtain(),reply=Parcel.obtain();try{data.writeInterfaceToken(TOKEN);data.writeStrongBinder(callback);data.writeString(challenge);if(!candidate.transact(HELLO,data,reply,0))throw new SecurityException("Handshake");reply.readException();if(!proved.get())throw new SecurityException("Unverified peer");remote=candidate;state="connected";candidate.linkToDeath(()->{if(remote==candidate){remote=null;state="disconnected";}},0);}catch(Exception e){state="handshake: "+e.getClass().getSimpleName();}finally{data.recycle();reply.recycle();}
 }
 public Bundle call(String method,Bundle bundle)throws Exception{IBinder binder=remote;if(binder==null)throw new RemoteException("No relay");Parcel data=Parcel.obtain(),reply=Parcel.obtain();try{data.writeInterfaceToken(TOKEN);data.writeString(method);String text=LocalFrames.encode(bundle).toString();if(text.length()>LocalFrames.MAX)throw new IllegalArgumentException("Frame size");data.writeString(text);if(!binder.transact(CALL,data,reply,0))throw new RemoteException("No reply");reply.readException();String response=reply.readString();if(response==null||response.length()>LocalFrames.MAX)throw new RemoteException("Invalid response");return LocalFrames.decode(new JSONArray(response));}catch(Exception e){if(!binder.isBinderAlive()&&remote==binder)remote=null;state="call: "+e.getClass().getSimpleName();throw e;}finally{data.recycle();reply.recycle();}}
 public void ping()throws Exception{control(PING);}
 public void gpsSession(IBinder session)throws Exception{IBinder binder=remote;if(binder==null)throw new RemoteException("No relay");Parcel data=Parcel.obtain(),reply=Parcel.obtain();try{data.writeInterfaceToken(TOKEN);data.writeStrongBinder(session);if(!binder.transact(GPS_CONFIG,data,reply,0))throw new RemoteException("Relay upgrade required");reply.readException();}finally{data.recycle();reply.recycle();}}
 public void sendGps(String id,double lat,double lng,String label,ResultReceiver callback)throws Exception{if(!GpsWire.valid(id,lat,lng,label)||callback==null)throw new IllegalArgumentException("Invalid GPS request");IBinder binder=remote;if(binder==null)throw new RemoteException("No relay");Parcel data=Parcel.obtain(),reply=Parcel.obtain();try{data.writeInterfaceToken(TOKEN);data.writeString(id);data.writeDouble(lat);data.writeDouble(lng);data.writeString(label);callback.writeToParcel(data,0);if(!binder.transact(GPS_SEND,data,reply,0))throw new RemoteException("Relay upgrade required");reply.readException();}finally{data.recycle();reply.recycle();}}
 private void control(int code)throws Exception{IBinder binder=remote;if(binder==null)return;Parcel data=Parcel.obtain(),reply=Parcel.obtain();try{data.writeInterfaceToken(TOKEN);binder.transact(code,data,reply,0);reply.readException();}catch(Exception e){if(remote==binder){remote=null;state="disconnected";}throw e;}finally{data.recycle();reply.recycle();}}
 public void close(){try{if(dispatcher!=null)control(STOP);}catch(Exception ignored){}remote=null;try{context.unregisterReceiver(receiver);}catch(Exception ignored){}state="closed";}
}
