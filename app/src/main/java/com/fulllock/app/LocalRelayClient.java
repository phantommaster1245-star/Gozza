package com.fulllock.app;
import android.content.Context;import android.os.*;import com.fulllock.transport.*;import java.util.concurrent.atomic.AtomicInteger;
/** Full Lock owns the control session and closes it when root reading stops. */
final class LocalRelayClient {
 static boolean connected(){RelayEndpoint peer=endpoint;return peer!=null&&peer.connected();}
 static void reconnect(Context c){if(!RootMode.enabled(c))return;retryRequested=true;start(c);}
 private static volatile boolean retryRequested;
 private static volatile java.lang.Process launcher;private static volatile Thread worker;private static volatile RelayEndpoint endpoint;private static volatile String status="not started";private static final AtomicInteger packets=new AtomicInteger();
 static String status(){RelayEndpoint peer=endpoint;return status+" "+(peer==null?"":peer.status())+" requests="+packets.get();}
 static synchronized void start(Context source){Context c=source.getApplicationContext();if(!"com.gozza.lalamove".equals(c.getPackageName())||!AppIntegrity.trusted(c)||(worker!=null&&worker.isAlive()))return;Thread thread=new Thread(()->run(c),"FullLock-local-relay");worker=thread;thread.start();}
 static synchronized void stop(){Thread old=worker;worker=null;if(old!=null)old.interrupt();RelayEndpoint peer=endpoint;endpoint=null;if(peer!=null)peer.close();status="closed";}
 private static void launch(Context c)throws Exception{java.lang.Process prior=launcher;if(prior!=null&&prior.isAlive())return;int driver=c.getPackageManager().getApplicationInfo(JobListener.DRIVER,0).uid;String apk="'"+c.getApplicationInfo().sourceDir.replace("'","'\\''")+"'";java.lang.Process process=new ProcessBuilder("su","-M","-c","CLASSPATH="+apk+" app_process /system/bin com.fulllock.app.LocalRelayMain "+android.os.Process.myUid()+" "+driver).redirectErrorStream(true).start();launcher=process;Thread drain=new Thread(()->{try(var input=process.getInputStream()){byte[] buffer=new byte[1024];while(input.read(buffer)!=-1){}}catch(Exception ignored){}},"relay-output");drain.setDaemon(true);drain.start();}
 private static void run(Context c){Thread self=Thread.currentThread();RelayEndpoint peer=new RelayEndpoint(c,(method,data)->{packets.incrementAndGet();return DriverBridge.localCall(c,method,data);});PhantomGpsConnection gps=new PhantomGpsConnection(c);endpoint=peer;long lastLaunch=0;status="waiting";
  try{while(worker==self&&RootMode.enabled(c)){try{long now=SystemClock.elapsedRealtime();if(peer.connected()){peer.ping();status="connected";try{gps.tick(peer);}catch(Exception ignored){/* GPS failure must not stop normal Driver reading. */}}else if(now-lastLaunch>10000||retryRequested){retryRequested=false;lastLaunch=now;status="starting";launch(c);}}catch(Exception e){status="waiting: "+e.getClass().getSimpleName();}Thread.sleep(1000);}}
  catch(InterruptedException ignored){self.interrupt();}finally{gps.close();peer.close();if(endpoint==peer)endpoint=null;}

 }
}
