package com.fulllock.app;
import org.json.*;import java.util.*;import java.util.regex.*;
/** Known detail resource IDs only; route and pickup distances are deliberately distinct. */
public final class DetailViewParser {
 public static Double kilometres(String text){Matcher m=Pattern.compile("(?<![0-9.\\-])([0-9]+(?:[.][0-9]+)?)\\s*(กม[.]?|km|เมตร|ม[.]|m)(?![\\p{L}])",Pattern.CASE_INSENSITIVE).matcher(text);if(!m.find())return null;try{double v=Double.parseDouble(m.group(1));String unit=m.group(2);if(!unit.startsWith("กม")&&!unit.equalsIgnoreCase("km"))v/=1000;return Double.isFinite(v)&&v>=0&&v<10000?v:null;}catch(Exception e){return null;}}
 public static String timing(String value){String text=value.replace('\u00a0',' ').trim();if(text.equals("ขนส่งทันที")||text.matches("^ขนส่งทันที(?:\\s.*)?$"))return "ขนส่งทันที";if(text.length()<=64&&text.matches("^(?:วันนี้|พรุ่งนี้|[0-9]{1,2}[/ ].*|วัน(?:จันทร์|อังคาร|พุธ|พฤหัสบดี|ศุกร์|เสาร์|อาทิตย์)).*[0-9]{1,2}:[0-9]{2}.*"))return "งานล่วงหน้า";return null;}
 public static JSONArray parse(JSONArray views,String token)throws JSONException{
  JSONArray rows=new JSONArray();List<String> stops=new ArrayList<>();Set<String> prices=new HashSet<>();
  for(int i=0;i<views.length();i++){JSONObject view=views.getJSONObject(i);String id=view.optString("id"),text=view.optString("text").trim();
   if(id.equals("tv_new_station_address")&&!text.isBlank())stops.add(text);
   if(id.equals("priceValue")){Matcher m=Pattern.compile("฿?\\s*([0-9][0-9,]*(?:[.]\\d{1,2})?)(?:\\s*[+]\\s*฿?\\s*([0-9][0-9,]*(?:[.]\\d{1,2})?))?").matcher(text);if(m.matches())prices.add(m.group(1)+(m.group(2)==null?"":" + "+m.group(2)));}
   if(id.equals("orderDistance")||id.equals("distance")){Double km=kilometres(text);if(km!=null)add(rows,token,id.equals("orderDistance")?"routeDistance":"pickupDistance",Double.toString(km*1000));}
   if(text.equals("ขนส่งสินค้า")||text.equals("รับ-ส่งผู้โดยสาร")||Set.of("ส่งปกติ","ส่งด่วนพิเศษ","ส่งประหยัด","ส่งแบบประหยัด").contains(text))add(rows,token,"label",text);
   if(view.optBoolean("header")||Set.of("text","titleWithDistance","title","time","date","orderTime","tv_order_time").contains(id)){String timing=timing(text);if(timing!=null)add(rows,token,"timing",timing);}
   if(id.equals("description")){if(text.contains("เครดิต"))add(rows,token,"payment","เครดิต");else if(text.contains("เก็บเงินจากลูกค้า")||text.contains("เงินสด")||text.contains("ชำระเงินกับลูกค้าโดยตรง"))add(rows,token,"payment","เงินสด");}
   String reduction=Commission.reductionLabel(text);if(reduction!=null)add(rows,token,"commissionReduction",reduction);
  }
  if(stops.size()>=2){add(rows,token,"origin",stops.get(0));add(rows,token,"destination",stops.get(stops.size()-1));}
  if(prices.size()==1)add(rows,token,"price",prices.iterator().next());return rows;
 }
 public static void add(JSONArray rows,String token,String kind,String value)throws JSONException{rows.put(new JSONObject().put("cardToken",token).put("cardKey",token).put("kind",kind).put("value",value));}
}
