package com.fulllock.app;

import android.app.Notification;
import android.content.Context;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Beta-only, opt-in-compatible inspection of Android notification extras.
 * No privileged APIs, hidden network endpoints, token/session reading or
 * persistence of addresses. If Driver does not attach distances, show none.
 */
final class BetaJobExtras {
    private static final Pattern PICKUP=Pattern.compile(
        "(?:ระยะ(?:ทาง)?(?:ไป|ถึง)จุดรับ(?:สินค้า)?|distance to pick.?up|to.pickup.distance)\\s*[:：=]?\\s*([0-9]+(?:[.,][0-9]{1,2})?)\\s*(ก\\.?ม\\.?|km|กิโลเมตร|m|เมตร)(?!\\S)",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern ROUTE=Pattern.compile(
        "(?:ระยะ(?:ทาง)?(?:ขนส่ง|ทั้งหมด|เส้นทาง)|route.distance|total.distance)\\s*[:：=]?\\s*([0-9]+(?:[.,][0-9]{1,2})?)\\s*(ก\\.?ม\\.?|km|กิโลเมตร|m|เมตร)(?!\\S)",
        Pattern.CASE_INSENSITIVE);

    static final class Details {
        final String summary;
        final int linesInspected;
        Details(String text,int count){summary=text;linesInspected=count;}
    }

    static Details read(Bundle extras) {
        if(extras==null)return new Details("",0);
        List<String> texts=new ArrayList<>();
        append(texts,extras.getCharSequence(Notification.EXTRA_SUB_TEXT));
        append(texts,extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT));
        append(texts,extras.getCharSequence(Notification.EXTRA_INFO_TEXT));
        CharSequence[] lines=extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if(lines!=null)for(CharSequence line:lines)append(texts,line);
        // Keep strict whitelist and explicit units. Never guess by an unlabeled number.
        String pickup=null,route=null;
        for(String line:texts){
            if(pickup==null)pickup=parse(line,PICKUP);
            if(route==null)route=parse(line,ROUTE);
        }
        StringBuilder summary=new StringBuilder();
        if(pickup!=null)summary.append("ถึงจุดรับ ").append(pickup);
        if(route!=null){if(summary.length()>0)summary.append(" • ");summary.append("ระยะงาน ").append(route);}
        return new Details(summary.toString(),texts.size());
    }
    private static void append(List<String> out,CharSequence text){
        if(text!=null && text.length()>0 && text.length()<1000 && out.size()<30)out.add(text.toString());
    }
    private static String parse(String line,Pattern p){
        Matcher m=p.matcher(line);
        if(!m.find())return null;
        String number=m.group(1).replace(',','.');
        try {
            double value=Double.parseDouble(number);
            if(!Double.isFinite(value)||value<0||value>100000)return null;
        }catch(NumberFormatException e){return null;}
        String unit=m.group(2).toLowerCase(java.util.Locale.ROOT);
        return number+(unit.equals("m")||unit.equals("เมตร")?" ม.":" กม.");
    }
    static void record(Context c,Details details){
        if(!BuildConfig.DEBUG)return;
        android.content.SharedPreferences p=Store.prefs(c);
        p.edit().putLong("betaCount",p.getLong("betaCount",0)+1)
          .putLong("betaLastSeen",System.currentTimeMillis())
          .putBoolean("betaExtraFound",!details.summary.isEmpty())
          .putInt("betaExtraLines",details.linesInspected).apply();
    }
    static void probeActive(Context context,android.service.notification.StatusBarNotification[] active){
        if(!BuildConfig.DEBUG)return;
        int matches=0,enriched=0;
        if(active!=null)for(android.service.notification.StatusBarNotification sbn:active){
            if(!"com.lalamove.global.driver.sea".equals(sbn.getPackageName()))continue;
            Notification notification=sbn.getNotification();
            Bundle fields=notification.extras;
            if(fields==null)continue;
            String title=String.valueOf(fields.getCharSequence(Notification.EXTRA_TITLE,""));
            String text=String.valueOf(fields.getCharSequence(Notification.EXTRA_TEXT,""));
            CharSequence big=fields.getCharSequence(Notification.EXTRA_BIG_TEXT);
            if(big!=null&&!big.toString().isBlank())text=big.toString();
            if(JobParser.notificationJob(title,text,false,false)==null)continue;
            matches++;
            if(!read(fields).summary.isEmpty())enriched++;
        }
        Store.prefs(context).edit()
          .putBoolean("betaListenerActive",true)
          .putInt("betaExistingOffers",matches)
          .putInt("betaExistingRichOffers",enriched)
          .apply();
    }
    static String status(Context c){
        android.content.SharedPreferences p=Store.prefs(c);
        long count=p.getLong("betaCount",0);
        String active=p.getBoolean("betaListenerActive",false)
            ?"อ่านการแจ้งเตือน Driver ผ่าน Android ได้แล้ว • งานที่ยังค้าง "+p.getInt("betaExistingOffers",0)+" รายการ • พบระยะทาง "+p.getInt("betaExistingRichOffers",0)+" รายการ"
            :"ยังไม่ได้อนุญาต Beta ให้อ่านการแจ้งเตือน";
        if(count==0)return active+"\\nยังไม่มีแจ้งเตือนงานใหม่หลังเริ่ม Beta";
        String when=new java.text.SimpleDateFormat("HH:mm:ss",new java.util.Locale("th","TH")).format(new java.util.Date(p.getLong("betaLastSeen",0)));
        return active+"\nรับแจ้งเตือนจริง "+count+" ครั้ง • ล่าสุด "+when+"\n"+
          (p.getBoolean("betaExtraFound",false)?"พบระยะทางที่มีหน่วยในข้อมูลแจ้งเตือน":"ยังไม่พบระยะทางที่มีหน่วยในข้อมูลแจ้งเตือน")
          +" • ข้อความเสริม "+p.getInt("betaExtraLines",0)+" ส่วน";
    }
}
