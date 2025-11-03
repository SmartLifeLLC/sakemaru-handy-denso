package biz.smt_life.android.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Individual item within a picking course
 */
@Serializable
data class OutboundCourseItem(
    val id: String,
    val itemCode: String,
    val itemName: String,
    val customerName: String,
    val capacity: String,
    val packSize: Int,
    val jan: String?,
    val area: String,
    val location: String,
    val orderQtyCase: Int,
    val orderQtyEach: Int,
    val outboundQtyCase: Int = 0,
    val outboundQtyEach: Int = 0,
    val isRegistered: Boolean = false,
    val imageUrl: String? = null
) {
    val hasImage: Boolean
        get() = !imageUrl.isNullOrBlank()
}
