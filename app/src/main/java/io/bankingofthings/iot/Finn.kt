package io.bankingofthings.iot

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.WIFI_SERVICE
import android.graphics.Bitmap
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import com.google.gson.Gson
import io.bankingofthings.iot.bluetooth.BluetoothManager
import io.bankingofthings.iot.callback.FinnStartCallback
import io.bankingofthings.iot.error.*
import io.bankingofthings.iot.interactors.*
import io.bankingofthings.iot.model.domain.ActionModel
import io.bankingofthings.iot.network.ApiHelper
import io.bankingofthings.iot.network.TLSManager
import io.bankingofthings.iot.network.pojo.BotDeviceSsidPojo
import io.bankingofthings.iot.repo.DeviceRepo
import io.bankingofthings.iot.repo.IdRepo
import io.bankingofthings.iot.repo.KeyRepo
import io.bankingofthings.iot.storage.SpHelper
import io.bankingofthings.iot.utils.QRUtil
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit


/**
 * Finn is created when application is started and can be used as a Singleton
 *
 * Before you start, please configure app.gradle with your makerID
 *
 * To start Finn, just call start(callback) and after pairing with companion app, you can trigger actions.

 * @param makerID Portal MakerID (36 characters)
 * @param hostName Manufacturer/Company name
 * @param deviceName Displayed when bluetooth device is discovered (max 8 characters)
 * @param buildDate Date to be stored at CORE (DD-MM-YYYY)
 * @param hasWifi Determines if app user can change the SSID/Password on the device
 * @param multiPair Determines if device can be added by multiple app users, with each unique aid
 * @param aid Alternative Identifier Display name, which will be shown to the app user
 */
