package io.github.trunone.batterymonitor

import android.os.BatteryManager
import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryUtilsTest {
    @Test
    fun getStatusString_returnsCorrectString() {
        // Values from Android SDK documentation
        assertEquals("Charging", BatteryUtils.getStatusString(2))
        assertEquals("Discharging", BatteryUtils.getStatusString(3))
        assertEquals("Full", BatteryUtils.getStatusString(5))
        assertEquals("Not Charging", BatteryUtils.getStatusString(4))
        assertEquals("Unknown", BatteryUtils.getStatusString(1))
    }

    @Test
    fun getPluggedString_returnsCorrectString() {
        // Values from Android SDK documentation
        assertEquals("AC", BatteryUtils.getPluggedString(1))
        assertEquals("USB", BatteryUtils.getPluggedString(2))
        assertEquals("Wireless", BatteryUtils.getPluggedString(4))
        assertEquals("Battery", BatteryUtils.getPluggedString(0))
    }
}
