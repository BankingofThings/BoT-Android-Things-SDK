package io.bankingofthings.iot.model.domain

/**
 * Contains the data which can be send to clients
 */
data class DeviceModel (
    val makerID:String,
    val deviceID:String,
    val publicKey:String,
    val name:String,
    val multipair: Int,
    val aid: String)
