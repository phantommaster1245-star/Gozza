package com.fulllock.app;
/** Fits in the header's free right side; long chips may only reserve their own row. */
public final class DetailPanelLayout {
 public static float[] attached(float width,float d,float titleTop,float titleRight,float titleBottom,float chipsTop,float chipsBottom,float chipsRight,float pickupRight){
  if(chipsBottom>titleTop){float[] normal=place(width,d,titleTop,titleRight,chipsTop,chipsBottom,chipsRight,pickupRight);if(normal!=null)return normal;}
  float left=titleRight+8*d,right=Math.min(width-8*d,left+160*d),height=46*d,top=(titleTop+titleBottom-height)/2;
  if(right-left<90*d||top<0)return null;return new float[]{left,top,right,top+height};
 }
 /** Passenger chips can span most of the header; keep the panel within its two title lines. */
 public static float[] passenger(float width,float d,float titleTop,float titleRight,float titleBottom,float chipsTop,float chipsBottom,float chipsRight,float pickupRight){
  float[] result=attached(width,d,titleTop,titleRight,titleBottom,chipsTop,chipsBottom,chipsRight,pickupRight);if(result==null)return null;
  float height=Math.max(30*d,titleBottom-titleTop),center=(titleTop+titleBottom)/2;result[1]=center-height/2;result[3]=center+height/2;return result;
 }
 public static float[] place(float width,float d,float titleTop,float titleRight,float chipsTop,float chipsBottom,float chipsRight,float pickupRight){
  float right=width-12*d,left=Math.max(width*.5f,chipsRight+10*d),bottom=chipsBottom+4*d,top=Math.max(titleTop,bottom-78*d);
  if(right-left>=138*d&&bottom-top>=38*d)return new float[]{left,top,right,bottom};
  left=Math.max(width*.5f,Math.max(titleRight,pickupRight)+10*d);top=titleTop;bottom=chipsTop-4*d;
  if(right-left>=125*d&&bottom-top>=34*d)return new float[]{left,top,right,bottom};
  return null;
 }
}
