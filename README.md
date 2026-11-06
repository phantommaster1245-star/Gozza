# Gozza Lalamove

แอป Android ส่วนตัวสำหรับผู้ทดสอบเฉพาะกลุ่ม รวม Full Lock และ PHANToM GPS ใน APK เดียว

- ฐาน Full Lock: working tree ล่าสุดจาก `C:\Project_winsurf\Full_lock` รุ่น 0.1.47 รวมไฟล์ที่ยังไม่ commit
- GPS: `C:\Project_winsurf\PHANToM GPS\phantom-gps\app` พร้อม LICENSE จาก workspace GPS
- Application ID: `com.gozza.lalamove`; Android 8.1 (API 27) ขึ้นไป
- หน้าหลัก: แผนที่; เมนูซ้ายเลือก **แผนที่และ GPS**, **อ่านข้อมูลและวาดทับ**, **ตั้งค่า GPS** ได้ภายใน APK เดียว
- แตะป้ายพิกัดจุดรับ (ไม่มีการคัดลอก) → Driver hook → root relay → GPS Binder ภายในแพ็กเกจเดียว → ตรวจความถูกต้องของแอปและพิกัด → ใช้พิกัดและรอ hook ยืนยัน
- รวม Driver hook และ GPS hook ใน `assets/xposed_init`; ไม่ติดตั้ง companion APK แยก
- เปิดโมดูล Gozza Lalamove ใน LSPosed เลือก System Framework, Lalamove Driver และ Gozza Lalamove แล้วรีสตาร์ตเครื่องตามขั้นตอนของ LSPosed
- ปิดการตรวจ/บังคับอัปเดตจาก Full Lock และ GeoX สาธารณะ แจก APK โดยตรง ไม่มีการ publish เว็บหรือ deploy backend

## โหมดอ่านข้อมูลและวาดทับ

ใช้ธีมเดียวกัน: ส้ม `#F16623`, พื้นดำ `#101416`, พื้นแผง `#1B2023` ทั้งหน้าจอ แผนที่ และป้ายข้อมูล

ถอดหน้ากรองงาน โปรไฟล์ตัวกรอง ปุ่มเริ่มอัตโนมัติ และคำสั่งเปิด/รับ/ย้อนหน้างานออก ค่าจากรุ่นเดิมไม่มีผลต่อการเปิดงาน Endpoint ที่เคยอนุมัติคำสั่งคืนค่าว่างเสมอ การอ่านข้อมูลและวาดทับยังทำงาน โดยไม่วาดป้ายงานที่ตรงเงื่อนไข

ป้ายพิกัดเรียก `PickupGpsSender` โดยตรง ไม่มีสาขาคัดลอกคลิปบอร์ด หากพิกัดไม่พร้อมหรือเชื่อม hook ไม่ได้จะแจ้งสถานะ ไม่เปิดงานหรือคัดลอกแทน

ชุดทดสอบอัตโนมัติเดิมที่อาศัยหน้า/คำสั่งกรองงานเก็บที่ `build-backups/upstream-job-automation-tests` แทนด้วย `ReadOnlyChecks` สำหรับตรวจค่าค้างจากรุ่นเดิม การปฏิเสธคำสั่งเปิด/รับงาน และการส่งพิกัดตรง ชุดนี้ build แล้วแต่ยังไม่ได้รันบนเครื่องทดสอบที่หลุดจาก ADB

## Build และตรวจสอบ

```powershell
.\gradlew.bat :app:assembleDebug :app:lintDebug :gps:testFullDebugUnitTest
```

APK: `app/build/outputs/apk/debug/app-debug.apk` เป็น debug build สำหรับทดสอบ ยังไม่ใช่ release สำหรับแจกใช้งานจริง

## ใช้งานโดยไม่ล็อกอิน

Gozza รุ่นนี้ไม่ใช้บัญชีสมาชิก ไม่มีวันหมดอายุสมาชิก และไม่ต้อง deploy backend เปิดแผนที่ได้จากเมนูทันที GPS hook ใช้สถานะเริ่ม/หยุดที่ผู้ใช้เลือกในเครื่อง คงการตรวจลายเซ็นแอป การตรวจข้อมูลพิกัดและการตรวจผู้เรียก Binder ไว้

ถอด Firebase SDK และซอร์สหน้าล็อกอินออกจาก build ไม่มีการเรียกตรวจหรือต่ออายุสมาชิกใน flow GPS ซอร์ส backend ที่เตรียมไว้ก่อนหน้านี้เก็บเป็นสำรองใน `build-backups/unused-private-backend` และไม่ได้ deploy

## การตรวจบนอุปกรณ์

ยังไม่ได้ติดตั้งหรือทดสอบบนเครื่องจริง ต้องตรวจเปิดแผนที่โดยไม่มีหน้าล็อกอิน, สิทธิ์ตำแหน่ง/แจ้งเตือน/overlay, การทำงาน LSPosed และการแตะจุดรับส่งพิกัดจนได้รับผลยืนยัน

สำรอง Beta เดิมก่อนอัปเดตอยู่ใน `build-backups/before-gozza-*` โฟลเดอร์ต้นทางทั้งสองโปรเจกต์ไม่ได้ถูกแก้ไข
เอกสาร upstream เดิมอยู่ที่ `docs/FULL_LOCK_UPSTREAM_README.md`; คำสั่งเผยแพร่เว็บในเอกสารเดิมไม่ใช้กับ Gozza
