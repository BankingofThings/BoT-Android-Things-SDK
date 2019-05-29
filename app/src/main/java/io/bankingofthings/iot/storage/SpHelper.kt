package io.bankingofthings.iot.storage

import android.content.SharedPreferences

/**
 * Shared Preferences Helper. Helper class to managing storage keys (SpNames)
 */
class SpHelper(private val sharedPreferences: SharedPreferences) {
    fun getHasRSAKeys(): Boolean = sharedPreferences.getBoolean(SpNames.HAS_RSA_KEYS.name, false)

    fun storeHasRSAKeys(hasRSAKeys: Boolean) =
        sharedPreferences.edit().putBoolean(SpNames.HAS_RSA_KEYS.name, hasRSAKeys).apply()

    fun getHasDeviceID(): Boolean = sharedPreferences.getBoolean(SpNames.HAS_DEVICE_ID.name, false)

    fun storeHasDeviceID(hasDeviceID: Boolean) =
        sharedPreferences.edit().putBoolean(SpNames.HAS_DEVICE_ID.name, hasDeviceID).apply()

    fun getDeviceID(): String = sharedPreferences.getString(SpNames.DEVICE_ID.name, "") ?: ""

    /**
     *
     * @param deviceID generated UUID
     */
    fun storeDeviceID(deviceID: String) =
        sharedPreferences.edit().putString(SpNames.DEVICE_ID.name, deviceID).apply()

    /**
     *
     * @param actionID the ID received from CORE
     */
    fun getActionFrequency(actionID: String): String? =
        sharedPreferences.getString(SpNames.ACTION_FREQUENCY.name + actionID, null)

    /**
     *
     * @param actionID the ID received from CORE
     */
    fun storeActionFrequency(actionID: String, frequency: String) =
        sharedPreferences.edit().putString(SpNames.ACTION_FREQUENCY.name + actionID, frequency).apply()

    /**
     * Get last execution time
     * @param actionID the ID received from CORE
     * @return time of execution (Calendar UTC milliseconds from the epoch). -1 is returned if none exists
     */
    fun getActionLastExecutionTime(actionID: String): Long =
        sharedPreferences.getLong(SpNames.ACTION_LAST_EXECUTION_TIME.name + actionID, -1L)

    /**
     * Store action last execution time
     * @param actionID the ID received from CORE
     * @param time time of execution (Calendar UTC milliseconds from the epoch). -1 is returned if none exists
     */
    fun storeActionLastExecutionTime(actionID: String, time: Long) =
        sharedPreferences.edit().putLong(SpNames.ACTION_LAST_EXECUTION_TIME.name + actionID, time).apply()

    /**
     * Removes all data
     */
    fun removeAllData() = sharedPreferences.edit().clear().commit()
}
