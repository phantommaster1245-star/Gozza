package com.fulllock.app;
import org.json.*;import java.math.BigDecimal;import java.util.*;
/** Same immutable rules implementation used by app and in-process module. */
public final class FilterRules {
 private final JSONObject p;private final BigDecimal feeOverride;
 public FilterRules(JSONObject value){this(value,null);}
 public FilterRules(JSONObject value,BigDecimal fee){p=value;feeOverride=fee;}
 public String rejection(JobParser.Job job){try{
  if(p.optBoolean("excludeOriginOn")&&JobParser.areaMatches(job.origin(),p.optString("excludedOrigins")))return "ต้นทางที่ไม่ต้องการ";
  if(p.optBoolean("excludeDestOn")&&JobParser.areaMatches(job.destination(),p.optString("excludedDestinations")))return "ปลายทางที่ไม่ต้องการ";
  List<String> reasons=new ArrayList<>();if(p.optBoolean("price",true)){int fee=p.optInt("fee",20),formula=p.optInt("formula");if(fee<0||fee>100||formula<0||formula>3)return "ตั้งค่าราคาไม่ถูกต้อง";BigDecimal value=feeOverride==null?job.price(formula,fee):Commission.price(job,formula,feeOverride);if(value.compareTo(new BigDecimal(p.optString("min","100")))<0)reasons.add("ราคาต่ำไป");if(value.compareTo(new BigDecimal(p.optString("max","2000")))>0)reasons.add("ราคาสูงไป");}
  if(p.optBoolean("originOn")&&!JobParser.areaMatches(job.origin(),p.optString("origins")))reasons.add("ต้นทางไม่ตรง");
  if(p.optBoolean("destOn")&&!JobParser.areaMatches(job.destination(),p.optString("destinations")))reasons.add("ปลายทางไม่ตรง");
  int service=job.category().equals("ขนส่งสินค้า")?1:job.category().equals("รับ-ส่งผู้โดยสาร")?2:0,type=p.optInt("type");if(service==0||(p.optInt("serviceMask",1)==2?2:1)!=service||type==1&&!job.type().equals("ขนส่งทันที")||type==2&&!job.type().equals("งานล่วงหน้า"))reasons.add("ประเภทงานไม่ตรง");return reasons.isEmpty()?null:String.join(" / ",reasons);
 }catch(RuntimeException e){return "ตั้งค่าตัวกรองไม่ถูกต้อง";}}
}
