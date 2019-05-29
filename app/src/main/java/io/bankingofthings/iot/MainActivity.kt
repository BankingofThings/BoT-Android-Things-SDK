package io.bankingofthings.iot

import android.annotation.SuppressLint
import android.app.Activity
import android.databinding.DataBindingUtil
import android.os.Bundle
import io.bankingofthings.iot.databinding.ActivityMainBinding
import io.bankingofthings.iot.error.ActionFrequencyNotFoundError
import io.bankingofthings.iot.error.ActionFrequencyTimeNotPassedError
import io.bankingofthings.iot.error.ActionTriggerFailedError
import io.bankingofthings.iot.model.domain.ActionModel
import io.reactivex.Observable
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
class MainActivity : Activity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var finn: Finn

    private val disposables: CompositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        startFinn()
    }

    /**
     * Example
     */
    private fun startFinn() {
        finn = Finn(
            this,
            "ff5e24b8-8082-4df6-9bf6-3476580d1cfc",
            "Finn - BoT",
            "Finn Things Device",
            "Things",
            "19-02-2019",
            true,
            true,
            "my unique ID",
            false
        )

        val aid = ""

        // Observable pattern example
//        finn.start()
//            .andThen(finn.getActions())
//            .map { it.forEach { System.out.println("MainActivity:startFinn $it") } }
//            .toCompletable()
//            .andThen(finn.triggerAction("43260E5C-C0A4-452C-8D03-38420AA9244C"))
//            .subscribe(
//                {
//                    System.out.println("MainActivity:startFinn action triggered")
//                },
//                {
//                    it.printStackTrace()
//                }
//            )
//            .apply { disposables.add(this) }

        // Callback pattern example
        finn.start(object:Finn.StartCallback {
            override fun onDevicePaired() {

                finn.getActions(object:Finn.GetActionsCallback{
                    override fun onGetActionsResult(actionList: List<ActionModel>) {

                        finn.triggerAction("43260E5C-C0A4-452C-8D03-38420AA9244C", null, object:Finn.TriggerActionCallback{
                            override fun onTriggerActionComplete() {
                                System.out.println("MainActivity:onTriggerActionComplete action triggered")
                            }

                            override fun onError(e: Throwable) {
                                e.printStackTrace()
                                System.out.println("MainActivity:onError action trigger failed")
                            }
                        })
                    }

                    override fun onError(e: Throwable) {
                        e.printStackTrace()
                    }
                })
            }
        })

        binding.qrHolder.setImageBitmap(finn.qrBitmap)
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
