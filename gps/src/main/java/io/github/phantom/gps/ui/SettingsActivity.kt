package io.github.phantom.gps.ui


import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import io.github.phantom.gps.update.UpdateChecker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.phantom.gps.BuildConfig
import androidx.preference.DropDownPreference
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceCategory
import io.github.phantom.gps.utils.ClipboardFormats
import io.github.phantom.gps.R
import io.github.phantom.gps.databinding.ActivitySettingsBinding
import io.github.phantom.gps.utils.AppIntegrity
import io.github.phantom.gps.utils.ClipboardMonitorService
import io.github.phantom.gps.utils.JoystickService
import io.github.phantom.gps.utils.LicenseGuard
import io.github.phantom.gps.utils.PrefManager
import io.github.phantom.gps.utils.ext.showToast

class ActivitySettings : AppCompatActivity() {

    private val binding by lazy {
        ActivitySettingsBinding.inflate(layoutInflater)
    }

    class SettingPreferenceDataStore() : PreferenceDataStore() {
        override fun getBoolean(key: String?, defValue: Boolean): Boolean {
            if (key in ClipboardFormats.KEYS) return PrefManager.isClipboardFormatEnabled(key!!)
            return when (key) {
                "system_hooked" -> PrefManager.isSystemHooked
                "random_position" -> PrefManager.isRandomPosition
                "update_disabled" -> PrefManager.isUpdateDisabled
                "joystick_enabled" -> PrefManager.isJoystickEnabled
                "clipboard_monitor_enabled" -> PrefManager.isClipboardMonitorEnabled
                "clipboard_monitor_auto_start" -> PrefManager.isClipboardAutoStart
                else -> throw IllegalArgumentException("Invalid key $key")
            }
        }

        override fun putBoolean(key: String?, value: Boolean) {
            if (key in ClipboardFormats.KEYS) { PrefManager.setClipboardFormatEnabled(key!!, value); return }
            return when (key) {
                "system_hooked" -> PrefManager.isSystemHooked = value
                "random_position" -> PrefManager.isRandomPosition = value
                "update_disabled" -> PrefManager.isUpdateDisabled = value
                "joystick_enabled" -> PrefManager.isJoystickEnabled = value
                "clipboard_monitor_enabled" -> PrefManager.isClipboardMonitorEnabled = value
                "clipboard_monitor_auto_start" -> PrefManager.isClipboardAutoStart = value
                else -> throw IllegalArgumentException("Invalid key $key")
            }
        }

        override fun getString(key: String?, defValue: String?): String? {
            return when (key) {
                "accuracy_level" -> PrefManager.accuracy
                "random_radius_meters" -> PrefManager.randomRadiusMeters
                "map_type" -> PrefManager.mapType.toString()
                "dark_theme" -> PrefManager.darkTheme.toString()
                else -> throw IllegalArgumentException("Invalid key $key")
            }
        }

