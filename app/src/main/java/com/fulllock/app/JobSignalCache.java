package com.fulllock.app;
import java.util.LinkedHashMap;
import java.util.Map;

/** Monotonic, bounded exact-reference join. Never matches by route or price. */
final class JobSignalCache {
 private static final long TTL=120000;
 private final LinkedHashMap<String,Entry> jobs=new LinkedHashMap<>();
 private static final class Entry {final String value;final long at;Entry(String v,long t){value=v;at=t;}}
 private String reference="";private long signalAt;private int signals;
 synchronized void put(String ref,String value,long now){if(!valid(ref))return;Entry previous=jobs.get(ref);if(previous!=null&&now<previous.at)return;clean(now);jobs.remove(ref);jobs.put(ref,new Entry(value,now));while(jobs.size()>60)jobs.remove(jobs.keySet().iterator().next());}
 synchronized void signal(String ref,long now){if(!valid(ref))return;if(!ref.equals(reference)||now-signalAt>=2000){signals++;reference=ref;signalAt=now;}}
 synchronized String match(long now){clean(now);if(reference.isEmpty()||now-signalAt>TTL)return null;Entry e=jobs.get(reference);return e==null?null:e.value;}
 synchronized java.util.List<String> snapshots(long now){return snapshots(now,10);}
 synchronized java.util.List<String> allSnapshots(long now){return snapshots(now,60);}
 private java.util.List<String> snapshots(long now,int limit){
  clean(now);java.util.List<String> values=new java.util.ArrayList<>();String priority=match(now);if(priority!=null)values.add(priority);
  java.util.List<Map.Entry<String,Entry>> entries=new java.util.ArrayList<>(jobs.entrySet());
  for(int i=entries.size()-1;i>=0&&values.size()<limit;i--){Map.Entry<String,Entry> e=entries.get(i);if(priority!=null&&e.getKey().equals(reference))continue;values.add(e.getValue().value);}
  return values;
 }
 synchronized String reference(){return reference;}
 synchronized int signals(){return signals;}
 synchronized boolean expired(long now){return !reference.isEmpty()&&now-signalAt>TTL;}
 synchronized int size(){return jobs.size();}
 private void clean(long now){jobs.entrySet().removeIf(e->now-e.getValue().at>TTL);}
 static boolean valid(String ref){return ref!=null&&ref.matches("[a-f0-9]{12}");}
}
