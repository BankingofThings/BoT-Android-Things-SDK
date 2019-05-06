package io.bankingofthings.iot.bluetooth

import android.bluetooth.*
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import android.provider.Settings
import com.google.gson.Gson
import io.bankingofthings.iot.BuildConfig
import io.bankingofthings.iot.model.domain.BotDeviceModel
import io.bankingofthings.iot.model.domain.DeviceModel
import io.bankingofthings.iot.model.domain.NetworkModel
import io.reactivex.Observable
import java.util.concurrent.TimeUnit

class BluetoothManager(
    private val context: Context,
    private val nativeBluetoothManager: BluetoothManager,
    private val deviceModel: DeviceModel,
    private val botDeviceModel: BotDeviceModel,
    private val networkModel: NetworkModel
) {
    private val UUID = java.util.UUID.fromString("729BE9C4-3C61-4EFB-884F-B310B6FFFFD1")
    private val UUID_DEVICE = java.util.UUID.fromString("CAD1B513-2DA4-4609-9908-234C6D1B2A9C")
    private val UUID_DEVICE_INFO = java.util.UUID.fromString("CD1B3A04-FA33-41AA-A25B-8BEB2D3BEF4E")
    private val UUID_DEVICE_NETWORK = java.util.UUID.fromString("C42639DC-270D-4690-A8B3-6BA661C6C899")
    private val UUID_DEVICE_WIFI = java.util.UUID.fromString("32BEAA1B-D20B-47AC-9385-B243B8071DE4")
    private var gattServer: BluetoothGattServer? = null

    private val advertisingCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)

            System.out.println("BluetoothManager:onStartSuccess $settingsInEffect")
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)

            when (errorCode) {
                ADVERTISE_FAILED_DATA_TOO_LARGE -> System.out.println("BluetoothManager:onStartFailure data too large")
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> System.out.println("BluetoothManager:onStartFailure too many advertisers")
                ADVERTISE_FAILED_ALREADY_STARTED -> System.out.println("BluetoothManager:onStartFailure already started")
                ADVERTISE_FAILED_INTERNAL_ERROR -> System.out.println("BluetoothManager:onStartFailure internal error")
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> System.out.println("BluetoothManager:onStartFailure feature unsupported")
            }
        }
    }

    /**
     * Initialize and start the bluetooth device
     */
    fun start() {
        System.out.println("BluetoothManager:start")

        Observable.interval(5, TimeUnit.SECONDS)
            .map {
                System.out.println("BluetoothManager:start ${nativeBluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER)}")
            }
            .subscribe({}, { it.printStackTrace() }, {})


        createGattService()

        nativeBluetoothManager.adapter.setName(BuildConfig.DEVICE_NAME)

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

    fun destroy() {
        System.out.println("BluetoothManager:destroy")
        nativeBluetoothManager.adapter.bluetoothLeAdvertiser.stopAdvertising(advertisingCallback)
        gattServer?.close()
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
                    System.out.println("BluetoothManager:onConnectionStateChange $device")

                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        System.out.println("BluetoothManager:onConnectionStateChange status success")
                    } else {
                        System.out.println("BluetoothManager:onConnectionStateChange status failed")
                    }

                    when (newState) {
                        BluetoothProfile.STATE_CONNECTED -> System.out.println("BluetoothManager:onConnectionStateChange connected")
                        BluetoothProfile.STATE_DISCONNECTED -> System.out.println("BluetoothManager:onConnectionStateChange disconnected")
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
                    System.out.println("BluetoothManager:onDescriptorReadRequest device?.name = ${device?.name}")

                    when (characteristic?.uuid) {
                        UUID_DEVICE -> {
                            val json = Gson().toJson(deviceModel)
                            System.out.println("BluetoothManager:onCharacteristicReadRequest $requestId $offset device json = ${json}")
                            gattServer?.sendResponse(
                                device,
                                requestId,
                                BluetoothGatt.GATT_SUCCESS,
                                offset,
                                takeOffset(json, offset)
                            )
                        }
                        UUID_DEVICE_INFO -> {
                            val json = Gson().toJson(botDeviceModel)
                            System.out.println("BluetoothManager:onCharacteristicReadRequest bot device json = ${json}")
                            gattServer?.sendResponse(
                                device,
                                requestId,
                                BluetoothGatt.GATT_SUCCESS,
                                offset,
                                takeOffset(json, offset)
                            )
                        }
                        UUID_DEVICE_NETWORK -> {
                            val json = Gson().toJson(networkModel)
                            System.out.println("BluetoothManager:onCharacteristicReadRequest network json = ${json}")
                            gattServer?.sendResponse(
                                device,
                                requestId,
                                BluetoothGatt.GATT_SUCCESS,
                                offset,
                                takeOffset(json, offset)
                            )
                        }
                        UUID_DEVICE_WIFI -> {
                            gattServer?.sendResponse(
                                device,
                                requestId,
                                BluetoothGatt.GATT_SUCCESS,
                                offset,
                                byteArrayOf()
                            )
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
        return json.toByteArray().let { it.copyOfRange(offset, it.size) }
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

                // Do not add this characteristic, if your device doesn't have a wifi module
                addCharacteristic(
                    BluetoothGattCharacteristic(
                        UUID_DEVICE_WIFI,
                        BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                        BluetoothGattCharacteristic.PERMISSION_READ
                    )
                )
            }
    }
}
