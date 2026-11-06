# Full Lock → GeoX Binder relay v1

วันที่ 2026-11-04 · Implementation ฝั่ง GPS พร้อมคอมไพล์/ทดสอบ JVM แล้ว; ยังไม่ทดสอบข้ามแอปจริง

## ขอบเขต
เพิ่ม bound service `io.github.phantom.gps/io.github.phantom.gps.utils.FullLockRelayService`.
ใช้ Explicit Component + bindService(BIND_AUTO_CREATE) จาก Full Lock ขณะเตรียมระบบ; คง binding ไว้ตลอด session.
ไม่เปิด Activity ไม่ใช้ clipboard รับพิกัด ไม่สร้าง network listener ไม่เปลี่ยน scope LSPosed.
Intent เดิมยังรองรับและใช้ pipeline เดียวกัน.

## Bootstrap และสิทธิ์
1. Full Lock bind service แล้ว transact OPEN จาก UID ของ `com.fulllock.app` จริงเท่านั้น.
2. GPS ตรวจ UID จาก Binder.getCallingUid และ PackageManager (ไม่รับ shared UID กับแพ็กเกจอื่น).
3. Full Lock แนบ Binder เปล่าสำหรับ lifetime ซึ่งต้องสร้างในโปรเซส Full Lock และเก็บไว้ตลอด session.
4. GPS คืน private session Binder; Full Lock ส่ง handle นี้ผ่านช่องทางที่ตรวจ UID อยู่แล้วให้ LocalRelayMain.
   **ห้ามส่ง session Binder ให้ Probe/Driver หรือ broadcast ออกไป.** ตัวกลางเป็นผู้เก็บ handle.
5. ตัวกลางใช้ session Binder ส่งพิกัดจาก UID 0. แค่ UID 0 ไม่พอ: ต้องถือ capability ที่ Full Lock ขอไว้.
6. Full Lock KEEP_ALIVE ทุก 5 วินาที (ห้ามรอการแตะ). อายุ 15 วินาที; หมดแล้วต้อง OPEN ใหม่ ไม่ชุบชีวิต session เดิม.
7. ปิดโหมด/หมดสิทธิ์ ให้ CLOSE ก่อน unbind; Full Lock ตาย, lifetime Binder ตาย, service unbind/destroy หรือเปิด session ใหม่ จะ revoke ของเก่า.
8. GPS ตรวจใบอนุญาต/ความถูกต้องของตัวเองก่อนใช้พิกัดทุกครั้ง. ไม่มีการข้าม license.

ขอบเขตความเชื่อถือ: เชื่อแอปที่ครอบครอง package/UID Full Lock ตาม contract เดิม ยังไม่ pin ใบรับรอง Full Lock.
ไม่มีระบบ IPC ในเครื่องที่ป้องกันผู้โจมตีที่ควบคุม root/แก้โค้ดทุกโปรเซสได้อย่างสมบูรณ์.

## Wire format: Android Parcel (ไม่ใช่ AIDL)
ทุก transact ใช้ flags=0 พร้อม reply แล้ว readException(). อย่าเรียกจาก UI thread.
ส่งเฉพาะ platform classes; ห้าม parcel subclass ResultReceiver ของแอปโดยตรง.
ใช้ local.writeToParcel แล้ว ResultReceiver.CREATOR.createFromParcel เพื่อสร้าง platform proxy ก่อนส่ง.

Gateway descriptor `io.github.phantom.gps.FullLockRelay.v1`
- OPEN = FIRST_CALL_TRANSACTION (1)
- writeInterfaceToken(descriptor), writeInt(1), writeStrongBinder(fullLockLifetime)
- reply: writeNoException(), writeStrongBinder(session)

Session descriptor `io.github.phantom.gps.LocationSession.v1`
- SUBMIT=1 (เฉพาะ root relay)
- KEEP_ALIVE=2 (เฉพาะ Full Lock UID)
- CLOSE=3 (เฉพาะ Full Lock UID)

SUBMIT payload ตามลำดับ:
1. writeInterfaceToken(sessionDescriptor)
2. writeInt(api_version=1)
3. writeString(request_id) UUID ใหม่ต่อการแตะ
4. writeDouble(latitude)
5. writeDouble(longitude)
6. writeString(label หรือ "")
7. writeString(target_package หรือ "")
8. platformResultReceiver.writeToParcel(parcel, 0) โดยตรง **ไม่มี presence marker**

reply ของ SUBMIT หมายถึงรับเข้าประมวลผลเท่านั้น ไม่ใช่ตั้งพิกัดแล้ว.
ผลจริงใช้ ResultReceiver พร้อม request_id / APPLIED / HOOK_LOADED ตาม Intent contract.
KEEP_ALIVE/CLOSE มีเพียง writeInterfaceToken และ readException().
คำขอสูงสุด 8192 bytes. ตรวจ finite/range/string length ผ่าน DirectLocationRequest เดิม.
เพิ่ม code503 status BUSY เมื่อคิวเต็ม (32 waiting); ไม่มี retry อัตโนมัติ และคำขอที่ถูกปฏิเสธไม่ทำให้คำขอที่รับแล้วกลายเป็น SUPERSEDED.

