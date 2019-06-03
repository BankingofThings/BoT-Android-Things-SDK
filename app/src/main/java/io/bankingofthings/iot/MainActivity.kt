package io.bankingofthings.iot

import android.app.Activity
import android.databinding.DataBindingUtil
import android.os.Bundle
import io.bankingofthings.iot.databinding.ActivityMainBinding
import io.bankingofthings.iot.model.domain.ActionModel
import io.reactivex.disposables.CompositeDisposable


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
class MainActivity : Activity() {
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
            "e454bc81-fca0-43d4-b047-7311bdee8c57",
            "Finn - BoT",
            "Finn Things Device",
            "Things",
            "19-02-2019",
            true,
            true,
            "my unique ID",
            false
        )

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
                        actionList.forEach { System.out.println("MainActivity:startFinnCallbackPattern action: $it") }

                        // Trigger 1 action
                        finn.triggerAction(
                            "248DF988-B811-418B-83BF-F55F5B46EEAB",
                            null,
                            object : Finn.TriggerActionCallback {
                                override fun onTriggerActionComplete() {
                                    System.out.println("MainActivity:startFinnCallbackPattern action triggered")
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
     * Starts FINN.
     * With observable style of coding
     */
    private fun startFinnObservablePattern() {
        finn.start()
            .andThen(finn.getActions())
            .map { it.forEach { System.out.println("MainActivity:startFinnObservablePattern action: $it") } }
            .toCompletable()
            .andThen(finn.triggerAction("248DF988-B811-418B-83BF-F55F5B46EEAB"))
            .subscribe(
                {
                    System.out.println("MainActivity:startFinnObservablePattern action triggered")
                },
                {
                    it.printStackTrace()
                }
            )
            .apply { disposables.add(this) }
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
