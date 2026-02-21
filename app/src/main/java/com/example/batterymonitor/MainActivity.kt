package com.example.batterymonitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var textStatus: TextView
    private lateinit var textLevel: TextView
    private lateinit var textSource: TextView
    private lateinit var textTech: TextView
    private lateinit var textTemp: TextView
    private lateinit var textVoltage: TextView
    private lateinit var textCurrent: TextView
    private lateinit var textCapacity: TextView
    private lateinit var textDebug: TextView
    private lateinit var cardMainMetric: View
    private lateinit var labelMainMetric: TextView

    private var previousChargeTime: Long = 0
    private var previousChargeCounter: Int = 0

    // For Energy based estimation
    private var previousEnergyTime: Long = 0
    private var previousEnergyCounter: Long = 0L

    // For Percentage based estimation
    private var previousPctTime: Long = 0
    private var previousPctLevel: Int = -1

    private var isDebugVisible: Boolean = false
    private var isPowerMode: Boolean = false
    private val PREFS_NAME = "BatteryMonitorPrefs"
    private val KEY_DEBUG_VISIBLE = "debug_visible"
    private val KEY_POWER_MODE = "power_mode"

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateLiveValues()
            handler.postDelayed(this, 1000) // Update every 1 second
        }
    }

    private var lastIntent: Intent? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            lastIntent = intent
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
        textDebug = findViewById(R.id.text_debug)
        cardMainMetric = findViewById(R.id.card_main_metric)
        labelMainMetric = findViewById(R.id.label_main_metric)

        // Restore preferences
        val settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isDebugVisible = settings.getBoolean(KEY_DEBUG_VISIBLE, false)
        isPowerMode = settings.getBoolean(KEY_POWER_MODE, false)

        updateDebugVisibility()
        updateMainMetricLabel()

        cardMainMetric.setOnClickListener {
            isPowerMode = !isPowerMode
            // Save preference
            val editor = settings.edit()
            editor.putBoolean(KEY_POWER_MODE, isPowerMode)
            editor.apply()

            updateMainMetricLabel()
            updateLiveValues()
        }
    }

    private fun updateMainMetricLabel() {
        labelMainMetric.text = if (isPowerMode) "Charging Power" else "Charging Current"
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_toggle_debug -> {
                isDebugVisible = !isDebugVisible
                // Save preference
                val settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val editor = settings.edit()
                editor.putBoolean(KEY_DEBUG_VISIBLE, isDebugVisible)
                editor.apply()

                updateDebugVisibility()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateDebugVisibility() {
        textDebug.visibility = if (isDebugVisible) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, intentFilter)
        handler.post(updateRunnable)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(batteryReceiver)
        handler.removeCallbacks(updateRunnable)
    }

    private fun updateLiveValues() {
        val intent = lastIntent ?: return
        updateBatteryInfo(intent)
    }

    private fun updateBatteryInfo(intent: Intent) {
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

        val statusString = BatteryUtils.getStatusString(status)
        textStatus.text = statusString

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = if (scale > 0) level * 100 / scale.toFloat() else 0f
        textLevel.text = String.format(Locale.getDefault(), "%.0f%%", batteryPct)

        val chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val sourceString = BatteryUtils.getPluggedString(chargePlug)
        textSource.text = sourceString

        val technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)
        textTech.text = technology

        val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0
        textTemp.text = String.format(Locale.getDefault(), "%.1f °C", temperature)

        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) / 1000.0
        textVoltage.text = String.format(Locale.getDefault(), "%.2f V", voltage)

        // --- Current/Speed Estimation Logic ---
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val currentTime = System.currentTimeMillis()

        // 1. Direct Properties
        val currentNow = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val currentAvg = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
        val chargeCounter = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val energyCounter = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)

        var estimatedCurrentMa = 0
        var estimationMethod = ""

        // Try 1: Current Now
        if (currentNow != 0 && currentNow != Int.MIN_VALUE) {
            // Heuristic: If value is small (< 10000), assume it's already in mA
            if (abs(currentNow) < 10000) {
                estimatedCurrentMa = currentNow
                estimationMethod = "Sensor (Now, mA)"
            } else {
                estimatedCurrentMa = currentNow / 1000
                estimationMethod = "Sensor (Now, uA)"
            }
        }
        // Try 2: Current Average
        else if (currentAvg != 0 && currentAvg != Int.MIN_VALUE) {
            if (abs(currentAvg) < 10000) {
                estimatedCurrentMa = currentAvg
                estimationMethod = "Sensor (Avg, mA)"
            } else {
                estimatedCurrentMa = currentAvg / 1000
                estimationMethod = "Sensor (Avg, uA)"
            }
        }

        // Try 3: Change in Charge Counter (Ah)
        if (estimatedCurrentMa == 0) {
             if (previousChargeTime > 0 && chargeCounter > 0 && currentTime > previousChargeTime) {
                 val deltaCharge = chargeCounter - previousChargeCounter // microAmpere-hours
                 val deltaTime = currentTime - previousChargeTime // milliseconds
                 val hours = deltaTime / 3600000.0

                 // If the change is significant enough to calculate speed
                 if (hours > 0 && abs(deltaCharge) > 0) {
                     val calculatedUa = deltaCharge / hours
                     estimatedCurrentMa = (calculatedUa / 1000).toInt()
                     estimationMethod = "Est. (Charge)"
                 }
             }

             if (previousChargeTime == 0L || currentTime - previousChargeTime > 5000) { // Update reference every 5s
                 previousChargeTime = currentTime
                 previousChargeCounter = chargeCounter
             }
        }

        // Try 4: Change in Energy Counter (Wh) -> Power / Voltage
        if (estimatedCurrentMa == 0) {
             if (previousEnergyTime > 0 && energyCounter > 0 && currentTime > previousEnergyTime) {
                 val deltaEnergy = energyCounter - previousEnergyCounter // nanowatt-hours
                 val deltaTime = currentTime - previousEnergyTime
                 val hours = deltaTime / 3600000.0

                 if (hours > 0 && abs(deltaEnergy) > 0 && voltage > 0) {
                     val powerNw = deltaEnergy / hours // nanowatts
                     val powerMw = powerNw / 1_000_000 // milliwatts
                     // P = V * I  => I = P / V
                     // I(mA) = P(mW) / V(V)
                     estimatedCurrentMa = (powerMw / voltage).toInt()
                     estimationMethod = "Est. (Energy)"
                 }
             }

             if (previousEnergyTime == 0L || currentTime - previousEnergyTime > 5000) {
                 previousEnergyTime = currentTime
                 previousEnergyCounter = energyCounter
             }
        }

        // Try 5: Change in Percentage (Current = Capacity * d%/dt)
        if (estimatedCurrentMa == 0) {
            // Use 4000 mAh as assumed capacity
            val assumedCapacity = 4000
            val currentPct = if (scale > 0) level * 100 / scale else 0

            if (previousPctTime > 0 && previousPctLevel != -1 && currentPct != previousPctLevel && currentTime > previousPctTime) {
                 val deltaPct = currentPct - previousPctLevel
                 val deltaTime = currentTime - previousPctTime
                 val hours = deltaTime / 3600000.0 // Convert ms to hours

                 val deltaCapacity = (deltaPct / 100.0) * assumedCapacity // mAh

                 estimatedCurrentMa = (deltaCapacity / hours).toInt()
                 estimationMethod = "Est. (Percent)"
            }

            // Only update reference if percentage changes (or first run)
            if (previousPctLevel == -1 || currentPct != previousPctLevel) {
                previousPctTime = currentTime
                previousPctLevel = currentPct
            }
        }

        if (isPowerMode) {
            // Power (W) = Current (A) * Voltage (V)
            // estimatedCurrentMa is in mA, voltage is in V.
            // Power (mW) = mA * V
            // Power (W) = (mA * V) / 1000
            val powerWatts = (estimatedCurrentMa * voltage) / 1000.0
            textCurrent.text = String.format(Locale.getDefault(), "%.2f W", powerWatts)
        } else {
            textCurrent.text = if (estimationMethod.isNotEmpty()) {
                "$estimatedCurrentMa mA"
            } else {
                "0 mA"
            }
        }

        // Capacity
        val capacityMah = chargeCounter / 1000
        if (capacityMah > 0) {
             textCapacity.text = "$capacityMah mAh"
        } else {
             textCapacity.text = "Unknown"
        }

        // Debug Info
        val debugInfo = StringBuilder()
        debugInfo.append("Method: $estimationMethod\n")
        debugInfo.append("Raw Sensors:\n")
        debugInfo.append("Now: $currentNow (Raw)\n")
        debugInfo.append("Avg: $currentAvg (Raw)\n")
        debugInfo.append("Cntr: $chargeCounter uAh\n")
        debugInfo.append("Engy: $energyCounter nWh\n")
        debugInfo.append("Lvl: $level / $scale")
        textDebug.text = debugInfo.toString()
    }
}
