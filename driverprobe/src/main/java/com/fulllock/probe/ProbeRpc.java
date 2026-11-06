package com.fulllock.probe;
import android.content.Context;import android.os.*;import java.util.concurrent.*;import java.util.concurrent.atomic.AtomicBoolean;
/** One in-flight IPC, no queued scan backlog and no Binder waits on Driver's UI thread. */
final class ProbeRpc {
 interface Reply {void receive(Bundle result,Exception error);}
 private static final ExecutorService worker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"FullLockProbeIPC");t.setDaemon(true);return t;});
 private static final AtomicBoolean busy=new AtomicBoolean();private static final Handler main=new Handler(Looper.getMainLooper());
 static boolean idle(){return !busy.get();}
 static boolean request(Context c,String method,Bundle data,Reply reply){Bundle copy=data==null?null:new Bundle(data);return submit(()->BridgeTransport.call(c,method,copy),reply);}
 static boolean submit(Callable<Bundle> task,Reply reply){if(!busy.compareAndSet(false,true))return false;worker.execute(()->{Bundle out=null;Exception failure=null;try{out=task.call();}catch(Exception error){failure=error;}Bundle result=out;Exception error=failure;main.post(()->{busy.set(false);try{reply.receive(result,error);}catch(Throwable ignored){android.util.Log.w("FullLockProbe","reply callback dropped");}});});return true;}
}
