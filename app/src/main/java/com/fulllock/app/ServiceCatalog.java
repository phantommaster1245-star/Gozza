package com.fulllock.app;
/** Full Lock symbols are our display labels, not copies of Lalamove brand assets. */
final class ServiceCatalog {
 static String describe(String label){if(label==null||label.isBlank())return "บริการ: ไม่ระบุ";String lower=label.toLowerCase(java.util.Locale.ROOT);String symbol=lower.contains("cod")||label.contains("เก็บเงิน")?"฿":label.contains("อาหาร")?"🍱":label.contains("ไป-กลับ")||label.contains("ไปกลับ")||label.contains("ไป–กลับ")?"↔":label.contains("เอกสาร")?"▤":label.contains("ช่วยยก")?"↥":label.contains("รถเข็น")?"▣":label.contains("สูง")?"↕":label.contains("ยาว")?"↔":label.contains("น้ำหนัก")?"⚖":label.contains("เหมา")?"◷":label.contains("ฝากส่ง")?"✉":"◇";return symbol+" "+label;}
}
