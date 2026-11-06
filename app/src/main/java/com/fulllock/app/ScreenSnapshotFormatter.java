package com.fulllock.app;
import org.json.*;import java.util.*;
/** Groups by actual shared card ancestor, never by matching a route name. */
final class ScreenSnapshotFormatter {
 static String format(JSONArray nodes,int count)throws JSONException{
  LinkedHashMap<Integer,LinkedHashMap<String,List<String>>> cards=new LinkedHashMap<>();
  for(int i=0;i<Math.min(60,nodes.length());i++){JSONObject n=nodes.getJSONObject(i);int top=n.optInt("cardTop",-1);if(top==-1)continue;String k=n.optString("kind"),v=n.optString("value");if(v.isBlank()||v.length()>80)continue;
   boolean valid=false;
   if(k.equals("origin")||k.equals("destination"))valid=v.codePoints().allMatch(x->Character.isLetterOrDigit(x)||Character.getType(x)==Character.NON_SPACING_MARK||" .()-".indexOf(x)>=0);
   else if(k.equals("price"))valid=v.matches("[0-9,. +]{1,40}");
   else if(k.equals("distance")||k.equals("routeDistance"))valid=v.matches("[0-9.]{1,15}");
   else if(k.equals("commissionReduction"))valid=v.matches("[0-9]+(?:[.][0-9]{1,2})?");
   else if(k.equals("payment"))valid=v.equals("เงินสด")||v.equals("เครดิต");
   else if(k.equals("service")||k.equals("label"))valid=v.codePoints().noneMatch(Character::isISOControl);
   if(!valid)continue;var card=cards.computeIfAbsent(top,x->new LinkedHashMap<>());var values=card.computeIfAbsent(k,x->new ArrayList<>());if(!values.contains(v))values.add(v);
  }
  StringBuilder s=new StringBuilder("อ่านโครงสร้างโดยตรง • ไม่ใช้ OCR\nข้อความบนจอ "+count+" จุด • "+cards.size()+" การ์ด");int number=0;
  for(var card:cards.values()){
   if(!card.containsKey("origin")&&!card.containsKey("destination"))continue;
   s.append("\n\nงาน ").append(++number).append("\nต้นทาง: ").append(join(card,"origin","ยังไม่เห็น")).append("\nปลายทาง: ").append(join(card,"destination","ยังไม่เห็น"));
   if(card.containsKey("price"))s.append("\nราคา ฿").append(join(card,"price",""));
   if(card.containsKey("distance"))s.append("\nระยะบนจอ ").append(join(card,"distance","")).append(" กม.");
   if(card.containsKey("routeDistance"))s.append("\nระยะทางงาน ").append(new java.math.BigDecimal(card.get("routeDistance").get(0)).movePointLeft(3).stripTrailingZeros().toPlainString()).append(" กม.");
   if(card.containsKey("commissionReduction"))s.append("\nลดค่าคอม: ").append(join(card,"commissionReduction","")).append("%");
   s.append("\nการรับเงิน: ").append(join(card,"payment","ไม่ระบุ"));
   s.append("\nบริการ: ").append(join(card,"service","ไม่ระบุ"));
   if(card.containsKey("label"))s.append("\n").append(join(card,"label",""));
  }return s.toString();
 }
 private static String join(Map<String,List<String>> card,String key,String fallback){return card.containsKey(key)?String.join(" / ",card.get(key)):fallback;}
}
