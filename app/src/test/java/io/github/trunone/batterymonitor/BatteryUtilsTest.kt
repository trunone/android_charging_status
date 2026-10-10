package io.github.trunone.batterymonitor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    @Test
    fun estimateCurrent_returnsCorrectEstimation() {
        // 0 or Int.MIN_VALUE returns null
        assertNull(BatteryUtils.estimateCurrent(0, "Now"))
        assertNull(BatteryUtils.estimateCurrent(Int.MIN_VALUE, "Now"))

        // Values with magnitude < 10000 are treated as mA
        val resultMa = BatteryUtils.estimateCurrent(1500, "Now")
        assertEquals(1500, resultMa?.first)
        assertEquals("Sensor (Now, mA)", resultMa?.second)

        val resultNegativeMa = BatteryUtils.estimateCurrent(-800, "Avg")
        assertEquals(-800, resultNegativeMa?.first)
        assertEquals("Sensor (Avg, mA)", resultNegativeMa?.second)

        // Values with magnitude >= 10000 are treated as uA and converted to mA
        val resultUa = BatteryUtils.estimateCurrent(1500000, "Now")
        assertEquals(1500, resultUa?.first)
        assertEquals("Sensor (Now, uA)", resultUa?.second)

        val resultNegativeUa = BatteryUtils.estimateCurrent(-2500000, "Avg")
        assertEquals(-2500, resultNegativeUa?.first)
        assertEquals("Sensor (Avg, uA)", resultNegativeUa?.second)
    }
}
