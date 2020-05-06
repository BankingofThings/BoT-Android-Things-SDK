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
 * Skeleton of an Android Things activity.
 *
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 *
 * connected to Android.local:5555
 *
 * <pre>{@code
 * val service = PeripheralManagerService()
 * val mLedGpio = service.openGpio("BCM6")
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
 * mLedGpio.value = true
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 * val keyPairBIG = Keys.keyPairFor(SignatureAlgorithm.RS256)
 *
 * ./adb connect Android.local
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
        binding.qrHolder.setImageBitmap(finn.qrBitmap)
    }

    /**
     * Example
     */
    private fun initFinn() {
        finn = Finn(
            this,
            "1909d76c-71c0-48b4-9b70-8d94aee8fca8",
            "32BFB7D5-ECF5-48E7-9DBA-BEFD56C221B8",
            "<Host name>",
            "<Device name>",
            "blename",
            "29-4-2020",
            false,
            false,
            "",
            false
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
