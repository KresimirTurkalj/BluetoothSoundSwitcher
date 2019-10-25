package com.example.android.bluetoothspeaker.services

import android.app.PendingIntent
import android.app.Service
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Parcelable
import android.util.Log
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import com.example.android.bluetoothspeaker.MainActivity
import com.example.android.bluetoothspeaker.R
import com.example.android.bluetoothspeaker.StartingActivity.Companion.EXTRA_A2DP
import com.example.android.bluetoothspeaker.StartingActivity.Companion.EXTRA_BLE


class BtForegroundService : Service() {

    companion object {
        const val NOTIFICATION_NUMBER = 1
        const val FAR = 150.0
        const val NOT_CONNECTED = -1
        const val THREAD_NAME = "ServiceThread"
    }

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val index = listOfGatts.indexOf(gatt)
                bleRssiValues[index][listIndexes[index]] = -rssi.toDouble()
                if (listIndexes[index] == 4) {
                    listIndexes[index] = 0
                } else {
                    listIndexes[index] += 1
                }
                Log.e("Service: ","Gatt $index received!")
            }
            super.onReadRemoteRssi(gatt, rssi, status)
        }
    }
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var audioManager: AudioManager
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                    if (intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, BluetoothAdapter.ERROR) == BluetoothA2dp.STATE_CONNECTED) {
                        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        if(musicIsOn){
                            val downKey = KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_MEDIA_PLAY)
                            val upKey = KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_MEDIA_PLAY)
                            audioManager.dispatchMediaKeyEvent(downKey)
                            audioManager.dispatchMediaKeyEvent(upKey)
                        }
                        currentlyConnected = listOfA2DpDevices.indexOf(device)
                        handler.post(btSearchRunnable)
                    }
                }
            }
        }
    }
    private val serviceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.A2DP) {
                bluetoothA2dp = null
            }
        }

        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile == BluetoothProfile.A2DP) {
                handler.post(btSearchRunnable)
                bluetoothA2dp = proxy as BluetoothA2dp
            }
        }
    }
    private var musicIsOn = false
    private var bluetoothA2dp: BluetoothA2dp? = null
    private var currentlyConnected: Int = NOT_CONNECTED
    private val listIndexes = mutableListOf<Int>()
    private val bleRssiValues = mutableListOf<DoubleArray>()
    private val averageRssiValues = mutableListOf<Double>()
    private val listOfA2DpDevices = mutableListOf<BluetoothDevice>()
    private val listOfBleDevices = mutableListOf<BluetoothDevice>()
    private val listOfGatts = mutableListOf<BluetoothGatt>()
    private val thread = HandlerThread(THREAD_NAME)
    private val connect = BluetoothA2dp::class.java.getDeclaredMethod("connect",BluetoothDevice::class.java)
    private val disconnect = BluetoothA2dp::class.java.getDeclaredMethod("disconnect",BluetoothDevice::class.java)

    private val handler by lazy{ Handler(thread.looper) }

    inner class BtSearchRunnable : Runnable {
        override fun run() {
            for (gatt in listOfGatts) {
                gatt.readRemoteRssi()
            }
            Log.e("Service: ","Gatts called!")
            findClosestDevice()
        }

        private fun findClosestDevice() {
            bleRssiValues.forEachIndexed { index, ints ->
                averageRssiValues[index] = ints.average()
            }
            val indexOfMinAverage = averageRssiValues.indexOf(averageRssiValues.min())
            if (currentlyConnected != indexOfMinAverage && isSubstantiallyCloser(indexOfMinAverage)) {
                musicIsOn = audioManager.isMusicActive
                    disconnectDevice()
                    connectToDevice(indexOfMinAverage)
            } else {
                handler.postDelayed(this, 500)
            }
        }

        private fun isSubstantiallyCloser(index: Int): Boolean {
            if(currentlyConnected != NOT_CONNECTED) {
                return (averageRssiValues[currentlyConnected] > (averageRssiValues[index] * 1.2))
            }
            return true
        }

        private fun connectToDevice(index: Int) {
            connect.invoke(bluetoothA2dp, listOfA2DpDevices[index]) //nije pouzdano, currentlyConnected treba zamijeniti sa gledanjem je li trenutni spojen isti
        }

        private fun disconnectDevice(){
            if(currentlyConnected != NOT_CONNECTED) {
                disconnect.invoke(bluetoothA2dp, listOfA2DpDevices[currentlyConnected])
            }
        }
    }

    private val btSearchRunnable = BtSearchRunnable()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (checkBluetoothLowEnergy()) {
            stopSelf()
        }
        thread.start()
        startNotification()
        getArrayOfDevices(intent)
        startBluetoothSearch()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        thread.quitSafely()
        stopBluetoothSearch()
        stopForeground(true)
    }

    private fun startNotification() {
        val notification = NotificationCompat.Builder(this, getString(R.string.channel_id))
            .setContentTitle(getText(R.string.headline_text))
            .setOngoing(true)
            .setContentText(getText(R.string.process_on)).setSmallIcon(R.drawable.bluetooth_on)
            .setContentIntent(PendingIntent.getActivity(this, 1, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT)
        ).setOnlyAlertOnce(true).build()
        startForeground(NOTIFICATION_NUMBER,notification)
    }

    private fun checkBluetoothLowEnergy(): Boolean {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return true
        }
        return false
    }

    private fun getArrayOfDevices(intent: Intent?) {
        val parcelableA2dpArray = intent?.getParcelableArrayExtra(EXTRA_A2DP) as Array<Parcelable>
        val parcelableBleArray = intent.getParcelableArrayExtra(EXTRA_BLE) as Array<Parcelable>
        for (parcelable in parcelableA2dpArray) {
            listOfA2DpDevices.add(parcelable as BluetoothDevice)
        }
        for (parcelable in parcelableBleArray) {
            listOfBleDevices.add(parcelable as BluetoothDevice)
        }
        addRssiArrays(listOfBleDevices.size)

    }

    private fun addRssiArrays(size: Int) {
        for (i in 1..size) {
            bleRssiValues.add(doubleArrayOf(FAR, FAR, FAR, FAR, FAR))
            listIndexes.add(0)
            averageRssiValues.add(FAR)
        }
    }

    private fun startBluetoothSearch() {
        connectGatts()
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        bluetoothManager.adapter.getProfileProxy(this, serviceListener, BluetoothProfile.A2DP)
        registerReceiver(broadcastReceiver,IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED))
    }

    private fun stopBluetoothSearch() {
        unregisterReceiver(broadcastReceiver)
        bluetoothManager.adapter.closeProfileProxy(BluetoothProfile.A2DP, bluetoothA2dp)
        handler.removeCallbacks(btSearchRunnable)
    }

    private fun connectGatts() {
        for (device in listOfBleDevices) {
            listOfGatts.add(
                device
                    .connectGatt(
                        this@BtForegroundService,
                        true,
                        bluetoothGattCallback
                    )
            )
        }
    }
}