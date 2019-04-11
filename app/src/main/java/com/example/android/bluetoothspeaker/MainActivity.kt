package com.example.android.bluetoothspeaker

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.NotificationCompat
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_BT_PERMISSION = 1
    }

    private val pendingIntent: PendingIntent =
        Intent(this, BluetoothIntentCheck::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, 0)
        }

    private var notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var notification: Notification = buildNotification()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupUI()
    }

    private fun setupUI() {
        createNotificationChannel()
        button.setOnClickListener {
            toggleButtonAndProcessState()
        }
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_id)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(getString(R.string.channel_id), name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, getString(R.string.channel_id))
            .setContentTitle(getText(R.string.headline_text))
            .setContentText(getText(R.string.process_off))
            .setSmallIcon(R.drawable.stat_sys_data_no_bluetooth)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun toggleButtonAndProcessState() {
        if (isServiceRunningInForeground(this, BluetoothIntentCheck::class.java)) {
            switchOffProcess()
        } else {
            switchOnProcess()
        }
    }

    private fun switchOffProcess() {
        button.text = getString(R.string.turn_on)
    }

    private fun switchOnProcess() {
        button.text = getString(R.string.turn_off)
    }

    private fun isServiceRunningInForeground(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.runningAppProcesses) {
            if (serviceClass.name == service.processName) {
                return true
            }
        }
        return false
    }

    private fun requestBluetoothPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN),
            REQUEST_BT_PERMISSION
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_BT_PERMISSION) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun pairBluetoothDevice() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADMIN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestBluetoothPermissions()
        } else {
            //TODO Pair with bluetooth device if it can be used as audio output
        }
    }

    private fun setBluetoothConnection() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestBluetoothPermissions()
        } else {
            //TODO Set closest bluetooth audio output as device output
        }
    }
}
