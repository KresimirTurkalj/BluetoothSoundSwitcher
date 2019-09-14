package com.example.android.bluetoothspeaker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build.VERSION
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import android.content.Context
import android.os.Build
import com.example.android.bluetoothspeaker.services.BtForegroundService
import android.app.ActivityManager
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.ParcelUuid
import android.widget.Toast
import java.util.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class MainActivity : AppCompatActivity() {

    companion object{
        private val PERMISSION_LOCATION = 1
    }

    private lateinit var serviceIntent: Intent

    private val bluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private fun startScan() {
        registerReceiver(broadcastReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        registerReceiver(
            broadcastReceiver,
            IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        )
        registerReceiver(broadcastReceiver, IntentFilter(BluetoothDevice.ACTION_UUID))
        pair_button.text = getString(R.string.turn_off_pair)
        pair_button.isEnabled = false
        search_button.isEnabled = false
        bluetoothAdapter.startDiscovery()
    }

    private fun enableButtons() {
        pair_button.text = getString(R.string.turn_on_pair)
        pair_button.isEnabled = true
        search_button.isEnabled = true
        unregisterReceiver(broadcastReceiver)
    }

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            Toast.makeText(this@MainActivity, rssi.toString(), Toast.LENGTH_SHORT).show()
        }
    }
    private val potentialDeviceList = mutableListOf<BluetoothDevice>()
    private val broadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    if (intent.getIntExtra(
                            BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR
                        ) == BluetoothAdapter.STATE_ON
                    ) {
                        startScan()
                    }
                }
                BluetoothDevice.ACTION_FOUND -> {
                    val bluetoothDevice: BluetoothDevice =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    if(!potentialDeviceList.contains(bluetoothDevice)){
                        potentialDeviceList.add(bluetoothDevice)
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    checkForPairing()
                }
                BluetoothDevice.ACTION_UUID -> {
                    val audioSinkUuid =
                        ParcelUuid(UUID.fromString("0000110B-0000-1000-8000-00805F9B34FB"))
                    val intentUuid =
                        intent.getParcelableExtra<ParcelUuid>(BluetoothDevice.EXTRA_UUID)
                    if (audioSinkUuid == intentUuid) {
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                            .createBond()
                    }
                    checkForPairing()
                }
            }
        }
    }

    private fun checkForPairing() {
        if (potentialDeviceList.isNotEmpty()) {
            pairNextDevice()
        } else {
            enableButtons()
        }
    }

    private fun pairNextDevice() {
        potentialDeviceList.first().fetchUuidsWithSdp()
        potentialDeviceList.remove(potentialDeviceList.first())
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupUI()
    }

    private fun setupUI() {
        createNotificationChannel()
        search_button.setOnClickListener {
            toggleSearch()
        }
        pair_button.setOnClickListener {
            checkLocationPermission()
        }
        serviceIntent = Intent(this, BtForegroundService::class.java)
    }

    private fun checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this,getString(R.string.permission_explanation),Toast.LENGTH_LONG).show()
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                PERMISSION_LOCATION
            )
        }
        else{
            togglePair()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    togglePair()
                }
                else{
                    pair_button.text = getString(R.string.pair_on_permission_denied)
                    pair_button.isEnabled = false
                }
                return
            }
            else -> {

            }
        }
    }

    private fun createNotificationChannel() {
        if (VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel =
                NotificationChannel(getString(R.string.channel_id), name, importance).apply {
                    description = descriptionText
                }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun togglePair() {
        if (!isServiceRunning(this, BtForegroundService::class.java)) {
            if (bluetoothAdapter != null) {
                if (bluetoothAdapter.isEnabled) {
                    startScan()
                } else {
                    bluetoothAdapter.enable()
                    registerReceiver(
                        broadcastReceiver,
                        IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
                    )
                }
            }
        } else {
            Toast.makeText(this, "Can't search now.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleSearch() {
        if (isServiceRunning(this, BtForegroundService::class.java)) {
            switchOffProcess()
        } else {
            switchOnProcess()
        }
    }

    @Suppress("DEPRECATION")
    private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val services = activityManager.getRunningServices(Integer.MAX_VALUE)
        for (runningServiceInfo in services) {
            if (runningServiceInfo.service.className == serviceClass.name) {
                return true
            }
        }
        return false
    }

    private fun switchOffProcess() {
        imageView.setImageDrawable(getDrawable(R.drawable.bluetooth_off))
        search_button.text = getString(R.string.turn_on_search)
        stopService(serviceIntent)
    }

    private fun switchOnProcess() {
        imageView.setImageDrawable(getDrawable(R.drawable.bluetooth_on))
        search_button.text = getString(R.string.turn_off_search)
        startService(serviceIntent)
    }
}
