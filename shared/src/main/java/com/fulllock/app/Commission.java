package com.fulllock.app;
import java.math.*;import java.util.regex.*;
/** Percentage-point reduction on the configured fee; never a mutation of profile settings. */
public final class Commission {
 private static final Pattern LABEL=Pattern.compile("^ค่าคอมมิ[\\p{L}\\p{M}]{2,8}ต่ำลง([0-9]+(?:[.][0-9]{1,2})?)[%％]$");
 public static String reductionLabel(String text){String s=text.replaceAll("[\\p{Z}\\s]+","");Matcher m=LABEL.matcher(s);if(m.matches())try{BigDecimal value=new BigDecimal(m.group(1));return value.signum()>=0&&value.compareTo(BigDecimal.valueOf(100))<=0?value.stripTrailingZeros().toPlainString():"invalid";}catch(RuntimeException e){return "invalid";}return s.startsWith("ค่าคอมมิ")&&s.contains("ต่ำลง")?"invalid":null;}
 public static BigDecimal fee(int configured,BigDecimal reduction){return BigDecimal.valueOf(Math.max(0,Math.min(100,configured))).subtract(reduction==null?BigDecimal.ZERO:reduction).max(BigDecimal.ZERO);}
 public static BigDecimal price(JobParser.Job job,int formula,BigDecimal fee){BigDecimal n=job.fare();if(formula>=2)n=n.multiply(BigDecimal.valueOf(100).subtract(fee)).movePointLeft(2);return formula%2==1?n.add(job.tip()):n;}
 public static BigDecimal net(JobParser.Job job,int configured,BigDecimal reduction){return price(job,3,fee(configured,reduction)).setScale(2,RoundingMode.HALF_UP);}
}
