package com.example.android.bluetoothspeaker.services

import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.widget.Toast
import com.example.android.bluetoothspeaker.MainActivity
import com.example.android.bluetoothspeaker.R


class BtForegroundService : Service() {

    companion object {
        const val NOTIFICATION_NUMBER = 1
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    var a2dpProfile: BluetoothA2dp? = null

    private val broadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                    BluetoothAdapter.STATE_ON -> {
                        searchNearbyA2dpDevices()
                        handler.post(btSearchRunnable)
                    }
                }
            }
        }
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.A2DP) {
                a2dpProfile = proxy as BluetoothA2dp
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.A2DP) {
                a2dpProfile = null
                Toast.makeText(
                    this@BtForegroundService,
                    "Service disconnected.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    var handler = Handler()

    inner class BtSearchRunnable : Runnable {
        override fun run() {
            handler.postDelayed(this, 1000)
        }
    }

    private val btSearchRunnable = BtSearchRunnable()

    private fun checkBluetoothLowEnergy(): Boolean {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return true
        }
        return false
    }

    private fun searchNearbyA2dpDevices() {
        bluetoothAdapter!!.startDiscovery()

    }

    private fun getPairedA2dpDevices() {
        val pairedDevices = bluetoothAdapter!!.bondedDevices
        for (device in pairedDevices) {
            Toast.makeText(this, device.name, Toast.LENGTH_SHORT).show()
        }
    }

    private fun startBluetoothCheck() {
        bluetoothAdapter!!.enable()
        bluetoothAdapter!!.getProfileProxy(this, profileListener, BluetoothProfile.A2DP)
        Toast.makeText(this, getString(R.string.process_started), Toast.LENGTH_SHORT).show()
    }

    private fun stopBluetoothCheck() {
        bluetoothAdapter!!.disable()
        bluetoothAdapter?.closeProfileProxy(BluetoothProfile.A2DP, a2dpProfile)
        handler.removeCallbacks(btSearchRunnable)
        Toast.makeText(this, getString(R.string.process_ended), Toast.LENGTH_SHORT).show()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Toast.makeText(this, "Service started.", Toast.LENGTH_SHORT).show()
        createNotification()
        if (bluetoothAdapter == null && checkBluetoothLowEnergy()) {
            stopSelf()
        } else {
            registerReceiver(broadcastReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
            startBluetoothCheck()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        stopBluetoothCheck()
        Toast.makeText(this, "Service destroyed.", Toast.LENGTH_SHORT).show()
        unregisterReceiver(broadcastReceiver)
        stopForeground(true)
    }

    private fun createNotification() {
        startForeground(
            NOTIFICATION_NUMBER,
            NotificationCompat.Builder(this, getString(R.string.channel_id))
                .setContentTitle(getText(R.string.headline_text))
                .setContentText(getText(R.string.process_on)).setSmallIcon(R.drawable.bluetooth_on)
                .setContentIntent(
                    PendingIntent.getActivity(
                        this,
                        1,
                        Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                ).setOnlyAlertOnce(true).build()
        )
    }
}