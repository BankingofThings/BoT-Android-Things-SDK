package io.bankingofthings.iot.network.pojo

data class ActivateDeviceParamPojo(val bot: Bot) {
    data class Bot(val deviceID: String)
}
