# คู่มือเชื่อม Full Lock → GeoX ด้วย Intent

API version **1** · GeoX **0.0.15 / versionCode 15 ขึ้นไป**

เอกสารนี้สำหรับ agent ที่ดูแล Full Lock เปลี่ยนปุ่ม/ป้ายตำแหน่งต้นทางจากการคัดลอก lat,lng เป็นการส่งตัวเลขตรงมายัง GeoX แล้วแสดงผลตอบกลับ ไม่ต้องเรียก `su`, `am`, ADB, ไม่ต้องเขียนคลิปบอร์ด และไม่ต้องเปิด Clipboard monitor

งานรอบนี้เพิ่มฝั่ง GeoX แล้ว **ยังไม่ได้แก้ Full Lock** การทำ Hook ยังต้องติดตั้ง/เปิดโมดูลและตั้ง scope ให้แอปปลายทางตามปกติ หลังอัปเดตโมดูลควรรีบูตหนึ่งครั้ง

## เส้นทางการทำงาน

1. ผู้ใช้แตะป้ายตำแหน่งต้นทางใน Full Lock
2. Full Lock สร้าง request ID ใหม่และส่ง Explicit Broadcast พร้อม lat,lng ชนิด `double`
3. PHANToM ตรวจความถูกต้องของตัวเลขและใบอนุญาตในเครื่อง จากนั้น commit พิกัดและสั่งเริ่มใช้ตำแหน่งทันที
4. ส่งสัญญาณให้ Hook รีเฟรช แล้วตอบ `APPLIED` กลับผ่าน Binder (`ResultReceiver`) **ก่อน** ทำงาน UI/แจ้งเตือน
5. ถ้าระบุ `target_package` จะตรวจยืนยันกับ Hook ใน package นั้นและส่ง `HOOK_LOADED` ตามมาเมื่อ Hook โหลดพิกัดของ request นี้แล้ว

ไม่มีการแปลงข้อความ ค้นหาสถานที่ หรือเรียกเซิร์ฟเวอร์เพื่อตรวจรหัสระหว่างคำสั่งนี้ พิกัดใหม่เก็บด้วยความละเอียด Double 64 บิต พิกัดจากคำสั่งนี้ไม่ผ่านการสุ่มรัศมี แม้ผู้ใช้เปิด Random Position ไว้สำหรับช่องทางอื่น

`APPLIED` ไม่ได้หมายความว่าแอปปลายทางอ่านตำแหน่งแล้ว ส่วน `HOOK_LOADED` ยืนยันว่าตัวโมดูลใน process ปลายทางโหลด snapshot นี้แล้ว **ยังไม่ใช่หลักฐานว่า UI/เซิร์ฟเวอร์ของแอปปลายทางอัปเดตตามแล้ว**

## ผู้ส่งที่รองรับ

รับเฉพาะ Full Lock package `com.fulllock.app` โดยต้องแนบ `PendingIntent` ที่ Android ระบุว่าถูกสร้างโดย UID ของ Full Lock ที่ติดตั้งอยู่ ไม่ใช้ extra ข้อความชื่อ package เป็นหลักฐานตัวตน

- ตัว token เป็น `PendingIntent` แบบ immutable ใช้พิสูจน์ตัวตนเท่านั้น PHANToM จะไม่เรียก `send()` บน token นี้
- ไม่ต้องใช้ certificate เดียวกับ GeoX
- build ที่ใช้ applicationId `.qa` ยังไม่อยู่ในรายชื่อที่อนุญาต ห้ามเปลี่ยน extra เพื่อแอบอ้างชื่อ package; ใช้ build `com.fulllock.app` สำหรับการเชื่อมนี้
- token เป็น capability ของ Full Lock อย่านำไปแชร์ให้แอปอื่น

## Intent contract

```text
Package:   io.github.phantom.gps
Component: io.github.phantom.gps/io.github.phantom.gps.utils.DirectLocationReceiver
Action:    io.github.phantom.gps.action.SET_LOCATION_DIRECT
```

ใช้ `setComponent()` เสมอ และเพิ่ม `Intent.FLAG_RECEIVER_FOREGROUND` เพื่อให้ Android จัดส่งในคิว foreground ไม่ใช่การเปิด Activity

