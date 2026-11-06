package com.fulllock.app;
import java.util.LinkedHashMap;
/** Small in-memory cache of already received order_detail results, addressed only by exact UUID. */
public final class DetailModelCache<M> {
 private record Entry<M>(M model,long at){}
 private final LinkedHashMap<String,Entry<M>> entries=new LinkedHashMap<>();
 public void put(String uuid,M model,long now){prune(now);if(uuid==null||uuid.isBlank()||uuid.length()>128||model==null)return;entries.remove(uuid);entries.put(uuid,new Entry<>(model,now));while(entries.size()>6)entries.remove(entries.keySet().iterator().next());}
 public M get(String uuid,long now){prune(now);Entry<M> entry=entries.get(uuid);return entry==null?null:entry.model();}
 public void clear(){entries.clear();}
 private void prune(long now){entries.entrySet().removeIf(e->now<e.getValue().at()||now-e.getValue().at()>120000);}
}
