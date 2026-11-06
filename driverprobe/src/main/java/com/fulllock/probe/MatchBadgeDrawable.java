package com.fulllock.probe;
import android.graphics.*;import android.graphics.drawable.Drawable;import java.util.function.BooleanSupplier;
/** Rectangular badge aligned to the actual native header, not a fixed card offset. */
final class MatchBadgeDrawable extends Drawable {
 private final float d;private final BooleanSupplier current;private final Paint p=new Paint(3);private final Path tick=new Path();private final RectF header;private int alpha=255;private android.os.Bundle style=new android.os.Bundle();private final Runnable frame=this::invalidateSelf;
 MatchBadgeDrawable(float d,BooleanSupplier current,RectF header,android.os.Bundle style){this(d,current,header);if(style!=null)this.style=new android.os.Bundle(style);}
 MatchBadgeDrawable(float d,BooleanSupplier current){this(d,current,null);}
 MatchBadgeDrawable(float d,BooleanSupplier current,RectF header){this.d=d;this.current=current;this.header=header==null?null:new RectF(header);}
 public void draw(Canvas c){if(!current.getAsBoolean()||getBounds().width()<100*d)return;
  boolean animate=style.getBoolean("rootOverlayPulse",true)&&android.animation.ValueAnimator.areAnimatorsEnabled();int save=c.saveLayerAlpha(null,animate?(int)(alpha*(.80+.20*(.5+.5*Math.sin(android.os.SystemClock.uptimeMillis()*Math.PI*2/Math.max(500,Math.min(5000,style.getInt("rootOverlayPulseMs",500))))))):alpha);
  RectF h=header==null?new RectF(0,0,getBounds().width(),44*d):header;float height=Math.min(30*d,h.height()-8*d);if(height<18*d){c.restoreToCount(save);return;}
  float right=Math.min(getBounds().width(),h.right)-6*d,left=right-108*d,top=h.centerY()-height/2,cy=h.centerY();RectF box=new RectF(left,top,right,top+height);
  p.setAlpha(alpha);p.setStyle(Paint.Style.FILL);p.setColor(style.getInt("rootOverlayBackground",0xFFFFFFFF));c.drawRoundRect(box,3*d,3*d,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(d);p.setColor(style.getInt("rootOverlayBorder",0xFFF16623));c.drawRoundRect(box,3*d,3*d,p);
  float x=left+14*d;p.setStrokeWidth(2.8f*d);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setColor(0xFF16A34A);tick.reset();tick.moveTo(x-5*d,cy);tick.lineTo(x-1*d,cy+4*d);tick.lineTo(x+6*d,cy-5*d);c.drawPath(tick,p);
  p.setColor(style.getInt("rootOverlayText",0xFF20262E));p.setStyle(Paint.Style.FILL);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setTextSize(11*d);p.setTextAlign(Paint.Align.CENTER);Paint.FontMetrics fm=p.getFontMetrics();c.drawText("ตรงเงื่อนไข",left+67*d,cy-(fm.ascent+fm.descent)/2,p);c.restoreToCount(save);if(animate)scheduleSelf(frame,android.os.SystemClock.uptimeMillis()+33);
 }
 public void setAlpha(int a){alpha=a;invalidateSelf();}public void setColorFilter(ColorFilter f){p.setColorFilter(f);invalidateSelf();}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