| Extra | ชนิด | จำเป็น | ความหมาย |
|---|---|---|---|
| `api_version` | int | ใช่ | `1` |
| `request_id` | String | ใช่ | ID ใหม่ต่อการแตะแต่ละครั้ง; UUID ใช้ได้ ความยาว 1–96 ตัว `[A-Za-z0-9_.-]` |
| `latitude` | double | ใช่ | finite, −90 ถึง 90; **อย่าส่ง String หรือ Float** |
| `longitude` | double | ใช่ | finite, −180 ถึง 180; **อย่าส่ง String หรือ Float** |
| `reply` | android.os.ResultReceiver | ใช่ | Binder callback ที่ parcel เป็น platform class แล้ว ดูตัวอย่างด้านล่าง |
| `caller_identity` | android.app.PendingIntent | ใช่ | immutable token ที่สร้างใน Full Lock |
| `label` | String | ไม่ | ชื่อจุด/ต้นทาง สูงสุด 160 ตัวอักษร |
| `target_package` | String | ไม่ | package จริงของแอปปลายทางที่เปิด GeoX ใน LSPosed scope; ใช้รับ `HOOK_LOADED` |

ไม่ระบุ `target_package` จะได้เพียงผลเร็ว `APPLIED` การระบุ package นี้เลือกผู้ที่ต้องยืนยัน ไม่ได้จำกัดการใช้พิกัดใหม่เฉพาะ package นี้: Hook ใน scope อื่นยังรับพิกัดใหม่ตามกลไกปกติ

## ผลตอบกลับ

`ResultReceiver.onReceiveResult(int resultCode, Bundle data)` จะถูกเรียกโดยไม่ต้องประกาศ callback BroadcastReceiver ใน manifest ของ Full Lock

| Code | `status` | ความหมาย/ข้อความ UI ที่แนะนำ |
|---|---|---|
| 100 | `APPLIED` | บันทึกพิกัดและส่งสัญญาณรีเฟรชแล้ว → “ตั้งพิกัดแล้ว” |
| 101 | `HOOK_LOADED` | Hook ของ target โหลด request นี้แล้ว → “Hook รับพิกัดแล้ว” |
| 400 | `INVALID_REQUEST` | version/ชนิดข้อมูล/ช่วงพิกัด/ID/label ไม่ถูกต้อง |
| 400 | `REQUEST_ID_REUSED` | ใช้ ID เดิมกับ payload ที่เปลี่ยนไป |
| 401 | `UNAUTHORIZED` | ไม่มี token ของ Full Lock ที่ถูกต้อง |
| 402 | `LICENSE_REQUIRED` | ต้องเข้าสู่ระบบสมาชิก/ต่อสิทธิ์ใน GeoX (รุ่นเก่าใช้ activation) |
| 403 | `LICENSE_LOCKED` | ใบอนุญาตถูกล็อก ให้ผู้ใช้เปิด GeoX ตรวจสอบ |
| 409 | `SUPERSEDED` | มีคำสั่งใหม่กว่า หรือพิกัดของ ID นี้ไม่ได้เป็นค่าปัจจุบันแล้ว |
| 412 | `HOOK_NOT_READY` | บันทึกแล้ว แต่ Hook ของ target ยังไม่พร้อม เช่น ปิด System hook หรือระบบรุ่นนั้นไม่รองรับ |
| 500 | `APPLY_FAILED` / `INTEGRITY_FAILED` | บันทึกไม่ได้หรือการตรวจตัวแอปไม่ผ่าน |
| 504 | `HOOK_TIMEOUT` | บันทึกแล้ว แต่ไม่ได้รับคำยืนยันจาก target ภายใน 1,500 ms |

Bundle มี `api_version:int`, `request_id:String`, `status:String`, `applied:boolean`, `hook_loaded:boolean`, `elapsed_ms:long` และมี `hook_package:String` เมื่อได้รับ `HOOK_LOADED`

`applied=true` หมายถึง request นี้เคย commit สำเร็จ ไม่ใช่การรับประกันว่ายังเป็นคำสั่งล่าสุดอยู่ตลอดไป โดย `HOOK_TIMEOUT` และ `HOOK_NOT_READY` จะมี `applied=true` จึงไม่ควรแสดงว่า “ส่งพิกัดไม่สำเร็จ” ให้แสดงว่า “ตั้งพิกัดแล้ว แต่ยังยืนยันโมดูลไม่ได้” แทน

`elapsed_ms` เริ่มนับเมื่อ Receiver ของ PHANToM ได้รับข้อความ ไม่รวมเวลาที่ Full Lock เตรียมข้อมูลหรือเวลาปลุก process ก่อนเข้า Receiver ฝั่ง Full Lock ควรจับ `SystemClock.elapsedRealtime()` ตั้งแต่แตะป้ายเพื่อวัดเวลา end-to-end อีกชั้น

