package com.fulllock.app;
import java.util.*;
/** Successful opens only; monotonic time, independent cooldowns, survives leaving the list. */
public final class RepeatJobGuard {
 private final Set<String> seen=new HashSet<>();
 public boolean ready(String identity,long now,int seconds,boolean noRepeat){return (!noRepeat||!seen.contains(identity))&&ready(identity,now,seconds);}
 private final LinkedHashMap<String,Long> opened=new LinkedHashMap<>();
 public void opened(String identity,long now){if(identity==null||identity.isBlank())return;seen.add(identity);prune(now);opened.remove(identity);opened.put(identity,now);while(opened.size()>256)opened.remove(opened.keySet().iterator().next());}
 public boolean ready(String identity,long now,int seconds){prune(now);Long at=opened.get(identity);return seconds<=0||at==null||now<at||now-at>=Math.min(20,seconds)*1000L;}
 private void prune(long now){opened.entrySet().removeIf(e->now<e.getValue()||now-e.getValue()>=20000);}
}
