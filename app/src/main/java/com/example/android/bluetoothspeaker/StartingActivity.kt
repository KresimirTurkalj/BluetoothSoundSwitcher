package com.example.android.bluetoothspeaker

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.*
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_starting.*
import android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
import android.os.Handler
import android.widget.Button
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout


class StartingActivity : AppCompatActivity() {

    companion object{
        const val EXTRA_A2DP = "com.example.android.bluetoothspeaker.extra.A2DP"
        const val EXTRA_BLE = "com.example.android.bluetoothspeaker.extra.BLE"
        const val PERMISSION_LOCATION = 1
        const val TEXT_VIEW = 0
        const val BUTTON = 1
        const val REQUEST_LOCATION_SERVICE = 2
    }
    private var bluetoothIsOn = false
    private var locationIsOn = false
    private val handler = Handler()
    private val mutableListOfA2dpDevices = mutableListOf<BluetoothDevice>()
    private val mutableListOfBleDevices = mutableListOf<BluetoothDevice>()
    private lateinit var bluetoothManager : BluetoothManager
    private lateinit var locationManager :LocationManager
    private val serviceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceDisconnected(profile: Int) {

        }

        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile == BluetoothProfile.A2DP) {
                mutableListOfA2dpDevices.addAll(proxy!!.getDevicesMatchingConnectionStates(IntArray(profile)))
               bluetoothManager.adapter.closeProfileProxy(BluetoothProfile.A2DP,proxy)
            }
        }
    }
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    if (intent.getIntExtra(
                            BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR
                        )
                        == BluetoothAdapter.STATE_ON
                    ) {
                        bluetoothIsOn = true
                        layout_enable_bluetooth.visibility = View.INVISIBLE
                        if(bluetoothIsOn.and(locationIsOn)){
                            startBleSearch()
                        }
                        unregisterReceiver(this)
                    }
                }
            }
        }
    }
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (result.rssi > -80) {
                mutableListOfBleDevices.add(result.device)
            }
        }
        override fun onScanFailed(errorCode: Int) {
            when(errorCode){
                SCAN_FAILED_ALREADY_STARTED -> {
                    Toast.makeText(this@StartingActivity,"Fails to start scan as BLE scan with the same settings is already started by the app.",Toast.LENGTH_SHORT).show()
                }
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> {
                    Toast.makeText(this@StartingActivity,"Fails to start scan as app cannot be registered.",Toast.LENGTH_SHORT).show()
                }
                SCAN_FAILED_FEATURE_UNSUPPORTED -> {
                    Toast.makeText(this@StartingActivity,"Fails to start power optimized scan as this feature is not supported.",Toast.LENGTH_SHORT).show()
                }
                SCAN_FAILED_INTERNAL_ERROR -> {
                    Toast.makeText(this@StartingActivity,"Fails to start scan due an internal error",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bluetoothManager =  getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        setContentView(R.layout.activity_starting)
        checkLocationPermission()

    }
    private fun checkBluetoothAndLocation() {
        if(bluetoothManager.adapter.isEnabled){
            bluetoothIsOn = true
        }
        else{
            showLayoutForBluetooth()
        }
        if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            locationIsOn = true
            if(bluetoothIsOn.and(locationIsOn)){
                startBleSearch()
            }
        }
        else {
            showLayoutForLocation()
        }
    }

    private fun showLayoutForLocation() {
        val layout = layout_enable_location as ConstraintLayout
        val textView = layout.getChildAt(TEXT_VIEW) as TextView
        val button = layout.getChildAt(BUTTON) as TextView
        layout.visibility = View.VISIBLE
        textView.text = getString(R.string.location_off)
        button.setOnClickListener{
            val enableLocationIntent = Intent(ACTION_LOCATION_SOURCE_SETTINGS)
            this.startActivityForResult(enableLocationIntent, REQUEST_LOCATION_SERVICE)
        }
    }

    private fun showLayoutForBluetooth() {
        val layout = layout_enable_bluetooth as ConstraintLayout
        val textView = layout.getChildAt(TEXT_VIEW) as TextView
        val button = layout.getChildAt(BUTTON) as Button
        layout.visibility = View.VISIBLE
        textView.text = getString(R.string.bluetooth_off)
        button.setOnClickListener{
            bluetoothManager.adapter.enable()
            registerReceiver(broadcastReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
            it.isEnabled = false
        }
    }

    private fun startBleSearch() {
        bluetoothManager.adapter.getProfileProxy(this,serviceListener, BluetoothProfile.A2DP)
        val leScanner = bluetoothManager.adapter.bluetoothLeScanner
        leScanner.startScan(scanCallback)
        handler.postDelayed({
            startMainActivity()
            leScanner.stopScan(scanCallback)
        }, 8000)
        view_bottom_text.text = getString(R.string.scan)
    }

    private fun startMainActivity() {
        val activityIntent = Intent(this, MainActivity::class.java)
        activityIntent.apply {
            val bundle = Bundle()
            bundle.putParcelableArray(EXTRA_A2DP,mutableListOfA2dpDevices.toTypedArray())
            bundle.putParcelableArray(EXTRA_BLE,mutableListOfBleDevices.toTypedArray())
            this.putExtras(bundle)
        }
        startActivity(activityIntent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_LOCATION_SERVICE) {
            if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
                locationIsOn = true
                layout_enable_location.visibility = View.INVISIBLE
                if(bluetoothIsOn.and(locationIsOn)){
                    startBleSearch()
                }
            }
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            ) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.title_location_permission)
                    .setMessage(R.string.text_location_permission)
                    .setPositiveButton(R.string.ok) { _: DialogInterface, _: Int ->
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                            PERMISSION_LOCATION
                        )
                    }
                    .create()
                    .show()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                    PERMISSION_LOCATION
                )
            }
        } else {
            checkBluetoothAndLocation()
        }
    }

    private fun locationPermissionGranted(): Boolean{
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (locationPermissionGranted()){
                        checkBluetoothAndLocation()
                    }

                } else {
                    view_bottom_text.text = getString(R.string.permission_denied)
                }
            }
        }
    }
}
