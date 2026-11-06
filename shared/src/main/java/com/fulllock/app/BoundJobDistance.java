package com.fulllock.app;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.*;
/** Resolves only models verified against this native card, never pickup distance. */
public final class BoundJobDistance {
 private BoundJobDistance(){}
 private static final Pattern PRICE=Pattern.compile("^\\s*(?:฿\\s*|THB\\s*)?([0-9][0-9,]*(?:\\.[0-9]{1,2})?)(?=\\s*(?:฿|บาท|\\+|$))");
 public static Object get(Object model,String name){try{if(model==null)return null;var method=model.getClass().getMethod(name);method.setAccessible(true);return method.invoke(model);}catch(Exception ignored){return null;}}
 public static Double metres(List<?> models,String origin,String destination,String price){return resolve(models,origin,destination,price,false);}
 /** Only for an exact current adapter item verified by position and unchanged card identity. */
 public static Double exactItemMetres(List<?> models,String origin,String destination,String price){return resolve(models,origin,destination,price,true);}
 private static Double resolve(List<?> models,String origin,String destination,String price,boolean exactItem){
  Matcher amount=PRICE.matcher(price.trim());if(!amount.find())return null;
  BigDecimal fen;try{fen=new BigDecimal(amount.group(1).replace(",","")).movePointRight(2);}catch(NumberFormatException ignored){return null;}
  String uuid=null;Double distance=null;
  for(Object model:models){Object id=get(model,"getUuid"),fare=get(model,"getTotalPriceFen"),stops=get(model,"getAddrInfo");
   if(id==null||id.toString().isBlank()||!(fare instanceof Number)||!(stops instanceof List<?> list)||list.size()<2)continue;
   if(!origin.trim().equals(String.valueOf(get(list.get(0),"getDistrictName")).trim())||!destination.trim().equals(String.valueOf(get(list.get(list.size()-1),"getDistrictName")).trim()))continue;
   try{if(!exactItem&&fen.compareTo(new BigDecimal(fare.toString()))!=0)continue;}catch(NumberFormatException ignored){continue;}
   if(uuid!=null&&!uuid.equals(id.toString()))return null;uuid=id.toString();
   Object raw=get(model,"getTotalDistance");if(!(raw instanceof Number))continue;double metres=((Number)raw).doubleValue();
   if(!Double.isFinite(metres)||metres<=0||metres>=10000000)continue;
   if(distance!=null&&Double.compare(distance,metres)!=0)return null;distance=metres;
  }return distance;
 }
}
