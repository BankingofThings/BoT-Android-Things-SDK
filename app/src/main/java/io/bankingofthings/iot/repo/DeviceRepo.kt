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

class DeviceRepo(private val context: Context, private val keyRepo: KeyRepo, private val idRepo: IdRepo) {
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
            BuildConfig.DEVICE_NAME,
            if (BuildConfig.MULTI_PAIR) 1 else 0,
            BuildConfig.ALTERNATIVE_IDENTIFIER_DISPLAYNAME
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
            BuildConfig.BUILD_DATE,
            Build.HARDWARE,
            Build.CPU_ABI,
            Runtime.getRuntime().availableProcessors().toString(),
            BuildConfig.HOSTNAME,
            if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) "little" else "big",
            (memoryInfo.availMem).toString() + "/" + (memoryInfo.totalMem).toString(),
            networkModel.network,
            networkModel.ip,
            false
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
