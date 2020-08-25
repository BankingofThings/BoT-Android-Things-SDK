package io.bankingofthings.iot.network.service

import io.bankingofthings.iot.network.pojo.*
import io.reactivex.Single
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface CoreApi {
    /**
     * Checks pair status
     */
    @GET("pair")
    fun getPairedStatus(
        @Header("makerID") makerID: String,
        @Header("deviceID") deviceID: String
    ): Single<ResponseBody>

    /**
     * Returns the list of actions, which is create by the maker in portal
     */
    @GET("actions")
    fun getActions(
        @Header("makerID") makerID: String,
        @Header("deviceID") deviceID: String
    ): Single<ResponseBody>

    @POST("actions")
    fun postActions(
        @Header("makerID") makerID: String,
        @Header("deviceID") deviceID: String,
        @Body pojo: TokenParamPojo
    ): Single<ResponseBody>

    /**
     * Fetch list of triggered actions
     * Not working
     */
    @GET("transactions")
    fun getTransactions(
        @Header("makerID") makerID: String,
        @Header("deviceID") deviceID: String
    ): Single<ResponseBody>

    @POST("status")
    fun activateDevice(
        @Header("makerID") makerID: String,
        @Header("deviceID") deviceID: String,
        @Body pojo: TokenParamPojo
    ): Single<ResponseBody>


    /**
     * Returns the list of actions, which is create by the maker in portal
     */
    @GET("messages")
    fun getMessages(
        @Header("makerID") makerID: String,
        @Header("deviceID") deviceID: String
    ): Single<ResponseBody>
}
