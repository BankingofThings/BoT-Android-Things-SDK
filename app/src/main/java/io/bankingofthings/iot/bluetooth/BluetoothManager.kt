package io.bankingofthings.iot.bluetooth

import android.bluetooth.*
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import android.provider.Settings
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import io.bankingofthings.iot.BuildConfig
import io.bankingofthings.iot.model.domain.BotDeviceModel
import io.bankingofthings.iot.model.domain.DeviceModel
import io.bankingofthings.iot.model.domain.NetworkModel
import io.bankingofthings.iot.network.pojo.BotDeviceSsidPojo
import io.reactivex.Observable
import java.util.concurrent.TimeUnit

class BluetoothManager(
    private val context: Context,
    private val nativeBluetoothManager: BluetoothManager,
    deviceModel: DeviceModel,
    botDeviceModel: BotDeviceModel,
    networkModel: NetworkModel,
    private val blueToothName: String,
    private val hasWifi: Boolean,
    private val callback: Callback
) {
    interface Callback {
        fun onWifiCredentialsChanged(pojo: BotDeviceSsidPojo)
    }

    var started: Boolean = false
    private val UUID = java.util.UUID.fromString("729BE9C4-3C61-4EFB-884F-B310B6FFFFD1")
    private val UUID_DEVICE = java.util.UUID.fromString("CAD1B513-2DA4-4609-9908-234C6D1B2A9C")
    private val UUID_DEVICE_INFO = java.util.UUID.fromString("CD1B3A04-FA33-41AA-A25B-8BEB2D3BEF4E")
    private val UUID_DEVICE_NETWORK = java.util.UUID.fromString("C42639DC-270D-4690-A8B3-6BA661C6C899")
    private val UUID_DEVICE_WIFI = java.util.UUID.fromString("32BEAA1B-D20B-47AC-9385-B243B8071DE4")
    private var gattServer: BluetoothGattServer? = null
    private var deviceModelJson: String = Gson().toJson(deviceModel)
    private var botDeviceModelJson: String = Gson().toJson(botDeviceModel)
    private var networkModelJson: String = Gson().toJson(networkModel)
    private var wifiWriteString = ""

    private val advertisingCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)

            when (errorCode) {
                ADVERTISE_FAILED_DATA_TOO_LARGE -> System.out.println("BluetoothManager:onStartFailure data too large")
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> System.out.println("BluetoothManager:onStartFailure too many advertisers")
                ADVERTISE_FAILED_ALREADY_STARTED -> System.out.println("BluetoothManager:onStartFailure already started")
                ADVERTISE_FAILED_INTERNAL_ERROR -> System.out.println("BluetoothManager:onStartFailure internal error")
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> System.out.println("BluetoothManager:onStartFailure feature unsupported")
                else -> System.out.println("BluetoothManager:onStartFailure ${errorCode}")
            }
        }
    }

    /**
     * Initialize and start the bluetooth device
     */
    fun start() {
        System.out.println("BluetoothManager:start")

        started = true

        createGattService()

        startAdvertising()
    }

    fun kill() {
        System.out.println("BluetoothManager:kill")
        stopAdvertising()
        gattServer?.close()

        started = false
    }

    private fun startAdvertising() {
        nativeBluetoothManager.adapter.setName(blueToothName)

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .setIncludeTxPowerLevel(false)
            .addServiceUuid(ParcelUuid(UUID))
            .build()

        nativeBluetoothManager.adapter.bluetoothLeAdvertiser.startAdvertising(
            settings,
            data,
            advertisingCallback
        )
    }

    private fun stopAdvertising() {
        nativeBluetoothManager.adapter.bluetoothLeAdvertiser.stopAdvertising(advertisingCallback)
    }

    /**
     * Create the content for the bluetooth device
     */
    private fun createGattService() {
        gattServer = nativeBluetoothManager.openGattServer(
            context,
            object : BluetoothGattServerCallback() {
                /**
                 * Connection changed
                 */
                override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
                    super.onConnectionStateChange(device, status, newState)

                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        System.out.println("BluetoothManager:onConnectionStateChange status success")
                    } else {
                        System.out.println("BluetoothManager:onConnectionStateChange status failed")
                    }

                    when (newState) {
                        BluetoothProfile.STATE_CONNECTED -> {
                            System.out.println("BluetoothManager:onConnectionStateChange connected")
                            stopAdvertising()
                        }
                        BluetoothProfile.STATE_DISCONNECTED -> {
                            System.out.println("BluetoothManager:onConnectionStateChange disconnected")
                            startAdvertising()
                        }
                        BluetoothProfile.STATE_CONNECTING -> System.out.println("BluetoothManager:onConnectionStateChange connecting")
                        BluetoothProfile.STATE_DISCONNECTING -> System.out.println("BluetoothManager:onConnectionStateChange disconnecting")
                    }
                }

                /**
                 * Read requests
                 */
                override fun onCharacteristicReadRequest(
                    device: BluetoothDevice?,
                    requestId: Int,
                    offset: Int,
                    characteristic: BluetoothGattCharacteristic?
                ) {
                    super.onCharacteristicReadRequest(device, requestId, offset, characteristic)

                    when (characteristic?.uuid) {
                        UUID_DEVICE -> {
                            gattServer?.sendResponse(
                                device,
                                requestId,
                                BluetoothGatt.GATT_SUCCESS,
                                offset,
                                takeOffset(deviceModelJson, offset)
                            )
                        }
                        UUID_DEVICE_INFO -> {
                            gattServer?.sendResponse(
                                device,
                                requestId,
                                BluetoothGatt.GATT_SUCCESS,
                                offset,
                                takeOffset(botDeviceModelJson, offset)
                            )
                        }
                        UUID_DEVICE_NETWORK -> {
                            gattServer?.sendResponse(
                                device,
                                requestId,
                                BluetoothGatt.GATT_SUCCESS,
                                offset,
                                takeOffset(networkModelJson, offset)
                            )
                        }
                        UUID_DEVICE_WIFI -> {
                            gattServer?.sendResponse(
                                device,
                                requestId,
                                BluetoothGatt.GATT_SUCCESS,
                                offset,
                                takeOffset(networkModelJson, offset)
                            )
                        }
                    }
                }

                override fun onCharacteristicWriteRequest(
                    device: BluetoothDevice?,
                    requestId: Int,
                    characteristic: BluetoothGattCharacteristic?,
                    preparedWrite: Boolean,
                    responseNeeded: Boolean,
                    offset: Int,
                    value: ByteArray?
                ) {
                    super.onCharacteristicWriteRequest(
                        device,
                        requestId,
                        characteristic,
                        preparedWrite,
                        responseNeeded,
                        offset,
                        value
                    )

                    if (characteristic?.uuid == UUID_DEVICE_WIFI) {
                        value?.let {
                            wifiWriteString += String(it)
                        }

                        gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)

                        try {
                            val pojo = Gson().fromJson(wifiWriteString, BotDeviceSsidPojo::class.java)

                            characteristic?.setValue(wifiWriteString)
                            gattServer?.notifyCharacteristicChanged(device, characteristic, false)

                            callback.onWifiCredentialsChanged(pojo)

                            // Clear after characteristic received
                            wifiWriteString = ""
                        } catch (e: JsonSyntaxException) {
                            // ignore
                        }
                    }
                }
            })

        gattServer?.addService(createFinnService())
    }

    /**
     * Returns next block (buffer) of json part
     */
    private fun takeOffset(json: String, offset: Int): ByteArray {
        System.out.println("BluetoothManager:takeOffset json = ${json}")

        return json.toByteArray().let { it.copyOfRange(offset, it.size) }
            .apply {
                System.out.println("BluetoothManager:takeOffset ${String(this)}")
            }
    }

    /**
     * Create one service and add 4 characteristics
     */
    private fun createFinnService(): BluetoothGattService {
        return BluetoothGattService(UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
            .apply {
                addCharacteristic(
                    BluetoothGattCharacteristic(
                        UUID_DEVICE,
                        BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                        BluetoothGattCharacteristic.PERMISSION_READ
                    )
                )
                addCharacteristic(
                    BluetoothGattCharacteristic(
                        UUID_DEVICE_INFO,
                        BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                        BluetoothGattCharacteristic.PERMISSION_READ
                    )
                )
                addCharacteristic(
                    BluetoothGattCharacteristic(
                        UUID_DEVICE_NETWORK,
                        BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                        BluetoothGattCharacteristic.PERMISSION_READ
                    )
                )

                if (hasWifi) {
                    addCharacteristic(
                        BluetoothGattCharacteristic(
                            UUID_DEVICE_WIFI,
                            BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                            BluetoothGattCharacteristic.PERMISSION_WRITE or BluetoothGattCharacteristic.PERMISSION_READ
                        )
                    )
                }
            }
    }
}
