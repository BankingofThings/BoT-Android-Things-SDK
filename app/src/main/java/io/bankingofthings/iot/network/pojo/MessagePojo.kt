package io.bankingofthings.iot.network.pojo

import com.google.gson.Gson

data class MessagePojo(
        val deviceID: String,
        val payload: String,
        val messageID: String,
        val event: String?,
        val delivered: Int
                      ) {
    var payloadModel: PayloadModel? = null

    data class PayloadModel(
            val actionID: String?,
            val customerID: String?,
            val deviceID: String?
                           )
}
