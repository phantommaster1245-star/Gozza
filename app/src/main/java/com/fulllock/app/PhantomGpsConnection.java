package com.fulllock.app;

import android.content.*;
import android.os.*;
import com.fulllock.transport.*;

/** Bind/open/renew before taps. Only the root relay receives the private GPS capability. */
final class PhantomGpsConnection implements AutoCloseable {
 private final Context context;private final Handler main=new Handler(Looper.getMainLooper());private final Binder lifetime=new Binder();
 private volatile IBinder gateway;private IBinder session,publishedTo,sessionGateway;private volatile boolean bound,closed;private long renewed,lastBind;
 private final ServiceConnection connection=new ServiceConnection(){
  public void onServiceConnected(ComponentName name,IBinder binder){if(!closed)gateway=binder;}
  public void onServiceDisconnected(ComponentName name){gateway=null;}
  public void onBindingDied(ComponentName name){gateway=null;unbind();}
  public void onNullBinding(ComponentName name){gateway=null;unbind();}
 };
 PhantomGpsConnection(Context c){context=c.getApplicationContext();}
 void tick(RelayEndpoint relay)throws Exception{
  if(closed)return;
  if(!PickupCopyAccess.available(context)||!Store.prefs(context).getBoolean("pickupDirectSend",false)){clear(relay);unbind();return;}
  IBinder service=gateway;
  if(service==null||!service.isBinderAlive()){
   clear(relay);long now=SystemClock.elapsedRealtime();if(now-lastBind>=10000){lastBind=now;main.post(()->{if(closed)return;unbind();try{bound=context.bindService(new Intent().setComponent(new ComponentName(GpsWire.PACKAGE,GpsWire.SERVICE)),connection,Context.BIND_AUTO_CREATE);}catch(RuntimeException ignored){bound=false;}});}return;
  }
  try{
   if(sessionGateway!=service)clear(relay);
   if(session==null||!session.isBinderAlive()){
    Parcel data=Parcel.obtain(),reply=Parcel.obtain();try{data.writeInterfaceToken(GpsWire.GATEWAY);data.writeInt(1);data.writeStrongBinder(lifetime);if(!service.transact(GpsWire.OPEN,data,reply,0))throw new RemoteException("GPS version unsupported");reply.readException();session=reply.readStrongBinder();if(session==null)throw new RemoteException("Missing GPS session");sessionGateway=service;renewed=SystemClock.elapsedRealtime();publishedTo=null;}finally{data.recycle();reply.recycle();}
   }
   if(SystemClock.elapsedRealtime()-renewed>=5000){control(GpsWire.KEEP_ALIVE);renewed=SystemClock.elapsedRealtime();}
   // Refresh publication even after the local root relay restarts.
   if(relay.connected()){relay.gpsSession(session);publishedTo=session;}
  }catch(Exception error){clear(relay);throw error;}
 }
 private void control(int code)throws Exception{IBinder target=session;if(target==null)return;Parcel data=Parcel.obtain(),reply=Parcel.obtain();try{data.writeInterfaceToken(GpsWire.SESSION);if(!target.transact(code,data,reply,0))throw new RemoteException("GPS session closed");reply.readException();}finally{data.recycle();reply.recycle();}}
 private void clear(RelayEndpoint relay){try{if(publishedTo!=null&&relay.connected())relay.gpsSession(null);}catch(Exception ignored){}publishedTo=null;try{control(GpsWire.CLOSE);}catch(Exception ignored){}session=null;sessionGateway=null;}
 private void unbind(){Runnable action=()->{if(bound){bound=false;try{context.unbindService(connection);}catch(RuntimeException ignored){}}};if(Looper.myLooper()==Looper.getMainLooper())action.run();else main.post(action);}
 public void close(){closed=true;try{control(GpsWire.CLOSE);}catch(Exception ignored){}session=null;gateway=null;unbind();}
}
