package io.bankingofthings.iot.model.domain

data class BotDeviceModel(
    val platform: String,
    val release: String,
    val type: String,
    val arch: String,
    val cpus: String,
    val hostname: String,
    val endianness: String,
    val totalMemory: String,
    val network: String? = null,
    val ip: String? = null
)
