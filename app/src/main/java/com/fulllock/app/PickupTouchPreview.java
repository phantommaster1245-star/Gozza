package com.fulllock.app;

import android.content.Context;
import android.animation.ValueAnimator;
import android.graphics.*;
import android.view.View;

/** Lightweight preview: no timer, work or allocations continue when idle. */
final class PickupTouchPreview extends View {
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final RectF label=new RectF(),area=new RectF();
 private int percent=PickupTouchArea.DEFAULT;
 private float shownPercent=PickupTouchArea.DEFAULT;
 private ValueAnimator resize;
 private final int accent,ink;
 PickupTouchPreview(Context context,int accent,int ink){super(context);this.accent=accent;this.ink=ink;setContentDescription("ตัวอย่างพื้นที่แตะป้ายพิกัด");}
 void percent(int value){
  percent=PickupTouchArea.percent(value);
  setContentDescription("พื้นที่แตะ "+percent+" เปอร์เซ็นต์ของขนาดป้าย");
  if(resize!=null)resize.cancel();
  if(!isAttachedToWindow()||!UiMotion.enabled()){shownPercent=percent;invalidate();return;}
  resize=ValueAnimator.ofFloat(shownPercent,percent);resize.setDuration(100);
  resize.addUpdateListener(a->{shownPercent=(float)a.getAnimatedValue();invalidate();});resize.start();
 }
 @Override protected void onDetachedFromWindow(){if(resize!=null){resize.cancel();resize=null;}super.onDetachedFromWindow();}
 @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);float d=getResources().getDisplayMetrics().density;float width=Math.min(110*d,(getWidth()-12*d)/3f),height=24*d;
  label.set((getWidth()-width)/2,(getHeight()-height)/2,(getWidth()+width)/2,(getHeight()+height)/2);
  area.set(label);area.inset(-width*(shownPercent-100)/200f,-height*(shownPercent-100)/200f);
  paint.setStyle(Paint.Style.FILL);paint.setColor((accent&0x00ffffff)|0x12000000);canvas.drawRoundRect(area,3*d,3*d,paint);
  paint.setColor(Store.prefs(getContext()).getInt("pickupOverlayBackground",GozzaDesign.surface));canvas.drawRoundRect(label,3*d,3*d,paint);paint.setColor(Store.prefs(getContext()).getInt("pickupOverlayText",GozzaDesign.primary));paint.setTextSize(11*d);paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));paint.setTextAlign(Paint.Align.CENTER);Paint.FontMetrics fm=paint.getFontMetrics();canvas.drawText("13.8622,100.4450",label.centerX(),label.centerY()-(fm.ascent+fm.descent)/2,paint);
  paint.setColor(accent);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.5f*d);paint.setPathEffect(new DashPathEffect(new float[]{4*d,3*d},0));canvas.drawRoundRect(area,3*d,3*d,paint);paint.setPathEffect(null);paint.setStyle(Paint.Style.FILL);
 }
}
