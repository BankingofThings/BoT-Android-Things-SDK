package io.bankingofthings.iot.storage

import android.content.SharedPreferences

class SpHelper(private val sharedPreferences: SharedPreferences) {
    fun getHasKeyPair(): Boolean = sharedPreferences.getBoolean(SpNames.HAS_KEY_PAIR.ordinal.toString(), false)

    fun getHasDeviceID(): Boolean = sharedPreferences.getBoolean(SpNames.HAS_DEVICE_ID.ordinal.toString(), false)

    fun storeDeviceID(value: String) =
        sharedPreferences.edit().putString(SpNames.DEVICE_ID.ordinal.toString(), value).apply()

    fun getDeviceID(): String = sharedPreferences.getString(SpNames.DEVICE_ID.ordinal.toString(), "") ?: ""

    fun setHasKeyPair(value: Boolean) =
        sharedPreferences.edit().putBoolean(SpNames.HAS_KEY_PAIR.ordinal.toString(), value).apply()

    fun setHasDeviceID(value: Boolean) =
        sharedPreferences.edit().putBoolean(SpNames.HAS_DEVICE_ID.ordinal.toString(), value).apply()
}
