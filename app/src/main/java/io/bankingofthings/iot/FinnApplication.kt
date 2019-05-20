package io.bankingofthings.iot

import android.app.Application

/**
 * Created by Ercan Bozoglu on 13/02/2019
 * Copyright @ 2018 BankingOfThings.io. All Right reserved.
 */
class FinnApplication : Application() {

    lateinit var finn: Finn

    override fun onCreate() {
        super.onCreate()

        finn = Finn(this)
    }

    override fun onTerminate() {
        super.onTerminate()

        finn.destroy()
    }
}
