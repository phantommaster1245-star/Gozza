package com.fulllock.app;
import java.util.*;import java.math.BigDecimal;import org.json.*;
/** Extra card-only rules. Missing data never substitutes pickup distance or zero. */
public final class RootRules {
 public static final String[] CRITERIA={"price","rate","payment","pricing","distance","services","timing"};
 public static String migrateLegacyPriority(String priority,String payOrder){List<String> result=new ArrayList<>();for(String item:list(priority)){if(item.startsWith("payment:"))item="payment:"+(list(payOrder).indexOf("เครดิต")==0?"desc":"asc");result.add(item);}return String.join(",",result);}
 public static final String[] PRIORITY_CRITERIA={"price","timing","distance","rate","payment"};
 public static boolean eligible(JSONObject p,String key){return switch(key){case "price"->p.optBoolean("price",true);case "timing"->p.optInt("type",0)==0;case "distance"->p.optBoolean("rootDistanceOn");case "rate"->p.optBoolean("rootRateOn");case "payment"->new HashSet<>(list(p.optString("rootPayment"))).containsAll(Set.of("เครดิต","เงินสด"));default->false;};}
 public static List<String> priorities(JSONObject p){List<String> out=new ArrayList<>();if(!p.optBoolean("rootPriorityOn",!list(p.optString("rootPriority")).isEmpty()))return out;Set<String> seen=new HashSet<>();for(String item:list(p.optString("rootPriority"))){String[] v=item.split(":");if(v.length==2&&Arrays.asList(PRIORITY_CRITERIA).contains(v[0])&&eligible(p,v[0])&&seen.add(v[0]))out.add(item);}for(String key:PRIORITY_CRITERIA)if(eligible(p,key)&&seen.add(key))out.add(key+(key.equals("price")||key.equals("rate")?":desc":":asc"));return out;}
 public static BigDecimal comparisonPrice(JSONObject p,ScreenCardParser.Card c){return Commission.price(c.job(),p.optInt("formula"),Commission.fee(p.optInt("fee",20),c.commissionReduction()));}
 public static List<String> list(String s){return s==null||s.isBlank()?List.of():Arrays.asList(s.split(",",-1));}
 public static void validate(Map<String,Object> m){
  for(String k:new String[]{"rootPayment","rootPricing","rootServices","rootPayOrder","rootPriceOrder","rootServiceOrder"}){String s=(String)m.get(k);if(s==null||s.length()>2000||s.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("ตัวเลือกไม่ถูกต้อง");}
  Set<String> seen=new HashSet<>();for(String entry:list((String)m.get("rootPriority"))){String[] x=entry.split(":");if(x.length!=2||!Arrays.asList(CRITERIA).contains(x[0])||!Set.of("asc","desc").contains(x[1])||!seen.add(x[0]))throw new IllegalArgumentException("ลำดับไม่ถูกต้อง");}
 }
 public static String baseRejection(JSONObject p,ScreenCardParser.Card c){return new FilterRules(p,Commission.fee(p.optInt("fee",20),c.commissionReduction())).rejection(c.job());}
 public static String pricing(ScreenCardParser.Card c){for(String v:c.labels())if(Set.of("ส่งปกติ","ส่งด่วนพิเศษ","ส่งประหยัด","ส่งแบบประหยัด").contains(v))return v.equals("ส่งแบบประหยัด")?"ส่งประหยัด":v;return "";}
 public static String service(String s){if(s.toLowerCase(Locale.ROOT).contains("cod")||s.contains("เก็บเงิน"))return "COD";if(s.contains("อาหาร"))return "กล่องอาหาร";if(s.contains("เอกสาร"))return "เอกสาร";if(s.contains("ไปกลับ")||s.contains("ไป-กลับ")||s.contains("ไป–กลับ"))return "ไป-กลับ";if(s.contains("ยก"))return "ช่วยยก";if(s.contains("รถเข็น"))return "รถเข็น";if(s.contains("ฝากส่ง"))return "ฝากส่ง";if(s.contains("สูง"))return "ความสูงพิเศษ";if(s.contains("ยาว"))return "ความยาวพิเศษ";if(s.contains("น้ำหนัก"))return "น้ำหนักพิเศษ";if(s.contains("ชั่วโมง"))return "เหมารายชั่วโมง";return s;}
 public static String rejection(JSONObject p,ScreenCardParser.Card c,Double km){
  List<String> pay=list(p.optString("rootPayment")),prices=list(p.optString("rootPricing")),services=list(p.optString("rootServices"));
  if(!pay.isEmpty()&&!pay.containsAll(List.of("เครดิต","เงินสด"))&&!pay.contains(c.payment()))return "จ่ายผ่านไม่ตรง";
  if(!prices.isEmpty()&&!prices.containsAll(List.of("ส่งปกติ","ส่งด่วนพิเศษ","ส่งประหยัด"))&&!prices.contains(pricing(c)))return "รูปแบบงานไม่ตรง";
  if(p.optBoolean("rootServicesOn",!services.isEmpty())&&!services.isEmpty()&&!(services.contains("ไม่มีบริการเสริม")&&c.services().isEmpty())&&c.services().stream().map(RootRules::service).noneMatch(services::contains))return "บริการเสริมไม่ตรง";
  try{if(p.optBoolean("rootPickupOn")){if(c.pickupKm()==null)return "ไม่ทราบระยะห่างจากต้นทาง";if(!within(BigDecimal.valueOf(c.pickupKm()),p,"rootPickup"))return "ระยะห่างจากต้นทางไม่ตรง";}if(p.optBoolean("rootDistanceOn")){if(km==null)return "ไม่ทราบระยะทางงาน";if(!within(BigDecimal.valueOf(km),p,"rootDistance"))return "ระยะทางไม่ตรง";}
   if(p.optBoolean("rootRateOn")){BigDecimal r=rate(p,c,km);if(r==null)return "ไม่ทราบราคาเฉลี่ย/กม.";if(!within(r,p,"rootRate"))return "ราคาเฉลี่ย/กม.ไม่ตรง";}
  }catch(RuntimeException e){return "ตั้งค่าตัวกรองไม่ถูกต้อง";}return null;
 }
 /** A known failure may reject a detail even while an unrelated field is still loading. */
 public static String definiteRejection(JSONObject p,ScreenCardParser.Card c,boolean servicesKnown){
  String base=baseRejection(p,c);if(base!=null)return base;
  List<String> pay=list(p.optString("rootPayment")),prices=list(p.optString("rootPricing")),services=list(p.optString("rootServices"));
  if(!pay.isEmpty()&&!pay.containsAll(List.of("เครดิต","เงินสด"))&&!c.payment().isBlank()&&!pay.contains(c.payment()))return "จ่ายผ่านไม่ตรง";
  String pricing=pricing(c);if(!prices.isEmpty()&&!prices.containsAll(List.of("ส่งปกติ","ส่งด่วนพิเศษ","ส่งประหยัด"))&&!pricing.isBlank()&&!prices.contains(pricing))return "รูปแบบงานไม่ตรง";
  if(servicesKnown&&p.optBoolean("rootServicesOn",!services.isEmpty())&&!services.isEmpty()&&!(services.contains("ไม่มีบริการเสริม")&&c.services().isEmpty())&&c.services().stream().map(RootRules::service).noneMatch(services::contains))return "บริการเสริมไม่ตรง";
  try{
   if(p.optBoolean("rootPickupOn")&&c.pickupKm()!=null&&!within(BigDecimal.valueOf(c.pickupKm()),p,"rootPickup"))return "ระยะห่างจากต้นทางไม่ตรง";
   if(p.optBoolean("rootDistanceOn")&&c.routeKm()!=null&&!within(BigDecimal.valueOf(c.routeKm()),p,"rootDistance"))return "ระยะทางไม่ตรง";
   BigDecimal rate=p.optBoolean("rootRateOn")?rate(p,c,c.routeKm()):null;if(rate!=null&&!within(rate,p,"rootRate"))return "ราคาเฉลี่ย/กม.ไม่ตรง";
  }catch(RuntimeException ignored){}return null;
 }
 private static boolean within(BigDecimal n,JSONObject p,String key){return (key.equals("rootPickup")||n.compareTo(new BigDecimal(p.optString(key+"Min","0")))>=0)&&(key.equals("rootRate")||n.compareTo(new BigDecimal(p.optString(key+"Max","100")))<=0);}
 public static BigDecimal rate(JSONObject p,ScreenCardParser.Card c,Double km){if(km==null||!Double.isFinite(km)||km<=0)return null;double distance=km;if(p.optBoolean("rootRateIncludePickup")){if(c.pickupKm()==null||!Double.isFinite(c.pickupKm())||c.pickupKm()<0)return null;distance+=c.pickupKm();}return comparisonPrice(p,c).divide(BigDecimal.valueOf(distance),8,java.math.RoundingMode.HALF_UP);}
 private static Integer rank(List<String> order,String s){int i=order.indexOf(s);return i<0?null:i;}
 public static int compare(JSONObject p,ScreenCardParser.Card a,Double akm,ScreenCardParser.Card b,Double bkm){
  for(String item:priorities(p)){String[] parts=item.split(":");if(parts.length!=2)continue;String key=parts[0];Comparable av=null,bv=null;boolean descending=parts[1].equals("desc");
   switch(key){case "price":av=Commission.price(a.job(),p.optInt("formula"),Commission.fee(p.optInt("fee",20),a.commissionReduction()));bv=Commission.price(b.job(),p.optInt("formula"),Commission.fee(p.optInt("fee",20),b.commissionReduction()));break;case "rate":av=rate(p,a,akm);bv=rate(p,b,bkm);break;case "distance":av=akm;bv=bkm;break;case "payment":av=rank(descending?List.of("เครดิต","เงินสด"):List.of("เงินสด","เครดิต"),a.payment());bv=rank(descending?List.of("เครดิต","เงินสด"):List.of("เงินสด","เครดิต"),b.payment());descending=false;break;case "timing":av=a.job().type().equals("ขนส่งทันที")?0:1;bv=b.job().type().equals("ขนส่งทันที")?0:1;break;case "pricing":av=rank(list(p.optString("rootPriceOrder")),pricing(a));bv=rank(list(p.optString("rootPriceOrder")),pricing(b));descending=false;break;case "services":av=serviceRank(p,a);bv=serviceRank(p,b);descending=false;break;}
   int result=av==null?(bv==null?0:1):bv==null?-1:av.compareTo(bv)*(descending?-1:1);if(result!=0)return result;
  }return 0;
 }
 private static Integer serviceRank(JSONObject p,ScreenCardParser.Card c){Integer best=null;for(String s:c.services()){Integer r=rank(list(p.optString("rootServiceOrder")),service(s));if(r!=null&&(best==null||r<best))best=r;}return best;}
}
