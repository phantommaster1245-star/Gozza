package io.github.phantom.gps.ui
import android.os.Bundle
import android.content.*
import android.graphics.Typeface
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import io.github.phantom.gps.R
import io.github.phantom.gps.utils.TesterAccess
import kotlinx.coroutines.launch
class TesterAccessActivity:AppCompatActivity() {
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  val outer=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_VERTICAL;setPadding(32,48,32,48);setBackgroundColor(getColor(R.color.gozza_background)) }
  fun label(value:String,size:Float)=TextView(this).apply {text=value;textSize=size;setTextColor(getColor(R.color.gozza_textPrimary));setPadding(0,12,0,12)}
  outer.addView(label("Gozza Lalamove",24f).apply{setTypeface(null,Typeface.BOLD)})
  outer.addView(label("สิทธิ์เครื่องทดสอบ",18f))
  val status=label("กำลังตรวจสอบ…",16f);outer.addView(status)
  outer.addView(label("ส่งรหัสเครื่องนี้ให้ผู้พัฒนาเพื่ออนุญาต ไม่ต้องสมัครบัญชี",14f))
  outer.addView(label(TesterAccess.deviceId(this),14f).apply{setTextIsSelectable(true);typeface=Typeface.MONOSPACE})
  outer.addView(MaterialButton(this).apply {text="คัดลอกรหัสเครื่อง";minHeight=56;setOnClickListener {getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Gozza tester ID",TesterAccess.deviceId(this@TesterAccessActivity)))} })
  outer.addView(MaterialButton(this).apply{text="ตรวจสอบอีกครั้ง";minHeight=56;setOnClickListener{TesterAccess.retry(this@TesterAccessActivity)}})
  val scroll=ScrollView(this);scroll.addView(outer);setContentView(scroll)
  lifecycleScope.launch{TesterAccess.status.collect{status.text=it;if(TesterAccess.available(this@TesterAccessActivity))finish()}}
 }
}
