package com.fulllock.app;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Read-only explanation of observed special-service price components.
 * Does not infer a tip from base fare, total, payouts or a price difference.
 * Even a raw TIP key is labelled unverified until compared with the Driver UI.
 */
final class PriceComponentDiagnostics {
    private PriceComponentDiagnostics() {}
    static String describe(String type, String key, Number valueFen) {
        if(valueFen == null)return "";
        BigDecimal amount;
        try {
            amount = new BigDecimal(valueFen.toString());
            if(amount.scale() > 0 || amount.abs().compareTo(new BigDecimal("1000000000"))>0)return "";
        }catch(NumberFormatException e){return "";}
        String code = safeCode(type, key);
        String money = amount.movePointLeft(2).setScale(2).toPlainString();
        if("TIP".equals(code) && amount.signum()>=0)
            return "รายการชนิด TIP (ยังไม่ยืนยันกับภาพงาน): ฿"+money;
        return "ค่าเสริม "+code+": ฿"+money;
    }
    private static String safeCode(String type,String key) {
        for(String s:new String[]{type,key})if(s!=null){
            String v=s.toUpperCase(Locale.ROOT);
            if(v.matches("[A-Z0-9_\\-]{1,48}"))return v;
        }
        return "UNKNOWN";
    }
}