package io.bankingofthings.iot.storage

import android.content.SharedPreferences
import java.util.*

class SpHelper(private val sharedPreferences: SharedPreferences) {
    fun getHasKeyPair(): Boolean = sharedPreferences.getBoolean(SpNames.HAS_KEY_PAIR.name, false)

    fun getHasDeviceID(): Boolean = sharedPreferences.getBoolean(SpNames.HAS_DEVICE_ID.name, false)

    fun storeDeviceID(value: String) =
        sharedPreferences.edit().putString(SpNames.DEVICE_ID.name, value).apply()

    fun getDeviceID(): String = sharedPreferences.getString(SpNames.DEVICE_ID.name, "") ?: ""

    fun setHasKeyPair(value: Boolean) =
        sharedPreferences.edit().putBoolean(SpNames.HAS_KEY_PAIR.name, value).apply()

    fun setHasDeviceID(value: Boolean) =
        sharedPreferences.edit().putBoolean(SpNames.HAS_DEVICE_ID.name, value).apply()

    /**
     * Store action frequency
     */
    fun setActionFrequency(actionID: String, frequency: String) {
        sharedPreferences.edit().putString(SpNames.ACTION_FREQUENCY.name + actionID, frequency).apply()
    }

    /**
     * Get action frequency
     */
    fun getActionFrequency(actionID: String): String? {
        return sharedPreferences.getString(SpNames.ACTION_FREQUENCY.name + actionID, null)
    }

    /**
     * Store action last execution time
     */
    fun setActionLastExecutionTime(actionID: String, time: Long) {
        sharedPreferences.edit().putLong(SpNames.ACTION_LAST_EXECUTION_TIME.name + actionID, time).apply()
    }

    /**
     * Get last execution time
     * @return time of exection (Calendar UTC milliseconds from the epoch)
     */
    fun getActionLastExecutionTime(actionID: String): Long {
        return sharedPreferences.getLong(SpNames.ACTION_LAST_EXECUTION_TIME.name + actionID, -1L)
    }
}
