package io.bankingofthings.iot.repo

import io.bankingofthings.iot.storage.SpHelper
import java.util.*

class IdRepo(spHelper: SpHelper, val makerID: String, var deviceID: String? = null) {

    init {
        if (deviceID == null) {
            if (!spHelper.getHasDeviceID()) {
                deviceID = generateID()

                spHelper.storeDeviceID(deviceID!!)
                spHelper.storeHasDeviceID(true)
            } else {
                deviceID = spHelper.getDeviceID()
            }
        }

        println("IdRepo.init deviceID:" + deviceID)
    }

    /**
     * Returns random UUID
     */
    fun generateID(): String {
        return UUID.randomUUID().toString()
    }
}
