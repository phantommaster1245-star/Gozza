package com.fulllock.app;
/** Only a notification-opened, identity-matched detail may return after a continuous rejection. */
public final class DetailReturnGuard {
 private String expectedRef="";private JobParser.Job expected;private long armedAt,rejectedAt=-1;private String visit="";private boolean spent;
 public void arm(JobParser.Job job,long now){arm(job,"",now);}
 public void arm(JobParser.Job job,String reference,long now){expectedRef=reference==null?"":reference;expected=job;armedAt=now;rejectedAt=-1;visit="";spent=false;}
 public void cancel(){expected=null;rejectedAt=-1;visit="";spent=true;}
 public void left(){if(!visit.isEmpty())cancel();}
 public boolean evaluate(JobParser.Job job,String token,boolean complete,boolean rejected,boolean automatic,long now){return evaluate(job,token,complete,rejected,automatic,now,3000);}
 public boolean evaluate(JobParser.Job job,String token,boolean complete,boolean rejected,boolean automatic,long now,long delayMs){return evaluate(job,token,complete,rejected,automatic,now,delayMs,"");}
 public boolean evaluate(JobParser.Job job,String token,boolean complete,boolean rejected,boolean automatic,long now,long delayMs,String reference){
  if(expected==null||spent||!automatic||now<armedAt||now-armedAt>120000){rejectedAt=-1;return false;}
  if(!sameRecord(expected,job,expectedRef,reference)){rejectedAt=-1;return false;}
  if(visit.isEmpty()){if(now-armedAt>20000)return false;visit=token;}else if(!visit.equals(token)){cancel();return false;}
  if(!complete||!rejected){rejectedAt=-1;return false;}
  if(rejectedAt<0)rejectedAt=now;return now-rejectedAt>=Math.max(0,Math.min(20000,delayMs));
 }
 public void consume(){cancel();}
 public void resetWait(){rejectedAt=-1;}
 public static boolean sameRecord(JobParser.Job a,JobParser.Job b,String expectedRef,String observedRef){if(expectedRef!=null&&!expectedRef.isEmpty())return observedRef!=null&&!observedRef.isEmpty()&&expectedRef.equals(observedRef)&&a!=null&&b!=null&&a.category().equals(b.category())&&a.origin().equals(b.origin())&&a.destination().equals(b.destination())&&a.type().equals(b.type());return same(a,b);}
 public static boolean same(JobParser.Job a,JobParser.Job b){return a!=null&&b!=null&&a.category().equals(b.category())&&a.origin().equals(b.origin())&&a.destination().equals(b.destination())&&a.type().equals(b.type())&&a.fare().compareTo(b.fare())==0&&a.tip().compareTo(b.tip())==0;}
}
