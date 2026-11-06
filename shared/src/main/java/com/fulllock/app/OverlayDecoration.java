package com.fulllock.app;
/** Only large, non-interactive, undescribed artwork may be treated as a background. */
public final class OverlayDecoration {
 public static boolean background(String id,float width,float height,float cardWidth,float cardHeight,boolean interactive,boolean described){
  if(interactive||described||cardWidth<=0||cardHeight<=0)return false;
  String name=id==null?"":id.toLowerCase(java.util.Locale.ROOT);
  boolean named=name.contains("background")||name.startsWith("bg_")||name.endsWith("_bg");
  return width>=cardWidth*.70f&&height>=cardHeight*.30f&&(named||width*height>=cardWidth*cardHeight*.45f);
 }
}
