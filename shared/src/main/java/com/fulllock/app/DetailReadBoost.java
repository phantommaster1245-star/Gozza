package com.fulllock.app;
/** Bounded, one-visit read scheduling. Never creates input or changes match decisions. */
public final class DetailReadBoost {
 private String token="";private Object owner;private long start;private int ticks;private boolean ended;
 public void update(String next,Object visit,long now){if(next==null||next.isEmpty()){if(!token.isEmpty())ended=true;return;}if(!next.equals(token)){token=next;owner=visit;start=now;ticks=0;ended=false;}else if(owner!=visit)ended=true;}
 public boolean active(long now){return !token.isEmpty()&&!ended&&now>=start&&now-start<1500&&ticks<30;}
 public void read(long now){if(active(now))ticks++;}
 public void stop(){ended=true;owner=null;}
 public int interval(long now){return active(now)?40:120;}
 public int modelInterval(long now){return active(now)?160:1000;}
 public int textInterval(long now){return active(now)?120:800;}
}
