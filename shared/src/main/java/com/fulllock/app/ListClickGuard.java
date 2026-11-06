package com.fulllock.app;
/** Navigation settling only: deliberately keeps no job-ID or previously-opened history. */
public final class ListClickGuard {
 private long until;
 public void sent(long now){until=now+350;}
 public void listLeft(){until=0;}
 public boolean ready(long now){return now>=until;}
}
