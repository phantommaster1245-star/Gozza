package com.fulllock.app;
import android.content.Context;import android.graphics.*;import android.graphics.drawable.Drawable;
/** Theme-tinted wallet and banknote outlines following the Driver payment cues. */
final class PaymentSymbol extends Drawable {
 private final Paint p=new Paint(3);private final boolean cash;private final float d;private final int color,surface;
 PaymentSymbol(Context c,boolean cash,int color){this.cash=cash;this.color=color;surface=AppTheme.surface(c);d=c.getResources().getDisplayMetrics().density;setBounds(0,0,Math.round(24*d),Math.round(24*d));}
 public void draw(Canvas canvas){int save=canvas.save();canvas.translate(getBounds().left,getBounds().top);canvas.scale(getBounds().width()/24f,getBounds().height()/24f);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.8f);p.setStrokeJoin(Paint.Join.ROUND);if(cash){canvas.drawRect(2,5,22,19,p);canvas.drawCircle(12,12,3.3f,p);for(int x:new int[]{2,22})for(int y:new int[]{5,19})canvas.drawCircle(x,y,3,p);}else{canvas.drawRoundRect(new RectF(2,3,19,21),2,2,p);canvas.drawLine(2,8,19,8,p);p.setStyle(Paint.Style.FILL);p.setColor(surface);canvas.drawRect(15,11,22,17,p);p.setColor(color);p.setStyle(Paint.Style.STROKE);canvas.drawRoundRect(new RectF(15,11,22,17),1,1,p);p.setStyle(Paint.Style.FILL);canvas.drawCircle(18,14,1,p);}canvas.restoreToCount(save);}
 public int getIntrinsicWidth(){return Math.round(24*d);}public int getIntrinsicHeight(){return Math.round(24*d);}public void setAlpha(int a){p.setAlpha(a);}public void setColorFilter(ColorFilter f){p.setColorFilter(f);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
