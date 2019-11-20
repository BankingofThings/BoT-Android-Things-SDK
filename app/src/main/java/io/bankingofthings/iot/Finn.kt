package io.bankingofthings.iot

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.WIFI_SERVICE
import android.graphics.Bitmap
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import io.bankingofthings.iot.bluetooth.BluetoothManager
import io.bankingofthings.iot.error.*
import io.bankingofthings.iot.interactors.*
import io.bankingofthings.iot.manager.NetworkManager
import io.bankingofthings.iot.model.domain.ActionModel
import io.bankingofthings.iot.network.ApiHelper
import io.bankingofthings.iot.network.TLSManager
import io.bankingofthings.iot.network.pojo.BotDeviceSsidPojo
import io.bankingofthings.iot.repo.DeviceRepo
import io.bankingofthings.iot.repo.IdRepo
import io.bankingofthings.iot.repo.KeyRepo
import io.bankingofthings.iot.repo.QrRepo
import io.bankingofthings.iot.storage.SpHelper
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import retrofit2.adapter.rxjava2.HttpException
import java.lang.Exception
import java.util.concurrent.TimeUnit

/**
 * Finn is a singleton and can be started, stopped and destroyed.
 * Start: check pair status, if paired activate and stop bluetooth advertising if is not a multi pair device. Otherwise keep advertising.
 * If not paired try every 10 seconds again.
 *
 * The initialization can throw an exception:
 * MakerID is invalid. MakerIDInvalidError
 * Host name empty. HostNameEmptyError
 * Bluetooth discover name is too long. BlueToothNameTooLongError
 * Alternative Identifier name is empty with multi pair. AlternativeIdentifierDisplayNameEmptyError
 *
 * To start Finn, just call start() and after pairing with companion app, you can trigger actions.

 * @param makerID Portal MakerID (36 characters)
 * @param hostName Manufacturer/Company name
 * @param deviceName Displayed when bluetooth device is discovered (max 8 characters)
 * @param buildDate Date to be stored at CORE (DD-MM-YYYY)
 * @param hasWifi Determines if app user can change the SSID/Password on the device
 * @param multiPair Determines if device can be added by multiple app users, with each unique aid
 * @param aid Alternative Identifier Display name, which will be shown to the app user
 * @param newInstall Is by default false, it will then reuse existing id's and keys. When true, all data will be cleared and new deviceID and keys will be regenerated.
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
    private val aid: String? = null,
    private val newInstall: Boolean = false
) {
    /**
     * Defines if device is ready (Paired with an user)
     */
    interface StartCallback {
        fun onDevicePaired()
    }

    interface GetActionsCallback {
        fun onGetActionsResult(actionList: List<ActionModel>)
        fun onError(e: Throwable)
    }

    interface TriggerActionCallback {
        fun onTriggerActionComplete()
        fun onError(e: Throwable)
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        var instance: Finn? = null
            get() {
                if (field == null) {
                    System.out.println("Finn is already been destroyed")
                }
                return field
            }
    }

    private val disposables: CompositeDisposable = CompositeDisposable()

    private val spHelper: SpHelper
    private val apiHelper: ApiHelper

    private val keyRepo: KeyRepo
    private val idRepo: IdRepo
    private val qrRepo: QrRepo
    private val deviceRepo: DeviceRepo
    private val networkManager = NetworkManager(context)

    private val triggerActionWorker: TriggerActionWorker
    private val checkDevicePairedWorker: CheckDevicePairedWorker
    private val getActionsWorker: GetActionsWorker
    private val activateDeviceWorker: ActivateDeviceWorker
    private val checkActionTriggerableWorker: CheckActionTriggerableWorker
    private val storeActionFrequencyWorker: StoreActionFrequencyWorker
    private val storeActionTriggeredWorker: StoreActionTriggeredWorker
    private val storeOfflineTriggeredActionWorker: StoreOfflineTriggeredActionWorker
    private val checkAndExecuteOfflineActionsWorker: CheckAndExecuteOfflineActionsWorker

    private val bluetoothManager: BluetoothManager

    var isPaired:Boolean = false

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
        spHelper = SpHelper(context.getSharedPreferences("bot", Context.MODE_PRIVATE))

        // Remove makerID, deviceID and keys
        if (newInstall) {
            spHelper.removeAllData()
        }

        checkParamsValid()

        instance = this

        apiHelper = ApiHelper(TLSManager()
            .apply { setCertificateInputStream(context.resources.openRawResource(R.raw.botdomain)) })

        keyRepo = KeyRepo(spHelper)
        idRepo = IdRepo(spHelper, makerID)
        deviceRepo = DeviceRepo(
            context,
            keyRepo,
            idRepo,
            hostName,
            deviceName,
            buildDate,
            hasWifi,
            multiPair,
            aid
        )

        qrRepo = QrRepo(spHelper, deviceRepo)

        checkDevicePairedWorker = CheckDevicePairedWorker(apiHelper, keyRepo, idRepo)
        activateDeviceWorker = ActivateDeviceWorker(apiHelper, keyRepo, idRepo)
        getActionsWorker = GetActionsWorker(apiHelper, keyRepo, idRepo)
        storeActionFrequencyWorker = StoreActionFrequencyWorker(spHelper)
        checkActionTriggerableWorker = CheckActionTriggerableWorker(spHelper)
        triggerActionWorker = TriggerActionWorker(apiHelper, keyRepo, idRepo)
        storeActionTriggeredWorker = StoreActionTriggeredWorker(spHelper)
        storeOfflineTriggeredActionWorker = StoreOfflineTriggeredActionWorker(spHelper)
        checkAndExecuteOfflineActionsWorker = CheckAndExecuteOfflineActionsWorker(spHelper, triggerActionWorker)

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
     * Cleanup: usefull when testing multiple makerID's.
     * Kills running api calls
     * Stops bluetooth
     * Removes all data; deviceID, keys and other locally stored data.
     */
    fun destroy() {
        stop()

        isPaired = false

        spHelper.removeAllData()

        instance = null
    }

    /**
     * @see start():Completable
     */
    fun start(startCallback: StartCallback) {
        start()
            .subscribe(startCallback::onDevicePaired) { it.printStackTrace() }
            .apply { disposables.add(this) }
    }

    /**
     * Pair device
     * if success > get actions and store, and then activate device
     * else > start bluetooth advertising and retry after 10 seconds
     *
     */
    fun start(): Completable {
        networkManager.start()

        return checkDevicePairedWorker.execute()
            .flatMapCompletable { isPaired ->
                if (isPaired) {
                    getActionsWorker.execute()
                        .flatMapCompletable(storeActionFrequencyWorker::execute)
                        .andThen(activateDeviceWorker.execute())
                } else {
                    throw DevicePairingFailed()
                }
            }
            .doOnError {
                when (it::class) {
                    DevicePairingFailed::class -> {
                        System.out.println("Finn:start device NOT paired yet")
                        if (!bluetoothManager.started) {
                            bluetoothManager.start()
                        }
                    }
                    else -> it.printStackTrace()
                }
            }
            .doOnComplete {
                if (multiPair) {
                    if (!bluetoothManager.started) {
                        bluetoothManager.start()
                    }
                } else {
                    bluetoothManager.kill()
                }

                isPaired = true
            }
            .retryWhen { it.delay(10, TimeUnit.SECONDS) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * Stops network processes
     * Stops rx streams (threads)
     * Stops bluetooth broadcasting
     * Clears bitmap cache
     */
    fun stop() {
        networkManager.stop()
        disposables.dispose()
        bluetoothManager.kill()
        qrRepo.destroyBitmap()
    }

    /**
     * Returns all actions.
     */
    fun getActions(): Single<List<ActionModel>> {
        return getActionsWorker.execute()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * Returns all actions.
     */
    fun getActions(callback: Finn.GetActionsCallback) {
        getActionsWorker.execute()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(callback::onGetActionsResult, callback::onError)
            .apply { disposables.add(this) }
    }

    /**
     * Trigger action.
     */
    @Throws(
        ActionFrequencyNotFoundError::class,
        ActionFrequencyTimeNotPassedError::class,
        ActionTriggerFailedError::class,
        ActionTriggerFailedAlternativeIdRequired::class,
        ActionNotActivatedError::class
    )
    fun triggerAction(actionID: String, alternativeID: String? = null): Completable {
        if (multiPair && alternativeID == null) {
            throw ActionTriggerFailedAlternativeIdRequired()
        } else {

            return if (networkManager.hasNetworkConnection()) {
                createCompositeSendAction(actionID, alternativeID)
                    .onErrorResumeNext {
                        when {
                            it is HttpException && it.code() == 400 -> Completable.error(ActionNotActivatedError())
                            else -> Completable.error(it)
                        }
                    }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())

            } else {
                createCompositeTriggerOfflineAction(actionID, alternativeID)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
            }
        }
    }

    /**
     * Trigger action, for callbacks
     */
    @Throws(ActionTriggerFailedAlternativeIdRequired::class)
    fun triggerAction(
        actionID: String,
        alternativeID: String? = null,
        callback: Finn.TriggerActionCallback
    ) {
        if (multiPair && alternativeID == null) {
            throw ActionTriggerFailedAlternativeIdRequired()
        } else {
            if (networkManager.hasNetworkConnection()) {
                createCompositeSendAction(actionID, alternativeID)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(callback::onTriggerActionComplete, callback::onError)
                    .apply { disposables.add(this) }

            } else {
                createCompositeTriggerOfflineAction(actionID, alternativeID)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(callback::onTriggerActionComplete, callback::onError)
                    .apply { disposables.add(this) }
            }
        }
    }

    private fun createCompositeSendAction(actionID: String, alternativeID: String?): Completable {
        return checkActionTriggerableWorker.execute(actionID)
            .andThen(triggerActionWorker.execute(actionID, idRepo.generateID(), alternativeID))
            .andThen(storeActionTriggeredWorker.execute(actionID))
            .andThen(checkAndExecuteOfflineActionsWorker.execute())
    }

    private fun createCompositeTriggerOfflineAction(actionID: String, alternativeID: String?): Completable {
        return checkActionTriggerableWorker.execute(actionID)
            .andThen(storeOfflineTriggeredActionWorker.put(actionID, idRepo.generateID(), alternativeID))
            .andThen(storeActionTriggeredWorker.execute(actionID))
    }

    fun getQrBitmap(): Bitmap {
        return qrRepo.qrBitmap
    }
}
