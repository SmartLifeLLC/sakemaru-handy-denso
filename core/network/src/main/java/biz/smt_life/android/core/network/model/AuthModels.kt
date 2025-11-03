package biz.smt_life.android.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Picker(
    val id: Int,
    val code: String,
    val name: String,
    @SerialName("default_warehouse_id") val defaultWarehouseId: Int
)

@Serializable
data class LoginData(
    val token: String,
    val picker: Picker
)
