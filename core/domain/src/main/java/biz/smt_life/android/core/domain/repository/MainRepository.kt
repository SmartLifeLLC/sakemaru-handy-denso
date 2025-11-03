package biz.smt_life.android.core.domain.repository

import biz.smt_life.android.core.domain.model.FeedItem
import biz.smt_life.android.core.domain.model.Summary
import kotlinx.coroutines.flow.Flow

interface MainRepository {
    fun summary(): Flow<Summary>
    fun feed(page: Int, pageSize: Int): Flow<List<FeedItem>>
}
