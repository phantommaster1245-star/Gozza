package io.github.phantom.gps.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Build
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.elevation.ElevationOverlayProvider
import com.google.android.material.progressindicator.LinearProgressIndicator
import dagger.hilt.android.AndroidEntryPoint
import io.github.phantom.gps.BuildConfig
import io.github.phantom.gps.R
import io.github.phantom.gps.adapter.FavListAdapter
import io.github.phantom.gps.databinding.ActivityMapBinding
import io.github.phantom.gps.ui.viewmodel.MainViewModel
import io.github.phantom.gps.utils.AppIntegrity
import io.github.phantom.gps.utils.ClipboardMonitorService
import io.github.phantom.gps.utils.JoystickService
import io.github.phantom.gps.utils.LicenseGuard
import io.github.phantom.gps.utils.LocationActions
import io.github.phantom.gps.utils.NotificationsChannel
import io.github.phantom.gps.utils.MembershipDisplay
import io.github.phantom.gps.utils.PrefManager
import io.github.phantom.gps.utils.NotificationIntents
import io.github.phantom.gps.utils.ext.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import java.io.IOException
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.properties.Delegates

@AndroidEntryPoint
abstract class BaseMapActivity: AppCompatActivity() {

    private var coordinatesReady = false
    protected var lat: Double = 0.0
        set(value) { field = value; updateLocationChrome() }
    protected var lon: Double = 0.0
        set(value) { field = value; coordinatesReady = true; updateLocationChrome() }
    protected val viewModel by viewModels<MainViewModel>()
    protected val binding by lazy { ActivityMapBinding.inflate(layoutInflater) }
    protected lateinit var alertDialog: MaterialAlertDialogBuilder
    protected lateinit var dialog: AlertDialog
    private var shownUpdateVersion: String? = null

    private val notificationsChannel by lazy { NotificationsChannel() }
    private var favListAdapter: FavListAdapter = FavListAdapter()
    private var xposedDialog: AlertDialog? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val PERMISSION_ID = 42
    private val NOTIFICATION_PERMISSION_ID = 43

