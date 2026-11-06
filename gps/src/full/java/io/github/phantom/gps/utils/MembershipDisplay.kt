package io.github.phantom.gps.utils
import android.app.Activity
import android.content.Context
object MembershipDisplay {
 data class Header(val username:String,val remaining:String,val expiry:String)
 fun header(c:Context)=Header("Gozza Lalamove","รุ่นทดสอบส่วนตัว", "แผนที่และ GPS")
 fun maybeRemind(activity:Activity)=Unit
 fun dismiss(activity:Activity)=Unit
}