class Finn(
    private val context: Context,
    private val makerID: String,
    private val hostName: String,
    private val deviceName: String,
    private val blueToothName: String,
    private val buildDate: String,
    private val hasWifi: Boolean,
    private val multiPair: Boolean,
    private val aid: String? = null
) {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var instance: Finn
    }

    private val disposables = arrayListOf<Disposable>()

    private val spHelper: SpHelper
    private val apiHelper: ApiHelper

    private val keyRepo: KeyRepo
    private val idRepo: IdRepo
    private val deviceRepo: DeviceRepo

    val qrBitmap: Bitmap

    private val triggerActionWorker: TriggerActionWorker
    private val checkDevicePairedWorker: CheckDevicePairedWorker
    private val getActionsWorker: GetActionsWorker
    private val activateDeviceWorker: ActivateDeviceWorker
    private val checkActionTriggerableWorker: CheckActionTriggerableWorker
    private val storeActionsWorker: StoreActionsWorker
    private val storeActionTriggeredWorker: StoreActionTriggeredWorker

    private val bluetoothManager: BluetoothManager

    private lateinit var callback: FinnStartCallback

    @Throws(
        MakerIDInvalidError::class,
        HostNameEmptyError::class,
        BlueToothNameTooLongError::class,
        AlternativeIdentifierDisplayNameEmptyError::class
    )
    private fun checkParamsValid() {
        if (makerID.length != 36) {
            throw MakerIDInvalidError()
        }

        if (hostName.isBlank()) {
            throw HostNameEmptyError()
        }

        if (blueToothName.isBlank() || blueToothName.length > 8) {
            throw BlueToothNameTooLongError()
        }

        if (multiPair && aid?.isEmpty() == true) {
            throw AlternativeIdentifierDisplayNameEmptyError()
        }
    }

    init {
        checkParamsValid()

        instance = this

        spHelper = SpHelper(context.getSharedPreferences("bot", Context.MODE_PRIVATE))

        // Uncomment when new DeviceID and RSA keys is needed. This also generates new data every run.
        spHelper.removeAllData()

        apiHelper = ApiHelper(TLSManager()
            .apply { setCertificateInputStream(context.resources.openRawResource(R.raw.botdomain)) })

        keyRepo = KeyRepo(spHelper)
        idRepo = IdRepo(spHelper, makerID)
        deviceRepo = DeviceRepo(context, keyRepo, idRepo, hostName, deviceName, buildDate, hasWifi, multiPair, aid)

        qrBitmap = QRUtil.encodeAsBitmap(Gson().toJson(deviceRepo.deviceModel))

        triggerActionWorker = TriggerActionWorker(apiHelper, keyRepo, idRepo)
        checkDevicePairedWorker = CheckDevicePairedWorker(apiHelper, keyRepo, idRepo)
        getActionsWorker = GetActionsWorker(apiHelper, keyRepo, idRepo)
        activateDeviceWorker = ActivateDeviceWorker(apiHelper, keyRepo, idRepo)
        checkActionTriggerableWorker = CheckActionTriggerableWorker(spHelper)
        storeActionsWorker = StoreActionsWorker(spHelper)
        storeActionTriggeredWorker = StoreActionTriggeredWorker(spHelper)

        bluetoothManager = BluetoothManager(
            context,
            context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager,
            deviceRepo.deviceModel,
            deviceRepo.botDeviceModel,
            deviceRepo.networkModel,
            blueToothName,
            hasWifi,
            object : BluetoothManager.Callback {
                override fun onWifiCredentialsChanged(pojo: BotDeviceSsidPojo) {
                    changeWifiCredentials(pojo)
                }
            }
        )
    }

    /**
     * Adds network and connect to it
     */
    private fun changeWifiCredentials(pojo: BotDeviceSsidPojo) {
        (context.getSystemService(WIFI_SERVICE) as WifiManager).apply {
            val netID = addNetwork(
                WifiConfiguration().apply {
                    SSID = String.format("\"%s\"", pojo.ssid)
                    preSharedKey = String.format("\"%s\"", pojo.password)
                }
            )

            if (netID != -1) {
                disconnect()
                enableNetwork(netID, true)
                reconnect()
            } else {
                System.out.println("Finn:changeWifiCredentials $netID network is invalid")
            }
        }
    }

    /**
     * Cleanup
     */
    fun destroy() {
        disposables.map { it.dispose() }
        bluetoothManager.kill()
    }

    /**
     * Start
     */
    fun start(callback: FinnStartCallback) {
        this.callback = callback

        bluetoothManager.start()

        checkDevicePaired()
    }

    /**
     * When device is already paired and device is restarted. If not paired, start interval check.
     */
    private fun checkDevicePaired() {
        var isPairedAndActivated = false

        Observable.interval(10, TimeUnit.SECONDS)
            .flatMapSingle {
                checkDevicePairedWorker.execute()
                    .flatMap {
                        System.out.println("Finn:checkDevicePaired it = ${it}")
                        // Paired ? Activated else start interval pair check
                        if (it) {
                            getActionsWorker.execute()
                                .flatMapCompletable(storeActionsWorker::execute)
                                .andThen(activateDeviceWorker.execute())
                                .toSingle { it }
                        } else {
                            Single.just(it)
                        }
                    }
            }
            .takeWhile { !isPairedAndActivated }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { isPairedAndActivated = it },
                {
                    // Can happen when changing network SSID
                    it.printStackTrace()

                    when (it) {
                        UnknownHostException::class -> checkDevicePaired()
                        DeviceActivationFailedError::class -> checkDevicePaired()
                        else -> checkDevicePaired()
                    }
                },
                { onDevicePaired() }
            )
            .apply { disposables.add(this) }
    }

    /**
     * When devices is by client paired
     * If device can only be paired with 1 user (NOT multi pair), then stop advertising
     */
    private fun onDevicePaired() {
        if (!multiPair) {
            bluetoothManager.kill()
        }

        callback.onDevicePaired()
    }

    /**
     * Returns all actions stored at CORE.
     */
    fun getActions(): Single<List<ActionModel>> {
        return getActionsWorker.execute()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * Trigger action at CORE
     */
    @Throws(
        ActionFrequencyNotFoundError::class,
        ActionFrequencyTimeNotPassedError::class,
        ActionTriggerFailedError::class
    )
    fun triggerAction(actionID: String, alternativeID: String? = null): Completable {
        return checkActionTriggerableWorker.execute(actionID)
            .andThen(triggerActionWorker.execute(actionID, idRepo.generateID(), alternativeID))
            .andThen(storeActionTriggeredWorker.execute(actionID))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }
}
