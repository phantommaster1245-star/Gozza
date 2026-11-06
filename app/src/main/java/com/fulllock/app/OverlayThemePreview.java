package com.fulllock.app;
import android.content.Context;import android.graphics.*;import android.view.View;
/** Uses the same preference keys as Driver drawing, without intercepting map/job touches. */
final class OverlayThemePreview extends View {
 private final Paint paint=new Paint(3);
 OverlayThemePreview(Context c){super(c);setContentDescription("ตัวอย่างสีป้ายข้อมูลและป้ายตำแหน่ง");}
 private int color(String key){return Store.prefs(getContext()).getInt(key,OverlayPalette.fallback(key));}
 @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);float d=getResources().getDisplayMetrics().density;
  RectF data=new RectF(12*d,10*d,getWidth()-12*d,84*d);paint.setStyle(Paint.Style.FILL);paint.setColor(color("rootOverlayBackground"));canvas.drawRoundRect(data,8*d,8*d,paint);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(d);paint.setColor(color("rootOverlayBorder"));canvas.drawRoundRect(data,8*d,8*d,paint);paint.setStyle(Paint.Style.FILL);paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));paint.setTextSize(14*d);paint.setColor(color("rootOverlayLabel"));canvas.drawText("รายได้สุทธิ · ระยะทาง · ราคา/กม.",24*d,34*d,paint);paint.setTextSize(26*d);paint.setFontFeatureSettings("tnum");paint.setColor(color("rootOverlayText"));canvas.drawText("฿180   12.5 กม.",24*d,68*d,paint);
  RectF tag=new RectF(12*d,96*d,getWidth()-12*d,136*d);paint.setColor(color("pickupOverlayBackground"));canvas.drawRoundRect(tag,8*d,8*d,paint);paint.setStyle(Paint.Style.STROKE);paint.setColor(color("pickupOverlayBorder"));canvas.drawRoundRect(tag,8*d,8*d,paint);paint.setStyle(Paint.Style.FILL);paint.setColor(color("pickupOverlayText"));paint.setTextSize(16*d);canvas.drawText("13.8622, 100.4450",24*d,122*d,paint);
 }
}
