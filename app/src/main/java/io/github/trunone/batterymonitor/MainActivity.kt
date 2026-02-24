package io.github.trunone.batterymonitor

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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
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

    private var isDebugVisible: Boolean = false
    private var isPowerMode: Boolean = false
    private var isDataSourceNow: Boolean = true // Default to Now
    private val PREFS_NAME = "BatteryMonitorPrefs"
    private val KEY_DEBUG_VISIBLE = "debug_visible"
    private val KEY_POWER_MODE = "power_mode"
    private val KEY_THEME = "theme_preference"
    private val KEY_DATA_SOURCE = "data_source"

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

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)

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
        isDataSourceNow = settings.getBoolean(KEY_DATA_SOURCE, true)

        // Restore Theme
        val themePref = settings.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(themePref)

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
            R.id.action_theme -> {
                showThemeSelectionDialog()
                true
            }
            R.id.action_data_source -> {
                showDataSourceSelectionDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDataSourceSelectionDialog() {
        val options = arrayOf(
            getString(R.string.data_source_now),
            getString(R.string.data_source_avg)
        )
        // If isDataSourceNow is true, index is 0. Else index is 1.
        val checkedItem = if (isDataSourceNow) 0 else 1

        AlertDialog.Builder(this)
            .setTitle(R.string.data_source_title)
            .setSingleChoiceItems(options, checkedItem) { dialog, which ->
                isDataSourceNow = (which == 0)

                val settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val editor = settings.edit()
                editor.putBoolean(KEY_DATA_SOURCE, isDataSourceNow)
                editor.apply()

                updateLiveValues()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showThemeSelectionDialog() {
        val themes = arrayOf(
            getString(R.string.theme_light),
            getString(R.string.theme_dark),
            getString(R.string.theme_system)
        )
        val themeValues = arrayOf(
            AppCompatDelegate.MODE_NIGHT_NO,
            AppCompatDelegate.MODE_NIGHT_YES,
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )

        val settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentTheme = settings.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        val checkedItem = themeValues.indexOf(currentTheme)
        // Default to System if not found
        val actualCheckedItem = if (checkedItem >= 0) checkedItem else 2

        AlertDialog.Builder(this)
            .setTitle(R.string.theme_title)
            .setSingleChoiceItems(themes, actualCheckedItem) { dialog, which ->
                val selectedTheme = themeValues[which]

                val editor = settings.edit()
                editor.putInt(KEY_THEME, selectedTheme)
                editor.apply()

                AppCompatDelegate.setDefaultNightMode(selectedTheme)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
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

        // 1. Direct Properties
        val currentNow = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val currentAvg = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
        val chargeCounter = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val energyCounter = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)

        var estimatedCurrentMa = 0
        var estimationMethod = ""

        // Choose source based on preference
        val rawValue = if (isDataSourceNow) currentNow else currentAvg
        val label = if (isDataSourceNow) "Now" else "Avg"

        val result = estimateCurrent(rawValue, label)

        if (result != null) {
            estimatedCurrentMa = result.first
            estimationMethod = result.second
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

    private fun estimateCurrent(rawValue: Int, sourceLabel: String): Pair<Int, String>? {
        if (rawValue != 0 && rawValue != Int.MIN_VALUE) {
            // Heuristic: If value is small (< 10000), assume it's already in mA
            if (abs(rawValue) < 10000) {
                return Pair(rawValue, "Sensor ($sourceLabel, mA)")
            } else {
                return Pair(rawValue / 1000, "Sensor ($sourceLabel, uA)")
            }
        }
        return null
    }
}
