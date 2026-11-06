package com.fulllock.app;
import org.json.*;import java.util.*;
/** Pure visible-card parser. Incomplete or conflicting identity fields cannot become action targets. */
public final class ScreenCardParser {
 public record Card(String token,String key,JobParser.Job job,String payment,List<String> services,List<String> labels,Double routeKm,java.math.BigDecimal commissionReduction,Double pickupKm){
  public Card(String token,String key,JobParser.Job job,String payment,List<String> services,List<String> labels,Double km,java.math.BigDecimal reduction){this(token,key,job,payment,services,labels,km,reduction,null);}
  public Card(String token,String key,JobParser.Job job,String payment,List<String> services,List<String> labels,Double km){this(token,key,job,payment,services,labels,km,java.math.BigDecimal.ZERO);}
 }
 public static List<Card> parse(JSONArray nodes)throws JSONException{
  Map<String,Map<String,Set<String>>> groups=new LinkedHashMap<>();Map<String,String> keys=new HashMap<>();
  for(int i=0;i<Math.min(60,nodes.length());i++){JSONObject n=nodes.optJSONObject(i);if(n==null)continue;String token=n.optString("cardToken"),kind=n.optString("kind"),value=n.optString("value");if(!token.matches("[a-f0-9]{24}")||value.isBlank()||value.length()>80)continue;String key=n.optString("cardKey",token);if(!key.matches("[a-f0-9]{24}"))continue;keys.put(token,key);groups.computeIfAbsent(token,k->new LinkedHashMap<>()).computeIfAbsent(kind,k->new LinkedHashSet<>()).add(value);}
  List<Card> out=new ArrayList<>();
  for(var group:groups.entrySet()){var f=group.getValue();String origin=one(f,"origin"),dest=one(f,"destination"),price=one(f,"price"),timing=one(f,"timing");Set<String> categories=new LinkedHashSet<>(f.getOrDefault("label",Set.of()));categories.retainAll(Set.of("ขนส่งสินค้า","รับ-ส่งผู้โดยสาร"));
   if(origin==null||dest==null||price==null||timing==null||categories.size()!=1||!area(origin)||!area(dest)||!price.matches("[0-9,.]+(?: [+] [0-9,.]+)?")||!Set.of("ขนส่งทันที","งานล่วงหน้า").contains(timing))continue;
   String title="["+categories.iterator().next()+"] "+origin+" > "+dest,text=timing+" ฿"+price.replace(" + "," + ฿");JobParser.Job job=JobParser.parse(title,text);if(job==null)continue;String payment=one(f,"payment");if(!Set.of("เงินสด","เครดิต").contains(payment==null?"":payment))payment="ไม่ระบุวิธีรับเงิน";
   java.math.BigDecimal reduction=java.math.BigDecimal.ZERO;if(f.containsKey("commissionReduction")){String raw=one(f,"commissionReduction");try{if(raw==null)continue;reduction=new java.math.BigDecimal(raw);if(reduction.signum()<0||reduction.compareTo(java.math.BigDecimal.valueOf(100))>0||reduction.scale()>2)continue;}catch(RuntimeException e){continue;}}
   out.add(new Card(group.getKey(),keys.get(group.getKey()),job,payment,new ArrayList<>(f.getOrDefault("service",Set.of())),new ArrayList<>(f.getOrDefault("label",Set.of())),distance(one(f,"routeDistance")),reduction,pickupDistance(one(f,"pickupDistance"))));
  }return out;
 }
 private static Double pickupDistance(String value){try{double n=Double.parseDouble(value);return Double.isFinite(n)&&n>=0&&n<10000000?n/1000:null;}catch(Exception e){return null;}}
 private static Double distance(String value){try{double n=Double.parseDouble(value);return Double.isFinite(n)&&n>0&&n<10000000? n/1000:null;}catch(Exception e){return null;}}
 private static String one(Map<String,Set<String>> fields,String key){Set<String> v=fields.get(key);return v!=null&&v.size()==1?v.iterator().next():null;}
 private static boolean area(String v){return v.codePoints().allMatch(x->Character.isLetterOrDigit(x)||Character.getType(x)==Character.NON_SPACING_MARK||" .()-".indexOf(x)>=0);}
}
