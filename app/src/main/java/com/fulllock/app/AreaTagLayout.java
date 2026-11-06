package com.fulllock.app;
import android.content.Context;import android.view.*;import android.widget.TextView;import android.graphics.Color;
/** Wrapping area chips; the enclosing scroll view owns the bounded viewport. */
final class AreaTagLayout extends ViewGroup {
 private final int gap,rowHeight;
 AreaTagLayout(Context c,String names,Runnable edit){super(c);float d=c.getResources().getDisplayMetrics().density;gap=Math.round(6*d);rowHeight=Math.round(32*d);for(String name:names.split(",")){if(name.isBlank())continue;TextView chip=new TextView(c);chip.setText(name.trim());chip.setTextSize(13);chip.setTextColor(0xFF333333);chip.setGravity(android.view.Gravity.CENTER_VERTICAL);chip.setPadding(gap*2,0,gap*2,0);chip.setBackground(AppTheme.background(0xFFEEEEEE,Math.round(3*d)));chip.setOnClickListener(v->edit.run());addView(chip);}}
 protected void onMeasure(int w,int h){int width=MeasureSpec.getSize(w),x=0,y=0;for(int i=0;i<getChildCount();i++){View v=getChildAt(i);v.measure(MeasureSpec.makeMeasureSpec(width,MeasureSpec.AT_MOST),MeasureSpec.makeMeasureSpec(rowHeight-gap,MeasureSpec.EXACTLY));if(x>0&&x+v.getMeasuredWidth()>width){x=0;y+=rowHeight;}x+=v.getMeasuredWidth()+gap;}setMeasuredDimension(width,getChildCount()==0?0:y+rowHeight);}
 protected void onLayout(boolean changed,int l,int t,int r,int b){int x=0,y=0,width=r-l;for(int i=0;i<getChildCount();i++){View v=getChildAt(i);int w=v.getMeasuredWidth();if(x>0&&x+w>width){x=0;y+=rowHeight;}v.layout(x,y,x+w,y+v.getMeasuredHeight());x+=w+gap;}}
}
