package com.fulllock.app;
import java.math.BigDecimal;
import java.math.RoundingMode;

final class JobDisplayUnits {
 private JobDisplayUnits(){}
 static String baht(Number minor){return "฿"+valid(minor).movePointLeft(2).setScale(2,RoundingMode.UNNECESSARY).toPlainString();}
 static String kilometres(Number metres){return valid(metres).movePointLeft(3).stripTrailingZeros().toPlainString()+" กม.";}
 private static BigDecimal valid(Number value){if(value==null)throw new IllegalArgumentException();BigDecimal n=new BigDecimal(value.toString());if(n.signum()<0||n.compareTo(new BigDecimal("1000000000000"))>0)throw new IllegalArgumentException();return n;}
 static boolean coordinates(double latitude,double longitude){return Double.isFinite(latitude)&&Double.isFinite(longitude)&&latitude>=-90&&latitude<=90&&longitude>=-180&&longitude<=180&&(latitude!=0||longitude!=0);}
}
