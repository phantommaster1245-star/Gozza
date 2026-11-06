package com.fulllock.app;
/** Passive gesture observation only. Never creates or injects input events. */
public final class SwipeGesturePlayback {
 public record Point(float x,float y,boolean released){}
 private SwipePlan plan;private long armedAt,downAt,lastAt;private float slop,x,y;private boolean started,released;
 public void arm(SwipePlan plan,long now,float slop){clear();this.plan=plan;armedAt=now;this.slop=Math.max(3,slop);}
 public void clear(){plan=null;started=false;released=false;}
 public boolean armed(long now){return plan!=null&&now>=armedAt&&now-armedAt<=3000;}
 public boolean observe(int action,float rawX,float rawY,long downTime,long eventTime){if(plan==null||!Float.isFinite(rawX)||!Float.isFinite(rawY)||eventTime<armedAt||eventTime-armedAt>3000)return false;if(action==0){if(started||Math.abs(rawX-plan.x1())>slop||Math.abs(rawY-plan.y1())>slop)return false;started=true;downAt=downTime;}else if(!started||released||downTime!=downAt||eventTime<lastAt)return false;if(action==3){clear();return false;}if(action!=0&&action!=1&&action!=2)return false;if(Math.abs(rawY-plan.y1())>slop*2){clear();return false;}x=rawX;y=rawY;lastAt=eventTime;released=action==1;return true;}
 public Point point(long now){if(!started||plan==null||now<lastAt||now-lastAt>(released?450:1000))return null;return new Point(x,y,released);}
}
