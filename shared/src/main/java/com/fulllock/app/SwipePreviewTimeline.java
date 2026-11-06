package com.fulllock.app;
/** Paint-only preview timing, independent of all gesture execution. */
public final class SwipePreviewTimeline {
 public static int duration(int ms){return Math.max(10,Math.min(500,ms));}
 public static float releaseX(float barRight,float screenWidth,float density){return Math.min(screenWidth-density,barRight+12*density);}
 public static float progress(long elapsed,int ms){int drag=duration(ms);long t=Math.floorMod(elapsed,350+drag+450+650);return t<350?0:t<350+drag?(t-350)/(float)drag:1;}
 public static boolean visible(long elapsed,int ms){return Math.floorMod(elapsed,350+duration(ms)+450+650)<350+duration(ms)+450;}
}
