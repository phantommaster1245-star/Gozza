package com.fulllock.app;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.widget.Switch;

/** Native Switch gestures/accessibility and animation, with a larger white thumb. */
final class BoldSwitch extends Switch {
 private final float visualScale;private float colorProgress;private android.animation.ValueAnimator colorAnimation;private boolean ready;
 BoldSwitch(Context context){this(context,1f);}
 BoldSwitch(Context context,float scale){
  super(context);visualScale=scale;
  setShowText(false);setSplitTrack(false);setSwitchMinWidth(dp(62));setMinHeight(Math.round(46*getResources().getDisplayMetrics().density));
  setLayerType(LAYER_TYPE_SOFTWARE,null);
  setTrackDrawable(new Shape(false));setThumbDrawable(new Shape(true));colorProgress=isChecked()?1f:0f;ready=true;
 }
 @Override public void setChecked(boolean checked){super.setChecked(checked);if(!ready)return;float target=isChecked()?1f:0f;if(colorAnimation!=null){colorAnimation.cancel();colorAnimation=null;}if(!isAttachedToWindow()||!isLaidOut()||!UiMotion.enabled()){colorProgress=target;invalidate();return;}colorAnimation=android.animation.ValueAnimator.ofFloat(colorProgress,target);colorAnimation.setDuration(UiMotion.QUICK);colorAnimation.setInterpolator(new android.view.animation.PathInterpolator(.2f,0f,0f,1f));colorAnimation.addUpdateListener(a->{colorProgress=(float)a.getAnimatedValue();invalidate();});colorAnimation.start();}
 @Override public void jumpDrawablesToCurrentState(){super.jumpDrawablesToCurrentState();if(colorAnimation!=null){colorAnimation.cancel();colorAnimation=null;}colorProgress=isChecked()?1f:0f;invalidate();}
 @Override protected void onDetachedFromWindow(){if(colorAnimation!=null){colorAnimation.cancel();colorAnimation=null;}colorProgress=isChecked()?1f:0f;super.onDetachedFromWindow();}
 private int dp(float value){return Math.round(value*visualScale*getResources().getDisplayMetrics().density);}
 private final class Shape extends Drawable {
  private final boolean thumb;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
  Shape(boolean thumb){this.thumb=thumb;}
  @Override public int getIntrinsicWidth(){return dp(thumb?38:62);}
  @Override public int getIntrinsicHeight(){return dp(38);}
  @Override public boolean getPadding(Rect padding){padding.set(dp(thumb?4:2),dp(4),dp(thumb?4:2),dp(4));return true;}
  @Override public boolean isStateful(){return true;}
  @Override protected boolean onStateChange(int[] state){invalidateSelf();return true;}
  @Override public void draw(Canvas canvas){
   Rect b=getBounds();float cx=b.exactCenterX(),cy=b.exactCenterY();paint.clearShadowLayer();paint.setStyle(Paint.Style.FILL);
   boolean on=false,enabled=false;for(int state:getState()){on|=state==android.R.attr.state_checked;enabled|=state==android.R.attr.state_enabled;}
   paint.setAlpha(enabled?255:120);
   if(thumb){float radius=Math.min(b.width(),b.height())/2f-dp(4);paint.setColor(0xFFFFFFFF);paint.setShadowLayer(dp(3),0,dp(2),0x55000000);canvas.drawCircle(cx,cy,radius,paint);paint.clearShadowLayer();paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(1));paint.setColor(0x22000000);canvas.drawCircle(cx,cy,radius,paint);}
   else {RectF r=new RectF(b.left,b.top+dp(4),b.right,b.bottom-dp(4));paint.setColor(AppTheme.blend(0xFFC5C7CB,AppTheme.accent(getContext()),colorProgress));canvas.drawRoundRect(r,r.height()/2,r.height()/2,paint);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(1));paint.setColor(on?0x22000000:0x33000000);canvas.drawRoundRect(r,r.height()/2,r.height()/2,paint);}
  }
  @Override public void setAlpha(int alpha){paint.setAlpha(alpha);invalidateSelf();}
  @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
  @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
 }
}
