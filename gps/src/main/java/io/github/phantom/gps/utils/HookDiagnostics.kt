package io.github.phantom.gps.utils

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.UUID

/** Read-only targeted diagnostics. Replies never authorize a location action or replace signed lease validation. */
object HookDiagnostics {
    fun show(activity:FragmentActivity,target:String) {
        val handler=Handler(Looper.getMainLooper())
        val query=UUID.randomUUID().toString()
        val sharing=if(PrefManager.isHookSharingAvailable)"Gozza Lalamove: ช่องทางค่าร่วมพร้อม" else "Gozza Lalamove: ช่องทางค่าร่วม LSPosed ไม่พร้อม (กำลังใช้ private prefs)"
        var answered=false
        val states=linkedSetOf<String>()
        val dialog=io.github.phantom.gps.utils.GozzaDialogs.builder(activity).setTitle("ตรวจ Hook: $target")
            .setMessage(sharing+"\n\nกำลังรอคำตอบจากแอปเป้าหมาย…").setPositiveButton("ปิด",null).create()
        val observer=object:DefaultLifecycleObserver { override fun onDestroy(owner:LifecycleOwner){dialog.dismiss()} }
        val timeout=Runnable {
            if(dialog.isShowing&&!answered)dialog.setMessage("ยังไม่ได้รับคำตอบ\nแอปอาจยังไม่เปิด ไม่อยู่ใน scope หรือยังโหลด Hook รุ่นเก่าที่ไม่รองรับการตรวจนี้\nสถานะนี้ไม่ได้ยืนยันว่าโมดูลปิดอยู่")
        }
        val reply=object:ResultReceiver(handler) {
            override fun onReceiveResult(code:Int,data:Bundle?) {
                if(!dialog.isShowing||data?.getString("query_id")!=query||data.getString("package")!=target)return
                answered=true
                val reason=if(target=="android"&&!data.getBoolean("system_mode_enabled"))"SYSTEM_MODE_OFF"
                    else if(target=="android"&&!data.getBoolean("system_hooks_ready"))"SYSTEM_HOOKS_NOT_READY" else data.getString("reason").orEmpty()
                val text=when(reason){
                    "READY"->"อ่านค่าร่วมและสิทธิ์ผ่าน / คำสั่ง START เปิดอยู่ (ยังไม่ใช่หลักฐานว่าแอปอ่านพิกัดแล้ว)"
                    "STOPPED"->"อ่านค่าร่วมและสิทธิ์ผ่าน / คำสั่ง START ยังปิดอยู่"
                    "SNAPSHOT_MISSING"->"Hook โหลดแล้ว แต่ไม่พบค่าการทำงานในไฟล์ร่วม"
                    "LEASE_MISSING"->"Hook โหลดแล้ว แต่ยังอ่านสิทธิ์สมาชิกไม่ได้"
                    "LEASE_INVALID"->"Hook ตรวจลายเซ็นหรือโครงสร้างสิทธิ์ไม่ผ่าน"
                    "BOOT_NOT_READY"->"Hook ยังอ่านหมายเลขรอบเปิดเครื่องไม่ได้"
                    "BOOT_MISMATCH"->"สิทธิ์ใน Hook เป็นของรอบเปิดเครื่องเก่า"
                    "LEASE_TIME_INVALID"->"สิทธิ์ใน Hook หมดช่วงเวลา หรือเวลาเครื่องไม่ตรง"
                    "SYSTEM_MODE_OFF"->"Hook โหลดในระบบ แต่ Advanced hook ยังปิดอยู่"
                    "SYSTEM_HOOKS_NOT_READY"->"ตัวรับโหลดแล้ว แต่ system hook ยังติดตั้งไม่ครบ"
                    else->"ตรวจ Hook ไม่สำเร็จ: $reason"
                }
                states.add("Hook ${data.getString("version","?")}: $text")
                dialog.setMessage(sharing+"\n\n"+states.joinToString("\n\n"))
            }
        }
        activity.lifecycle.addObserver(observer)
        dialog.setOnDismissListener {handler.removeCallbacks(timeout);activity.lifecycle.removeObserver(observer)}
        dialog.show()
        runCatching { LocationRefresh.diagnose(activity,target,query,reply) }.onFailure {dialog.setMessage("ส่งคำขอตรวจสอบไม่ได้")}
        handler.postDelayed(timeout,8000)
    }
}
