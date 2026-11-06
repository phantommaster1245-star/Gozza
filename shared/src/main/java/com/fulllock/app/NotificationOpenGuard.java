package com.fulllock.app;
/** Sending is distinct from seeing the requested detail. Tokens are single-use, time-bounded, and latest-only. */
public final class NotificationOpenGuard {
 public enum State { IDLE, REQUESTED, DISPATCHED, CONFIRMED, FAILED, UNCONFIRMED, CANCELLED }
 private String reference="";private String token="";private JobParser.Job job;private long started,sent;private State state=State.IDLE;
 public synchronized void begin(String token,JobParser.Job job,long now){begin(token,job,"",now);}
 public synchronized void begin(String token,JobParser.Job job,String reference,long now){if(token==null||!token.matches("[a-f0-9]{32}")||job==null)throw new IllegalArgumentException("Invalid opening request");this.reference=reference==null?"":reference;this.token=token;this.job=job;started=now;state=State.REQUESTED;sent=-1;}
 public synchronized boolean targets(JobParser.Job observed,String observedRef){return (state==State.DISPATCHED||state==State.CONFIRMED)&&DetailReturnGuard.sameRecord(job,observed,reference,observedRef);}
 public synchronized State state(){return state;}
 public synchronized String token(){return token;}
 public synchronized boolean awaiting(String token,long now){return this.token.equals(token)&&state==State.REQUESTED&&now>=started&&now-started<=8000;}
 public synchronized boolean claim(String token,long now){if(!awaiting(token,now))return false;state=State.DISPATCHED;sent=now;return true;}
 public synchronized boolean confirm(JobParser.Job observed,long observedAt,long now){return confirm(observed,"",observedAt,now);}
 public synchronized boolean confirm(JobParser.Job observed,String reference,long observedAt,long now){if(state!=State.DISPATCHED||observedAt<sent||now<observedAt||now-observedAt>500||now<started||now-started>20000||!DetailReturnGuard.sameRecord(job,observed,this.reference,reference))return false;state=State.CONFIRMED;return true;}
 public synchronized boolean failBeforeDispatch(String token){if(!this.token.equals(token)||state!=State.REQUESTED)return false;state=State.FAILED;return true;}
 public synchronized boolean failDispatched(String token){if(!this.token.equals(token)||state!=State.DISPATCHED)return false;state=State.FAILED;return true;}
 public synchronized boolean expire(String token,long now){if(!this.token.equals(token)||(state!=State.REQUESTED&&state!=State.DISPATCHED)||now<started||now-started<20000)return false;state=State.UNCONFIRMED;return true;}
 public synchronized boolean cancel(){if(state!=State.REQUESTED&&state!=State.DISPATCHED)return false;state=State.CANCELLED;return true;}
}
