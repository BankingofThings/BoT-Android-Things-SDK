package io.bankingofthings.iot.manager

import android.content.Context
import android.net.*

/**
 * Manages total available networks.
 */
class NetworkManager(private val context: Context) {
    private var totalNetworks: Int = 0;

    private val callback = object : ConnectivityManager.NetworkCallback() {
        /**
         * This function is called per network discovery. wlan or eth
         */
        override fun onAvailable(network: Network?) {
            super.onAvailable(network)
            System.out.println("Finn:onAvailable")
            totalNetworks++
        }

        override fun onLost(network: Network?) {
            super.onLost(network)
            System.out.println("Finn:onLost")
            totalNetworks--
        }

        override fun onUnavailable() {
            super.onUnavailable()
            System.out.println("Finn:onUnavailable")
        }

        override fun onLosing(network: Network?, maxMsToLive: Int) {
            super.onLosing(network, maxMsToLive)
            System.out.println("Finn:onLosing")
        }

        override fun onCapabilitiesChanged(
            network: Network?,
            networkCapabilities: NetworkCapabilities?
        ) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            System.out.println("Finn:onCapabilitiesChanged")
        }

        override fun onLinkPropertiesChanged(
            network: Network?,
            linkProperties: LinkProperties?
        ) {
            super.onLinkPropertiesChanged(network, linkProperties)
            System.out.println("Finn:onLinkPropertiesChanged")
        }
    }

    fun start() {
        (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
            .registerNetworkCallback(NetworkRequest.Builder().build(), callback)
    }

    fun stop() {
        (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
            .unregisterNetworkCallback(callback)
    }

    fun hasNetworkConnection() = totalNetworks > 0
}
