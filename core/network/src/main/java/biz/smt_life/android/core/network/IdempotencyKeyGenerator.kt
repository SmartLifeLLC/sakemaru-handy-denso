package biz.smt_life.android.core.network

import java.util.UUID

object IdempotencyKeyGenerator {
    fun generate(): String = UUID.randomUUID().toString()
}
