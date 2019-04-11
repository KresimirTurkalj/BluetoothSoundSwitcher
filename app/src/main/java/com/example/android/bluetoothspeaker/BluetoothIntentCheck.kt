package com.example.android.bluetoothspeaker

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.support.v7.app.ActionBarDrawerToggle

class BluetoothIntentCheck : Service(){
    private val startMode = Service.START_STICKY
    private var processBinder: IBinder? = null        // interface for clients that bind

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return startMode
    }

    override fun onBind(intent: Intent): IBinder? {
        // A client is binding to the service with bindService()
        return processBinder
    }

    override fun onDestroy() {
        // The service is no longer used and is being destroyed
    }

}