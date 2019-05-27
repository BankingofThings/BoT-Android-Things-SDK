package io.bankingofthings.iot.repo

import android.app.ActivityManager
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.text.format.Formatter
import android.util.Base64
import io.bankingofthings.iot.BuildConfig
import io.bankingofthings.iot.model.domain.BotDeviceModel
import io.bankingofthings.iot.model.domain.DeviceModel
import io.bankingofthings.iot.model.domain.NetworkModel
import java.nio.ByteOrder

/**
 * Contains different type of device models. These are used for converting to pojo models.
 */
class DeviceRepo(
    private val context: Context,
    private val keyRepo: KeyRepo,
    private val idRepo: IdRepo,
    private val hostName: String,
    private val deviceName: String,
    private val buildDate: String,
    private val hasWifi:Boolean,
    private val multiPair: Boolean,
    private val aid: String?
) {
    val deviceModel: DeviceModel
    val networkModel: NetworkModel
    val botDeviceModel: BotDeviceModel

    init {
        deviceModel = createDeviceModel()
        networkModel = createNetworkModel()
        botDeviceModel = createBotDeviceModel()
    }

    /**
     * Business level model
     */
    private fun createDeviceModel(): DeviceModel {
        return DeviceModel(
            idRepo.makerID,
            idRepo.deviceID,
            Base64.encodeToString(keyRepo.publicKey.encoded, Base64.NO_WRAP),
            deviceName,
            if (multiPair) 1 else 0,
            aid
        )
    }

    /**
     * Hardware level model
     */
    private fun createBotDeviceModel(): BotDeviceModel {
        val memoryInfo = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memoryInfo)

        return BotDeviceModel(
            "android things",
            buildDate,
            Build.HARDWARE,
            Build.CPU_ABI,
            Runtime.getRuntime().availableProcessors().toString(),
            hostName,
            if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) "little" else "big",
            (memoryInfo.availMem).toString() + "/" + (memoryInfo.totalMem).toString(),
            networkModel.network,
            networkModel.ip,
            hasWifi
        )
    }

    /**
     * Model containing WiFi SSID and ip address
     */
    private fun createNetworkModel(): NetworkModel {
        return (context.getSystemService(Context.WIFI_SERVICE) as WifiManager).connectionInfo.let {
            NetworkModel(it.ssid, Formatter.formatIpAddress(it.ipAddress))
        }
    }
}
