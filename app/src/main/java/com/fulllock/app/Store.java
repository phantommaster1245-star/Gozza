package com.fulllock.app;

import android.content.*;
import org.json.*;
import java.math.BigDecimal;

final class Store {
    static android.content.SharedPreferences prefs(Context c) { return c.getSharedPreferences("settings", Context.MODE_PRIVATE); }
    static JSONObject ruleData(Context c){JSONObject out=new JSONObject();var p=prefs(c);try{
        for(String key:new String[]{"price","originOn","destOn","excludeOriginOn","excludeDestOn"})out.put(key,p.getBoolean(key,key.equals("price")));
        for(String key:new String[]{"min","max","origins","destinations","excludedOrigins","excludedDestinations"})out.put(key,p.getString(key,key.equals("min")?"100":key.equals("max")?"2000":""));
        for(String key:new String[]{"fee","formula","type","serviceMask"})out.put(key,p.getInt(key,key.equals("fee")?20:key.equals("serviceMask")?1:0));
    for(var field:ProfileFile.DEFAULTS.entrySet())if(field.getKey().startsWith("root"))out.put(field.getKey(),p.getAll().getOrDefault(field.getKey(),null)==null?field.getValue():p.getAll().get(field.getKey()));
    }catch(JSONException ignored){}return out;}
    static String rejection(Context c,JobParser.Job job){return new FilterRules(ruleData(c)).rejection(job);}
    // Render previous versions' filter reasons concisely without modifying saved history.
    static String shortReason(String result) {
        java.util.List<String> lines=new java.util.ArrayList<>();
        for(String line:result.split("\n")) {
            if(line.startsWith("ยังไม่ยืนยันว่าหน้างานเปิด")){lines.add("ยังยืนยันการเปิดไม่ได้");continue;}
            if(line.contains("พักการเปิดงานซ้อน")){lines.add("บันทึกจากรุ่นก่อน");continue;}
            if(line.contains("CanceledException")){lines.add("งานหมดอายุแล้ว");continue;}
            if(line.contains("SecurityException")){lines.add("ระบบไม่อนุญาตให้เปิดงาน");continue;}
            if(line.contains("ส่งคำสั่งเปิดแล้ว")){lines.add("ส่งคำสั่งเปิดแล้ว");continue;}
            if(line.contains("ไม่มีคำสั่งเปิด")){lines.add("เปิดงานนี้ไม่ได้");continue;}
            if(line.contains("ปิดเปิดงานอัตโนมัติ")){lines.add("ตรงเงื่อนไข / ยังไม่เปิดอัตโนมัติ");continue;}
            if(line.startsWith("อ่านข้อมูลงานไม่ได้")){lines.add("อ่านงานนี้ไม่ได้");continue;}
            if(line.contains("ต่ำกว่าขั้นต่ำ") || line.equals("ราคาต่ำกว่าที่ตั้งไว้")) line="ราคาต่ำไป";
            else if(line.contains("สูงกว่าขั้นสูง") || line.equals("ราคาสูงกว่าที่ตั้งไว้")) line="ราคาสูงไป";
            else if(line.startsWith("ไม่ตรงเงื่อนไข: ต้นทาง") || line.equals("ต้นทางไม่ตรงเงื่อนไข")) line="ต้นทางไม่ตรง";
            else if(line.startsWith("ไม่ตรงเงื่อนไข: ปลายทาง") || line.equals("ปลายทางไม่ตรงเงื่อนไข")) line="ปลายทางไม่ตรง";
            else if(line.startsWith("เปิดบล็อกงานเหมา:") || line.equals("ราคาตรงรายการบล็อกงานเหมา")) line="คาดว่าเป็นงานเหมา";
            else if(line.startsWith("ไม่ตรงเงื่อนไข: เป็น") || line.equals("ประเภทงานไม่ตรงเงื่อนไข")) line="ประเภทงานไม่ตรง";
            lines.add(line);
        }
        return String.join(" / ",lines);
    }
    static synchronized void log(Context c, String title, String text, boolean worked, String result) {
        log(c,title,text,worked,result,"",0);
    }
    static synchronized void log(Context c,String title,String text,boolean worked,String result,String key,long postTime) {append(c,title,text,worked,result,key,postTime,"");}
    static synchronized void logOpening(Context c,String title,String text,String key,long postTime,String attempt){append(c,title,text,false,"กำลังเปิดงาน",key,postTime,attempt);}
    private static void append(Context c,String title,String text,boolean worked,String result,String key,long postTime,String attempt) {
        try {
            JSONArray old = new JSONArray(prefs(c).getString("history", "[]")), next = new JSONArray();
            next.put(new JSONObject().put("title",title).put("text",text).put("worked",worked).put("result",result).put("time",System.currentTimeMillis()).put("notificationKey",key).put("notificationTime",postTime).put("openAttempt",attempt));
            for (int i=0;i<Math.min(old.length(),29);i++) next.put(old.get(i));
            prefs(c).edit().putString("history",next.toString()).apply();
        } catch(JSONException ignored) { }
    }
    static synchronized void openingStatus(Context c,String attempt,boolean confirmed,String message){
        try{JSONArray history=new JSONArray(prefs(c).getString("history","[]"));for(int i=0;i<history.length();i++){JSONObject item=history.optJSONObject(i);if(item!=null&&attempt.equals(item.optString("openAttempt"))){item.put("worked",confirmed).put("result",message);prefs(c).edit().putString("history",history.toString()).apply();return;}}}catch(JSONException ignored){}
    }

    static synchronized boolean recoverInterruptedOpenings(Context c){boolean changed=false;try{JSONArray history=new JSONArray(prefs(c).getString("history","[]"));for(int i=0;i<history.length();i++){JSONObject item=history.optJSONObject(i);if(item!=null&&!item.optString("openAttempt").isEmpty()&&java.util.Set.of("กำลังเปิดงาน","ส่งคำสั่งเปิดแล้ว").contains(item.optString("result"))){item.put("worked",false).put("result","ยังยืนยันการเปิดไม่ได้ · แอปหยุดระหว่างเปิดงาน");changed=true;}}if(changed)prefs(c).edit().putString("history",history.toString()).apply();}catch(JSONException ignored){}return changed;}

}
