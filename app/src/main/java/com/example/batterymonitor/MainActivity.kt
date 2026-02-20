package com.example.batterymonitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var textStatus: TextView
    private lateinit var textLevel: TextView
    private lateinit var textSource: TextView
    private lateinit var textTech: TextView
    private lateinit var textTemp: TextView
    private lateinit var textVoltage: TextView
    private lateinit var textCurrent: TextView
    private lateinit var textCapacity: TextView

    private var previousChargeTime: Long = 0
    private var previousChargeCounter: Int = 0

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateBatteryInfo(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textStatus = findViewById(R.id.text_status)
        textLevel = findViewById(R.id.text_level)
        textSource = findViewById(R.id.text_source)
        textTech = findViewById(R.id.text_tech)
        textTemp = findViewById(R.id.text_temp)
        textVoltage = findViewById(R.id.text_voltage)
        textCurrent = findViewById(R.id.text_current)
        textCapacity = findViewById(R.id.text_capacity)
    }

    override fun onResume() {
        super.onResume()
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(batteryReceiver)
    }

    private fun updateBatteryInfo(intent: Intent) {
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

        val statusString = BatteryUtils.getStatusString(status)
        textStatus.text = "Charging Status: $statusString"

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = if (scale > 0) level * 100 / scale.toFloat() else 0f
        textLevel.text = String.format(Locale.getDefault(), "Battery Level: %.0f%%", batteryPct)

        val chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val sourceString = BatteryUtils.getPluggedString(chargePlug)
        textSource.text = "Power Source: $sourceString"

        val technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)
        textTech.text = "Technology: $technology"

        val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0
        textTemp.text = String.format(Locale.getDefault(), "Temperature: %.1f °C", temperature)

        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) / 1000.0
        textVoltage.text = String.format(Locale.getDefault(), "Voltage: %.2f V", voltage)

        // Current/Speed
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        var currentMicroAmps = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)

        if (currentMicroAmps == 0 || currentMicroAmps == Int.MIN_VALUE) {
             currentMicroAmps = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
        }

        // If still 0, try to estimate from capacity change
        if (currentMicroAmps == 0 || currentMicroAmps == Int.MIN_VALUE) {
             val currentChargeCounter = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
             val currentTime = System.currentTimeMillis()

             if (previousChargeTime > 0 && currentChargeCounter > 0 && currentTime > previousChargeTime) {
                 val deltaCharge = currentChargeCounter - previousChargeCounter // microAmpere-hours
                 val deltaTime = currentTime - previousChargeTime // milliseconds

                 // Current (uA) = (Delta Charge (uAh) / Delta Time (h))
                 val hours = deltaTime / 3600000.0
                 currentMicroAmps = (deltaCharge / hours).toInt()
             }

             // Update reference points
             if (previousChargeTime == 0L || currentTime - previousChargeTime > 10000) { // Update every 10 seconds or first run
                 previousChargeTime = currentTime
                 previousChargeCounter = currentChargeCounter
             }
        }

        // Usually in microamperes. Sometimes reported as negative for discharge.
        val currentMa = currentMicroAmps / 1000
        textCurrent.text = "Current: $currentMa mA"

        // Capacity
        val capacityMicroAh = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val capacityMah = capacityMicroAh / 1000
        if (capacityMah > 0) {
             textCapacity.text = "Capacity: $capacityMah mAh"
        } else {
             textCapacity.text = "Capacity: Unknown"
        }
    }
}
