package com.fulllock.app;

/** User-facing results; membership failures are distinct from transport failures. */
public final class GeoXFeedback {
 public static String result(int code,boolean applied){
  return switch(code){
   case 100 -> "ตั้งพิกัดแล้ว";
   case 101 -> "โมดูลรับพิกัดแล้ว";
   case 402 -> "ยังตั้งพิกัดไม่ได้ · ตรวจสถานะ GPS";
   case 403 -> "ยังตั้งพิกัดไม่ได้ · ตรวจสถานะ GPS";
   case 409 -> "มีคำสั่งพิกัดใหม่กว่า";
   case 503 -> "Gozza Lalamove กำลังทำงาน กรุณาแตะใหม่";
   case 401 -> "เชื่อมต่อ Gozza Lalamove ใหม่";
   default -> applied?"ตั้งพิกัดแล้ว · ยังยืนยันโมดูลไม่ได้":"ส่งพิกัดไป Gozza Lalamove ไม่สำเร็จ";
  };
 }
 public static String connectionFailed(){return "เชื่อมต่อ Gozza Lalamove ไม่สำเร็จ";}
 private GeoXFeedback(){}
}
