package io.github.trunone.batterymonitor

import android.os.BatteryManager
import kotlin.math.abs

object BatteryUtils {
    fun getStatusString(status: Int): String {
        return when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            else -> "Unknown"
        }
    }

    fun getPluggedString(plugged: Int): String {
        return when (plugged) {
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "Battery"
        }
    }

    fun estimateCurrent(rawValue: Int, sourceLabel: String): Pair<Int, String>? {
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
