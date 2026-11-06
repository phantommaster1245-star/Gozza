# GeoX development

`app/` คือซอร์สแอปปัจจุบันที่ build จริง รวมส่วนที่สืบทอด/ดัดแปลงจาก GPS Setter ไม่ใช่เฉพาะไฟล์ที่เขียนใหม่
`installer/` และ `firebase/` เป็นโมดูลและ backend ที่เพิ่มสำหรับ GeoX

เรียก Gradle จาก root workspace หนึ่งระดับขึ้นไป ไม่ต้องย้าย google-services.json หรือ build ใหม่จากต้นแบบ

ส่วนเชื่อมต่อ Full Lock เริ่มที่ `app/src/main/java/io/github/phantom/gps/utils/DirectLocationReceiver.kt` และ `INTENT_LOCATION_BRIDGE.md` ชุดส่งต่อพร้อมรายการไฟล์อยู่ `../full-lock-integration/`

ระบบสมาชิกใหม่อยู่ใน `firebase/functions/membership*.js`, `app/` และ `web/` (เว็บไซต์สำเนาทำงานในเครื่อง) Installer เปลี่ยนเป็น GeoX Admin สำหรับค้นหา/ต่ออายุ อ่าน `docs/MEMBERSHIP_HANDOFF.md` ก่อนเปิดใช้จริง
