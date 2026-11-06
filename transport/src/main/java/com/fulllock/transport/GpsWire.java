package com.fulllock.transport;

import android.os.*;

/** Numeric-only IPC contract; no GPS implementation or clipboard dependency. */
public final class GpsWire {
 public static final String PACKAGE="com.gozza.lalamove",SERVICE="io.github.phantom.gps.utils.FullLockRelayService";
 public static final String GATEWAY="io.github.phantom.gps.FullLockRelay.v1",SESSION="io.github.phantom.gps.LocationSession.v1";
 public static final int OPEN=1,SUBMIT=1,KEEP_ALIVE=2,CLOSE=3;
 public static boolean valid(String id,double lat,double lng,String label){return id!=null&&id.matches("[A-Za-z0-9_.-]{1,96}")&&Double.isFinite(lat)&&Double.isFinite(lng)&&lat>=-90&&lat<=90&&lng>=-180&&lng<=180&&label!=null&&label.length()<=160;}
 public static void submit(IBinder session,String id,double lat,double lng,String label,ResultReceiver callback)throws RemoteException{
  if(!valid(id,lat,lng,label)||callback==null)throw new IllegalArgumentException("Invalid location");
  Parcel data=Parcel.obtain(),reply=Parcel.obtain();try{data.writeInterfaceToken(SESSION);data.writeInt(1);data.writeString(id);data.writeDouble(lat);data.writeDouble(lng);data.writeString(label);data.writeString("");callback.writeToParcel(data,0);if(!session.transact(SUBMIT,data,reply,0))throw new RemoteException("GPS protocol unavailable");reply.readException();}finally{data.recycle();reply.recycle();}
 }
 private GpsWire(){}
}
