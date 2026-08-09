package biz.smt_life.android.core.network.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IncomingSnapshotResponseTest {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun sampleSnapshotCanBeDecoded() {
        val resource = javaClass.classLoader!!
            .getResource("handy-incoming-snapshot-v2-sample.json")!!
            .readText()

        val envelope = json.decodeFromString(
            ApiEnvelope.serializer(IncomingSnapshotResponse.serializer()),
            resource
        )
        val snapshot = envelope.result!!.data!!

        assertTrue(envelope.isSuccess)
        assertEquals("2026-08-08", snapshot.inspectionDate)
        assertEquals(1, snapshot.schedules.size)
        assertEquals(2, snapshot.items.size)
        assertEquals("4901004201812", snapshot.items.first().searchCodes.first().code)
    }
}
