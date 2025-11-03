package biz.smt_life.android.core.domain.model

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * History entry for outbound items
 */
@Serializable
data class OutboundHistoryEntry(
    val id: String,
    val courseId: String,
    val courseName: String,
    val itemId: String,
    val itemName: String,
    val customerName: String,
    val qtyCase: Int,
    val qtyEach: Int,
    val packSize: Int,
    val jan: String?,
    val area: String,
    val location: String,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant,
    val status: HistoryStatus
)

/**
 * Serializer for Instant (as epoch millis)
 */
object InstantSerializer : kotlinx.serialization.KSerializer<Instant> {
    override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor(
        "Instant",
        kotlinx.serialization.descriptors.PrimitiveKind.LONG
    )

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: Instant) {
        encoder.encodeLong(value.toEpochMilli())
    }

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): Instant {
        return Instant.ofEpochMilli(decoder.decodeLong())
    }
}
