package io.bankingofthings.iot

import android.app.Activity
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.view.KeyEvent
import com.google.android.things.contrib.driver.button.Button
import com.google.android.things.contrib.driver.button.ButtonInputDriver
import com.google.android.things.contrib.driver.pwmservo.Servo
import com.google.android.things.pio.PeripheralManager
import io.bankingofthings.iot.callback.FinnStartCallback
import io.bankingofthings.iot.databinding.ActivityMainBinding
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import retrofit2.adapter.rxjava2.HttpException
import java.io.IOException
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
    private var servo: Servo? = null
    private var startEngineDisposable: Disposable? = null
    var goingUp = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)


        startFinn()
    }

    private fun setupEngine() {
        try {
            servo = Servo("PWM0", 50.0)
                .apply {
                    setEnabled(true)
                    angle = 90.0
                }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        ButtonInputDriver("BCM21", Button.LogicState.PRESSED_WHEN_LOW, KeyEvent.KEYCODE_SPACE)
            .apply { register() }
    }

    /**
     * Example
     */
    private fun startFinn() {
        finn = Finn.instance

        finn.start(object : FinnStartCallback {

            /**
             * When user has paired the device with QR, Bluetooth or NFC
             */
            override fun onDevicePaired() {
                setupEngine()
                startEngine()
            }
        })

        binding.qrHolder.setImageBitmap(finn.qrBitmap)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        startEngine()
        return super.onKeyUp(keyCode, event)
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        System.out.println("MainActivity:onKeyDown")

        if (startEngineDisposable?.isDisposed == false) {
            startEngineDisposable?.dispose()
            System.out.println("MainActivity:onKeyDown ${servo?.angle}")

            finn.triggerAction("248DF988-B811-418B-83BF-F55F5B46EEAB", "my license plate")
                .subscribe(
                    {
                        System.out.println("MainActivity:onKeyDown triggered")
                    }
                    , {
                        it.printStackTrace()

                        when (it::class) {
                            HttpException::class -> {
                                if ((it as HttpException).code() == 400) {
                                    System.out.println("MainActivity:onKeyDown trigger not active")
                                }
                            }
                        }

                    }
                )
        }

        return super.onKeyDown(keyCode, event)
    }

    private fun startEngine() {
        startEngineDisposable?.dispose()

        startEngineDisposable = Observable.interval(10, TimeUnit.MILLISECONDS)
            .map {
                if (goingUp) {
                    servo?.angle = servo?.angle!! + 1

                    if (servo?.angle!! >= 180.0) {
                        goingUp = false
                    }
                } else {
                    servo?.angle = servo?.angle!! - 1

                    if (servo?.angle!! <= 0.0) {
                        goingUp = true
                    }
                }
            }
            .subscribe({}, { it.printStackTrace() })
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
