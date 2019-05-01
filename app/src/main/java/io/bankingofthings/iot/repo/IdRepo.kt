package io.bankingofthings.iot.repo

import io.bankingofthings.iot.BuildConfig
import io.bankingofthings.iot.storage.SpHelper
import java.util.*

class IdRepo(spHelper: SpHelper) {
    var deviceID: String

    val makerID = BuildConfig.MAKER_ID

    init {
        if (!spHelper.getHasDeviceID() || BuildConfig.GENERATE_DEVICE_ID_EVERY_RUN) {
            deviceID = generateID()

            System.out.println("IdRepo: creating new DeviceID $deviceID")

            spHelper.storeDeviceID(deviceID)
            spHelper.setHasDeviceID(true)
        } else {
            deviceID = spHelper.getDeviceID()
            System.out.println("IdRepo: using existing DeviceID $deviceID")
        }
    }

    /**
     * Returns random UUID
     */
    public fun generateID(): String {
        return UUID.randomUUID().toString()
    }
}