    private val locationActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            when (intent?.action) {
                LocationActions.ACTION_LOCATION_STARTED -> {
                    val newLat = if (intent.hasExtra(LocationActions.EXTRA_LAT)) {
                        intent.getDoubleExtra(LocationActions.EXTRA_LAT, PrefManager.getLat)
                    } else {
                        PrefManager.getLat
                    }
                    val newLon = if (intent.hasExtra(LocationActions.EXTRA_LON)) {
                        intent.getDoubleExtra(LocationActions.EXTRA_LON, PrefManager.getLng)
                    } else {
                        PrefManager.getLng
                    }
                    lat = newLat
                    lon = newLon
                    runCatching { moveMapToNewLocation(true) }
                    binding.startButton.visibility = View.GONE
                    binding.stopButton.visibility = View.VISIBLE
                }
                LocationActions.ACTION_LOCATION_STOPPED -> {
                    binding.stopButton.visibility = View.GONE
                    binding.startButton.visibility = View.VISIBLE
                    onLocationStopped()
                    updateLocationChrome()
                }
            }
        }
    }

    private val locationPrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            "start", "latitude", "longitude" -> {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    syncLocationUiFromPrefs()
                } else {
                    runOnUiThread { syncLocationUiFromPrefs() }
                }
            }
        }
    }

    private val elevationOverlayProvider by lazy {
        ElevationOverlayProvider(this)
    }

    private val headerBackground by lazy {
        elevationOverlayProvider.compositeOverlayWithThemeSurfaceColorIfNeeded(
            resources.getDimension(R.dimen.bottom_sheet_elevation)
        )
    }

    protected abstract fun getActivityInstance(): BaseMapActivity
    protected abstract fun hasMarker(): Boolean
    protected abstract fun initializeMap()
    protected abstract fun setupButtons()
    protected abstract fun moveMapToNewLocation(moveNewLocation: Boolean)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!AppIntegrity.isSelfValid(this)) {
            finish()
            return
        }
        if (LicenseGuard.routeToRequiredScreen(this)) {
            finish()
            return
        }
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.rgb(16,20,22)), navigationBarStyle = SystemBarStyle.dark(Color.rgb(16,20,22)))

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(binding.root)
        setupMapInsets()
        setSupportActionBar(binding.toolbar)
        initializeMap()
        checkModuleEnabled()
        checkUpdates()
        setupNavView()
        setupButtons()
        setupDrawer()
        if (PrefManager.isJoystickEnabled){
            startService(Intent(this, JoystickService::class.java))
        }
        ensureNotificationPermission()
    }

    private var membershipUiJob: Job? = null
    override fun onResume() {
        super.onResume()
        if(!io.github.phantom.gps.utils.TesterAccess.ensure(this))return
        viewModel.updateXposedState()
        membershipUiJob?.cancel()
        membershipUiJob = lifecycleScope.launch {
            while (isActive) { if(!io.github.phantom.gps.utils.TesterAccess.ensure(this@BaseMapActivity))break; updateMemberDetails(); delay(1000) }
        }
    }
    override fun onPause() { membershipUiJob?.cancel(); super.onPause() }

    override fun onStart() {
        super.onStart()
        val filter = android.content.IntentFilter().apply {
            addAction(LocationActions.ACTION_LOCATION_STARTED)
            addAction(LocationActions.ACTION_LOCATION_STOPPED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(locationActionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(locationActionReceiver, filter)
        }
        PrefManager.registerListener(locationPrefsListener)
        syncLocationUiFromPrefs()
    }

    override fun onStop() {
        PrefManager.unregisterListener(locationPrefsListener)
        unregisterReceiver(locationActionReceiver)
        super.onStop()
    }

    private fun syncLocationUiFromPrefs() {
        val wasShowingStop = binding.stopButton.visibility == View.VISIBLE
        val started = PrefManager.isStarted
        updateLocationChrome()

        binding.startButton.visibility = if (started) View.GONE else View.VISIBLE
        binding.stopButton.visibility = if (started) View.VISIBLE else View.GONE

        if (started) {
            lat = PrefManager.getLat
            lon = PrefManager.getLng
            runCatching { moveMapToNewLocation(true) }
        } else if (wasShowingStop) {
            // Location was stopped while the UI was not receiving broadcasts (e.g., app in background).
            runCatching { onLocationStopped() }
        }
    }

    private fun updateMemberDetails() {
        val header=binding.navView.getHeaderView(0) ?: return
        val data=MembershipDisplay.header(this)
        header.findViewById<TextView>(R.id.member_username).text=data.username
        header.findViewById<TextView>(R.id.member_remaining).text=data.remaining
        header.findViewById<TextView>(R.id.member_details).text=data.expiry
    }

    private fun setupMapInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            binding.mapContent.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, ime.bottom))
            binding.navView.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    protected fun updateLocationChrome() {
        updateMemberDetails()
        binding.selectedCoordinates.text = if(coordinatesReady) String.format(java.util.Locale.US,"%.6f, %.6f",lat,lon) else "เลือกตำแหน่งบนแผนที่"
        binding.startButton.isEnabled = coordinatesReady
        if(!PrefManager.isHookSharingAvailable)binding.locationStatus.text="LSPosed?"
        else binding.locationStatus.setText(if (PrefManager.isStarted) R.string.geox_active else R.string.geox_ready)
    }

    private fun setupDrawer() {
        supportActionBar?.setDisplayShowTitleEnabled(false)
        val mDrawerToggle = object : ActionBarDrawerToggle(
            this,
            binding.container,
            binding.toolbar,
            R.string.drawer_open,
            R.string.drawer_close
        ) {
            override fun onDrawerClosed(view: View) {
                super.onDrawerClosed(view)
                invalidateOptionsMenu()
            }

            override fun onDrawerOpened(drawerView: View) {
                updateMemberDetails()
                super.onDrawerOpened(drawerView)
                invalidateOptionsMenu()
            }
        }
        mDrawerToggle.drawerArrowDrawable.color = Color.rgb(239,245,240)
        binding.container.addDrawerListener(mDrawerToggle)
        mDrawerToggle.syncState()
    }

    private fun setupNavView() {

        val progress = binding.search.searchProgress
        binding.search.searchBox.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                if (isNetworkConnected()) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        val getInput = v.text.toString()
                        if (getInput.isNotEmpty()){
                            getSearchAddress(getInput).let {
                                it.collect { result ->
                                    when(result) {
                                        is SearchProgress.Progress -> {
                                            progress.visibility = View.VISIBLE
                                        }
                                        is SearchProgress.Complete -> {
                                            progress.visibility = View.GONE
                                            lat = result.lat
                                            lon = result.lon
                                            moveMapToNewLocation(true)
                                            io.github.phantom.gps.utils.LocationNotification.show(this@BaseMapActivity,getInput,lat,lon)
                                        }
                                        is SearchProgress.Fail -> {
                                            progress.visibility = View.GONE
                                            showToast(result.error!!)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    showToast(getString(R.string.no_internet))
                }
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        binding.navView.setNavigationItemSelectedListener {
            when(it.itemId){
                R.id.gozza_map -> { /* Already displaying the map. */ }
                R.id.gozza_filters -> {
                    startActivity(Intent().setClassName(packageName,"com.fulllock.app.MainActivity").addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
                }


                R.id.settings -> {
                    startActivity(Intent(this,ActivitySettings::class.java))
                }
                R.id.about -> {
                    aboutDialog()
                }
            }
            binding.container.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_ID
                )
            }
        }
        if (PrefManager.isClipboardMonitorEnabled) {
            ClipboardMonitorService.ensureRunning(this)
        }
        if (!PrefManager.isClipboardMonitorEnabled) {
            stopService(Intent(this, ClipboardMonitorService::class.java))
        }
    }

    private fun checkModuleEnabled(){
        // Self-process injection is not evidence that a selected target process is hooked.
        // An absent self hook must not repeatedly tell users their LSPosed module is disabled.
        viewModel.isXposed.observe(this) { injected ->
            binding.toolbar.contentDescription = if(injected) "Gozza Lalamove: พบ Hook ในโปรเซสแอป" else "Gozza Lalamove: ตรวจ Hook ของแอปเป้าหมายได้ใน Settings"
        }
    }

    protected fun aboutDialog(){
        alertDialog = io.github.phantom.gps.utils.GozzaDialogs.builder(this)
        android.view.LayoutInflater.from(alertDialog.context).inflate(R.layout.about,null).apply {
            val  titlele = findViewById<TextView>(R.id.design_about_title)
            val  version = findViewById<TextView>(R.id.design_about_version)
            val  info = findViewById<TextView>(R.id.design_about_info)
            titlele.text = getString(R.string.app_name)
            version.text = BuildConfig.VERSION_NAME
            info.text = getString(R.string.about_info)
        }.run {
            alertDialog.setView(this)
            alertDialog.show()
        }
    }

    protected fun addFavoriteDialog() {
        alertDialog =  io.github.phantom.gps.utils.GozzaDialogs.builder(this).apply {
            val view = layoutInflater.inflate(R.layout.dialog,null)
            val editText = view.findViewById<EditText>(R.id.search_edittxt)
            setTitle(getString(R.string.add_fav_dialog_title))
            setPositiveButton(getString(R.string.dialog_button_add)) { _, _ ->
                val s = editText.text.toString()
                if (hasMarker()){
                  showToast(getString(R.string.location_not_select))
                }else{
                    viewModel.storeFavorite(s, lat, lon)
                    viewModel.response.observe(getActivityInstance()){
                        if (it == (-1).toLong()) showToast(getString(R.string.cant_save)) else showToast(getString(R.string.save))
                    }
                }
            }
            setView(view)
            show()
        }
    }

    protected fun openFavoriteListDialog() {
        if (favoriteListJob == null) getAllUpdatedFavList()
        alertDialog = io.github.phantom.gps.utils.GozzaDialogs.builder(this)
        alertDialog.setTitle(getString(R.string.favorites))
        val view = android.view.LayoutInflater.from(alertDialog.context).inflate(R.layout.fav,null)
        val rcv = view.findViewById<RecyclerView>(R.id.favorites_list)
        rcv.layoutManager = LinearLayoutManager(this)
        rcv.layoutParams.height = minOf((resources.displayMetrics.heightPixels * 0.45).toInt(), (320 * resources.displayMetrics.density).toInt())
        rcv.adapter = favListAdapter
        view.findViewById<View>(R.id.add_favorite).isEnabled = coordinatesReady
        view.findViewById<View>(R.id.add_favorite).setOnClickListener { dialog.dismiss(); addFavoriteDialog() }
        favListAdapter.onItemClick = {
            it.let {
                lat = it.lat!!
                lon = it.lng!!
            }
            moveMapToNewLocation(true)
            if (dialog.isShowing) dialog.dismiss()

        }
        favListAdapter.onItemDelete = {
            viewModel.deleteFavorite(it)
        }
        alertDialog.setView(view)
        dialog = alertDialog.create()
        dialog.show()

    }

    private var favoriteListJob: Job? = null
    private fun getAllUpdatedFavList(){
        favoriteListJob = lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.doGetUserDetails()
                viewModel.allFavList.collect {
                    favListAdapter.submitList(it)
                }
            }
        }

    }

    private fun checkUpdates() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.update.collect { update ->
                    if (update != null && shownUpdateVersion != update.version) {
                        shownUpdateVersion = update.version
                        io.github.phantom.gps.utils.GozzaDialogs.builder(this@BaseMapActivity)
                            .setTitle(update.name)
                            .setMessage(getString(R.string.update_website_message, update.changelog))
                            .setPositiveButton(R.string.update_button) { _, _ ->
                                runCatching {
                                    startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(update.releaseUrl)))
                                }.onFailure { Toast.makeText(this@BaseMapActivity, R.string.app_update_failed, Toast.LENGTH_LONG).show() }
                            }
                            .setNegativeButton(R.string.update_later, null).show()
                    }
                }
            }
        }
    }

    private suspend fun getSearchAddress(address: String) = callbackFlow {
        withContext(Dispatchers.IO){
            trySend(SearchProgress.Progress)
            val matcher: Matcher =
                Pattern.compile("[-+]?\\d{1,3}([.]\\d+)?, *[-+]?\\d{1,3}([.]\\d+)?").matcher(address)

            if (matcher.matches()){
                delay(3000)
                trySend(SearchProgress.Complete(matcher.group().split(",")[0].toDouble(),matcher.group().split(",")[1].toDouble()))
            }else {
                val geocoder = Geocoder(getActivityInstance())
                val addressList: List<Address>? = geocoder.getFromLocationName(address,3)

                try {
                    addressList?.let {
                        if (it.size == 1){
                           trySend(SearchProgress.Complete(addressList[0].latitude, addressList[0].longitude))
                        }else {
                            trySend(SearchProgress.Fail(getString(R.string.address_not_found)))
                        }
                    }
                } catch (io : IOException){
                    trySend(SearchProgress.Fail(getString(R.string.no_internet)))
                }
            }
        }
        awaitClose { this.cancel() }
    }

    protected open fun onLocationStopped() {
        // Subclasses can override to update map marker state when stopped externally.
    }

    // Get current location
    @SuppressLint("MissingPermission")
    protected fun getLastLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                fusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
                    val location: Location? = task.result
                    if (location == null) {
                        requestNewLocationData()
                    } else {
                        lat = location.latitude
                        lon = location.longitude
                        moveMapToNewLocation(true)
                    }
                }
            } else {
                showToast("Turn on location")
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 0
        mLocationRequest.fastestInterval = 0
        mLocationRequest.numUpdates = 1

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation!!
            lat = mLastLocation.latitude
            lon = mLastLocation.longitude
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_ID
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_ID) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLastLocation()
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_ID) {
            if (PrefManager.isClipboardMonitorEnabled) {
                ClipboardMonitorService.ensureRunning(this)
            }
        }
    }
}

sealed class SearchProgress {
    object Progress : SearchProgress()
    data class Complete(val lat: Double , val lon : Double) : SearchProgress()
    data class Fail(val error: String?) : SearchProgress()
}

