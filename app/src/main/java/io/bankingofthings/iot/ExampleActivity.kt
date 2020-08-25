package io.bankingofthings.iot

import android.app.Activity
import android.databinding.DataBindingUtil
import android.os.Bundle
import io.bankingofthings.iot.databinding.ActivityMainBinding
import io.bankingofthings.iot.model.domain.ActionModel
import io.bankingofthings.iot.model.domain.ProductType
import io.reactivex.Completable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import java.util.concurrent.TimeUnit


/**
 * FINN example class for simple implementation
 *
 * ./adb connect Android.local:5555
 */
class ExampleActivity : Activity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var finn: Finn

    // Used with observables
    private val disposables: CompositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        initFinn()

        showQRbitmapOnDisplay()

        startFinnObservablePattern()
    }

    private fun showQRbitmapOnDisplay() {
        binding.qrHolder.setImageBitmap(finn.getQrBitmap())
    }

    /**
     * Example
     */
    private fun initFinn() {
        finn = Finn(
            this,
            null,
            "<ProductID>",
            "<Host name>",
            "",
            "blename",
            "25-8-2020",
            false,
            false,
            "aid",
            false
        )
    }

    private var triggerBotTalkActionDisposable: Disposable? = null

    /**
     * Starts FINN.
     * With observable style of coding
     */
    private fun startFinnObservablePattern() {
        finn.setBotTalkListener(object : Finn.BotTalkListener {
            override fun onActionActivatedByClient(actionID: String, customerID: String) {
                // do some pre trigger actions here
                finn.triggerBotTalkAction(actionID, customerID)
                    .subscribe(
                        {
                            // do some post trigger actions here
                        }, Throwable::printStackTrace
                    )
                    .apply { triggerBotTalkActionDisposable = this }
            }
        })

        finn.start()
            .andThen(finn.getActions())
            .map { it.forEach { System.out.println("ExampleActivity:startFinnObservablePattern action: $it") } }
            .ignoreElement()
            .subscribe(
                {
                    System.out.println("ExampleActivity:startFinnObservablePattern action triggered")
                },
                {
                    it.printStackTrace()
                }
            )
            .apply { disposables.add(this) }
    }

    /**
     * Starts FINN.
     * With callback (traditional) style of coding
     */
    private fun startFinnCallbackPattern() {
        finn.start(object : Finn.StartCallback {
            override fun onDevicePaired() {

                finn.getActions(object : Finn.GetActionsCallback {
                    override fun onGetActionsResult(actionList: List<ActionModel>) {

                        // Log all available actions
                        actionList.forEach { System.out.println("ExampleActivity:startFinnCallbackPattern action: $it") }

                        // Trigger 1 action
                        finn.triggerAction(
                            "<Your action ID>",
                            null,
                            object : Finn.TriggerActionCallback {
                                override fun onTriggerActionComplete() {
                                    System.out.println("ExampleActivity:startFinnCallbackPattern action triggered")
                                }

                                override fun onError(e: Throwable) {
                                    e.printStackTrace()
                                }
                            })
                    }

                    override fun onError(e: Throwable) {
                        e.printStackTrace()
                    }
                })
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        finn.stop()
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }

    }
}
