package io.bankingofthings.iot

import android.app.Application
import io.bankingofthings.iot.network.ApiHelper
import io.bankingofthings.iot.network.SSLManager

/**
 * Created by Ercan Bozoglu on 13/02/2019
 * Copyright @ 2018 BankingOfThings.io. All Right reserved.
 */
class FinnApplication : Application() {

    lateinit var finn: Finn

    override fun onCreate() {
        super.onCreate()

        System.out.println("FinnApplication:onCreate")

        finn = Finn(this)
    }

    override fun onTerminate() {
        super.onTerminate()

        System.out.println("FinnApplication:onTerminate")

        finn.destroy()
    }
}
