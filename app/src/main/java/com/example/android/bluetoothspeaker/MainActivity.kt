package com.example.android.bluetoothspeaker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build.VERSION
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import android.os.Build
import com.example.android.bluetoothspeaker.services.BtForegroundService
import android.app.ActivityManager
import android.bluetooth.*
import android.content.*
import android.os.Parcelable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.android.bluetoothspeaker.StartingActivity.Companion.EXTRA_A2DP
import com.example.android.bluetoothspeaker.StartingActivity.Companion.EXTRA_BLE


class MainActivity : AppCompatActivity() {

    private lateinit var serviceIntent: Intent
    private lateinit var listA2dpView: RecyclerView
    private lateinit var listBleView: RecyclerView
    private lateinit var bluetoothAdapter : BluetoothAdapter
    private val mutableListOfA2dpDevices = mutableListOf<BluetoothDevice>()
    private val mutableListOfBleDevices = mutableListOf<BluetoothDevice>()
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getExtraDevices()
        setBluetoothAdapter()
        setupUI()
    }

    private fun getExtraDevices() {
        val arrayOfA2dpParcelables =
            intent.extras?.getParcelableArray(EXTRA_A2DP) as Array<Parcelable>
        val arrayOfBleParcelables =
            intent.extras?.getParcelableArray(EXTRA_BLE) as Array<Parcelable>
        arrayOfA2dpParcelables.forEach {
            mutableListOfA2dpDevices.add(it as BluetoothDevice)
        }
        arrayOfBleParcelables.forEach {
            mutableListOfBleDevices.add(it as BluetoothDevice)
        }
    }

    private fun setBluetoothAdapter(){
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
    }

    private fun setupUI() {
        createNotificationChannel()
        displayRecyclerViews()
        search_button.setOnClickListener {
            toggleSearch()
        }
        serviceIntent = Intent(this, BtForegroundService::class.java)
    }

    private fun createNotificationChannel() {
        if (VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_MIN
            val channel =
                NotificationChannel(getString(R.string.channel_id), name, importance).apply {
                    description = descriptionText
                }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun displayRecyclerViews() {
        listBleView = findViewById(R.id.list_ble_view)
        listA2dpView = findViewById(R.id.list_a2dp_view)
        listBleView.layoutManager = LinearLayoutManager(this)
        listBleView.adapter =
            ListRecyclerViewAdapter(
                this,
                mutableListOfBleDevices.distinct(),
                listBleView,
                listA2dpView
            )
        listA2dpView.layoutManager = LinearLayoutManager(this)
        listA2dpView.adapter =
            ListRecyclerViewAdapter(
                this,
                mutableListOfA2dpDevices.distinct(),
                listA2dpView,
                listBleView
            )
    }

    private fun toggleSearch() {
        if (isServiceRunning(this, BtForegroundService::class.java)) {
            switchOffProcess()
        } else {
            val a2dpAdapter = listA2dpView.adapter as ListRecyclerViewAdapter
            val bleAdapter = listBleView.adapter as ListRecyclerViewAdapter
            switchOnProcess(
                a2dpAdapter.retrieveSelectedDevices(),
                bleAdapter.retrieveSelectedDevices()
            )
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
        image_view.setImageDrawable(getDrawable(R.drawable.bluetooth_off))
        search_button.text = getString(R.string.turn_on_search)
        stopService(serviceIntent)
    }

    private fun switchOnProcess(
        a2dpList: MutableList<BluetoothDevice>,
        bleList: MutableList<BluetoothDevice>
    ) {
        image_view.setImageDrawable(getDrawable(R.drawable.bluetooth_on))
        search_button.text = getString(R.string.turn_off_search)
        serviceIntent.apply {
            val bundle = Bundle()
            bundle.putParcelableArray(EXTRA_A2DP, a2dpList.toTypedArray())
            bundle.putParcelableArray(EXTRA_BLE, bleList.toTypedArray())
            this.putExtras(bundle)
        }
        ContextCompat.startForegroundService(this, serviceIntent)
    }
}