## ตัวอย่าง Java ฝั่ง Full Lock

เพิ่ม package visibility ที่ระดับ `<manifest>` ของ Full Lock เพื่อใช้ตรวจว่าติดตั้ง PHANToM และอ่าน versionCode ได้:

```xml
<queries>
    <package android:name="io.github.phantom.gps" />
</queries>
```

ไม่ต้องเพิ่ม permission สำหรับ IPC นี้ และไม่ต้องสร้าง callback receiver ใน manifest

```java
package com.fulllock.app;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.ResultReceiver;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PhantomGpsBridge {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ConcurrentHashMap<String, ResultReceiver> PENDING = new ConcurrentHashMap<>();

    private static PendingIntent IDENTITY;
    private static synchronized PendingIntent identity(Context app) {
        if (IDENTITY == null) {
            Intent marker = new Intent(app.getPackageName() + ".PHANTOM_BRIDGE_IDENTITY")
                    .setPackage(app.getPackageName());
            IDENTITY = PendingIntent.getBroadcast(app, 0, marker,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        }
        return IDENTITY;
    }

    public interface Callback {
        void onResult(String requestId, int code, Bundle result);
    }

    // Important: send as android.os.ResultReceiver, not this app's anonymous subclass.
    // Otherwise the other process may be unable to load the Full Lock subclass from the Parcel.
    private static ResultReceiver platformReceiver(ResultReceiver local) {
        Parcel parcel = Parcel.obtain();
        try {
            local.writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            return ResultReceiver.CREATOR.createFromParcel(parcel);
        } finally {
            parcel.recycle();
        }
    }

    public static String send(Context source, double latitude, double longitude,
                              String label, String targetPackage, Callback callback) {
        Context app = source.getApplicationContext();
        String id = UUID.randomUUID().toString();
        boolean wantHookConfirmation = targetPackage != null && !targetPackage.isEmpty();
        AtomicBoolean appliedSeen = new AtomicBoolean(false);
        ResultReceiver local = new ResultReceiver(MAIN) {
            @Override protected void onReceiveResult(int code, Bundle data) {
                if (data == null || !id.equals(data.getString("request_id"))) return;
                if (data.getBoolean("applied")) appliedSeen.set(true);
                if (code != 100 || !wantHookConfirmation) PENDING.remove(id);
                callback.onResult(id, code, data);
            }
        };
        PENDING.put(id, local); // Keep it alive until the terminal reply/watchdog.

        Intent request = new Intent("io.github.phantom.gps.action.SET_LOCATION_DIRECT")
                .setComponent(new ComponentName("io.github.phantom.gps",
                        "io.github.phantom.gps.utils.DirectLocationReceiver"))
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                .putExtra("api_version", 1)
                .putExtra("request_id", id)
                .putExtra("latitude", latitude)
                .putExtra("longitude", longitude)
                .putExtra("caller_identity", identity(app))
                .putExtra("reply", platformReceiver(local));
        if (label != null) request.putExtra("label", label);
        if (wantHookConfirmation) request.putExtra("target_package", targetPackage);

        try {
            app.sendBroadcast(request);
        } catch (RuntimeException failure) {
            PENDING.remove(id);
            Bundle error = new Bundle();
            error.putString("request_id", id);
            error.putString("status", "CLIENT_SEND_FAILED");
            MAIN.post(() -> callback.onResult(id, -1, error));
        }
        // Watchdog only; it never delays a reply or starts a retry automatically.
        MAIN.postDelayed(() -> {
            if (PENDING.remove(id) != null) {
                Bundle timeout = new Bundle();
                timeout.putString("request_id", id);
                timeout.putString("status", appliedSeen.get() ? "CLIENT_HOOK_TIMEOUT" : "CLIENT_TIMEOUT");
                timeout.putBoolean("applied", appliedSeen.get());
                callback.onResult(id, -2, timeout);
            }
        }, 6000);
        return id;
    }

    private PhantomGpsBridge() {}
}
```

จุดที่ agent ฝั่ง Full Lock ต้องเชื่อม: ใน click listener ของป้ายพิกัดต้นทาง นำ double lat/lng ที่มีอยู่แล้วเรียก helper ด้านบน **แทน** การเขียนคลิปบอร์ด อย่า parse พิกัดกลับจากข้อความบนป้ายหากมี numeric model อยู่แล้ว

