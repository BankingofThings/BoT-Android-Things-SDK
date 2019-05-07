package io.bankingofthings.iot.network.pojo

import com.google.gson.annotations.SerializedName

data class BotDeviceSsidPojo(
    @SerializedName("SSID")
    val ssid: String,
    @SerializedName("PWD")
    val password: String
)
