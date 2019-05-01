package io.bankingofthings.iot.callback

/**
 * Defines if device is ready (Paired with an user)
 */
interface FinnStartCallback {
    fun onDevicePaired()
}