## รายการที่ต้องตรวจฝั่ง Full Lock (มีโค้ดแล้ว ยังต้องทดสอบร่วมกัน)
- เพิ่ม operation สำหรับมอบ session Binder ให้ LocalRelayMain จาก owner UID เท่านั้น.
- เพิ่ม operation ส่งพิกัดแยกจากคิวอ่านหน้าจอ ให้รับจาก Driver UID ที่อนุญาตเท่านั้น.
- ตัวกลางส่ง GPS บน worker แยก ไม่ block Main/Binder thread ด้วย license/disk/Hook wait.
- Probe ส่ง full precision numeric model ที่แตะ; ห้ามใช้ค่าปัดเศษที่วาดบนป้าย.
- ข้อตกลงล่าสุด 2026-11-04: เมื่อมี GPS แตะป้ายแล้วส่ง Binder อย่างเดียว ไม่คัดลอก clipboard และไม่คัดลอกเป็น fallback แม้ส่งไม่ได้. ไม่มี GPS ให้แสดงป้ายอย่างเดียวตามกติกา Full Lock.
- ใช้ request_id ล่าสุดต่อการเลือก กรองผลเก่าทั้ง UI และ fallback.
- ใช้ death recipient ต่อ GPS session/service; reconnect ผ่าน Full Lock และ OPEN ใหม่.
- ถ้าทำ Intent fallback ต้องมี dedupe ข้ามช่องทาง ไม่ส่งซ้ำอย่างไม่ทราบผล.
- Callback จับเวลา end-to-end ด้วย elapsedRealtime จาก Probe; elapsed_ms GPS นับเฉพาะฝั่ง GPS.
- ไม่บันทึก lat/lng/session capability ใน log ที่ส่งต่อ.

## ลำดับและข้อจำกัด
Intent/Binder ใช้ worker เดียว คิวจำกัด และ sequence เดียว: คำขอที่ยังไม่เริ่มและเก่ากว่าจะ SUPERSEDED.
คำขอที่ commit ไปแล้วอาจมีผลชั่วคราวก่อนคำขอใหม่ commit; การถอน session ไม่ย้อนคืนพิกัดที่ commit แล้ว.
ตรวจ session ก่อนเริ่มและก่อน commit path; การ revoke ระหว่าง commit ที่เริ่มไปแล้วไม่รับประกัน rollback.
Service ไม่ใช้ foreground notification; Android อาจฆ่าเมื่อ Full Lock/client ถูกจำกัดเบื้องหลัง ต้อง reconnect.
เริ่มเผยแพร่ Binder ใน GeoX 0.0.16 / versionCode16; ควรใช้ bind+OPEN handshake ตรวจรองรับด้วย.

## ต้องทดสอบจริงก่อนเปิดใช้
HMA ซ่อน/ไม่ซ่อน Full Lock; session หมดอายุ/ปิดโหมด/kill GPS/kill relay/kill Full Lock;
ผู้ส่งปลอม/UID อื่น/parcel ผิด/NaN/ช่วงเกิน; ใบอนุญาตหมดอายุ;
แตะเร็วหลายจุด; APPLIED กับ HOOK_LOADED; clipboard; manual Intent เดิมไม่ถอยหลัง.
วัด p50/p95 แตะ→GPSรับ และแตะ→APPLIED เทียบ Intent บนเครื่องเดียวกัน.

## Full Lock implementation update 2026-11-04
Full Lock เพิ่ม owner-only GPS_CONFIG=5 และ Driver-only GPS_SEND=6 ใน LocalRelayMain แล้ว. GPS session Binder เก็บเฉพาะตัวกลาง ไม่ส่งให้ Driver. ตัวกลาง clearCallingIdentity ก่อน transact GPS เพื่อให้ Binder เห็น root UIDจริง. Full Lock bind/OPENล่วงหน้าและ KEEP_ALIVE5วินาที. Probe ใช้ workerแยกจากการอ่านหน้าจอ ไม่เขียนclipboard/ไม่retryเอง. ยังต้องทดสอบroot/HMA/IPCจริงร่วมกันก่อนเผยแพร่.

## Membership 0.0.17

รอบนี้เพิ่ม signed membership lease ใน Engine/Hook ต้อง login บัญชี PHANToM ที่ยังไม่หมดอายุก่อนส่ง การตรวจต่อการแตะยัง local ไม่มี network. 402/403 ให้ผู้ใช้เปิดแอปดูสถานะหรือเข้าสู่ระบบ ไม่ขอ activation code; bootstrap session กับ membership session เป็นคนละสิทธิ์. อ่านเอกสารสมาชิกในโปรเจค GPS ก่อนนำรุ่นใหม่ไปใช้จริง (ยังไม่ deploy/push)
