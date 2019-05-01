package io.bankingofthings.iot.network.pojo

data class ResponseStatus(val status: String) {
    fun isOK(): Boolean = status == "OK"
}
