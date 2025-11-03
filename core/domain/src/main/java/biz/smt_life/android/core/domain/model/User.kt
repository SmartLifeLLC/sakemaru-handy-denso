package biz.smt_life.android.core.domain.model

data class User(
    val id: String,
    val name: String,
    val staffCode: String,
    val role: String = "Operator"
)
