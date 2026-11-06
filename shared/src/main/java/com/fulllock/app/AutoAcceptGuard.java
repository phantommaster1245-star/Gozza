package com.fulllock.app;
/** One-shot, fresh-detail lease. A failed attempt is never automatically retried in the same visit. */
public final class AutoAcceptGuard {
 private long epoch,seen;private String job="",rules="";private boolean eligible,consumed;
 public synchronized long observe(String job,String rules,boolean eligible,long at){if(!this.job.equals(job)){epoch++;this.job=job;}this.rules=rules;this.eligible=eligible;seen=at;return epoch;}
 public synchronized boolean ready(long epoch,String job,String rules,long now){return this.epoch==epoch&&this.job.equals(job)&&this.rules.equals(rules)&&eligible&&!consumed&&now>=seen&&now-seen<=300;}
 public synchronized boolean consume(long epoch,String job,String rules,long now){if(!ready(epoch,job,rules,now))return false;consumed=true;return true;}
 public synchronized void cancel(){epoch++;eligible=false;}
 public synchronized void left(){cancel();job="";consumed=false;}
 public static boolean geometry(int x1,int y1,int x2,int y2,int l,int t,int r,int b,int width,int height){return width>0&&width<=20000&&height>0&&height<=20000&&l>=0&&t>=0&&r<width&&b<=height&&r>l&&b>t&&x1>l&&x1<r&&x2>r&&x2<width&&y1>t&&y1<b&&y1==y2;}
}
