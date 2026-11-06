package com.fulllock.app;

import android.app.*;
import android.content.*;
import android.os.*;
import android.service.notification.*;
import java.util.*;

public class JobListener extends NotificationListenerService {
    static volatile boolean connected;
    private static volatile JobListener instance;
    private FloatingJobView floating;
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener rootChanges=(p,key)->{if(floating!=null){updateWatcher();syncFloating();ScreenActions.wake(this);}};
    private ForegroundWatcher foregroundWatcher;
    private boolean wasDriverForeground;
    private final Handler updateHandler=new Handler(Looper.getMainLooper());
    private final Runnable updateCheck=new Runnable(){public void run(){if(!connected)return;Updates.check(JobListener.this);updateHandler.postDelayed(this,6*60*60*1000L);}};
    private boolean trimQueued;
    private final Runnable trimTask=()->{trimQueued=false;trimJobNotifications();};
    void dismissOldJob(String key){cancelNotification(key);}
    private void scheduleTrim(){if(!connected||trimQueued)return;trimQueued=true;updateHandler.postDelayed(trimTask,200);}
    private NotificationRetention.Item retentionItem(StatusBarNotification sbn){if(sbn==null||!DRIVER.equals(sbn.getPackageName()))return null;Notification n=sbn.getNotification();if(n==null||n.extras==null)return null;boolean ongoing=(n.flags&(Notification.FLAG_ONGOING_EVENT|Notification.FLAG_FOREGROUND_SERVICE))!=0,summary=(n.flags&Notification.FLAG_GROUP_SUMMARY)!=0;String title=String.valueOf(n.extras.getCharSequence(Notification.EXTRA_TITLE,"")),regular=String.valueOf(n.extras.getCharSequence(Notification.EXTRA_TEXT,""));CharSequence big=n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT);String text=big==null||big.toString().isBlank()?regular:big.toString();NotificationRetention.Item i=new NotificationRetention.Item(sbn.getPackageName(),sbn.getKey(),sbn.getPostTime(),title,text,ongoing,summary,sbn.isClearable());if(!NotificationRetention.eligible(i)&&!text.equals(regular))i=new NotificationRetention.Item(i.pkg(),i.key(),i.time(),title,regular,ongoing,summary,sbn.isClearable());return i;}
    private void trimJobNotifications(){if(!connected||!AppIntegrity.trusted(this))return;try{StatusBarNotification[] active=getActiveNotifications();if(active==null)return;List<NotificationRetention.Item> items=new ArrayList<>();for(StatusBarNotification sbn:active){var item=retentionItem(sbn);if(item!=null)items.add(item);}for(var victim:NotificationRetention.victims(items,NotificationOpener.protectedKey(),NotificationOpener.protectedTime())){if(NotificationOpener.protects(victim.key(),victim.time()))continue;StatusBarNotification[] latest=getActiveNotifications();if(latest==null)return;boolean same=false;for(StatusBarNotification current:latest){var item=retentionItem(current);if(item!=null&&item.key().equals(victim.key())&&item.time()==victim.time()&&item.title().equals(victim.title())&&item.text().equals(victim.text())&&NotificationRetention.eligible(item)){same=true;break;}}if(!same||NotificationOpener.protects(victim.key(),victim.time()))continue;if(!Objects.equals(seen.get(victim.key()),victim.title()+"\n"+victim.text()))Store.log(this,victim.title(),victim.text(),false,"จัดเก็บแจ้งเตือนเก่า",victim.key(),victim.time());dismissOldJob(victim.key());}}catch(SecurityException|IllegalStateException ignored){}}
    private final Set<String> screenSeen=new LinkedHashSet<>();
    private final Map<String,PendingIntent> jobActions=new LinkedHashMap<>();
    private final Map<String,String> seen = new LinkedHashMap<>();
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener changes=(p,key)->{
        if(!key.equals("history"))ScreenActions.wake(this);
        if("enabled".equals(key)&&!p.getBoolean("enabled",false)){ScreenActions.stopAutomatic();NotificationOpener.cancel(this);}
        if("floatingEnabled".equals(key)) { if(p.getBoolean(key,true))floating.restore();updateWatcher();syncFloating(); }
        if("transitionMs".equals(key))floating.refreshMotion();
        if("showUnavailableFloating".equals(key))floating.refreshTheme();
        if("themeColor".equals(key)||"invertText".equals(key)||"fee".equals(key))floating.refreshTheme();
    };
    static final String DRIVER = "com.lalamove.global.driver.sea";
    @Override public void onCreate() {
        super.onCreate();if(!AppIntegrity.trusted(this)){stopSelf();return;}instance=this;
        floating=new FloatingJobView(this,this::openFloatingJob);
        foregroundWatcher=new ForegroundWatcher(this,this::syncFloating);
        Store.prefs(this).registerOnSharedPreferenceChangeListener(changes);
        getSharedPreferences("root_research",0).registerOnSharedPreferenceChangeListener(rootChanges);
    }
    static void screenJob(String token,long wall,JobParser.Job job,boolean matches,String extra,java.math.BigDecimal reduction){JobListener current=instance;if(current==null)return;boolean merge=ScreenActions.uniqueForMerge(job);current.updateHandler.post(()->{if(current.floating==null)return;FloatingJobs.Item item=new FloatingJobs.Item("screen:"+token,wall,job.title(),job.text(),job,true,false,extra);item.commissionReduction=reduction;item.allowMerge=merge;if(!current.screenSeen.add(token)){current.floating.updateScreen(item);return;}while(current.screenSeen.size()>80)current.screenSeen.remove(current.screenSeen.iterator().next());current.floating.offer(item);});}
    static void screenOpenFailed(String key){JobListener c=instance;if(c!=null)c.updateHandler.post(()->c.floating.failedScreen(key));}
    static void rootDataChanged(){JobListener c=instance;if(c!=null)c.updateHandler.post(c::syncFloating);}
    static void repairServices(){JobListener c=instance;if(c!=null)c.updateHandler.post(()->{c.updateWatcher();c.syncFloating();});}
    static void refreshFloating() { if(instance!=null)instance.syncFloating(); }
    static void restoreFloating(){if(instance!=null&&instance.floating!=null){instance.floating.restore();instance.syncFloating();}}
    static boolean floatingVisible(){return instance!=null && instance.floating!=null && instance.floating.visible();}
    private void syncFloating() {
        if(floating!=null)floating.hide();
    }

    private void updateWatcher(){if(connected && (Store.prefs(this).getBoolean("floatingEnabled",true)||DriverBridge.viewsEnabled(this))){foregroundWatcher.refresh();foregroundWatcher.start();}else foregroundWatcher.stop();}
    @Override public void onListenerConnected() { if(floating==null)return;connected=true;BetaJobExtras.probeActive(this,getActiveNotifications());scheduleTrim();updateWatcher();syncFloating();updateHandler.removeCallbacks(updateCheck);updateHandler.post(updateCheck); }
    @Override public void onListenerDisconnected() { connected=false;NotificationOpener.cancel(this);updateHandler.removeCallbacks(trimTask);trimQueued=false;updateHandler.removeCallbacks(updateCheck);if(foregroundWatcher!=null)foregroundWatcher.stop();if(floating!=null)floating.hide(); }
    @Override public void onDestroy() {
        getSharedPreferences("root_research",0).unregisterOnSharedPreferenceChangeListener(rootChanges);
        connected=false;updateHandler.removeCallbacks(trimTask);trimQueued=false;updateHandler.removeCallbacks(updateCheck);Store.prefs(this).unregisterOnSharedPreferenceChangeListener(changes);
        NotificationOpener.cancel(this);if(foregroundWatcher!=null)foregroundWatcher.stop();if(floating!=null)floating.hide();if(instance==this)instance=null;super.onDestroy();
    }
    @Override public void onNotificationRemoved(StatusBarNotification sbn) {
        if(floating==null)return;
        seen.remove(sbn.getKey());
        jobActions.keySet().removeIf(key->key.startsWith(sbn.getKey()+"\n"+sbn.getPostTime()+"\n"));
        floating.removed(sbn.getKey(),sbn.getPostTime());
    }
    private void record(StatusBarNotification notification,String title,String text,boolean worked,String reason) {
        Store.log(this,title,text,worked,reason,notification.getKey(),notification.getPostTime());
    }
    private void openFloatingJob(FloatingJobs.Item item) {}
    private static String actionKey(String key,long time,String title,String text){return key+"\n"+time+"\n"+title+"\n"+text;}
    static boolean openHistory(org.json.JSONObject entry){return false;}
    private boolean openHistory(org.json.JSONObject entry,StatusBarNotification[] active){return false;}
    private void toast(String message){android.widget.Toast.makeText(this,message,android.widget.Toast.LENGTH_SHORT).show();}
    private void sendOpen(PendingIntent intent) throws PendingIntent.CanceledException {NotificationOpener.cancel(this);NotificationOpener.send(this,intent);}
    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        if(floating==null||Updates.blocked(this))return;
        if (!DRIVER.equals(sbn.getPackageName())) return;
        Notification n = sbn.getNotification();
        boolean ongoing=(n.flags & Notification.FLAG_ONGOING_EVENT)!=0, summary=(n.flags & Notification.FLAG_GROUP_SUMMARY)!=0;
        if(ongoing || summary)return;
        String title = String.valueOf(n.extras.getCharSequence(Notification.EXTRA_TITLE,""));
        String regular = String.valueOf(n.extras.getCharSequence(Notification.EXTRA_TEXT,""));
        CharSequence big = n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        String text = big == null || big.toString().isBlank() ? regular : big.toString();
        JobParser.Job job=JobParser.notificationJob(title,text,ongoing,summary);
        if(job==null && !text.equals(regular)){job=JobParser.notificationJob(title,regular,ongoing,summary);if(job!=null)text=regular;}
        // Only real job offers enter history, counters, filtering, or the floating list.
        if(job==null)return;
        scheduleTrim();
        BetaJobExtras.Details beta=BetaJobExtras.read(n.extras);
        BetaJobExtras.record(this,beta);
        if(n.contentIntent!=null){jobActions.put(actionKey(sbn.getKey(),sbn.getPostTime(),title,text),n.contentIntent);if(jobActions.size()>60)jobActions.remove(jobActions.keySet().iterator().next());}
        String fingerprint = title + "\n" + text;
        if (fingerprint.equals(seen.get(sbn.getKey()))) return;
        seen.put(sbn.getKey(),fingerprint);
        if(DriverBridge.viewsEnabled(this))DriverBridge.notificationSignal(n.extras);
        if (seen.size()>200) seen.remove(seen.keySet().iterator().next());
        Store.prefs(this).edit().putLong("lastReceived",System.currentTimeMillis()).apply();
        floating.restore();syncFloating();floating.offer(new FloatingJobs.Item(sbn.getKey(),sbn.getPostTime(),title,text,job,true,false,beta.summary));
        record(sbn,title,text,false,"อ่านข้อมูลเท่านั้น");
    }
    private void offerManual(PendingIntent intent,String title) {}
}
