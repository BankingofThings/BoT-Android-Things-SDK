package io.bankingofthings.iot.network

import com.google.gson.GsonBuilder
import io.bankingofthings.iot.BuildConfig
import io.bankingofthings.iot.network.pojo.TokenParamPojo
import io.bankingofthings.iot.network.service.CoreApi
import io.reactivex.Single
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Created by Ercan Bozoglu on 13/02/2019
 * Copyright @ 2018 BankingOfThings.io. All Right reserved.
 */
class ApiHelper(private val tlsManager: TLSManager) {
    private var retrofit: Retrofit
    private var coreApi: CoreApi

    init {
        retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .client(createClient())
            .build()

        coreApi = retrofit.create(CoreApi::class.java)
    }

    /**
     * Create default client
     * Uses tlsManager socketFactory for the security
     * Triggered at every api call
     */
    private fun createClient(): OkHttpClient {
        return OkHttpClient().newBuilder()
            .certificatePinner(tlsManager.getCertificatePinner())
            .hostnameVerifier(tlsManager::verifyHostName)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    this.level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .addInterceptor(object : Interceptor {
                override fun intercept(chain: Interceptor.Chain): Response {
                    return chain.proceed(
                        chain.request()
                            .newBuilder()
                            .addHeader("accept", "application/json; charset=utf-8")
                            .addHeader("content-type", "application/json; charset=utf-8")
                            .addHeader("cache-control", "no-cache")
                            .build()
                    )
                }
            })
            .build()
    }

    /**
     * Returns if device is paired.
     * Create JWT object
     * Get 'bot' claim object as String
     * Parse to json
     * Get status value
     */
    fun checkDeviceIsPaired(makerID: String, deviceID: String): Single<ResponseBody> {
        return coreApi.getPairedStatus(makerID, deviceID)
    }

    fun activateDevice(makerID: String, deviceID: String, token: String): Single<ResponseBody> {
        return coreApi.activateDevice(makerID, deviceID, TokenParamPojo(token))
    }

    fun getActions(makerID: String, deviceID: String): Single<ResponseBody> {
        return coreApi.getActions(makerID, deviceID)
    }

    fun triggerAction(makerID: String, deviceID: String, token: String): Single<ResponseBody> {
        return coreApi.postActions(makerID, deviceID, TokenParamPojo(token))
    }

    fun getMessages(makerID: String, deviceID: String): Single<ResponseBody> {
        return coreApi.getMessages(makerID, deviceID)
    }
}