        override fun putString(key: String?, value: String?) {
            return when (key) {
                "accuracy_level" -> PrefManager.accuracy = value
                "random_radius_meters" -> PrefManager.randomRadiusMeters = value
                "map_type" -> PrefManager.mapType = value!!.toInt()
                "dark_theme" -> PrefManager.darkTheme = value!!.toInt()
                else -> throw IllegalArgumentException("Invalid key $key")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!io.github.phantom.gps.utils.TesterAccess.ensure(this)) { finish(); return }
        if (!AppIntegrity.isSelfValid(this)) {
            finish()
            return
        }
        if (LicenseGuard.routeToRequiredScreen(this)) {
            finish()
            return
        }
        enableEdgeToEdge(navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsPreferenceFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finish()
                }
            }
        )

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    class SettingsPreferenceFragment : PreferenceFragmentCompat() {
        private var preferenceDialog: androidx.appcompat.app.AlertDialog? = null
        override fun onDisplayPreferenceDialog(preference: Preference) {
            if(preferenceDialog?.isShowing==true)return
            val builder=io.github.phantom.gps.utils.GozzaDialogs.builder(requireContext())
                .setTitle(preference.title).setNegativeButton(android.R.string.cancel,null)
            when(preference) {
                is EditTextPreference -> {
                    val input=EditText(builder.context).apply {
                        setText(preference.text);setSelectAllOnFocus(true)
                        inputType=InputType.TYPE_CLASS_NUMBER or if(preference.key=="accuracy_level")InputType.TYPE_NUMBER_FLAG_DECIMAL else 0
                        keyListener=DigitsKeyListener.getInstance(if(preference.key=="accuracy_level")"0123456789." else "0123456789")
                    }
                    val frame=android.widget.FrameLayout(builder.context).apply {
                        val pad=(24*resources.displayMetrics.density).toInt();setPadding(pad,0,pad,0);addView(input)
                    }
                    val dialog=builder.setView(frame).setPositiveButton(android.R.string.ok,null).create()
                    dialog.setOnShowListener {
                        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            val value=input.text.toString()
                            val valid=if(preference.key=="accuracy_level")value.toDoubleOrNull()?.let{it.isFinite()&&it>=0}==true else value.toIntOrNull()?.let{it>=1}==true
                            if(!valid){input.error=getString(R.string.enter_valid_input);return@setOnClickListener}
                            if(preference.callChangeListener(value)){preference.text=value;dialog.dismiss()}
                        }
                    }
                    preferenceDialog=dialog;dialog.show()
                }
                is androidx.preference.ListPreference -> {
                    preferenceDialog=builder.setSingleChoiceItems(preference.entries,preference.findIndexOfValue(preference.value)){dialog,index->
                        val value=preference.entryValues[index].toString()
                        if(preference.callChangeListener(value))preference.value=value
                        dialog.dismiss()
                    }.show()
                }
                else -> super.onDisplayPreferenceDialog(preference)
            }
        }
        override fun onDestroyView() { preferenceDialog?.dismiss();preferenceDialog=null;super.onDestroyView() }
        private var formatsExpanded = false

        override fun onSaveInstanceState(outState: Bundle) {
            outState.putBoolean("clipboard_formats_expanded", formatsExpanded)
            super.onSaveInstanceState(outState)
        }


        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager?.preferenceDataStore = SettingPreferenceDataStore()
            setPreferencesFromResource(R.xml.preferences, rootKey)
            findPreference<Preference>("dark_theme")?.isVisible = false
            val formatGroup = findPreference<PreferenceCategory>("clipboard_formats_group")!!
            val formatHeader = findPreference<Preference>("clipboard_formats_expand")!!
            formatsExpanded = savedInstanceState?.getBoolean("clipboard_formats_expanded") ?: false
            fun updateFormatHeader(changedKey: String? = null, changedValue: Boolean? = null) {
                val count = ClipboardFormats.KEYS.count { key ->
                    if (key == changedKey) changedValue == true else PrefManager.isClipboardFormatEnabled(key)
                }
                formatGroup.isVisible = formatsExpanded
                formatHeader.title = getString(if (formatsExpanded) R.string.clipboard_formats_collapse else R.string.clipboard_formats_expand)
                formatHeader.summary = if (count == 0) getString(R.string.clipboard_formats_none) else getString(R.string.clipboard_formats_count, count)
            }
            updateFormatHeader()
            formatHeader.setOnPreferenceClickListener {
                formatsExpanded = !formatsExpanded
                updateFormatHeader()
                true
            }
            ClipboardFormats.KEYS.forEach { key ->
                findPreference<Preference>(key)?.setOnPreferenceChangeListener { _, newValue ->
                    updateFormatHeader(key, newValue as Boolean)
                    true
                }
            }

            findPreference<Preference>("check_update_now")?.apply {
                summary = "Gozza รุ่นทดสอบส่วนตัว · รับ APK จากผู้พัฒนา"
                setOnPreferenceClickListener {
                    Toast.makeText(requireContext(), "รับไฟล์ APK รุ่นทดสอบจากผู้พัฒนาโดยตรง", Toast.LENGTH_LONG).show()
                    true
                }
            }

            findPreference<EditTextPreference>("accuracy_level")?.let {
                it.summary = "${PrefManager.accuracy} m"
                it.setOnBindEditTextListener { editText ->
                    editText.inputType = InputType.TYPE_CLASS_NUMBER;
                    editText.keyListener = DigitsKeyListener.getInstance("0123456789.,");
                    editText.addTextChangedListener(getCommaReplacerTextWatcher(editText));
                }

                it.setOnPreferenceChangeListener { preference, newValue ->
                    try {
                        newValue as String?
                        preference.summary = "$newValue m"
                    } catch (n: NumberFormatException) {
                        n.printStackTrace()
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.enter_valid_input),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    true
                }
            }

