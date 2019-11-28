package io.bankingofthings.iot.repo

import io.bankingofthings.iot.storage.SpHelper
import java.util.*

class IdRepo(spHelper: SpHelper, val makerID: String) {
    var deviceID: String

    init {
        if (!spHelper.getHasDeviceID()) {
            deviceID = generateID()

            spHelper.storeDeviceID(deviceID)
            spHelper.storeHasDeviceID(true)
        } else {
            deviceID = spHelper.getDeviceID()
        }
        
        System.out.println("IdRepo: deviceID = ${deviceID}")
    }

    /**
     * Returns random UUID
     */
    fun generateID(): String {
        return UUID.randomUUID().toString()
    }
}
