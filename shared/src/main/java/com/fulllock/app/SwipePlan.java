package com.fulllock.app;
/** A single pixel-accurate plan shared by the command and its visual guide. */
public record SwipePlan(int x1,int y1,int x2,int y2,int durationMs) {
 public static SwipePlan fromBar(float left,float top,float right,float bottom,int width,int height,float density,int duration){if(!Float.isFinite(left)||!Float.isFinite(top)||!Float.isFinite(right)||!Float.isFinite(bottom)||!Float.isFinite(density)||density<=0)return null;int x1=Math.round(left+(bottom-top)/2),x2=Math.round(SwipePreviewTimeline.releaseX(right,width,density)),y=Math.round((top+bottom)/2);if(!AutoAcceptGuard.geometry(x1,y,x2,y,Math.round(left),Math.round(top),Math.round(right),Math.round(bottom),width,height))return null;return new SwipePlan(x1,y,x2,y,SwipePreviewTimeline.duration(duration));}
}
