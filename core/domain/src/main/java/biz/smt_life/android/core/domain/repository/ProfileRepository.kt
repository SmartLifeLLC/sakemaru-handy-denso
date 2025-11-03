package biz.smt_life.android.core.domain.repository

import biz.smt_life.android.core.domain.model.User
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun currentUser(): Flow<User?>
}
