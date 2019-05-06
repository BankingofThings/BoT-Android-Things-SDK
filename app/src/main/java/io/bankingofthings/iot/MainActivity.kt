package io.bankingofthings.iot

import android.app.Activity
import android.databinding.DataBindingUtil
import android.os.Bundle
import io.bankingofthings.iot.callback.FinnStartCallback
import io.bankingofthings.iot.databinding.ActivityMainBinding


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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        startFinn()
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
                System.out.println("MainActivity:onDevicePaired device ready to trigger actions")
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
