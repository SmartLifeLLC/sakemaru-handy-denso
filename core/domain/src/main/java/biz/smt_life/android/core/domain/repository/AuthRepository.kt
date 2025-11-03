package biz.smt_life.android.core.domain.repository

import biz.smt_life.android.core.domain.model.AuthResult

interface AuthRepository {
    suspend fun login(staffCode: String, password: String): Result<AuthResult>
}