            findPreference<Preference>("hook_diagnostics")?.setOnPreferenceClickListener {
                val input=EditText(requireContext()).apply { hint="เช่น com.example.app หรือ android";inputType=InputType.TYPE_CLASS_TEXT }
                val dialog=io.github.phantom.gps.utils.GozzaDialogs.builder(requireContext()).setTitle("ตรวจ Hook ของแอปที่เปิดอยู่")
                    .setMessage("เปิดแอปเป้าหมายไว้ก่อน แล้วกรอกชื่อแพ็กเกจ หากเพิ่งอัปเดตโมดูล อาจต้องเปิดแอปเป้าหมายใหม่หรือรีเครื่อง")
                    .setView(input).setPositiveButton("ตรวจสอบ",null).setNegativeButton("ยกเลิก",null).create()
                dialog.setOnShowListener { dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val pkg=input.text.toString().trim()
                    if(pkg!="android"&&!pkg.matches(Regex("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)+")))input.error="ชื่อแพ็กเกจไม่ถูกต้อง"
                    else { dialog.dismiss(); io.github.phantom.gps.utils.HookDiagnostics.show(requireActivity(),pkg) }
                } };dialog.show();true
            }

            findPreference<Preference>("membership_account")?.isVisible = false

            findPreference<EditTextPreference>("random_radius_meters")?.let {
                it.summary = "${PrefManager.randomRadiusMeters} m"
                it.setOnBindEditTextListener { editText ->
                    editText.inputType = InputType.TYPE_CLASS_NUMBER
                    editText.keyListener = DigitsKeyListener.getInstance("0123456789")
                }

                it.setOnPreferenceChangeListener { preference, newValue ->
                    val radius = (newValue as? String)?.toIntOrNull()
                    if (radius == null || radius < 1) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.enter_valid_radius_input),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnPreferenceChangeListener false
                    }

                    preference.summary = "$radius m"
                    true
                }
            }


            findPreference<Preference>("joystick_enabled")?.let {
                it.setOnPreferenceClickListener {
                    if (askOverlayPermission()){
                        if (isJoystickRunning()) {
                            requireContext().stopService(Intent(context,JoystickService::class.java))
                            it.summary = "Joystick disabled"
                        } else if (PrefManager.isStarted) {
                            requireContext().startService(Intent(context,JoystickService::class.java))
                            it.summary = "Joystick enabled"
                        } else {
                            requireContext().showToast(requireContext().getString(R.string.location_not_select))
                        }
                    }
                    true
                }
            }

            findPreference<Preference>("clipboard_monitor_enabled")?.let { pref ->
                pref.setOnPreferenceChangeListener { _, newValue ->
                    val enabled = newValue as? Boolean ?: return@setOnPreferenceChangeListener false
                    PrefManager.isClipboardMonitorEnabled=enabled
                    if (enabled) {
                        ClipboardMonitorService.ensureRunning(requireContext())
                    } else {
                        ClipboardMonitorService.stopForegroundCapture()
                        requireContext().stopService(Intent(requireContext(), ClipboardMonitorService::class.java))
                    }
                    true
                }
            }
        }

        private fun isJoystickRunning(): Boolean {
            var isRunning = false
            val manager = requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager? ?: return false
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if ("io.github.phantom.gps.utils.JoystickService" == service.service.className) {
                    isRunning = true
                }
            }
            return isRunning
        }


        private fun askOverlayPermission() : Boolean {
            if (Settings.canDrawOverlays(context)){
                return true
            }
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context?.applicationContext?.packageName}" ))
            requireContext().startActivity(intent)
            return false
        }


        private fun getCommaReplacerTextWatcher(editText: EditText): TextWatcher {
            return object : TextWatcher {
                override fun beforeTextChanged(
                    charSequence: CharSequence,
                    i: Int,
                    i1: Int,
                    i2: Int
                ) {
                }

                override fun onTextChanged(
                    charSequence: CharSequence,
                    i: Int,
                    i1: Int,
                    i2: Int
                ) {
                }

                override fun afterTextChanged(editable: Editable) {
                    val text = editable.toString()
                    if (text.contains(",")) {
                        editText.setText(text.replace(",", "."))
                        editText.setSelection(editText.text.length)
                    }
                }
            }
        }
    }
}

