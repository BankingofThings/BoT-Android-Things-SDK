package io.bankingofthings.iot.network.pojo

/**
 * Created by Ercan Bozoglu on 25/02/2019
 * Copyright @ 2018 BankingOfThings.io. All Right reserved.
 */
data class TriggerActionAlternativeIDParamPojo(val bot: Bot) {
    data class Bot(val deviceID: String,
                   val actionID: String,
                   val alternativeID:String,
                   val value: String)
}