- หากใช้ scope **Android Framework** ตามค่าแนะนำของ PHANToM ให้ใช้ `targetPackage = "android"` และเปิด System hook ใน GeoX
- หากใช้ scope แบบรายแอป ส่ง `targetPackage` จาก package จริงของแอปนั้น ไม่ใช้ package ของ Full Lock และไม่ hardcode ชื่อตัวอย่าง
- หากต้องการเพียงผลเร็ว ไม่ต้องส่ง `target_package`; ใช้ `APPLIED` แสดงสำเร็จได้โดยระบุข้อความให้ตรงความหมาย
- เก็บ request ID ล่าสุดต่อป้าย/การเลือก แล้วละทิ้งผลของ request เก่า เพื่อไม่ให้ผลตอบกลับช้ามาทับการแตะครั้งใหม่
- ใน UI ให้สถานะ `HOOK_LOADED` มีลำดับสูงกว่า `APPLIED`; อย่าถอยสถานะเมื่อผลเก่ามาถึงทีหลัง
- ไม่ block main thread และไม่รอด้วย sleep/polling ขณะส่ง
- ตรวจว่าติดตั้ง PHANToM versionCode >=15 ก่อนส่ง ถ้าไม่ติดตั้ง/ถูก force-stop/ROM ไม่ส่ง Broadcast อาจไม่มี callback ให้ใช้ watchdog และให้ผู้ใช้เปิด GeoX หนึ่งครั้ง
- หาก retry network/การส่ง ให้ retry ด้วย ID และ payload เดิม ห้ามใช้ ID เดิมกับพิกัดใหม่ ผลล่าสุดถูกจำไว้ใน process สูงสุด 64 รายการ ไม่ใช่ระบบ exactly-once แบบถาวรข้ามการปิด process
- ไม่ต้องสร้างรหัส Activate ใหม่เมื่อเกิด timeout ของ IPC ให้แยกกรณี License ออกจากการส่งข้อมูลให้ชัดเจน

## การทดสอบร่วมกันบนเครื่องจริง

1. ติดตั้ง PHANToM 0.0.15 ที่ Activate แล้ว เปิด LSPosed scope ให้แอปปลายทาง และรีบูต
2. ปิด Clipboard monitor และเปิด/ปิด Auto start สลับกัน การส่ง direct ต้องเริ่มพิกัดได้เหมือนเดิม
3. แตะป้ายใน Full Lock แล้วตรวจ `APPLIED`; ถ้าระบุ target ที่กำลังทำงานและโหลด Hook อยู่ ให้ตรวจ `HOOK_LOADED` ต่อ
4. ตรวจว่า clipboard เดิมไม่เปลี่ยน และไม่มีหน้ากรอกรหัสเด้งจาก direct request
5. แตะสองจุดติดกัน ตรวจว่าจุดล่าสุดเป็นค่าท้ายสุดและ UI ไม่รับผลของ ID เก่า
6. ทดสอบพิกัดละเอียดหลายตำแหน่งทศนิยม และเปิด Random Position ใน GeoX: direct ต้องใช้จุดตรงที่ส่งมา
7. ทดสอบ NaN/Infinity/พิกัดเกินช่วง/ส่ง String แทน Double ต้องไม่เปลี่ยนพิกัด
8. ทดสอบ target ไม่ทำงานหรือไม่มี Hook: `APPLIED` ยังเกิดได้ แต่ไม่ควรรายงาน `HOOK_LOADED` เท็จ

ฝั่ง PHANToM มี unit tests และ release build สำหรับ contract/precision/validation แล้ว แต่ยังไม่มีอุปกรณ์ Android เชื่อมต่อสำหรับทดสอบ Intent ข้ามสองแอปจริงในรอบนี้

## ระบบสมาชิกตั้งแต่ 0.0.17 (ซอร์สเตรียมเปิดใช้)

การรับ Intent/Binder ไม่เรียก network เพื่อ login/ต่ออายุ แต่ตรวจ signed membership lease ในเครื่อง ต้องให้ผู้ใช้เข้าสู่ PHANToM และยังมีสิทธิ์อยู่ก่อนส่งคำขอ หากได้ 402/403 ให้เปิด GeoX เพื่อดูสถานะ/เข้าสู่ระบบหรือติดต่อแอดมิน ไม่ขอ activation code ใหม่ ระบบเก่าถูกแทนด้วยบัญชี 7 วันเมื่อเปิดใช้รุ่นนี้

Lease อายุสูงสุด 10 นาที ต่ออายุเบื้องหลังทุก 2 นาที; เครื่องต้องต่ออินเทอร์เน็ตเป็นระยะและต้องต่อ lease ใหม่หลัง reboot ไม่ถือว่า bootstrap Binder 15 วินาทีเป็นสิทธิ์สมาชิกแทนกัน
