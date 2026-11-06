package io.github.phantom.gps.utils
import android.content.Context
import android.graphics.drawable.GradientDrawable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.phantom.gps.R
object GozzaDialogs {
 fun builder(context:Context):MaterialAlertDialogBuilder {
  val panel=GradientDrawable().apply {setColor(context.getColor(R.color.gozza_elevated));cornerRadius=18*context.resources.displayMetrics.density}
  return MaterialAlertDialogBuilder(context,R.style.ThemeOverlay_GeoX_Dialog).setBackground(panel)
 }
}
