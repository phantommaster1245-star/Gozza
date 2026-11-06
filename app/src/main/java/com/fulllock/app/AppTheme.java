package com.fulllock.app;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

/** Shared palette for the activity, dialogs, swipe control, and floating windows. */
final class AppTheme {
    static final int DEFAULT=0xFFF16623;
    static int accent(Context c){return DEFAULT;}
    static int jobColor(Context c,JobParser.Job job){int color=accent(c);if(job!=null&&"งานล่วงหน้า".equals(job.type())){float[] hsv=new float[3];Color.colorToHSV(color,hsv);hsv[0]=(hsv[0]+180)%360;return Color.HSVToColor(hsv);}return color;}
    static boolean inverted(Context c){return true;}
    static int ink(Context c){return 0xFFEFF5F0;}
    static int muted(Context c){return 0xFF9AA8A3;}
    static int onAccent(Context c){return Color.WHITE;}
    static int surface(Context c){return 0xFF1B2023;}
    static int page(Context c){return 0xFF101416;}
    static int divider(Context c){return 0xFF384047;}
    static int tint(Context c){return blend(surface(c),accent(c),.11f);}
    static int blend(int base,int color,float amount){return Color.rgb(Math.round(Color.red(base)*(1-amount)+Color.red(color)*amount),Math.round(Color.green(base)*(1-amount)+Color.green(color)*amount),Math.round(Color.blue(base)*(1-amount)+Color.blue(color)*amount));}
    static GradientDrawable background(int color,int radiusPx){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radiusPx);return d;}
    static void styleDialog(Context c,AlertDialog dialog){
        if(dialog.getWindow()==null)return;
        dialog.getWindow().setBackgroundDrawable(background(surface(c),Math.round(4*c.getResources().getDisplayMetrics().density)));
        styleTree(c,dialog.getWindow().getDecorView());
        if(dialog.getListView()!=null){ListView list=dialog.getListView();list.setBackgroundColor(surface(c));list.setDivider(new android.graphics.drawable.ColorDrawable(divider(c)));list.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener(){public void onChildViewAdded(View parent,View child){styleTree(c,child);}public void onChildViewRemoved(View parent,View child){}});}
        int gap=Math.round(8*c.getResources().getDisplayMetrics().density);
        int titleId=c.getResources().getIdentifier("alertTitle","id","android");View title=dialog.findViewById(titleId);if(title instanceof TextView t){t.setTextColor(accent(c));t.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);t.setTextSize(20);}
        for(int which:new int[]{-1,-2,-3}){Button b=dialog.getButton(which);if(b!=null){boolean primary=which==-1;b.setAllCaps(false);b.setTextSize(14);b.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);b.setTextColor(primary?onAccent(c):accent(c));GradientDrawable bg=background(primary?accent(c):surface(c),Math.round(5*c.getResources().getDisplayMetrics().density));if(!primary)bg.setStroke(Math.max(1,gap/8),divider(c));b.setBackground(bg);b.setPadding(gap*2,gap,gap*2,gap);if(b.getLayoutParams() instanceof ViewGroup.MarginLayoutParams lp){lp.setMargins(gap/2,gap/2,gap/2,gap/2);b.setLayoutParams(lp);}}}
    }
    private static void styleTree(Context c,View v){
        if(v instanceof TextView){TextView t=(TextView)v;t.setTextColor(ink(c));t.setHintTextColor(muted(c));}
        if(v instanceof EditText)v.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accent(c)));
        if(v instanceof ViewGroup){ViewGroup group=(ViewGroup)v;for(int i=0;i<group.getChildCount();i++)styleTree(c,group.getChildAt(i));}
    }
}
