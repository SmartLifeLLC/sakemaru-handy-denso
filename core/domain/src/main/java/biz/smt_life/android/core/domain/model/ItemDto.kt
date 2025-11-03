package biz.smt_life.android.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ItemDto(
    val id: String,
    val code: String,
    val name: String,
    val packSize: Int,
    val jan: String?
)
