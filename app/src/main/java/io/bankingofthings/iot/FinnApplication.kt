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

        finn = Finn(
            this,
            "ff5e24b8-8082-4df6-9bf6-3476580d1cfc",
            "Finn - BoT",
            "Finn Things Device",
            "Things",
            "19-02-2019",
            true,
            false,
            "my unique ID")
    }

    override fun onTerminate() {
        super.onTerminate()

        finn.destroy()
    }
}
