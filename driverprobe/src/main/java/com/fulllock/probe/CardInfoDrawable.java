package com.fulllock.probe;
import android.graphics.*;import android.graphics.drawable.Drawable;import java.util.*;import java.util.function.BooleanSupplier;
/** Quiet typography on the original card; never paints a replacement card background. */
final class CardInfoDrawable extends Drawable {
 record Slot(RectF rect,String label,String value,boolean prominent,boolean inline){Slot(RectF rect,String label,String value,boolean prominent){this(rect,label,value,prominent,false);}}
 private final List<Slot> slots;private final float d;private final BooleanSupplier current;private final Paint p=new Paint(3);private android.os.Bundle style=new android.os.Bundle();
 CardInfoDrawable(float d,List<Slot> slots,BooleanSupplier current,android.os.Bundle style){this(d,slots,current);if(style!=null)this.style=new android.os.Bundle(style);}
 CardInfoDrawable(float d,List<Slot> slots,BooleanSupplier current){this.d=d;this.slots=slots;this.current=current;}
 public void draw(Canvas c){if(!current.getAsBoolean())return;for(Slot s:slots){RectF r=s.rect();
  {p.setColor(style.getInt("rootOverlayBackground",com.fulllock.app.GozzaDesign.surface));p.setStyle(Paint.Style.FILL);c.drawRoundRect(r,3*d,3*d,p);}
  if(s.inline()){
   p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setTextAlign(Paint.Align.LEFT);p.setTextSize(11*d);p.setFontFeatureSettings("tnum");float labelWidth=p.measureText(s.label()),gap=7*d;
   p.setTextSize(27*d);float available=r.width()-12*d-labelWidth-gap;while(p.measureText(s.value())>available&&p.getTextSize()>14*d)p.setTextSize(p.getTextSize()-.5f*d);
   Paint.FontMetrics fm=p.getFontMetrics();float baseline=r.centerY()-(fm.ascent+fm.descent)/2;
   p.setColor(style.getInt("rootOverlayText",com.fulllock.app.GozzaDesign.textPrimary));p.setTextAlign(Paint.Align.RIGHT);c.drawText(s.value(),r.right-5*d,baseline,p);
   p.setTextAlign(Paint.Align.LEFT);p.setTextSize(11*d);p.setColor(com.fulllock.app.GozzaDesign.primary);c.drawText(s.label(),r.left+5*d,baseline,p);continue;
  }
  p.setTextAlign(Paint.Align.LEFT);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setColor(com.fulllock.app.GozzaDesign.primary);p.setTextSize(12*d);c.drawText(s.label(),r.left+4*d,r.top+13*d,p);
  p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setColor(style.getInt("rootOverlayText",com.fulllock.app.GozzaDesign.textPrimary));p.setTextSize((s.prominent()?20:12)*d);
  while(p.measureText(s.value())>r.width()-8*d&&p.getTextSize()>10*d)p.setTextSize(p.getTextSize()-d);
  c.drawText(s.value(),r.left+4*d,r.bottom-(s.prominent()?6:3)*d,p);
 }}
 public void setAlpha(int a){p.setAlpha(a);invalidateSelf();}public void setColorFilter(ColorFilter f){p.setColorFilter(f);invalidateSelf();}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
