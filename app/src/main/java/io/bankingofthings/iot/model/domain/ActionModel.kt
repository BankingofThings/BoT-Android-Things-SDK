package io.bankingofthings.iot.model.domain

import com.google.gson.annotations.SerializedName

data class ActionModel(
    val frequency: String? = null,
    @SerializedName("date_created")
    val dateCreated: String? = null,
    val makerID: String? = null,
    val actionName: String? = null,
    val price: Double? = null,
    val actionID: String? = null,
    val prerequisite: String? = null,
    val type: String? = null,
    val info: String? = null,
    val metadata: String? = null
) {
    override fun toString(): String {
        return "$actionID ActionModel $actionName: $info $price $frequency"
    }
}
