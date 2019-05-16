package io.bankingofthings.iot

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import com.google.gson.Gson
import io.bankingofthings.iot.bluetooth.BluetoothManager
import io.bankingofthings.iot.callback.FinnStartCallback
import io.bankingofthings.iot.interactors.ActivateDeviceWorker
import io.bankingofthings.iot.interactors.CheckDevicePairedWorker
import io.bankingofthings.iot.interactors.GetActionsWorker
import io.bankingofthings.iot.interactors.TriggerActionWorker
import io.bankingofthings.iot.model.domain.ActionModel
import io.bankingofthings.iot.network.ApiHelper
import io.bankingofthings.iot.network.SSLManager
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
import java.util.concurrent.TimeUnit
import android.content.Context.WIFI_SERVICE
import android.net.wifi.WifiManager
import android.net.wifi.WifiConfiguration
import java.net.UnknownHostException


/**
 * Finn is created when application is started and can be used as a Singleton
 *
 * Before you start, please configure app.gradle with your makerID
 *
 * To start Finn, just call start(callback) and after pairing with companion app, you can trigger actions.
 */
class Finn(private val context: Context) {
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

    private val bluetoothManager: BluetoothManager

    private lateinit var callback: FinnStartCallback

    init {
        instance = this

        spHelper = SpHelper(context.getSharedPreferences("bot", Context.MODE_PRIVATE))
        apiHelper = ApiHelper(SSLManager()
            .apply { setCertificateInputStream(context.resources.openRawResource(R.raw.botdomain)) })

        keyRepo = KeyRepo(spHelper)
        idRepo = IdRepo(spHelper)
        deviceRepo = DeviceRepo(context, keyRepo, idRepo)

        qrBitmap = QRUtil.encodeAsBitmap(Gson().toJson(deviceRepo.deviceModel))

        triggerActionWorker = TriggerActionWorker(apiHelper, keyRepo, idRepo)
        checkDevicePairedWorker = CheckDevicePairedWorker(apiHelper, keyRepo, idRepo)
        getActionsWorker = GetActionsWorker(apiHelper, keyRepo, idRepo)
        activateDeviceWorker = ActivateDeviceWorker(apiHelper, keyRepo, idRepo)

        bluetoothManager = BluetoothManager(
            context,
            context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager,
            deviceRepo.deviceModel,
            deviceRepo.botDeviceModel,
            deviceRepo.networkModel,
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

        checkDeviceIsAlreadyPaired()
    }

    /**
     * When device is already paired and device is restarted. If not paired, start interval check.
     */
    private fun checkDeviceIsAlreadyPaired() {
        checkDevicePairedWorker.execute()
            .flatMap {
                // Paired ? Activated else start interval pair check
                if (it) {
                    activateDeviceWorker.execute()
                } else {
                    Single.just(it)
                }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    if (!it) {
                        startIsPairedCheck()
                    } else {
                        onDevicePaired()
                    }
                },
                {
                    // Can happen when changing network SSID
                    it.printStackTrace()
                    when (it) {
                        UnknownHostException::class -> checkDeviceIsAlreadyPaired()
                        else -> checkDeviceIsAlreadyPaired()
                    }
                }
            )
            .apply { disposables.add(this) }
    }

    /**
     * Check with intervals, if device is paired. Stops when an user pairs the device.
     */
    private fun startIsPairedCheck() {
        var disposable: Disposable? = null

        Observable.interval(10, TimeUnit.SECONDS)
            .flatMapSingle {
                checkDevicePairedWorker.execute()
                    .flatMap {
                        // Paired ? Activated else start interval pair check
                        if (it) {
                            activateDeviceWorker.execute()
                        } else {
                            Single.just(it)
                        }
                    }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    if (it) {
                        disposable?.dispose()
                        onDevicePaired()
                    }
                },
                {
                    it.printStackTrace()

                    startIsPairedCheck()
                }
            )
            .apply { disposable = this }
    }

    /**
     * When devices is by client paired
     * If device can only be paired with 1 user (NOT multipair), then stop advertising
     */
    private fun onDevicePaired() {
        if (!BuildConfig.MULTI_PAIR) {
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
    fun triggerAction(actionID: String, alternativeID: String? = null): Completable {
        return triggerActionWorker.execute(actionID, idRepo.generateID(), alternativeID)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }
}
