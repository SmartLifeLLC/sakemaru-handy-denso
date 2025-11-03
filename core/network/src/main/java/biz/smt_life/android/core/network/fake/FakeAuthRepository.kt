package biz.smt_life.android.core.network.fake

import biz.smt_life.android.core.domain.model.AuthResult
import biz.smt_life.android.core.domain.model.User
import biz.smt_life.android.core.domain.repository.AuthRepository
import biz.smt_life.android.core.network.NetworkException
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeAuthRepository @Inject constructor(
    private val profileRepository: FakeProfileRepository
) : AuthRepository {

    companion object {
        private const val VALID_CODE = "worker01"
        private const val VALID_PASSWORD = "1234"
        private const val LATENCY_MS = 800L
    }

    override suspend fun login(staffCode: String, password: String): Result<AuthResult> {
        delay(LATENCY_MS)

        return if (staffCode == VALID_CODE && password == VALID_PASSWORD) {
            val authResult = AuthResult(
                token = "fake-jwt-token-${System.currentTimeMillis()}",
                pickerId = 1,
                pickerCode = staffCode,
                pickerName = "Warehouse Worker",
                defaultWarehouseId = 991
            )

            // Set current user in profile repository
            profileRepository.setCurrentUser(
                User(
                    id = authResult.pickerId.toString(),
                    name = authResult.pickerName,
                    staffCode = staffCode,
                    role = "operator"
                )
            )

            Result.success(authResult)
        } else {
            Result.failure(NetworkException.Unauthorized("Invalid credentials"))
        }
    }

    override suspend fun logout(): Result<Unit> {
        delay(LATENCY_MS)
        return Result.success(Unit)
    }

    override suspend fun validateSession(): Result<AuthResult> {
        delay(LATENCY_MS)
        // Fake validation - always succeeds if called
        return Result.success(
            AuthResult(
                token = "",
                pickerId = 1,
                pickerCode = "worker01",
                pickerName = "Warehouse Worker",
                defaultWarehouseId = 991
            )
        )
    }
}
