package com.fulllock.app;
import java.util.*;import java.math.*;import java.util.regex.*;
/** Coordinates are read only from an identified, unmasked native pickup model. */
public final class PickupCoordinates {
 public record Point(String latitude,String longitude){public String clipboard(){return latitude+","+longitude;}}
 private static Object get(Object o,String name){return BoundJobDistance.get(o,name);}
 private static Object axis(Object coordinates,String getter,String field){Object value=get(coordinates,getter);if(value instanceof Number)return value;try{return coordinates==null?null:coordinates.getClass().getField(field).get(coordinates);}catch(Exception ignored){return null;}}
 public static Point resolve(List<?> models,String origin,String destination,String display,boolean exactItem){
  Matcher price=Pattern.compile("^\\s*฿?\\s*([0-9][0-9,]*(?:[.]\\d{1,2})?)").matcher(display);if(!price.find())return null;BigDecimal fen;try{fen=new BigDecimal(price.group(1).replace(",","")).movePointRight(2);}catch(Exception e){return null;}
  String uuid=null;Point result=null;
  for(Object model:models){if(!Boolean.FALSE.equals(get(model,"getMaskedAddress")))continue;Object id=get(model,"getUuid"),fare=get(model,"getTotalPriceFen"),raw=get(model,"getAddrInfo");if(id==null||id.toString().isBlank()||!(fare instanceof Number)||!(raw instanceof List<?> stops)||stops.size()<2)continue;
   if(!origin.equals(String.valueOf(get(stops.get(0),"getDistrictName")))||!destination.equals(String.valueOf(get(stops.get(stops.size()-1),"getDistrictName"))))continue;
   try{if(!exactItem&&fen.compareTo(new BigDecimal(fare.toString()))!=0)continue;}catch(Exception e){continue;}
   if(uuid!=null&&!uuid.equals(id.toString()))return null;uuid=id.toString();Object pickup=null;for(Object stop:stops)if("PICK_UP".equals(String.valueOf(get(stop,"getAddressType")))){if(pickup!=null)return null;pickup=stop;}if(pickup==null)pickup=stops.get(0);
   Object coordinates=get(pickup,"getLatLng"),lat=axis(coordinates,"getLatitude","latitude"),lng=axis(coordinates,"getLongitude","longitude");if(!(lat instanceof Number)||!(lng instanceof Number))continue;Point point=point((Number)lat,(Number)lng);if(point==null)continue;if(result!=null&&!result.equals(point))return null;result=point;
  }return result;
 }
 public static Point point(Number latitude,Number longitude){double lat=latitude.doubleValue(),lng=longitude.doubleValue();if(!Double.isFinite(lat)||!Double.isFinite(lng)||Math.abs(lat)>90||Math.abs(lng)>180||(lat==0&&lng==0))return null;try{return new Point(new BigDecimal(latitude.toString()).stripTrailingZeros().toPlainString(),new BigDecimal(longitude.toString()).stripTrailingZeros().toPlainString());}catch(Exception e){return null;}}
}
