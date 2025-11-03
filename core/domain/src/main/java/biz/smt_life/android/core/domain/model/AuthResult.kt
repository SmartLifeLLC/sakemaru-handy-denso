package biz.smt_life.android.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthResult(
    val token: String,
    val staffId: String,
    val staffName: String,
    val role: String
)
