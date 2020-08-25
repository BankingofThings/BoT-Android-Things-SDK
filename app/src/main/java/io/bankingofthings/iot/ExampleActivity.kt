package io.bankingofthings.iot

import android.app.Activity
import android.databinding.DataBindingUtil
import android.os.Bundle
import io.bankingofthings.iot.databinding.ActivityMainBinding
import io.bankingofthings.iot.model.domain.ActionModel
import io.bankingofthings.iot.model.domain.ProductType
import io.reactivex.Completable
import io.reactivex.disposables.CompositeDisposable
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
            "fe54e5b2-6f26-4eca-b1fe-91e3f2cc77c0",
            "D49B5D33-348B-470F-89A4-265313D166CE",
            "<Host name>",
            "loki-rpi-z",
            "blename",
            "29-4-2020",
            false,
            true,
            "svmld",
            true
        )
    }

    /**
     * Starts FINN.
     * With observable style of coding
     */
    private fun startFinnObservablePattern() {
        finn.start()
            .andThen(finn.getActions())
            .map { it.forEach { System.out.println("ExampleActivity:startFinnObservablePattern action: $it") } }
            .toCompletable()
            .andThen(finn.triggerAction("7AFD572E-3C97-4C56-8D37-BFBA04965913"))
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
