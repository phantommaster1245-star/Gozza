package com.fulllock.app;

/** Total width and height as a percentage of the visible coordinate label. */
public final class PickupTouchArea {
 public static final int DEFAULT=100;
 private PickupTouchArea(){}
 public static int percent(int value){return Math.max(100,Math.min(300,value));}
 public static float padding(float size,int value){return size*(percent(value)-100)/200f;}
}
