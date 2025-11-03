package biz.smt_life.android.core.network.fake

import biz.smt_life.android.core.domain.model.User
import biz.smt_life.android.core.domain.repository.ProfileRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeProfileRepository @Inject constructor() : ProfileRepository {

    private var currentUser: User? = null

    override fun currentUser(): Flow<User?> = flow {
        delay(300) // Simulate network delay
        emit(currentUser)
    }

    fun setCurrentUser(user: User?) {
        currentUser = user
    }
}
