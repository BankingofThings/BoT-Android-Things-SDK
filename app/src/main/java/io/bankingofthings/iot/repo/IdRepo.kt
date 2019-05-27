package io.bankingofthings.iot.repo

import io.bankingofthings.iot.BuildConfig
import io.bankingofthings.iot.storage.SpHelper
import java.util.*

class IdRepo(spHelper: SpHelper, val makerID: String) {
    var deviceID: String

    init {
        if (!spHelper.getHasDeviceID()) {
            deviceID = generateID()

            spHelper.storeDeviceID(deviceID)
            spHelper.setHasDeviceID(true)
        } else {
            deviceID = spHelper.getDeviceID()
        }
    }

    /**
     * Returns random UUID
     */
    fun generateID(): String {
        return UUID.randomUUID().toString()
    }
}
